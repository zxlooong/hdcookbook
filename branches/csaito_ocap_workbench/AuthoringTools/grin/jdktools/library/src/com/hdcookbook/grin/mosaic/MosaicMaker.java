
/*  
 * Copyright (c) 2007, Sun Microsystems, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

package com.hdcookbook.grin.mosaic;

import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.MosaicSpec;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.text.GenericExtensionParser;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.AssetFinder;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.io.DataOutputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Iterator;
import java.util.Comparator;
import javax.imageio.ImageIO;


/**
 * This class has the main logic for making a set of image
 * mosaics for one or more GRIN show files.  An image mosaic is a bunch of
 * little images that are stuck together into one big image, plus
 * data about where each little image was put in the mosaic.  By
 * packaging images as a mosaic, the startup time of an xlet can
 * be dramatically improved, since many image decoders have
 * a substantial fixed latency that doesn't vary much with image size.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
 public class MosaicMaker extends Frame {

    private File outputDir;
    private String[] assetPath;
    private SEShow[] showTrees;

    private ArrayList<ManagedImage> images = new ArrayList<ManagedImage>();
    private Mosaic defaultMosaic = null;
    private HashMap<String, MosaicPart> partsByName 
    		= new HashMap<String, MosaicPart>();
    private HashMap<String, String> imageToMosaic 
    		= new HashMap<String, String>();  // file name to mosaic name
    private HashMap<String, Mosaic> nameToMosaic
                = new HashMap<String, Mosaic>(); // mosaic name to Mosaic
    Graphics2D frameG;
    private Mosaic currentMosaic = null;

    /**
     * Create a mosaic maker
     *
     * @param showTrees	The GRIN shows these mosaics are for
     * @param outputDir Where to write the mosaics
     **/
    public MosaicMaker(SEShow[] showTrees, File outputDir) {
	this.showTrees = showTrees;
	this.outputDir = outputDir;
    }

    public void init() throws IOException {
	ShowBuilder builder = new ShowBuilder();
        builder.setExtensionParser(new GenericExtensionParser());
        
        for (SEShow show: showTrees) {
            
            MosaicSpec[] specs = show.getMosaicSpecs();
            
            for (MosaicSpec spec : specs) {
                String name = spec.name;
               
		for (String imageName : spec.imagesToConsider) {
		    imageToMosaic.put(imageName, name);
                }
		Mosaic m = new Mosaic(spec);
		if (spec.takeAllImages) {
		    defaultMosaic = m;
		}
		addMosaic(name, m);
            }
        }

	setLayout(null);
	setSize(960, 570);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                destroy();
		System.out.println("Window closed; compilation aborted.");
                System.exit(1);
            }
        });
	setVisible(true);
	frameG = (Graphics2D) getGraphics();
	frameG.setComposite(AlphaComposite.Src);
    }

    public void destroy() {
        // close the window
        dispose();
    }

    /**
     * Paint something.  
     **/
    public void paint(Graphics g) {
	g.setColor(Color.black);
	g.fillRect(0, 0, getWidth(), getHeight());
	g.setColor(Color.white);
	Mosaic m = currentMosaic;
	if (m == null) {
	    g.drawString("Making mosaics...", 30, 16);
	} else {
	    m.paintStatus((Graphics2D) g);
	}
    }

    private void addImage(ManagedImage mi) {
	mi.prepare();
	mi.load(this);
	images.add(mi);
	mi.draw(frameG, 0, 30, null);
    }

    private void addAllToMosaics() throws IOException {
	// Sort by maximum dimension, since the maximum dimension of a
	// rectangle constrains the placement of subsequent rectangles
	// more.
	Collections.sort(images, new Comparator<ManagedImage>() {
	    public int compare(ManagedImage m1, ManagedImage m2) {
		int d1 = Math.max(m1.getWidth(), m1.getHeight());
		int d2 = Math.max(m2.getWidth(), m2.getHeight());
		return d2 - d1;
	    }
	});


	for (int i = 0; i < images.size(); i++) {
	    addToMosaic(images.get(i));
	}
    }

    private void addToMosaic(ManagedImage mi) throws IOException {
	String name = mi.getName();
	MosaicPart part = partsByName.get(name);
	if (part != null) {
	    return;
	}
	BufferedImage imAdded = ImageIO.read(AssetFinder.getURL(mi.getName()));
	String special = (String) imageToMosaic.get(mi.getName());
	if (special != null) {
	    imageToMosaic.remove(mi.getName());	  // image has been taken
	    Mosaic m = nameToMosaic.get(special);
	    assert m != null;
	    part = m.putImage(mi, imAdded);
	} else {
	    if (defaultMosaic == null) {
		defaultMosaic = new Mosaic(new MosaicSpec("im0.png"));
		addMosaic("im0.png", defaultMosaic);
	    }
	    part = defaultMosaic.putImage(mi, imAdded);
	}
	assert part != null;
	partsByName.put(name, part);
    }

    private void addMosaic(String name, Mosaic m) throws IOException {
	if (nameToMosaic.get(name) != null) {
	    throw new IOException("Duplicate mosaic \"" + name + "\".");
	}
	nameToMosaic.put(name, m);
    }

    /**
     * The main loop that reads all the shows, adds all the
     * images to mosaics, then writes the mosaics out.
     **/
    public void makeMosaics() throws IOException {
	for (int i = 0; i < showTrees.length; i++) {
	    SEShow show = showTrees[i];
	    show.initialize(this);
	    Feature[] features = show.getFeatures();
	    for (int j = 0; j < features.length; j++) {
		Feature f = features[j];
		if (f instanceof FixedImage) {
		    FixedImage fi = (FixedImage) f;
		    addImage(fi.getImage());
		} else if (f instanceof ImageSequence) {
		    ImageSequence is = (ImageSequence) f;
		    ManagedImage[] ims = is.getImages();
		    for (int k = 0; k < ims.length; k++) {
			if (ims[k] != null) {
			    addImage(ims[k]);
			}
		    }
		}
	    }
	}
	frameG.setColor(Color.black);
	frameG.fillRect(0, 0, getWidth(), getHeight());
	addAllToMosaics();
	LinkedList<Mosaic> mosaics = new LinkedList<Mosaic>();
        for (Map.Entry<String, Mosaic> special : nameToMosaic.entrySet()) {
            Mosaic m = special.getValue();
            if (compile(m)) {
                mosaics.add(m);
            } else {
                System.out.println("Warning:  None of the images in mosaic \"" 
			+ special.getKey()
                        + "\" were used in show.  Discarding empty mosaic.");
            }
        }
	for (Map.Entry<String, String> unused : imageToMosaic.entrySet()) {
	    System.out.println("Warning:  Image \"" +  unused.getKey() 
	    		       + "\" in mosaic \"" + unused.getValue()
			       + "\" was never used in a show.  Discarded.");
	}
	System.out.println(mosaics.size() + " mosaics created.");
	File mapFile = new File(outputDir, "images.map");
            // This file is read by 
            // com.hdcookbook.grin.util.ImageManager.readImageMap()
	DataOutputStream mapOS  = new DataOutputStream(new BufferedOutputStream(
				  new FileOutputStream(mapFile)));
	mapOS.writeInt(mosaics.size());
	int totalPixels = 0;
        Iterator<Mosaic> mit = mosaics.iterator();
	for (int i = 0; mit.hasNext(); i++) {
	    Mosaic m = mit.next();
	    m.setPosition(i);
	    File out = new File(outputDir, m.getOutputName());
	    m.writeMosaicImage(out);
	    totalPixels += m.getWidthUsed() * m.getHeightUsed();
	    System.out.println("    Wrote " + out);
	    mapOS.writeUTF(m.getOutputName());
	}

	mapOS.writeInt(partsByName.size());
	Iterator<MosaicPart> mpit = partsByName.values().iterator();
	while (mpit.hasNext()) {
	    MosaicPart part = mpit.next();
	    mapOS.writeUTF(part.getName());
	    mapOS.writeInt(part.getMosaic().getPosition());
	    Rectangle pl = part.getPlacement();
	    mapOS.writeInt(pl.x);
	    mapOS.writeInt(pl.y);
	    mapOS.writeInt(pl.width);
	    mapOS.writeInt(pl.height);
	}
	mapOS.close();
	System.out.println("Wrote " + mapFile);
	System.out.printf("Mosaics occupy a total of %,d pixels.\n",
			   totalPixels);
    }

    private boolean compile(Mosaic m) {
	currentMosaic = m;
	boolean result = m.compile(this);
	return result;
    }
}

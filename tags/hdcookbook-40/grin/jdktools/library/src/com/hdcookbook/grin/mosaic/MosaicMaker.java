
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
import com.hdcookbook.grin.MosaicHint;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.text.GenericExtensionParser;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.AssetFinder;

import java.awt.AlphaComposite;
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

    private ArrayList images = new ArrayList();
    private LinkedList mosaics = new LinkedList();
    private HashMap partsByName = new HashMap();
    private HashMap mosaicHint = new HashMap();  // file name to mosaic name
    private HashMap specialMosaics = new HashMap(); // mosaic name to Mosaic
    Graphics2D frameG;

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
            
            MosaicHint[] hints = show.getMosaicHints();
            
            for (MosaicHint hint : hints) {
                String name = hint.getName();
                String[] mosaicImages = hint.getImages();
                int width = hint.getWidth();
                int height = hint.getHeight();
                
                for (String imageName : mosaicImages) {
                    mosaicHint.put(imageName, name);
                }
                specialMosaics.put(name, new Mosaic(width, height));                         
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
                System.exit(0);
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
     * Paint a little image.  We don't really show our state
     * here, because our frame is really just a bit of eye candy
     * so you can see what's going on.  The interesting stuff is
     * just blasted to the framebufer using direct draw.
     **/
    public void paint(Graphics g) {
	g.drawString("Making mosaics...", 30, 16);
    }

    private void addImage(ManagedImage mi) {
	mi.prepare(this);
	images.add(mi);
	mi.draw(frameG, 0, 30, null);
    }

    private void addAllToMosaic() throws IOException {
	// Sort by tallest first.  I think this probably helps pack
	// images a little better.
	Collections.sort(images, new Comparator() {
	    public int compare(Object o1, Object o2) {
		int h1 = ((ManagedImage) o1).getHeight();
		int h2 = ((ManagedImage) o2).getHeight();
		return h2 - h1;
	    }
	});


	for (int i = 0; i < images.size(); i++) {
	    addToMosaic((ManagedImage) images.get(i));
	}
    }

    private void addToMosaic(ManagedImage mi) throws IOException {
	String name = mi.getName();
	MosaicPart part = (MosaicPart) partsByName.get(name);
	BufferedImage imAdded = null;
	if (part == null) {
	    imAdded = ImageIO.read(AssetFinder.getURL(mi.getName()));
	}
	boolean already = part != null;
	if (part == null) {
	    String special = (String) mosaicHint.get(mi.getName());
	    if (special != null) {
		Mosaic m = (Mosaic) specialMosaics.get(special);
		assert m != null;
		part = m.putImage(mi, imAdded);  // null if doesn't fit
		if (part == null) {
		    System.out.println("***  Warning:  " + mi.getName()
		    		       + " did not fit in mosaic " + special
				       + ".");
		}
	    }
	}
	for (Iterator it = mosaics.iterator(); part == null && it.hasNext(); ) {
	    Mosaic m = (Mosaic) it.next();
	    part = m.putImage(mi, imAdded);
	}
	if (part == null) {
	    Mosaic m = new Mosaic();
	    part = m.putImage(mi, imAdded);
	    if (part == null) {
		System.out.println();
		System.out.println("Unable to add an image.  Perhaps it's too big?");
		System.out.println("It has a width of " + mi.getWidth() + " and a height of " + mi.getHeight() + ".");
		System.out.println("You might want to make a mosaic_hint for it.");
		System.out.println();
		throw new IOException("Unable to add image " + mi);
	    }
            mosaics.add(m);
	}
	if (imAdded != null) {
	    partsByName.put(name, part);
	    Mosaic m = part.getMosaic();
	    frameG.drawImage(m.getBuffer(), 0, 30, 960, 540, 
			      0, 0, m.getWidth(), m.getHeight(),
                              null);
	    Toolkit.getDefaultToolkit().sync();
	}
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
	addAllToMosaic();
	mosaics.addAll(specialMosaics.values());
	System.out.println(mosaics.size() + " mosaics created.");
        Iterator it = mosaics.iterator();
	File mapFile = new File(outputDir, "images.map");
            // This file is read by 
            // com.hdcookbook.grin.util.ImageManager.readImageMap()
	DataOutputStream mapOS  = new DataOutputStream(new BufferedOutputStream(
				  new FileOutputStream(mapFile)));
	mapOS.writeInt(mosaics.size());
	int totalPixels = 0;
	for (int i = 0; it.hasNext(); i++) {
	    Mosaic m = (Mosaic) it.next();
	    String name = "im" + i + ".png";
	    m.setOutputName(name);
	    m.setPosition(i);
	    File out = new File(outputDir, "im" + i + ".png");
	    m.writeBuffer(out);
	    totalPixels += m.getWidthUsed() * m.getHeightUsed();
	    System.out.println("    Wrote " + out);
	    mapOS.writeUTF(name);
	}

	mapOS.writeInt(partsByName.size());
	it = partsByName.values().iterator();
	while (it.hasNext()) {
	    MosaicPart part = (MosaicPart) it.next();
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
 }

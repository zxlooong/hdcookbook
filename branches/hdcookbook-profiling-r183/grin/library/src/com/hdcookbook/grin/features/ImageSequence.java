
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



package com.hdcookbook.grin.features;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.Debug;


import java.io.IOException;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * An image sequence does "cell" animation.  It consists of a number
 * of images that are displayed one after another.  All of the images
 * in a sequence are assumed to be the same size.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ImageSequence extends Feature {

    private int x;
    private int y;
    private int width = 0;
    private int height = 0;
    private String fileName;
    private String[] middle;
    private String extension;
    private boolean repeat;
    private Command[] endCommands;

    private ManagedImage[] images;
    	// The images in this sequence.  A null image is simply not
	// painted, thus leaving the previous image in place.
    private boolean setupMode = false;
    private boolean imagesSetup = false;
    private Object setupMonitor = new Object();
    private boolean isActivated = false;

    private ImageSequence model;	
    	// We use model to count our frame and for end commands.  If
	// we're our own model, it's set to null.
    private int activeModelCount = 0;	
    	// # of active sequences using us as a model, including ourselves
        // (if we're active).  This tells us how many
	// time nextFrame() will be called per frame
    private int nextFrameCalls = 0;
    	// How many times we've been called without advancing currFrame;
    private int currFrame = 0;	// Frame of our animation

    private ManagedImage lastImage = null;
    private ManagedImage currImage = null;
    private DrawRecord drawRecord = new DrawRecord();

    public ImageSequence(Show show, String name, int x, int y, String fileName,
    			 String[] middle, String extension, boolean repeat,
			 Command[] endCommands) 
    		throws IOException {
	super(show, name);
	this.x = x;
	this.y = y;
	this.fileName = fileName;
	this.middle = middle;
	this.extension = extension;
	this.repeat = repeat;
	this.model = null;
	this.endCommands = endCommands;
    }
    
    public String implGetFileName() {
       return fileName;
    }
    
    public String[] implGetMiddle() {
       return middle;
    }
    
    public String implGetExtension() {
       return extension;
    }
    
    public boolean implGetRepeat() {
       return repeat;
    }
    
    public Command[] implGetEndCommands() {
       return endCommands;
    }
    
    public ImageSequence implGetModel() {
        return model;
    }
    
    public void implSetFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public void implSetMiddle(String[] middle) {
        this.middle = middle;
    }
    
    public void implSetExtension(String extension) {
        this.extension = extension;
    }
    
    public void implSetRepeat(boolean repeat) {
        this.repeat = repeat;
    }
    
    public void implSetEndCommands(Command[] endCommands) {
       this.endCommands = endCommands;
    }
    
    public void implSetModel(ImageSequence model) {
        this.model = model;
    }
    
    public void implSetX(int x) {
        this.x = x;
    }
    
    public void implSetY(int y) {
        this.y = y;
    }
    
    /**
     * Called by the parser and the binary file reader.  Animations can be 
     * linked, so that they
     * progress together, even when one is invisible.  This is done by
     * setting one image sequence's model to be a different image
     * sequence.
     **/
    public void setModel(ImageSequence model) throws IOException {
	this.model = model;
	if (model.middle.length != middle.length) {
	    throw new IOException("Mismatched number of frames in model");
	}
        if (model == this) {
            throw new IOException("Can't set model to self; use null");
        }
    }


    /**
     * @inheritDoc
     **/
    public int getX() {
	return x;
    }

    /**
     * @inheritDoc
     **/
    public int getY() {
	return y;
    }

    /**
     * Get the underlying images in this sequence.  Some of them might be
     * null.
     **/
    public ManagedImage[] getImages() {
	return images;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    public void initialize() {
	images = new ManagedImage[middle.length];
	for (int i = 0; i < middle.length; i++) {
	    if (middle[i] == null) {
		images[i] = null;
	    } else {
		String nm = fileName + middle[i] + extension;
		images[i] = ImageManager.getImage(nm);
	    }
	}
    }

    /**
     * Free any resources held by this feature.  It is the opposite of
     * setup; each call to setup() shall be balanced by
     * a call to unsetup(), and they shall *not* be nested.  
     * <p>
     * It's possible an active phase may be destroyed.  For example,
     * the last phase a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty phase
     * too!).
     **/
    public void destroy() {
	for (int i = 0; i < images.length; i++) {
	    if (images[i] != null) {
		ImageManager.ungetImage(images[i]);
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    protected void setActivateMode(boolean mode) {
	isActivated = mode;
	if (model != null) {
	    if (mode) {
		if (!model.isActivated && model.activeModelCount == 0) {
		    model.currFrame = 0;
		}
		model.activeModelCount++;
	    } else {
		model.activeModelCount--;
	    }
	} else {
	    if (mode) {
                if (activeModelCount == 0) {
                    currFrame = 0;
                }
                activeModelCount++;
	    } else {
                activeModelCount--;
            }
	}
	if (mode) {
	    lastImage = null;
	    currImage = images[getStateHolder().currFrame];
	}
    }

    /**
     * @inheritDoc
     **/
    protected int setSetupMode(boolean mode) {
	synchronized(setupMonitor) {
	    setupMode = mode;
	    if (setupMode) {
		if (prepareAllNow()) {
		    imagesSetup = true;
		    return 0;
		} else {
		    show.setupManager.scheduleSetup(this);
		    return 1;
		}
	    } else if (imagesSetup) {
		for (int i = 0; i < images.length; i++) {
		    if (images[i] != null) {
			images[i].unprepare();
		    }
		}
		imagesSetup = false;
	    }
	    return 0;
	}
    }

    private boolean prepareAllNow() {
	int num;
	for (num = 0; num < images.length; num++) {
	    ManagedImage mi = images[num];
	    if (mi != null) {
		if (!mi.incrementIfPrepared()) {
		    break;
		}
	    }
	}
	if (num == images.length) {
	    return true;
	}
	for (int i = 0; i < num; i++) {
	    ManagedImage mi = images[i];
	    if (mi != null) {
		mi.unprepare();
	    }
	}
	return false;
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
	for (int i = 0; i < images.length; i++) {
	    synchronized(setupMonitor) {
		if (!setupMode) {
		    for (int j = 0; j < i; j++) {
			ManagedImage mi = images[j];
			if (mi != null)  {
			    images[i].unprepare();
			}
		    }
		    return;
		}
	    }
	    ManagedImage mi = images[i];
	    mi.prepare(show.component);
	    int w = mi.getWidth();
	    int h = mi.getHeight();
	    if (w > width) {
		width = w;
	    }
	    if (h > height) {
		height = h;
	    }
	}
	synchronized(setupMonitor) {
	    if (setupMode) {
		imagesSetup = true;
		sendFeatureSetup();
	    } else {
		for (int j = 0; j < images.length; j++) {
		    ManagedImage mi = images[j];
		    if (mi != null)  {
			mi.unprepare();
		    }
		}
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public boolean needsMoreSetup() {
	synchronized (setupMonitor) {
	    return setupMode && !imagesSetup;
	}
    }

    private ImageSequence getStateHolder() {
	if (model == null) {
	    return this;
	} else {
	    return model;
	}
    }

    /**
     * @inheritDoc
     **/
    public void nextFrame() {
	if (Debug.LEVEL > 0 && !isActivated) {
	    Debug.println("\n*** WARNING:  Advancing inactive sequence " 
                          + getName() + "\n");
	}
	if (model != null) {
	    model.nextFrame();
	} else {
	    nextFrameCalls++;
	    if (nextFrameCalls >= activeModelCount) {
		nextFrameCalls = 0;	// We've got them all
		currFrame++;
		if (currFrame == images.length) {
		    if (endCommands != null) {
			for (int i = 0; i < endCommands.length; i++) {
			    show.runCommand(endCommands[i]);
			}
		    }
		    currFrame = 0;
		}
	    }
        }
	currImage = images[getStateHolder().currFrame];
    }


    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderContext context) {
	drawRecord.setArea(x, y, width, height);
	if (currImage != lastImage) {
	    drawRecord.setChanged();
	}
	context.addArea(drawRecord);
	lastImage = currImage;
    }

    /**
     * @inheritDoc
     **/
    public void paintFrame(Graphics2D gr) {
	if (!isActivated) {
	    return;
	}
	doPaint(gr);
    }

    private void doPaint(Graphics2D g) {
	if (currImage != null) {
	    currImage.draw(g, x, y, show.component);
	}
    }

}


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

import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * An image sequence does "cell" animation.  It consists of a number
 * of images that are displayed one after another.  All of the images
 * in a sequence are assumed to be the same size.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ImageSequence extends Feature implements Node {

    protected Rectangle[] placements;	// Same length as fileNames[]
    protected String[] fileNames; 
    protected boolean repeat;
    protected InterpolatedModel scalingModel = null;
    protected Rectangle scaledBounds = null;
    protected Command[] endCommands;

    protected ManagedImage[] images;
    	// The images in this sequence.  A null image will show up as
	// blank, that is, any previous image will be erased.
    private boolean setupMode = false;
    private int imagesSetup = 0;
    private Object setupMonitor = new Object();
    private boolean isActivated = false;

    protected ImageSequence model;	
    	// We use model to count our frame and for end commands.  If
	// we're our own model, it's set to null.
    private int activeModelCount = 0;	
    	// # of active sequences using us as a model, including ourselves
        // (if we're active).  This tells us how many
	// time nextFrame() will be called per frame
    private int nextFrameCalls = 0;
    	// How many times we've been called without advancing currFrame;
    private int currFrame = 0;	// Frame of our animation
    private boolean atEnd;	// At end of animation

    private ManagedImage lastImage = null;
    private ManagedImage currImage = null;
    private Rectangle currPlacement = null;
    private DrawRecord drawRecord = new DrawRecord();

    public ImageSequence(Show show) {
        super(show);
    }
    
    /**
     * @inheritDoc
     **/
    public int getX() {
	return placements[getStateHolder().currFrame].x;
    }

    /**
     * @inheritDoc
     **/
    public int getY() {
	return placements[getStateHolder().currFrame].y;
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
     * <p>
     * It's OK to call this method earlier if needed, e.g. in order to
     * determine image widths.
     **/
    public void initialize() {
	if (images != null) {
	    return;	// Already initialized
	}
        images = new ManagedImage[fileNames.length]; 
        for (int i = 0; i < fileNames.length; i++) { 
            if (fileNames[i] == null) { 
                images[i] = null;
            } else {
                images[i] = ImageManager.getImage(fileNames[i]); 

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
		    model.atEnd = false;
		}
		model.activeModelCount++;
	    } else {
		model.activeModelCount--;
	    }
	} else {
	    if (mode) {
                if (activeModelCount == 0) {
                    currFrame = 0;
		    atEnd = false;
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
    protected void setSetupMode(boolean mode) {
	synchronized(setupMonitor) {
	    setupMode = mode;
	    if (setupMode) {
		show.setupManager.scheduleSetup(this);
	    } else {
		for (int i = 0; i < imagesSetup; i++) {
		    if (images[i] != null) {
			images[i].unprepare();
		    }
		}
		imagesSetup = 0;
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
	ManagedImage im;
	synchronized(setupMonitor) {
	    if (!setupMode) {
		return;
	    }
	    im = images[imagesSetup];
	    while (im == null && (imagesSetup+1) < images.length) {
		imagesSetup++;
		im = images[imagesSetup];
	    }
	}
	if (im != null) {
	    im.prepare(show.component);
	    synchronized(setupMonitor) {
		if (setupMode) {
		    imagesSetup++;
		} else {
		    im.unprepare();
		}
	    }
	}
	if (!needsMoreSetup()) {
	    sendFeatureSetup();
	}
    }

    /**
     * @inheritDoc
     **/
    public boolean needsMoreSetup() {
	synchronized (setupMonitor) {
	    return setupMode && (imagesSetup < images.length);
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
		if (!atEnd) {
		    currFrame++;
		    if (currFrame == images.length) {
			if (endCommands != null) {
			    for (int i = 0; i < endCommands.length; i++) {
				show.runCommand(endCommands[i]);
			    }
			}
			if (repeat)  {
			    currFrame = 0;
			} else {
			    atEnd = true;
			    currFrame--;
			}
		    }
		}
	    }
        }
    }


    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderContext context) {
	int frame = getStateHolder().currFrame;
	currImage = images[frame];
	currPlacement = placements[frame];
	if (scalingModel == null) {
	    drawRecord.setArea(currPlacement.x, currPlacement.y, 
	    		       currPlacement.width, currPlacement.height);
	} else {
	    boolean changed = 
		scalingModel.scaleBounds(currPlacement.x, currPlacement.y, 
					 currPlacement.width, 
					 currPlacement.height, 
					 scaledBounds);
		    // When newly activated, we might get a false positive
		    // on changed, but that's OK because our draw area is
		    // changed anyway.
	    drawRecord.setArea(scaledBounds.x, scaledBounds.y, 
	    		       scaledBounds.width, scaledBounds.height);
	    if (changed) {
		drawRecord.setChanged();
	    }
	}
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
	    if (scalingModel == null) {
		currImage.drawScaled(g, currPlacement, show.component);
	    } else {
		currImage.drawScaled(g, scaledBounds, show.component);
	    }
	}
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException 
    {
        in.readSuperClassData(this);
        this.placements = in.readSharedRectangleArray();
        this.fileNames = in.readStringArray();
        this.repeat = in.readBoolean();
        if (in.readBoolean()) {
            this.model = (ImageSequence) in.readFeatureReference();
        }
        this.endCommands = in.readCommands();       
        if (in.readBoolean()) {
            this.scalingModel = (InterpolatedModel) in.readFeatureReference();
            this.scaledBounds = new Rectangle();
        }
	if (Debug.ASSERT && placements.length != fileNames.length) {
	    Debug.assertFail();
	}
    }
}

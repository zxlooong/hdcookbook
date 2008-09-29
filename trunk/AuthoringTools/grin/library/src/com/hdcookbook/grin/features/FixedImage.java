
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
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.HashMap;

/**
 * Represents a fixed image.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class FixedImage extends Feature implements Node {

    protected Rectangle placement;
    protected String fileName;
    protected InterpolatedModel scalingModel = null;
    protected Rectangle scaledBounds = null;

    protected ManagedImage image;
    private boolean setupMode = false;
    private Object setupMonitor = new Object();
    private boolean imageSetup = false;
    private boolean isActivated = false;
    private DrawRecord drawRecord = new DrawRecord();
    
    public FixedImage(Show show) {
        super(show);
    }

    /**
     * @inheritDoc
     **/
    public Feature makeNewClone(HashMap clones) {
	if (!setupMode || !imageSetup || isActivated) {
	    throw new IllegalStateException();
	}
	FixedImage result = new FixedImage(show);
	result.placement = placement;
	result.fileName = fileName;
	if (scaledBounds != null) {
	    result.scaledBounds = new Rectangle(scaledBounds);
	}
	result.image = ImageManager.getImage(fileName);
		// This increments the reference count of this ManagedImage,
		// which is necessary because when the clone is destroyed,
		// it will decrement that reference count.
	result.imageSetup = true;
	result.setupMode = true;
	return result;
    }

    /**
     * @inheritDoc
     **/
    protected void initializeClone(Feature original, HashMap clones) {
	super.initializeClone(original, clones);
	FixedImage other = (FixedImage) original;
	scalingModel = (InterpolatedModel)
                Feature.clonedReference(other.scalingModel, clones);
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    public void initialize() {
	image = ImageManager.getImage(fileName);
    }

    /**
     * @inheritDoc
     **/
    public int getX() {
	return placement.x;
    }

    /**
     * @inheritDoc
     **/
    public int getY() {
	return placement.y;
    }
    
    /**
     * Get the underlying image that we display.
     **/
    public ManagedImage getImage() {
	return image;
    }
    
    /**
     * Free any resources held by this feature.  It is the opposite of
     * initialize().
     * <p>
     * It's possible an active segment may be destroyed.  For example,
     * the last segment a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty phase
     * too!).
     **/
    public void destroy() {
	ImageManager.ungetImage(image);
    }


    /**
     * @inheritDoc
     **/
    protected void setActivateMode(boolean mode) {
	isActivated = mode;
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
		image.unprepare();
		imageSetup = false;
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
	synchronized(setupMonitor) {
	    if (!setupMode) {
		return;
	    }
	}
	image.prepare(show.component);
	synchronized(setupMonitor) {
	    if (setupMode) {
		imageSetup = true;
	    } else {
		image.unprepare();
	    }
	}
	sendFeatureSetup();
    }

    /**
     * @inheritDoc
     **/
    public boolean needsMoreSetup() {
	synchronized (setupMonitor) {
	    return setupMode && (!imageSetup);
	}
    }

    /**
     * @inheritDoc
     **/
    public void paintFrame(Graphics2D gr) {
	if (!isActivated) {
	    return;
	}
	if (scalingModel == null) {
	    image.drawScaled(gr, placement, show.component);
	} else {
	    image.drawScaled(gr, scaledBounds, show.component);
	}
    }

    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderContext context) {
	if (scalingModel == null) {
	    drawRecord.setArea(placement.x, placement.y, 
	    		       placement.width, placement.height);
	} else {
	    boolean changed 
		= scalingModel.scaleBounds(placement.x, placement.y, 
					   placement.width, placement.height, 
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
	context.addArea(drawRecord);
    }

    /**
     * @inheritDoc
     **/
    public void nextFrame() {
	// do nothing
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.placement = in.readSharedRectangle();
        this.fileName = in.readString();    
        if (in.readBoolean()) {
            this.scalingModel = (InterpolatedModel) in.readFeatureReference();                    
            this.scaledBounds = new Rectangle();
        } 
    }
}


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
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * A Translator wraps other features, and adds movement taken from a
 * Translation to it.  The upper-left hand corner of the subfeature is
 * made to follow the path of the translation.
 *
 * @see Translation
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Translator extends Feature {

    private Translation translation;
    private Feature[] features;
    private boolean isActivated = false;

    private int fx;		// Feature's start position
    private int fy;

    private int currDx;		// delta-x and y for current frame
    private int currDy;

    private int lastDx;		// For last frame shown
    private int lastDy;

	//
	// Here, we make an inner class of RenderContext.  We
	// pass this instance to our child; it modifies calls to the
	// parent RenderContext from our child.
	//
    private ChildContext childContext = new ChildContext();
    
    class ChildContext extends RenderContext {
	RenderContext	parent;
	int dx;
	int dy;

	public void addArea(Rectangle area) {
	    addArea(area.x, area.y, area.width, area.height);
	}

	public void addArea(int x, int y, int width, int height) {
	    parent.addArea(x + dx, y + dy, width, height);
	}

	public void clearAndAddArea(Rectangle area) {
	    clearAndAddArea(area.x, area.y, area.width, area.height);
	}

	public void clearAndAddArea(int x, int y, int width, int height) {
	    parent.clearAndAddArea(x + dx, y + dy, width, height);
	}

	public void guaranteeAreaFilled(Rectangle area) {
	    guaranteeAreaFilled(area.x, area.y, area.width, area.height);
	}

	public void guaranteeAreaFilled(int x, int y, int width, int height) {
	    parent.guaranteeAreaFilled(x+dx, y+dy, width, height);
	}

	public int setTarget(int target) {
	    return parent.setTarget(target);
	}
    };	// End of RenderContext anonymous inner class

    public Translator(Show show, String name) {
	super(show, name);
    }
    
    /**
     * Called from the parser
     **/
    public void setup(Translation translation, Feature[] features) {
	this.translation = translation;
	this.features = features;
	fx = Integer.MAX_VALUE;
	fy = Integer.MAX_VALUE;
	for (int i = 0; i < features.length; i++) {
	    int xi = features[i].getStartX();
	    if (xi < fx) {
		fx = xi;
	    }
	    int yi = features[i].getStartY();
	    if (yi < fy) {
		fy = yi;
	    }
	}
    }

    /**
     * Get our child features
     **/
    public Feature[] getFeatures() {
	return features;
    }

    /**
     * Get the translation that moves us
     **/
    public Translation getTranslation() {
	return translation;
    }

    /**
     * @inheritDoc
     **/
    public int getStartX() {
	return translation.getTranslatorStartX();
    }

    /**
     * @inheritDoc
     **/
    public int getStartY() {
	return translation.getTranslatorStartY();
    }

    /**
     * @inheritDoc
     **/
    public void initialize() {
	// The show will initialize our sub-feature, so we don't
	// need to do anything here.
    }

    /**
     * @inheritDoc
     **/
    public void destroy() {
	// The show will destroy our sub-features, so we don't
	// need to do anything here.
    }

    /**
     * @inheritDoc
     **/
    protected void setActivateMode(boolean mode) {
	// This is synchronized to only occur within model updates.
	isActivated = mode;
	if (mode) {
	    for (int i = 0; i < features.length; i++) {
		features[i].activate();
	    }
	    lastDx = Integer.MIN_VALUE;
	} else {
	    for (int i = 0; i < features.length; i++) {
		features[i].deactivate();
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    protected void setSetupMode(boolean mode) {
	if (mode) {
	    for (int i = 0; i < features.length; i++) {
		features[i].setup();
	    }
	} else {
	    for (int i = 0; i < features.length; i++) {
		features[i].unsetup();
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
	for (int i = 0; i < features.length; i++) {
	    if (features[i].needsMoreSetup()) {
		features[i].doSomeSetup();
		return;
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public boolean needsMoreSetup() {
	for (int i = 0; i < features.length; i++) {
	    if (features[i].needsMoreSetup()) {
		return true;
	    }
	}
	return false;
    }

    /**
     * @inheritDoc
     **/
    public void nextFrame() {
	if (Debug.ASSERT && !translation.getIsActivated()) {
	    Debug.assertFail();
	}
	for (int i = 0; i < features.length; i++) {
	    features[i].nextFrame();
	}
	currDx = translation.getX() - fx;
	currDy = translation.getY() - fy;
    }


    /**
     * @inheritDoc
     **/
    public void addEraseAreas(RenderContext context, boolean srcOver,
    			      boolean envChanged) 
    {
	if (lastDx != Integer.MIN_VALUE) {
	    if (!isActivated || lastDx != currDx || lastDy != currDy) {
		childContext.dx = lastDx;
		childContext.dy = lastDy;
		childContext.parent = context;
		for (int i = 0; i < features.length; i++) {
		    features[i].addEraseAreas(childContext, srcOver, true);
		}
	    }
	}
	childContext.dx = currDx;
	childContext.dy = currDy;
	childContext.parent = context;
	for (int i = 0; i < features.length; i++) {
	    features[i].addEraseAreas(childContext, srcOver, envChanged);
	}
    }

    /**
     * @inheritDoc
     **/
    public void addDrawAreas(RenderContext context, boolean envChanged) {
	if (lastDx != currDx || lastDy != currDy) {
	    envChanged = true;
	}
	childContext.dx = currDx;
	childContext.dy = currDy;
	childContext.parent = context;
	for (int i = 0; i < features.length; i++) {
	    features[i].addDrawAreas(childContext, envChanged);
	}
	lastDx = currDx;
	lastDy = currDy;
    }


    /**
     * See superclass definition.
     **/
    public void paintFrame(Graphics2D gr) {
	if (!isActivated) {
	    return;
	}
	int dx = translation.getX() - fx;
	int dy = translation.getY() - fy;
	gr.translate(dx, dy);
	for (int i = 0; i < features.length; i++) {
	    features[i].paintFrame(gr);
	}
	gr.translate(-dx, -dy);
    }

}
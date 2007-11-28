
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

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Represents a clipped version of another feature.  When painting, a
 * clipping rectangle is set.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Clipped extends Modifier {

    private Rectangle clipRegion;
    private Rectangle lastClipRegion = new Rectangle();

	//
	// Here, we make an inner class of RenderContext.  We
	// pass this instance to our child; it modifies calls to the
	// parent RenderContext from our child.
	//
    private ChildContext childContext = new ChildContext();
    
    class ChildContext extends RenderContext {
	RenderContext	parent;
	private int x;
	private int y;
	private int width;
	private int height;

	public void addArea(Rectangle area) {
	    addArea(area.x, area.y, area.width, area.height);
	}

	public void addArea(int x, int y, int width, int height) {
	    this.x = x;
	    this.y = y;
	    this.width = width;
	    this.height = height;
	    clipArea();
	    if (this.width >= 0 && this.height >= 0) {
		parent.addArea(this.x, this.y, this.width, this.height);
	    }
	}

	public void clearAndAddArea(Rectangle area) {
	    clearAndAddArea(area.x, area.y, area.width, area.height);
	}

	public void clearAndAddArea(int x, int y, int width, int height) {
	    this.x = x;
	    this.y = y;
	    this.width = width;
	    this.height = height;
	    clipArea();
	    if (this.width >= 0 && this.height >= 0) {
		parent.clearAndAddArea(this.x, this.y, this.width, this.height);
	    }
	}

	public void guaranteeAreaFilled(Rectangle area) {
	    guaranteeAreaFilled(area.x, area.y, area.width, area.height);
	}

	public void guaranteeAreaFilled(int x, int y, int width, int height) {
	    this.x = x;
	    this.y = y;
	    this.width = width;
	    this.height = height;
	    clipArea();
	    if (this.width >= 0 && this.height >= 0) {
		parent.guaranteeAreaFilled(this.x, this.y, 
					   this.width, this.height);
	    }
	}

	public int setTarget(int target) {
	    return parent.setTarget(target);
	}

	private void clipArea() {
	    if (x < clipRegion.x) {
		width -= clipRegion.x - x;
		x = clipRegion.x;
	    }
	    if (y < clipRegion.y) {
		height -= clipRegion.y - y;
		y = clipRegion.y;
	    }
	    if (x + width > clipRegion.x + clipRegion.width) {
		width = clipRegion.x + clipRegion.width - x;
	    }
	    if (y + height > clipRegion.y + clipRegion.height) {
		height = clipRegion.y + clipRegion.height - y;
	    }
	}
    };	// End of RenderContext anonymous inner class

    public Clipped(Show show, String name, Rectangle clipRegion) {
	super(show, name);
	this.clipRegion = clipRegion;
    }
    
    public Rectangle getClipRegion() {
        return clipRegion;
    }


    /**
     * @inheritDoc
     **/
    public void addEraseAreas(RenderContext context, boolean srcOver,
    			      boolean envChanged) 
    {
	childContext.parent = context;
	super.addEraseAreas(childContext, srcOver, envChanged);
    }

    /**
     * @inheritDoc
     **/
    public void addDrawAreas(RenderContext context, boolean envChanged) {
	childContext.parent = context;
	super.addDrawAreas(childContext, envChanged);
    }


    /**
     * See superclass definition.
     **/
    public void paintFrame(Graphics2D gr) {
	// This is synchronized by Show.paintFrame, so we don't
	// have to worry about concurrent calls.
	lastClipRegion.x = Integer.MIN_VALUE;
	gr.getClipBounds(lastClipRegion);
	gr.setClip(clipRegion);
	part.paintFrame(gr);
	if (lastClipRegion.x == Integer.MIN_VALUE) {
	    gr.setClip(null);
	} else {
	    gr.setClip(lastClipRegion);
	}
    }
}

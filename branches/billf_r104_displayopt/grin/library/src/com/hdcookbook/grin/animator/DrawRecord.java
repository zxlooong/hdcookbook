
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

package com.hdcookbook.grin.animator;

import java.awt.Rectangle;

/**
 * This class represents a record of drawing to a rectangular area.  It
 * helps in the calculation of an optimized erase/redraw set for a frame
 * of animation, by allowing a convenient means to tracking what was drawn
 * in the previous frame, and what is to be drawn in the current frame.
 *
 * @see RenderContext
 **/

public class DrawRecord {

    private int x;
    private int y;
    private int width;
    private int height;

    private boolean changed;
    private boolean opaque;
    private boolean clipped;

    private int lastX;
    private int lastY;
    private int lastWidth;
    private int lastHeight;

    // RenderContextBase some lists of DrawRecord instances.  At times
    // they're singly linked, and at times doubly-linked.  A DrawRecord
    // instances is never on more than one list at a time.  We maintain these
    // lists "old school" style (not using java.util) for greater speed, and
    // to avoid generating heap traffic during animation.
    DrawRecord prev = null;
    DrawRecord next = null;

    // RenderContextBase keeps track of which target we were sent to
    int target;

    /**
     * Flag this DrawRecord as newly activated.  This means that this
     * DrawRecord wasn't used in the previous frame of animation, and
     * therefore any record of drawing a previous frame is stale, and
     * should be discarded.
     **/
    public void activate() {
	lastWidth = Integer.MIN_VALUE;
    }

    /**
     * Set the area to be drawn to in this frame of animation.  After this
     * is called, the other public methods of this class can be called
     * to indicate the characteristics of this draw operation.
     *
     * @param   x  x coordinate of upper left hand corner of drawing
     * @param   y  y coordinate of upper left hand corner of drawing
     * @param   width width of drawing
     * @param   height height of drawing
     **/
    public void setArea(int x, int y, int width, int height) {
	this.x = x;
	this.y = y;
	this.width = width;
	this.height = height;
	this.changed = false;
	this.opaque = true;
	this.clipped = false;
    }

    /**
     * Flags that some of the pixels in the area to be drawn might
     * be transparent or semi-transparent, either because they're 
     * not drawn to, or because
     * they're drawn to in SrcOver mode.  If this is called, then any
     * redraw of the area will need to be erased, unless there is a
     * guarantee from another DrawRecord that all of the pixels will
     * be filled with source mode drawing.
     **/
    public void setSemiTransparent() {
	opaque = false;
    }

    /**
     * Flags that the contents of the area being drawn to has changed
     * since the last frame.  If this is called, then the area will be
     * drawn; if it isn't, then the area will only be drawn to if that's
     * required by a different DrawRecord.
     **/
    public void setChanged() {
	changed = true;
    }

    /**
     * Indicates that the area is subject to the given translation.
     *
     * @param	dx	Change in x coordinate
     * @param	dy	Change in y coordinate
     **/
    public void applyTranslation(int dx, int dy) {
	this.x += dx;
	this.y += dy;
    }

    /**
     * Applies a clip to the area to be drawn.  This can reduce the area
     * set by setArea().  
     * 
     * @param	x	x coordinate of the clip
     * @param	y	y coordinate of the clip
     * @param	width	width coordinate of the clip
     * @param	height	height coordinate of the clip
     **/
    public void addClip(int x, int y, int width, int height) {
	if (this.x < x) {
	    this.width -= x - this.x;
	    this.x = x;
	}
	if (this.y < y) {
	    this.height -= y - this.y;
	    this.y = y;
	}
	if (this.x + this.width > x + width) {
	    this.width = x + width - this.x;
	}
	if (this.y + this.height > y + height) {
	    this.height = y + height - this.y;
	}

	// This could easily leave width and height negative!
    }


    //
    // Called from RenderContextBase for a DrawRecord used in
    // RenderContextBase.guaranteeAreaFilled.  This reduces the
    // area passed in as argument by any area that completely overlaps
    // with our guarantee area.
    //
    void applyGuarantee(Rectangle area) {
	int x1 = x;
	int y1 = y;
	int x2 = x1 + width;
	int y2 = y1 + height;
	int ax1 = area.x;
	int ay1 = area.y;
	int ax2 = ax1 + area.width;
	int ay2 = ay1 + area.height;

	// First, try moving sides in.  We can only do this if
	// the guaranteed area completely covers the erase area vertically.
	//
	if (y1 <= ay1 && y2 >= ay2) {

	    // Try moving left side to the right
	    if (x1 <= ax1 && x2 > ax1) {
		int d = x2 - ax1;
		area.x += d;
		area.width -= d;
		if (area.width <= 0) {
		    RenderContextBase.setEmpty(area);
		    return;
		}
	    }

	    // Try moving right side to the left
	    if (x2 >= ax2 && x1 < ax2) {
		int d = ax2 - x1;
		area.width -= d;
		if (area.width <= 0) {
		    RenderContextBase.setEmpty(area);
		    return;
		}
	    }
	}

	// Next, try squeezing the top and bottom.  WE can ondly do this
	// if the guaranteed area completely covers the erase area
	// horizontally.
	//
	if (x1 <= ax1 && x2 >= ax2) {

	    // Try moving the top down
	    if (y1 <= ay1 && y2 > ay1) {
		int d = y2 - ay1;
		area.y += d;
		area.height -= d;
		if (area.height <= 0) {
		    RenderContextBase.setEmpty(area);
		    return;
		}
	    }

	    // Try moving the bottom up
	    if (y2 >= ay2 && y1 < ay2) {
		int d = ay2 - y1;
		area.height -= d;
		if (area.height <= 0) {
		    RenderContextBase.setEmpty(area);
		    return;
		}
	    }
	}
    }
    
    //
    // Called from RenderContextBase when an area to be drawn to is
    // added to the context.  This is where we actually add erase areas
    // and draw areas.
    //
    void addAreaTo(Rectangle drawTarget, Rectangle eraseTarget) {

	boolean newCoords = x != lastX || y != lastY
	                    || width != lastWidth || height != lastHeight;

	// If we were visible in the last frame, we need to check
	// for areas to erase.
	//
	if (lastWidth > 0 && lastHeight > 0) {
	    if (newCoords || (changed && !opaque)) {
		// erase the whole last frame
		addToRect(eraseTarget, lastX, lastY, lastWidth, lastHeight);
		addToRect(drawTarget, lastX, lastY, lastWidth, lastHeight);
	    } 
	}

	// Now, if we're visible in this frame, check for areas to
	// draw.
	if (width > 0 && height > 0) {
	    if (lastWidth == Integer.MIN_VALUE   // We were just activated
	        || changed || newCoords)
	    {
		addToRect(drawTarget, x, y, width, height);
	    }
	}

	lastX = x;
	lastY = y;
	lastWidth = width;
	lastHeight = height;
    }

    //
    // Called from RenderContextBase when this DrawRecord was used in the
    // last frame of animation, but isn't used in this frame.
    //
    void eraseLastFrame(Rectangle drawTarget, Rectangle eraseTarget) {
	if (lastWidth > 0 && lastHeight > 0) {
	    addToRect(eraseTarget, lastX, lastY, lastWidth, lastHeight);
	    addToRect(drawTarget, lastX, lastY, lastWidth, lastHeight);
	}
    }

    //
    // Add the given area to the given rectangle.
    //
    private void addToRect(Rectangle r, int x, int y, int width, int height) {
	if (width <= 0 || height <= 0) {
	    return;
	}
	if (RenderContextBase.isEmpty(r)) {
	    r.setBounds(x, y, width, height);
	} else {
	    r.add(x, y);
	    r.add(x+width, y+height);
                // This is correct.  Rectangle.add() (and AWT in general)
		// believes that the lower-right hand coordinate 
		// at x+width, y+height is "outside" of a rectangle, and that
		// adding a coordinate that pushes the lower-right boundary
		// adds the point in question just _outside_ of the rectangle.
		// In other words, this:
		//
		//         Rectangle r = new Rectangle(2, 2, 0, 0);
		//         System.out.println(r.contains(2,2));
		//         System.out.println(r.width + " x " + r.height);
		//         r.add(2, 2);
		//         System.out.println(r.width + " x " + r.height);
		//         r.add(4, 4);
		//         System.out.println(r.width + " x " + r.height);
		//         System.out.println(r.contains(1,1) + ", " 
		//                            + r.contains(2,2) + ", "
		//                            + r.contains(3,3) + ", " 
		//			      + r.contains(4,4));
		// yields this:
		//
		//     false
		//     0 x 0
		//     0 x 0
		//     2 x 2
		//     false, true, true, false
		//
		// Node that after the call to r.add(4, 4), 
		// r.contains(4,4) is false.

	}
    }

}

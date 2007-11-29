
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
 * This class represents a context for tracking the updates that will be
 * needed to render the next frame of animation.  It tracks areas that will
 * need to be displayed, and areas that will need to be erased and then
 * displayed.  The animation framework collapses these areas into an optimized
 * set.
 **/
public class RenderContextBase extends RenderContext {


    private int currTarget; 
    	// The render area target for erasing and drawing

    Rectangle[] drawTargets;
    	// Targets for drawing

    int numDrawTargets;
    	// Number of drawTargets that need to be drawn to in the current
	// frame

    Rectangle[] eraseTargets;
    	// Targets for erasing

    int numEraseTargets;
    	// Number of eraseTargets that need to be drawn in the current
	// frame

    private Rectangle collapsed = new Rectangle(); 
    	// see collapseTargets(Rectangle[])

    RenderContextBase(int numTargets) {
	this.currTarget = 0;
	this.drawTargets = newRectArray(numTargets);
	this.eraseTargets = newRectArray(numTargets);
    }

    private Rectangle[] newRectArray(int n) {
	Rectangle[] r = new Rectangle[n];
	for (int i = 0; i < n; i++) {
	    r[i] = new Rectangle();
	}
	return r;
    }

    /**
     * @inheritDoc
     **/
    public void addArea(Rectangle area) {
	addArea(area.x, area.y, area.width, area.height);
    }

    /**
     * @inheritDoc
     **/
    public void clearAndAddArea(Rectangle area) {
	clearAndAddArea(area.x, area.y, area.width, area.height);
    }

    /**
     * @inheritDoc
     **/
    public void guaranteeAreaFilled(Rectangle area) {
	guaranteeAreaFilled(area.x, area.y, area.width, area.height);
    }

    /**
     * @inheritDoc
     **/
    public void addArea(int x, int y, int width, int height) {
	addAreaTo(drawTargets[currTarget], x, y, width, height);
    }

    /**
     * @inheritDoc
     **/
    public void clearAndAddArea(int x, int y, int width, int height) {
	addAreaTo(drawTargets[currTarget], x, y, width, height);
	addAreaTo(eraseTargets[currTarget], x, y, width, height);
    }

    /**
     * @inheritDoc
     **/
    public void guaranteeAreaFilled(int x1, int y1, int width, int height) {
	Rectangle area = eraseTargets[currTarget];
	if (isEmpty(area)) {
	    return;
	}
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
		    setEmpty(area);
		    return;
		}
	    }

	    // Try moving right side to the left
	    if (x2 >= ax2 && x1 < ax2) {
		int d = ax2 - x1;
		area.width -= d;
		if (area.width <= 0) {
		    setEmpty(area);
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
		    setEmpty(area);
		    return;
		}
	    }

	    // Try moving the bottom up
	    if (y2 >= ay2 && y1 < ay2) {
		int d = ay2 - y1;
		area.height -= d;
		if (area.height <= 0) {
		    setEmpty(area);
		    return;
		}
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public int setTarget(int newTarget) {
	int old = currTarget;
	currTarget = newTarget;
	return old;
    }

    //
    // Set the extent of this render context to empty.  This is done at the
    // beginning of each animation cycle.
    //
    void setEmpty() {
	for (int i = 0; i < drawTargets.length; i++) {
	    drawTargets[i].width = -1;
	    eraseTargets[i].width = -1;
	}
    }

    private void setEmpty(Rectangle r) {
	r.width = -1;
    }

    private boolean isEmpty(Rectangle r) {
	return r.width < 0;
    }

    //
    // Add the given area to the given rectangle.
    //
    private void addAreaTo(Rectangle r, int x, int y, int width, int height) {
	if (width <= 0 || height <= 0) {
	    return;
	}
	if (isEmpty(r)) {
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


    /**
     * Collapse the erase targets and the draw targets to an optimal
     * set.
     **/
    void collapseTargets() {
	numEraseTargets = collapseTargets(eraseTargets);
	numDrawTargets = collapseTargets(drawTargets);
    }

    //
    // Collapse the render areas into an optimal set.  Return the number
    // of targets that need to be drawn; targets[0..n-1]
    // will need to be drawn.  If no paint is needed, n will be 0.
    //
    private int collapseTargets(Rectangle[] targets) {

	int n = targets.length - 1;

		// First, the easy part:  Put all of the empty targets
		// at the end of the list

	while (n >= 0 && isEmpty(targets[n])) {
	    n--;
	}
	// Now, targets[n] is non-empty, or n is -1

	for (int i = 0; i < n; ) {
	    if (isEmpty(targets[i])) {
		Rectangle a = targets[n];
		targets[n] = targets[i];
		targets[i] = a;
		n--;
	    } else {
		i++;
	    }
	}
	// Now, targets[0..n] are non-empty

		// Next, figure out which areas should be collapsed.
		// As a SWAG, we collapse areas when combining them
		// at most doubles the area of the screen drawn to.
		//
		// This is an area where it would be worth measuring what
		// is optimal, and perhaps even using different heuristics
		// based on player.
		//
		// Note that this algorithm is O(n^3) on the number of
		// targets.

    collapse: 
	for (;;) {
	    for (int i = 0; i < n; ) {
		for (int j = i+1; j <= n; j++) {
		    collapsed.setBounds(targets[i]);
		    collapsed.add(targets[j]);
		    int ac = collapsed.width * collapsed.height;
		    int a = targets[i].width * targets[i].height
		    	   + targets[j].width * targets[j].height;
		    if (ac <= 2*a) {
			// combine them
			targets[i].setBounds(collapsed);
			if (j < n) {
			    Rectangle ra = targets[j];
			    targets[j] = targets[n];
			    targets[n] = ra;
			    setEmpty(targets[n]);  
			    	// setEmpty() is not strictly necessary,
				// but it's fast and makes the code a bit
				// more robust.
			}
			n--;
			continue collapse;   // yay goto!
		    }
		}
	    }
	    break collapse;
	}

	// At this point, targets[0..n] represents an optimal set of
	// the areas we need to display.  Add one to get the length of
	// the list of targets.

	return n+1;
    }
}

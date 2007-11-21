
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

import com.hdcookbook.grin.util.Debug;
import java.awt.Rectangle;

/**
 * A RenderArea represents a rectangular area of the screen that
 * will be rendered to in the current frame of animation.  The animation
 * manager provides its animation clients with a set of render areas,
 * so that they can indicate where they plan to draw.  After that's done,
 * it's OK if the RenderArea instances represent overlapping areas.  Before
 * drawing, the animation manager might collapse some of the render areas
 * together, in order to optimize the amount of drawing that has to be
 * done.
 **/
public class RenderArea {

    Rectangle bounds = new Rectangle();		// Bounds used by engine
    private AnimationEngine engine;

    //
    // package-private constructor
    //
    RenderArea(AnimationEngine engine) {
	this.engine = engine;
	setEmpty();
    }

    /**
     * Give the bounds to be drawn for this render area.  The width
     * will be -1 if nothing is to be drawn.
     **/
    public Rectangle getBounds() {
	return bounds;
    }

    //
    // Set the extent of this RenderArea to empty.  This is done at the
    // beginning of each animation cycle.
    //
    void setEmpty() {
	bounds.width = -1;
    }

    //
    // Determine if this render area is empty, that is, if no
    // screen area has been added to it.
    //
    boolean isEmpty() {
	return bounds.width <= 0;
    }

    /**
     * Add the given area to this render area, so that the animation manager
     * will cause this area of the screen to be drawn to.  The given area
     * will <i>not</i> be erased, so the caller must ensure that every
     * pixel within the given area is drawn to.
     * <p>
     * This is equivalent to addArea(area.x, area.y, area.width, area.height)
     **/
    public void addArea(Rectangle area) {
	addArea(area.x, area.y, area.width, area.height);
    }

    /**
     * Add the given area to this render area, so that the animation manager
     * will cause this area of the screen to be drawn to.  The given area
     * will be erased by the animation manager, before any animation client
     * is asked to draw to it.  In this way, it's OK for the animation client
     * to do SrcOver drawing to this area, or drawing that doesn't fill every
     * pixel in the given area.
     * <p>
     * This is equivalent to 
     * clearAndddArea(area.x, area.y, area.width, area.height)
     **/
    public void clearAndAddArea(Rectangle area) {
	clearAndAddArea(area.x, area.y, area.width, area.height);
    }

    /**
     * Add the given area to this render area, so that the animation manager
     * will cause this area of the screen to be drawn to.  The given area
     * will <i>not</i> be erased, so the caller must ensure that every
     * pixel within the given area is drawn to.
     **/
    public void addArea(int x, int y, int width, int height) {
	if (width <= 0 || height <= 0) {
	    return;
	}
	if (isEmpty()) {
	    bounds.setBounds(x, y, width, height);
	} else {
	    bounds.add(x, y);
	    bounds.add(x+width, y+height);
                // This is correct.  Rectangle.add() (and AWT in general)
		// believes that the lower-right hand coordinate is "outside"
		// of a rectangle.
	}
    }

    /**
     * Add the given area to this render area, so that the animation manager
     * will cause this area of the screen to be drawn to.  The given area
     * will be erased by the animation manager, before any animation client
     * is asked to draw to it.  In this way, it's OK for the animation client
     * to do SrcOver drawing to this area, or drawing that doesn't fill every
     * pixel in the given area.
     **/
    public void clearAndAddArea(int x, int y, int width, int height) {
	if (width <= 0 || height <= 0) {
	    return;
	}
	engine.clearArea(x, y, width, height);
	addArea(x, y, width, height);
    }
}

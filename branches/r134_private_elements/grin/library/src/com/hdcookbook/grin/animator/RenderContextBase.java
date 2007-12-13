
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

    private DrawRecord thisFrameList = null;
	// A list of DrawRecord instances used to addArea() in this
	// frame of animation.  It's a singly-linked list kept as a
	// stack (that is, LIFO).

    private DrawRecord guaranteeList = null;
	// A list of DrawRecord instances used in guaranteeAreaFilled.
	// A singly-linked list, kept in order of insertion (that
	// is, FIFO).

    private DrawRecord guaranteeListLast = null;
	// The last record on guaranteeList.

    private DrawRecord lastFrameList = new DrawRecord();
	// A list of the DrawRecord instances used to addArea() in
	// the last frame of animation.  It's kept as a doubly-linked
	// list with a dummy node at the head.  It's in the same order 
	// as thisFrameList, and nodes are taken off of lastFrameList
	// as they are added to thisFrameList.

    RenderContextBase(int numTargets) {
	this.currTarget = 0;
	this.drawTargets = newRectArray(numTargets);
	this.eraseTargets = newRectArray(numTargets);
	lastFrameList.prev = lastFrameList;
	lastFrameList.next = lastFrameList;
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
    public void addArea(DrawRecord r) {
	r.target = currTarget;
	r.addAreaTo(drawTargets[currTarget]);
	// Remove from lastFrameList, if on it.
	if (r.prev != null) {
	    r.prev.next = r.next;
	    r.next.prev = r.prev;
	}
	// Add to this frame's list
	r.next = thisFrameList;
	thisFrameList = r;
	r.prev = null;
    }

    /**
     * @inheritDoc
     **/
    public void guaranteeAreaFilled(DrawRecord filled) {
	filled.target = currTarget;

	// Remove from lastFrameList, if on it.
	if (filled.prev != null) {
	    filled.prev.next = filled.next;
	    filled.next.prev = filled.prev;
	}
	// add to guaranteeList
	if (guaranteeList == null) {
	    guaranteeList = filled;
	} else {
	    guaranteeListLast.next = filled;
	}
	guaranteeListLast = filled;
	filled.next = null;
	filled.prev = null;
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
	    drawTargets[i].width = 0;
	    eraseTargets[i].width = 0;
	}
    }

    //
    // Sets the initial area that needs to be drawn.  This can be called
    // just after setEmpty(), but at no other time.
    //
    void setFullPaint(int x, int y, int width, int height) {
	drawTargets[0].setBounds(x, y, width, height);
    }

    //
    // Process any DrawRecord instances that were used in the previous
    // frame of animation, but that aren't used in this frame.  This is
    // done after AnimationClient.addDisplayAreas()
    //
    void processLastFrameRecords() {
	//
	// First, run through the list in reverse order, which gets us
	// the original order of calls to addArea()
	//
	DrawRecord n = lastFrameList.prev;
	while (n != lastFrameList) {
	    n.eraseLastFrame(drawTargets[n.target]);
	    DrawRecord tmp = n;
	    n = n.prev;
	    tmp.prev = null;	
	    	// Nodes not on a list need to have a prev set to null,
		// since we use prev to know to take a node off lastFrameList
		// in other methods of this class.
	}
	//
	// Now, set lastFrameList to thisFrameList, and set up the
	// backwards links, in preparation for the next frame.
	// thisFrameList becomes empty.
	//
	lastFrameList.next = thisFrameList;  // head is dummy node
	thisFrameList = null;
	n = lastFrameList.next;
	DrawRecord prev = lastFrameList;
	while (n != null) {
	    n.prev = prev;
	    prev = n;
	    n = n.next;
	}
	prev.next = lastFrameList;  // make it circular
	lastFrameList.prev = prev;
    }

    static void setEmpty(Rectangle r) {
	r.width = 0;
	r.height = 0;
    }

    static boolean isEmpty(Rectangle r) {
	return r.width <= 0;
    }


    /**
     * Collapse the erase targets and the draw targets to an optimal
     * set.
     **/
    void collapseTargets() {

    		// First, we try to optimally collapse the targets.
	numDrawTargets = collapseTargets(drawTargets);
    }

    //
    // Collapse the draw areas into an optimal set.  Return the number
    // of targets that need to be drawn; targets[0..n-1]
    // will need to be drawn .  If no drawing is needed, n will
    // be 0.
    //
    private int collapseTargets(Rectangle[] targets) {

	int n = purgeEmpty(targets, targets.length) - 1;

	// Now, targets[0..n] are non-empty

		// Next, figure out which areas should be collapsed.
		// As a SWAG, we collapse areas when combining them
		// at most adds 200x200 pixels to the area of the screen
		// drawn to.
		//
		// This is an area where it would be worth measuring what
		// is optimal, and perhaps even using different heuristics
		// based on player.
		//
		// Note that this algorithm is O(n^3) on the number of
		// targets.

	if (Debug.ASSERT) {
	    for (int i = 0; i < n; i++) {
		if (isEmpty(targets[i])) {
		    Debug.assertFail();
		}
	    }
	}
    collapse: 
	for (;;) {
	    for (int i = 0; i < n; i++) {
		for (int j = i+1; j <= n; j++) {
		    collapsed.setBounds(targets[i]);
		    collapsed.add(targets[j]);
			// We conservatively combine intersecting draw rects
			// here, since it's not OK to draw an area twice
			// in SrcOver mode.
			//
			// This could be a bit more efficient, in the
			// case where the intersection is compeletely
			// contained within one of the rectangles and
			// all on one side of the other.  In this case,
			// instead of collapsing, the other rectangle
			// could be made smaller.
		    boolean combine = targets[i].intersects(targets[j]);
		    if (!combine) {
			int ac = collapsed.width * collapsed.height;
			int a = targets[i].width * targets[i].height
			       + targets[j].width * targets[j].height;

			combine = ac <= a + 200*200;
		    }
		    if (combine) {
			// combine them
			targets[i].setBounds(collapsed);
			if (j < n) {
			    Rectangle ra = targets[j];
			    targets[j] = targets[n];
			    targets[n] = ra;
			}
			setEmpty(targets[n]);  
			    // Not necessary, but fast and adds some robustness
			n--;
			continue collapse;   // yay goto!
		    }
		}
	    }
	    break collapse;
	}

	// At this point, targets[0..n] represents an optimal set of
	// the areas we need to display and erase.  Add one to get the 
	// length of the list of targets.

	return n+1;
    }


    // 
    // Purge the empty targets from the given array considering
    // [0..num-1]
    //
    private int purgeEmpty(Rectangle[] targets, int num) {

	int n = num - 1;

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

	return n+1;
    }

    //
    // Called by the animation engine just after collapsing targets,
    // this process the areas that are guaranteed to be painted, in an
    // effort to minimize erasing.
    //
    void calculateEraseTargets() {
	numEraseTargets = numDrawTargets;
	for (int i = 0; i < numEraseTargets; i++) {
	    eraseTargets[i].setBounds(drawTargets[i]);
	}
	while (guaranteeList != null) {
	    for (int i = 0; i < numEraseTargets; i++) {
		Rectangle area = eraseTargets[i];
		if (!isEmpty(area)) {
		    guaranteeList.applyGuarantee(area);
		}
	    }
	    guaranteeList = guaranteeList.next;
	}
	guaranteeListLast = null;
	numEraseTargets = purgeEmpty(eraseTargets, numEraseTargets);
    }

}

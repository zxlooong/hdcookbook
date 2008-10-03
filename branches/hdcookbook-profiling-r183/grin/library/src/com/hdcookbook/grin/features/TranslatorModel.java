
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
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * A Translation is a feature that moves other features.  A translation
 * needs to be used by a translator.  A translation is a set of
 * screen coordinates, but doesn't have any visual sub-features.
 * A translator takes its coordinates from the translation, and has
 * a visual sub-feature.
 *
 * @see Translator
 *
 * @author Bill Foote (http://jovial.com)
 *
 */
public class TranslatorModel extends Feature {

    private int[] frames;	// Frame number of keyframes, [0] is always 0
    private int[] xs;		// Position of keyframes
    private int[] ys;
    private int repeatFrame;	// Frame to go to after the end

    private boolean isActivated = false;
    private int currFrame;      // Current frame in cycle
    private int currIndex;      // frames[index] <= currFrame < frames[index+1]
    private int repeatIndex;	// Index when currFrame is repeatFrame-1
    private int currX;
    private int currY;
    private boolean isRelative;
    private Command[] endCommands;

    public TranslatorModel(Show show, String name, int[] frames, int[] xs, int[] ys,
    		       int repeatFrame, boolean isRelative,
                       Command[] endCommands) 
    {
	super(show, name);
	this.frames = frames;
	this.xs = xs;
	this.ys = ys;
	this.repeatFrame = repeatFrame;
        this.isRelative = isRelative;        
	this.endCommands = endCommands;
	repeatIndex = 0;
	// This is tricky.  We must calculate the index such
	// that frames[i] <= (repeatFrame - 1) < frames[i+1]
	while (repeatFrame-1 >= frames[repeatIndex + 1]) {
	    repeatIndex++;
	}
	// now repeatFrame-1 < frames[repeatIndex + 1]
	// and repeatFrame-1 >= frames[repeatIndex]
	
	currX = xs[0];
	currY = ys[0];
    }

    public int[] implGetFrames() {
        return frames;
    }
    
    public int[] implGetXs() {
        return xs;
    }
    
    public int[] implGetYs() {
        return ys;
    }
    
    public int implGetRepeatFrame() {
        return repeatFrame;
    }
    
    /**
     * Returns true if this translation model uses relative x y coordinates
     * from the parent node, false otherwise.
     */
    public boolean getIsRelative() {
        return isRelative;
    }
        
    public void implSetFrames(int[] frames) {
        this.frames = frames;
    }
    
    public void implSetXs(int[] xs) {
        this.xs = xs;
    }
    
    public void implSetYs(int[] ys) {
        this.ys = ys;
    }
    
    public void implSetRepeatFrame(int repeatFrame) {
        this.repeatFrame = repeatFrame;
    }
    
    public void implSetIsRelative(boolean isRelative) {
        this.isRelative = isRelative;
    }    
    
    final int getCurrX() {
	return currX;
    }

    final int getCurrY() {
	return currY;
    }

    /**
     * @inheritDoc
     * <p>
     * Since a TranslatorModel is invisible, this returns a garbage value 
     * (Integer.MAX_VALUE)
     **/
    public int getX() {
	return Integer.MAX_VALUE;
    }

    /**
     * @inheritDoc
     * <p>
     * Since a TranslatorModel is invisible, this returns a garbage value 
     * (Integer.MAX_VALUE)
     **/
    public int getY() {
	return Integer.MAX_VALUE;
    }

    /**
     * Return the list of commands that are executed at the end
     * of doing our translation.
     **/
    public Command[] getEndCommands() {
	return endCommands;
    }

    final boolean getIsActivated() {
	return isActivated;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    public void initialize() {
    }

    /**
     * @inheritDoc
     **/
    public void destroy() {
    }


    //
    // This is synchronized to only occur within model updates.
    //
    protected void setActivateMode(boolean mode) {
	isActivated = mode;
	if (mode) {
	    currFrame = 0;
	    currIndex = 0;
	    currX = xs[0];
	    currY = ys[0];
	}
    }

    protected void setSetupMode(boolean mode) {
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
    }

    /**
     * @inheritDoc
     **/
    public boolean needsMoreSetup() {
	return false;
    }

    /**
     * @inheritDoc
     **/
    public void nextFrame() {
	if (Debug.ASSERT && !isActivated) {
	    Debug.assertFail("Translation " + getName() + " not activated");
	}
	currFrame++;
	int nextIndex  = currIndex + 1;
	int dist = frames[nextIndex] - frames[currIndex];
	int distNext = frames[nextIndex] - currFrame;
	int distLast = currFrame - frames[currIndex];
	if (Debug.ASSERT && (distNext < 0 || distLast < 0)) {
	    Debug.assertFail();
	}
	currX = (xs[nextIndex] * distLast + xs[currIndex] * distNext) /dist;
	currY = (ys[nextIndex] * distLast + ys[currIndex] * distNext) /dist;
	if (distNext <= 0) {
	    currIndex = nextIndex;
	    if (currIndex+1 >= frames.length) {
		currFrame = repeatFrame - 1;
		currIndex = repeatIndex;
		for (int i = 0; i < endCommands.length; i++) {
		    show.runCommand(endCommands[i]);
		}
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderContext context) {
    }

    /**
     * @inheritDoc
     **/
    public void paintFrame(Graphics2D gr) {
    }
}

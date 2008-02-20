
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
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;

/**
 * An InterpolatedModel is a feature that controls one or more integer
 * factors, and interpolates their values according to keyframes.  This
 * is used by other features to animate the parameters in queestion.  At
 * the end of the sequence, the InterpolatedModel can kick off a list of
 * commands, it can repeat the animation, or it can stick at the last frame.
 * <p>
 * An InterpolatedModel with no values can function as a timer.  A timer
 * simply has a number of keyframes, and triggers a set of commands after
 * those keyframes.
 *
 * @see Translator
 *
 * @author Bill Foote (http://jovial.com)
 *
 */
public class InterpolatedModel extends Feature implements Node {

    /**
     * For a scale_model, the field for the X value 
     **/
    public final static int SCALE_X_FIELD = 0;

    /**
     * For a scale_model, the field for the Y value 
     **/
    public final static int SCALE_Y_FIELD = 1;

    /**
     * For a scale_model, the field for the horizontal scale factor in 
     * mills (1/1000)
     **/
    public final static int SCALE_X_FACTOR_FIELD = 2;

    /**
     * For a scale_model, the field for the horizontal scale factor in 
     * mills (1/1000)
     **/
    public final static int SCALE_Y_FACTOR_FIELD = 3;

    protected int[] frames;	// Frame number of keyframes, [0] is always 0
    protected int[] currValues;	// Current value of each field
    protected int[][] values;	// Values at keyframe, indexed by field.
    				// The array for a field can be null, in which
				// case the initial value of currValues will be
				// maintained.
    protected int repeatFrame;	// Frame to go to after the end.  
    				// Integer.MAX_VALUE means "stick at end"
				// 0 will cause a cycle.

    private boolean isActivated = false;
    private int currFrame;      // Current frame in cycle
    private int currIndex;      // frames[index] <= currFrame < frames[index+1]
    private int repeatIndex;	// Index when currFrame is repeatFrame-1
    protected Command[] endCommands;

    public InterpolatedModel(Show show) {
        super(show);
    }
 
    /**
     * Give the current value for the given field
     *
     * @param	fieldNum 	The field number, counting from 0
     **/
    public final int getCurrValue(int fieldNum) {
	return currValues[fieldNum];
    }

    /**
     * @inheritDoc
     * <p>
     * Since an InterpolatedModel is invisible, this returns a very large value
     * (Integer.MAX_VALUE)
     **/
    public int getX() {
	return Integer.MAX_VALUE;
    }

    /**
     * @inheritDoc
     * <p>
     * Since an InterpolatedModel is invisible, this returns a very large
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
 	if (repeatFrame == Integer.MAX_VALUE) {
	    repeatIndex = Integer.MAX_VALUE;
	}  else {
	    repeatIndex = 0;
	    // This is tricky.  We must calculate the index such
	    // that frames[i] <= (repeatFrame - 1) < frames[i+1]
	    while (repeatFrame-1 >= frames[repeatIndex + 1]) {
		repeatIndex++;
	    }
	    // now repeatFrame-1 < frames[repeatIndex + 1]
	    // and repeatFrame-1 >= frames[repeatIndex]
	}       
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
	    if (frames.length <= 1) {
		currFrame = Integer.MAX_VALUE;
		currIndex = Integer.MAX_VALUE;
	    } else {
		currFrame = 0;
		currIndex = 0;
		for (int i = 0; i < currValues.length; i++) {
		    if (values[i] != null) {
			currValues[i] = values[i][0];
		    }
		}
	    }
	}
    }

    protected void setSetupMode(boolean mode) {
	// do nothing
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
	// do nothing
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
	    Debug.assertFail("InterpolatedModel " + getName()+" not activated");
	}
	if (currFrame == Integer.MAX_VALUE) {
	    return;
	}
	currFrame++;
	int nextIndex  = currIndex + 1;
	int dist = frames[nextIndex] - frames[currIndex];
	int distNext = frames[nextIndex] - currFrame;
	int distLast = currFrame - frames[currIndex];
	if (Debug.ASSERT && (distNext < 0 || distLast < 0)) {
	    Debug.assertFail();
	}
	for (int i = 0; i < currValues.length; i++) {
	    int[] vs = values[i];
            
	    if (vs != null) {
		currValues[i] = (vs[nextIndex] * distLast 
			          + vs[currIndex] * distNext) /dist;
	    }
	}
	if (distNext <= 0) {
	    currIndex = nextIndex;
	    if (currIndex+1 >= frames.length) {
		currFrame = repeatFrame;
		if (currFrame  != Integer.MAX_VALUE) {
		    currFrame--;
		}
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
	// do nothing
    }

    /**
     * @inheritDoc
     **/
    public void paintFrame(Graphics2D gr) {
	// do nothing
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException { 
                
        in.readSuperClassData(this);
        
        this.frames = in.readSharedIntArray();
        this.currValues = in.readSharedIntArray();
	this.values = new int[currValues.length][];
	for (int i = 0; i < values.length; i++) {
	    values[i] = in.readSharedIntArray();
	}
        this.repeatFrame = in.readInt();
        this.endCommands = in.readCommands();       
    }

    /**
     * Scale the x, y, width and heigh values according to the current
     * values of SCALE_X_FIELD, SCALE_Y_FIELD, SCALE_X_FACTOR_FIELD
     * and SCALE_Y_FACTOR_FIELD, storing them in scaledBounds.  This,
     * of course, depends on this InterpolatedModel having these four
     * values in it, which means that this is a scaling_model.
     *
     * @return true if the value of scaledBounds have changed, false
     *			otherwise.
     **/
    public boolean scaleBounds(int x, int y, int width, int height,
    			       Rectangle scaledBounds)
    {
	int dx = getCurrValue(SCALE_X_FIELD);
	int dy = getCurrValue(SCALE_Y_FIELD);
	int xf = getCurrValue(SCALE_X_FACTOR_FIELD);
	int yf = getCurrValue(SCALE_Y_FACTOR_FIELD);

	x = (x - dx) * xf;
	if (x < 0) {
	    x -= 500;
	} else {
	    x += 500;
	}
	x = (x / 1000) + dx;

	y = (y - dy) * yf;
	if (y < 0) {
	    y -= 500;
	} else {
	    y += 500;
	}
	y = (y / 1000) + dy;

	width *= xf;
	if (width < 0) {
	    width -= 500;
	} else {
	    width += 500;
	}
	width /= 1000;

	height *= yf;
	if (height < 0) {
	    height -= 500;
	} else {
	    height += 500;
	}
	height /= 1000;

	if (x != scaledBounds.x || y != scaledBounds.y
	    || width != scaledBounds.width || height != scaledBounds.height)
	{
	    scaledBounds.x = x;
	    scaledBounds.y = y;
	    scaledBounds.width = width;
	    scaledBounds.height = height;
	    return true;
	} else {
	    return false;
	}
    }
}


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
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Timer extends Feature {
    
    private Command[] endCommands;
    private int numFrames;	// # of frames
    private boolean repeat;

    private boolean isActivated = false;

    private int startAnimationFrame;	// First frame we animate
    private boolean triggered;

    public Timer(Show show, String name, int numFrames, boolean repeat,
                 Command[] endCommands)  
    {
	super(show, name);
	this.numFrames = numFrames;
	this.repeat = repeat;
	this.endCommands = endCommands;
    }

    public Command[] getEndCommands() {
	return endCommands;
    }

    public int getStartX() {
	// This only matters to translate in determining upper-left corner
	return Integer.MAX_VALUE;
    }

    public int getStartY() {
	// This only matters to translate in determining upper-left corner
	return Integer.MAX_VALUE;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    public void initialize() {
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
    }

    protected void setActivateMode(boolean mode) {
	isActivated = mode;
	if (mode) {
	    startAnimationFrame = show.getCurrentFrame();
	    triggered = false;
	}
    }

    protected void setSetupMode(boolean mode) {
    }

    public void doSomeSetup() {
    }

    public boolean needsMoreSetup() {
        return false;
    }

    public void advanceToFrame(int newFrame) {
	if (Debug.ASSERT && !isActivated) {
	    Debug.assertFail("Advancing inactive sequence");
	}
        int curr = newFrame - startAnimationFrame;
        if (curr > numFrames && !triggered) {
	    if (repeat) {
		// Don't set triggered
		while (curr > numFrames) {
		    startAnimationFrame += numFrames;
		    curr = newFrame - startAnimationFrame;
		}
	    } else {
		triggered = true;
	    }
            for (int i = 0; i < endCommands.length; i++) {
                show.runCommand(endCommands[i]);
            }
        }
    }

    public void  addDisplayArea(Rectangle area) {
    }

    public void paintFrame(Graphics2D gr) {
    }

}


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
import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.AlphaComposite;

/**
 * Modifies a child feature by applying an alpha value when drawing in
 * it.  This lets you animate a fade-in and fade-out effect.  It works
 * by specifying alpha values at a few keyframes, and doing linear
 * interpolation between those keyframes.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Fade extends Modifier {

    private AlphaComposite[] alphas;
    private int[] keyframes;
    private int[] keyAlphas;
    private boolean srcOver;
    private boolean isActivated = false;
    private int startAnimationFrame;
    private int alphaIndex;
    private Command[] endCommands;

    public Fade(Show show, String name, boolean srcOver, 
    		int[] keyframes, int[] keyAlphas, Command[] endCommands) 
    {
	super(show, name);
	this.endCommands = endCommands;
        this.keyframes = keyframes;
        this.keyAlphas = keyAlphas;
        this.srcOver = srcOver;
    }
    
    /* 
     * Internal use only 
     */
    public int[] getKeyframes() {
       return keyframes;
    }
    
    /* 
     * Internal use only 
     */
    public int[] getKeyAlphas() {
       return keyAlphas;
    }
    
    /* 
     * Internal use only 
     */
    public boolean getSrcOver() {
       return srcOver;
    }
    
    /* 
     * Internal use only 
     */    
    public Command[] getEndCommands() {
       return endCommands;
    }
    
    /**
     * See superclass definition.
     **/
    public void initialize() {
	if (keyframes.length == 1) {
	    AlphaComposite ac = show.initializer.getAlpha(srcOver,keyAlphas[0]);
	    alphas = new AlphaComposite[] { ac };
	} else {
	    alphas = new AlphaComposite[keyframes[keyframes.length-1]+1];
	    int i = 0;		// keyframes[i] <= f < keyframes[i+1]
	    for (int f = 0; f < alphas.length; f++) {
		// Restore invariant on i
		while ((i+1) < keyframes.length && f >= keyframes[i+1]) {
		    i++;
		}
		int alpha;
		if (f == keyframes[i]) {
		    alpha = keyAlphas[i];
		} else {
		    int dist = keyframes[i+1] - keyframes[i];
		    int distNext = keyframes[i+1] - f;
		    int distLast = f - keyframes[i];
		    if (Debug.ASSERT && (distNext < 0 || distLast < 0)) {
			Debug.assertFail();
		    }
		    alpha = (keyAlphas[i+1]*distLast + keyAlphas[i]*distNext + dist/2) / dist;
		}
		alphas[f] = show.initializer.getAlpha(srcOver, alpha);
	    }
	}
    }

    /**
     * See superclass definition.
     **/
    protected void setActivateMode(boolean mode) {
	super.setActivateMode(mode);
	if (mode) {
	    startAnimationFrame = show.getCurrentFrame();
	    alphaIndex = 0;
	}
    }

    /**
     * See superclass definition.
     **/
    public void advanceToFrame(int newFrame) {
	super.advanceToFrame(newFrame);
	alphaIndex = newFrame - startAnimationFrame;
	if (alphaIndex == alphas.length) {
	    for (int i = 0; i < endCommands.length; i++) {
		show.runCommand(endCommands[i]);
	    }
	}
    }

    /**
     * See superclass definition.
     **/
    public void paintFrame(Graphics2D gr) {
	if (alphaIndex < alphas.length) {
	    Composite old = gr.getComposite();
	    gr.setComposite(alphas[alphaIndex]);
	    part.paintFrame(gr);
	    gr.setComposite(old);
	} else {
	    part.paintFrame(gr);
	}
    }
}

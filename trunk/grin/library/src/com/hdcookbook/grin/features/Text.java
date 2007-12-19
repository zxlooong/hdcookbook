
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
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;


/**
 * Display text.  LIke all features, the upper-left hand corner of
 * the visible text is given.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Text extends Feature {
   
    private int x;
    private int y;
    private String[] strings;
    private int vspace;
    private Font font;
    private Color[] colors;
    private Color currColor = null;
    private Color lastColor = null;
    private Color background;

    private boolean isActivated;
    private int ascent;
    private int descent;
    private int width = -1;
    private int height = -1;
    private int colorIndex;		// index into colors

    private boolean changed = false;
    private DrawRecord drawRecord = new DrawRecord();

    public Text(Show show, String name, int x, int y, String[] strings, 
    		int vspace, Font font, Color[] colors, Color background) 
    {
	super(show, name);
	this.x = x;
	this.y = y;
	this.strings = strings;
        this.vspace = vspace;
	this.font = font;
	this.colors = colors;
	this.background = background;
    }
    
    public String[] getStrings() {
        return strings;
    }
    
    public int getVspace() {
        return vspace;
    }
    
    public Font getFont() {
        return font;
    }
    
    public Color[] getColors() {
        return colors;
    }
    
    public Color getBackground() {
        return background;
    }
   
    /**
     * @inheritDoc
     **/
    public int getX() {
	return x;
    }

    /**
     * @inheritDoc
     **/
    public int getY() {
	return y;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    // This is also called from setText
    public void initialize() {
	changed = true;
	FontMetrics fm = show.component.getFontMetrics(font);
        width = 0;
        for (int i = 0; i < strings.length; i++) {
            int w = fm.stringWidth(strings[i]);
            if (w > width) {
                width = w;
            }
        }
	ascent = fm.getMaxAscent();
	descent = fm.getMaxDescent();
        height = vspace * (strings.length - 1)
		 + strings.length * (ascent + descent + 1);
    }

    /**
     * Get the text that's being displayed.
     **/
    public String[] getText() {
	return strings;
    }

    /** 
     * Change the text to display.
     * This should only be called with the show lock held, at an
     * appropriate time in the frame pump loop.  A good time to call
     * this is from within a command.
     **/
    public void setText(String[] newText) {
	synchronized(show) {	// Shouldn't be necessary, but doesn't hurt
	    strings = newText;
	    initialize();
	}
    }

    public void destroy() {
    }

    /**
     * @inheritDoc
     **/
    protected void setActivateMode(boolean mode) {
	// This is synchronized to only occur within model updates.
	isActivated = mode;
	if (mode) {
	    colorIndex = 0;
	    lastColor = null;
	    currColor = colors[colorIndex];
	    drawRecord.activate();
	}
    }

    /**
     * @inheritDoc
     **/
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
	colorIndex++;
	if (colorIndex >= colors.length) {
	    colorIndex = colors.length - 1;
	}
	currColor = colors[colorIndex];
    }


    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderContext context) {
	drawRecord.setArea(x, y, width, height);
	if (lastColor != currColor || changed) {
	    drawRecord.setChanged();
	}
	drawRecord.setSemiTransparent();
	context.addArea(drawRecord);
	lastColor = currColor;
	changed = false;
    }

    /**
     * @inheritDoc
     **/
    public void paintFrame(Graphics2D gr) {
	if (!isActivated) {
	    return;
	}
	if (background != null) {
	    gr.setColor(background);
	    gr.fillRect(x, y, width, height);
	}
	gr.setFont(font);
	gr.setColor(currColor);
        int y2 = y + ascent;
        for (int i = 0; i < strings.length; i++) {
            gr.drawString(strings[i], x, y2);
            y2 += ascent + descent + vspace;
        }
    }
}

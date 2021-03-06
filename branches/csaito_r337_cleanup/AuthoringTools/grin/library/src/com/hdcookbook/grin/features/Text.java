
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
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;

import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import java.awt.Graphics2D;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Color;
import java.io.IOException;


/**
 * Display text.  Like all features, the upper-left hand corner of
 * the visible text is given.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Text extends Feature implements Node {
 
    /**
     * Value for alignment indicating that x refers to the left side
     * of the text.
     **/
    public final static int LEFT = 0x01;

    /**
     * Value for alignment indicating that x refers to the middle
     * of the text.
     **/
    public final static int MIDDLE = 0x02;

    /**
     * Value for alignment indicating that x refers to the right side
     * of the text.
     **/
    public final static int RIGHT = 0x03;

    /**
     * Value for alignment indicating that y refers to the top side
     * of the text.
     **/
    public final static int TOP = 0x04;

    /**
     * Value for alignment indicating that y refers to the baseline
     * of the text.
     **/
    public final static int BASELINE = 0x08;

    /**
     * Value for alignment indicating that y refers to the baseline
     * of the text.
     **/
    public final static int BOTTOM = 0x0c;

    /**
     * The alignment to apply to x and y.  The value is obtained by or-ing
     * (or by adding) a horizontal value (LEFT, MIDDLE or RIGHT) with
     * a vertical value (TOP, BASELINE or BOTTOM).
     **/
    protected int alignment;

    protected int xArg;
    protected int yArg;
    protected String[] strings;
    protected int vspace;
    protected Font font;
    protected Color[] colors;
    private Color currColor = null;
    private Color lastColor = null;
    protected int loopCount;	
    	// # of times to repeat images before sending end commands
	// Integer.MAX_VALUE means "infinite"
    protected Color background;

    private boolean isActivated;
    private int alignedX;
    private int alignedY;
    private int ascent;
    private int descent;
    private int width = -1;
    private int height = -1;
    private int colorIndex;		// index into colors
    private int loopsRemaining;	// see loopCount

    private boolean changed = false;
    private DrawRecord drawRecord = new DrawRecord();
    
    public Text(Show show) {
        super(show);
    }
    
    /**
     * @inheritDoc
     **/
    public int getX() {
	return alignedX;
    }

    /**
     * @inheritDoc
     **/
    public int getY() {
	return alignedY;
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
	int a = (alignment & 0x03);
	if (a == MIDDLE) {
	    alignedX = xArg - (width / 2);
	} else if (a == RIGHT) {
	    alignedX = xArg - width;
	} else {
	    alignedX = xArg;
	}
	a = (alignment & 0x0c);
	if (a == BASELINE) {
	    alignedY = yArg - ascent;
	} else if (a == BOTTOM) {
	    alignedY = yArg - height;
	} else {
	    alignedY = yArg;
	}
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
     * <p>
     * A good way to write this command that calls this is by using
     * the java_command structure.  There's an example of this in the
     * cookbook menu.
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
	    loopsRemaining = loopCount;
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
	    if (loopCount != Integer.MAX_VALUE) {
		loopsRemaining--;
	    }
	    if (loopsRemaining > 0) {
		colorIndex = 0;
	    } else {
		loopsRemaining = loopCount;
		colorIndex = colors.length - 1;
	    }
	}
	currColor = colors[colorIndex];
    }


    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderContext context) {
	drawRecord.setArea(alignedX, alignedY, width, height);
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
	    gr.fillRect(alignedX, alignedY, width, height);
	}
	gr.setFont(font);
	gr.setColor(currColor);
        int y2 = alignedY + ascent;
        for (int i = 0; i < strings.length; i++) {
            gr.drawString(strings[i], alignedX, y2);
            y2 += ascent + descent + vspace;
        }
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
                
        in.readSuperClassData(this);
        this.xArg = in.readInt();
        this.yArg = in.readInt();
	this.alignment = in.readInt();
        this.strings = in.readStringArray();
        this.vspace = in.readInt();
        this.font = in.readFont();
        this.colors = new Color[in.readInt()];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = in.readColor();
        }
	loopCount = in.readInt();
        this.background = in.readColor();        
    }
}

/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

package com.hdcookbook.grin.fontstrip;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.SetupClient;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;

public class FontStripText extends Feature implements Node, SetupClient {
    
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
    protected String   fontImageFileName;
    
    protected int hspace;
    protected int vspace;
    protected Color background;

    private boolean isActivated = false;
    private int alignedX;
    private int alignedY;
    private int ascent;
    private int descent;
    private int leading;
    private int width = -1;
    private int height = -1;
    private boolean changed = false;
    private int maxAscent   = 0;
    private int maxDescent  = 0;
    private DrawRecord drawRecord = new DrawRecord();
    private Object     setupMonitor = new Object();
    private boolean    setupMode    = false;    
    private boolean imageSetup = false;

    private ManagedImage fontImage = null;
    private HashMap      charMap   = null;   
    
    private static boolean loadingFailed = false;
    
    public FontStripText(Show show) {
        super(show);
    }
    
    public int getX() {
        return alignedX;
    }

    public int getY() {
        return alignedY;
    }

    public void initialize() {
        if (!FontImageFileInfo.initialized) {
            try {
                FontImageFileInfo.initFontImageFileInfo("fontstrip.inf");
            } catch (IOException e) {
                e.printStackTrace();
                loadingFailed = true;
            }
        }
        
        if (loadingFailed) {
            return;
        }
     
        fontImage = FontImageFileInfo.getImageFile(fontImageFileName);
        if (fontImage == null) {
            fontImage = ImageManager.getImage(fontImageFileName);
        }
        
	charMap = FontImageFileInfo.getCharMap(fontImageFileName);
        if (charMap == null) {
            System.err.println("ERROR: entry for " + fontImageFileName + " not found in the info file.");
            loadingFailed = true;
            return;
        }        
 
	changed = true;
        
        // We know the charactor size without loading the actual font image,
        // since the size is recorded in the fontstrip info size.
        width = 0;
        for (int i = 0; i < strings.length; i++) {
            int w = getStringWidth(strings[i]);
            if (w > width) {
                width = w;
            }
        }
	ascent = this.maxAscent;
	descent = this.maxDescent;
        leading = FontImageFileInfo.getMaxLeading(fontImageFileName);
        height = (vspace + leading) * (strings.length - 1)
		 + (strings.length * (ascent + descent + 1));
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

    public void destroy() {
	ImageManager.ungetImage(fontImage);
    }

    /**
     * @inheritDoc
     **/
    protected int setSetupMode(boolean mode) {
        if (loadingFailed) {
            return 0;
        }
        
	synchronized(setupMonitor) {
	    setupMode = mode;
	    if (setupMode) {
		fontImage.prepare();
		if (fontImage.isLoaded()) {
		    imageSetup = true;
		    return 0;
		} else {
		    show.setupManager.scheduleSetup(this);
		    return 1;
		}
	    } else {
		fontImage.unprepare();
		imageSetup = false;
		return 0;
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
	synchronized(setupMonitor) {
	    if (!setupMode) {
		return;
	    }
	}
	fontImage.load(show.component);
	synchronized(setupMonitor) {
	    if (!setupMode) {
		return;
	    }
	    imageSetup = true;
	}
	sendFeatureSetup();
    }

    /**
     * @inheritDoc
     **/
    public boolean needsMoreSetup() {
	synchronized (setupMonitor) {
	    return setupMode && (!imageSetup);
	}
    }
    protected void setActivateMode(boolean mode) {
        this.isActivated = mode;
    }

    public void addDisplayAreas(RenderContext context) {
	drawRecord.setArea(alignedX, alignedY, width, height);
	if (changed) {
	    drawRecord.setChanged();
	}
	drawRecord.setSemiTransparent();
	context.addArea(drawRecord);
	changed = false;
    }

    public void paintFrame(Graphics2D gr) {
	if (!isActivated || loadingFailed) {
	    return;
	}
	if (background != null) {
	    gr.setColor(background);
	    gr.fillRect(alignedX, alignedY, width, height);
	}
        int y2 = alignedY;

	Composite old = gr.getComposite();
	gr.setComposite(AlphaComposite.SrcOver);        
        for (int i = 0; i < strings.length; i++) {
            drawString(gr, strings[i], alignedX, y2);
            y2 += ascent + descent + leading + vspace;
        }
	gr.setComposite(old);            
    }

    public void nextFrame() {
        // nothing to do.
    }

    public void markDisplayAreasChanged() {
	drawRecord.setChanged();
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException {
        in.readSuperClassData(this);
        this.xArg = in.readInt();
        this.yArg = in.readInt();
	this.alignment = in.readInt();
        this.strings = in.readStringArray();
        this.fontImageFileName = in.readString();
        this.hspace = in.readInt();
        this.vspace = in.readInt();
        this.background = in.readColor();     
        initialize();
    }

    private int getStringWidth(String string) {
        char[] chars = string.toCharArray();
        int w = 0;
        int maxPixelWidth  = 0;
        CharImageInfo charInfo;
        
        for (int i = 0; i < chars.length; i++) {
           charInfo = (CharImageInfo) charMap.get(new Character(chars[i]));;
            
           if (charInfo == null) {
               System.err.println("No charInfo found for " + chars[i]);
               charInfo = new CharImageInfo();
               charInfo.boundRect = new Rectangle(0,0,maxAscent+maxDescent, 20);
               charInfo.baseline  = 0;
               charInfo.charRect = new Rectangle(0,0,0,0);       
               charMap.put(new Character(chars[i]), charInfo);
           }
           
           int charAscent = (charInfo.baseline - charInfo.boundRect.y);
           if (this.maxAscent < charAscent) {
               this.maxAscent = charAscent;
           }
           int charDescent = (charInfo.boundRect.height - charAscent);
           if (this.maxDescent < charDescent) {
               this.maxDescent = charDescent;
           }
           maxPixelWidth = Math.max(maxPixelWidth, w + charInfo.charRect.width+hspace);
           w += charInfo.boundRect.width+hspace;
        }
        
        return Math.max(maxPixelWidth, w);
    }
    
    private void drawString(Graphics2D g2, String string, int x, int y) {
        char[] chars = string.toCharArray();
        CharImageInfo charInfo;
        for (int i = 0; i < chars.length; i++) {
           charInfo = (CharImageInfo) charMap.get(new Character(chars[i]));
           fontImage.drawClipped(g2,
                    x - charInfo.boundRect.x, 
                    y - charInfo.boundRect.y,
                    charInfo.charRect,
                    show.component);            
           x += charInfo.boundRect.width+hspace;
        }        
    }
    
    /**
     * Get the text that's being displayed.
     **/
    public String[] getText() {
	return strings;
    }

    /**
     * Get the height of a line, including any vertical padding to take it
     * to the next line.
     **/
    public int getLineHeight() {
	return vspace + ascent + descent + 1;
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
    
}


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
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Represents a fixed image.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class FixedImage extends Feature {

    private int x;
    private int y;
    private int width = 0;
    private int height = 0;
    private String fileName;

    private ManagedImage image;
    private boolean setupMode = false;
    private Object setupMonitor = new Object();
    private boolean imageSetup = false;
    private boolean isActivated = false;

    public FixedImage(Show show, String name, int x, int y, String fileName) 
    		throws IOException 
    {
	super(show, name);
	this.x = x;
	this.y = y;
	this.fileName = fileName;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    public void initialize() {
	image = ImageManager.getImage(fileName);
    }

    /**
     * See superclass definition.
     **/
    public int getStartX() {
	return x;
    }

    /**
     * See superclass definition.
     **/
    public int getStartY() {
	return y;
    }

    /* 
     * Internal use only 
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Get the underlying image that we display.
     **/
    public ManagedImage getImage() {
	return image;
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
	ImageManager.ungetImage(image);
    }


    /**
     * See superclass definition.
     **/
    protected void setActivateMode(boolean mode) {
	isActivated = mode;
    }

    /**
     * See superclass definition.
     **/
    protected void setSetupMode(boolean mode) {
	synchronized(setupMonitor) {
	    setupMode = mode;
	    if (setupMode) {
		show.setupManager.scheduleSetup(this);
	    } else {
		image.unprepare();
		imageSetup = false;
	    }
	}
    }

    /**
     * See superclass definition.
     **/
    public void doSomeSetup() {
	synchronized(setupMonitor) {
	    if (!setupMode) {
		return;
	    }
	}
	image.prepare(show.component);
	synchronized(setupMonitor) {
	    if (setupMode) {
		imageSetup = true;
		width = image.getWidth();
		height = image.getHeight();
	    } else {
		image.unprepare();
	    }
	}
	sendFeatureSetup();
    }

    /**
     * See superclass definition.
     **/
    public boolean needsMoreSetup() {
	synchronized (setupMonitor) {
	    return setupMode && (!imageSetup);
	}
    }

    /**
     * See superclass definition.
     **/
    public void paintFrame(Graphics2D gr) {
	if (!isActivated) {
	    return;
	}
	image.draw(gr, x, y, show.component);
    }

    /**
     * See superclass definition.
     **/
    public void addDisplayArea(Rectangle area) {
	if (area.width == 0) {
	    area.setBounds(x, y, width, height);
	} else {
	    area.add(x, y);
	    area.add(x+width, y+height);
	    	// This is correct.  Rectangle.add() (and AWT in general)
		// believes that the lower-right hand coordinate is "outside"
		// of a rectangle.
	}
    }

    /**
     * See superclass definition.
     **/
    public void advanceToFrame(int newFrame) {
	// do nothing
    }
}

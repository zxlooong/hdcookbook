
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
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.Debug;


import java.io.IOException;
import java.awt.Image;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class ImageSequence extends Feature {

    private int x;
    private int y;
    private int width = 0;
    private int height = 0;
    private String fileName;
    private String[] middle;
    private String extension;
    private boolean repeat;
    private Command[] endCommands;

    private ManagedImage[] images;
    private boolean setupMode = false;
    private int imagesSetup = 0;
    private Object setupMonitor = new Object();
    private boolean isActivated = false;

    private ImageSequence linkedTo;	
    	// We use linkedTo to count our frame and for end commands.  If
	// we aren't linkedTo another ImageSequence, it's set to null.
    private int startAnimationFrame;	// First frame we animate
    private ImageSequence activeSlave = null;	// Our active slave, if one
    private int activeLinkedCount = 0;	// # of active sequences linked to us

    public ImageSequence(Show show, String name, int x, int y, String fileName,
    			 String[] middle, String extension, boolean repeat,
			 Command[] endCommands) 
    		throws IOException {
	super(show, name);
	this.x = x;
	this.y = y;
	this.fileName = fileName;
	this.middle = middle;
	this.extension = extension;
	this.repeat = repeat;
	this.linkedTo = null;
	this.endCommands = endCommands;
    }

    public void setLinkedTo(ImageSequence linkedTo) throws IOException {
	this.linkedTo = linkedTo;
	if (linkedTo.middle.length != middle.length) {
	    throw new IOException("Mismatched number of frames in linkedTo");
	}
    }

    public int getStartX() {
	return x;
    }

    public int getStartY() {
	return y;
    }

    public ManagedImage[] getImages() {
	return images;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    public void initialize() {
	images = new ManagedImage[middle.length];
	for (int i = 0; i < middle.length; i++) {
	    if (middle[i] == null) {
		images[i] = null;
	    } else {
		String nm = fileName + middle[i] + extension;
		images[i] = ImageManager.getImage(nm);
	    }
	}
	if (show.keepAllImages) {
	    for (int i = 0; i < images.length; i++) {
		if (images[i] != null) {
		    images[i].prepare(show.component);
		}
	    }
	}
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
	if (show.keepAllImages) {
	    for (int i = 0; i < images.length; i++) {
		if (images[i] != null) {
		    images[i].unprepare();
		}
	    }
	}
	for (int i = 0; i < images.length; i++) {
	    if (images[i] != null) {
		ImageManager.ungetImage(images[i]);
	    }
	}
    }

    protected void setActivateMode(boolean mode) {
	isActivated = mode;
	if (linkedTo != null) {
	    if (mode) {
		if (!linkedTo.isActivated && linkedTo.activeLinkedCount == 0) {
		    linkedTo.startAnimationFrame  = show.getCurrentFrame();
		}
		linkedTo.activeLinkedCount++;
		linkedTo.activeSlave = this;
	    } else {
		linkedTo.activeLinkedCount--;
		linkedTo.activeSlave = null;
	    }
	} else {
	    if (mode && activeLinkedCount == 0) {
		startAnimationFrame = show.getCurrentFrame();
	    }
	}
    }

    protected void setSetupMode(boolean mode) {
	synchronized(setupMonitor) {
	    setupMode = mode;
	    if (setupMode) {
		show.setupManager.scheduleSetup(this);
	    } else {
		for (int i = 0; i < imagesSetup; i++) {
		    if (images[i] != null) {
			images[i].unprepare();
		    }
		}
		imagesSetup = 0;
	    }
	}
    }

    public void doSomeSetup() {
	ManagedImage im;
	synchronized(setupMonitor) {
	    if (!setupMode) {
		return;
	    }
	    im = images[imagesSetup];
	    while (im == null && (imagesSetup+1) < images.length) {
		imagesSetup++;
		im = images[imagesSetup];
	    }
	}
	if (im != null) {
	    im.prepare(show.component);
	    synchronized(setupMonitor) {
		if (setupMode) {
		    int w = images[imagesSetup].getWidth();
		    int h = images[imagesSetup].getHeight();
		    if (w > width) {
			width = w;
		    }
		    if (h > height) {
			height = h;
		    }
		    imagesSetup++;
		} else {
		    im.unprepare();
		}
	    }
	}
	if (!needsMoreSetup()) {
	    sendFeatureSetup();
	}
    }

    public boolean needsMoreSetup() {
	synchronized (setupMonitor) {
	    return setupMode && (imagesSetup < images.length);
	}
    }

    private ImageSequence getStateHolder() {
	if (linkedTo == null) {
	    return this;
	} else {
	    return linkedTo;
	}
    }

    public void advanceToFrame(int newFrame) {
	if (Debug.LEVEL > 0 && !isActivated) {
	    Debug.println("\n*** WARNING:  Advancing inactive sequence " 
                          + getName() + "\n");
	}
        ImageSequence sh = getStateHolder();
        int curr = newFrame - sh.startAnimationFrame;
        if (curr >= images.length && endCommands != null) {
            for (int i = 0; i < endCommands.length; i++) {
                show.runCommand(endCommands[i]);
            }
        }
    }

    public void  addDisplayArea(Rectangle area) {
	if (!isActivated) {
	    return;
	}
	if (activeSlave == null) {
	    doAddDisplayArea(area);
	} else {
	    activeSlave.doAddDisplayArea(area);
	}
    }

    void doAddDisplayArea(Rectangle area) {
	if (area.width == 0) {
	    area.setBounds(x, y, width + 1, height + 1);
	} else {
	    area.add(x, y);
	    area.add(x+width, y+height);
	}
    }

    public void paintFrame(Graphics2D gr) {
	if (!isActivated) {
	    return;
	}
	if (activeSlave == null) {
	    doPaint(gr);
	} else {
	    activeSlave.doPaint(gr);
	}
    }

    private void doPaint(Graphics2D g) {
	ImageSequence sh = getStateHolder();
	int curr = (show.getCurrentFrame() - sh.startAnimationFrame) 
			% images.length;
	if (images[curr] != null) {
	    images[curr].draw(g, x, y, show.component);
	}
    }

}

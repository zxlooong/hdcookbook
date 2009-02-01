
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

package com.hdcookbook.grin.util;

import java.awt.Image;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.ImageObserver;
import java.net.URL;

/**
 * A managed image that's loaded from its own image file (and not
 * as a part of a mosaic).
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ManagedFullImage extends ManagedImage implements ImageObserver {

    private String name;
    private URL url = null;	// Stays null unless special constructor used
    private int numReferences = 0;
    private int numPrepares = 0;
    Image image = null;		// Accessed by ManagedSubImage
    private boolean loaded = false;
    	// If image == null && !loaded, then we're loading.

    ManagedFullImage(String name) {
	this.name = name;
    }

    ManagedFullImage(String name, URL url) {
	this.name = name;
	this.url = url;
    }

    public String getName() {
	return name;
    }

    public int getWidth() {
	// assert image != null
	return image.getWidth(null);
    }
    
    public int getHeight() {
	// assert image != null
	return image.getHeight(null);
    }

    synchronized void addReference() {
	numReferences++;
    }

    synchronized void removeReference() {
	numReferences--;
    }

    synchronized boolean isReferenced() {
	return numReferences > 0;
    }

    /**
     * @inheritDoc
     **/
    public synchronized void prepare() {
	    // See ManagedImage's main class documentation under
	    //  "ManagedImage contract - image loading and unloading".
	numPrepares++;
    }

    /**
     * @inheritDoc
     **/
    public synchronized boolean isLoaded() {
	    //  See ManagedImage's main class documentation under
	    //  "ManagedImage contract - image loading and unloading".
	return loaded;
    }

    /**
     * @inheritDoc
     **/
    public void load(Component comp) {
	    // See ManagedImage's main class documentation under
	    //  "ManagedImage contract - image loading and unloading".
	synchronized(this) {
	    while (true) {
		if (loaded || numPrepares <= 0) {
			// If load is done in a different thread than
			// unprepare, it's possible for our client to lose
			// interest in us before we even start preparing.
			// For example, in GRIN, the show could possibly
			// move to a different segment before the setup
			// thread starts preparing an image from the previous
			// segment.
		    return;
		}
		if (image == null) {
		    startLoading(comp);	// Sets image to non-null
		} else {
		    // Image is being loaded, so we wait.
		    try {
			wait();	// Until that other thread loads image
		    } catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			return;
		    }
		    // Now, go back around the loop
		}
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public synchronized void startLoading(Component  comp) {
	if (image != null || numPrepares <= 0) {
	    return;
	}
	if (url == null) {
	    image = AssetFinder.loadImage(name);
	} else {
	    image = Toolkit.getDefaultToolkit().createImage(url);
	}
	//
	// The JDK seems to put the image fetching thread priority
	// really high, which is the opposite of what we want.  By
	// yielding, we increase the odds that higher-priority animation
	// will be given a chance before our lower-priority setup thread
	// grabs the CPU with the image fetching thread.  On all 
	// implementations, yielding in this manner should be at worst 
	// harmless.
	//
	Thread.currentThread().yield();
	comp.prepareImage(image, this);
    }

    //
    // Implementation of the ImageObserver method.  This gets called by the
    // system on the image loading thread.
    //
    public synchronized boolean 
    imageUpdate(Image img, int infoflags, int x, int y, int width, int height)
    {
	if (img != image) {		
		// They've lost interest in us.  So sad.
		//
		// Usually, we'd get here because image is null, because
		// unprepare() got called.  It's possible that after image
		// becomes null, it might become non-null with a fresh
		// instance of Image; in this case, the old image in img
		// is still something we don't care about, so we flush it and
		// tell the system to stop updating us.
	    img.flush();
	    return false;
	}
	if ((infoflags & (ALLBITS | ERROR | ABORT)) != 0) {
		// ERROR and ABORT shouldn't really happen, but if it does
		// the best we can do is blithely accept the fact, and treat
		// the image as though it were loaded.
	    loaded = true;
	    notifyAll();
	    if (Debug.LEVEL > 1) {
		Debug.println("Loaded image " + name);
	    }
	    return false;
	} else {
	    return true;
	}
    }

    /** 
     * @inheritDoc
     **/
    public synchronized void unprepare() {
	    // See ManagedImage's main class documentation under
	    //  "ManagedImage contract - image loading and unloading".
	numPrepares--;
	if (numPrepares <= 0 && loaded) {
	    if (image != null) {
		image.flush();
		image = null;
	    }
	    if (Debug.LEVEL > 1 && loaded) {
		Debug.println("Unloaded image " + name);
	    }
	    loaded = false;
	}
    }

    /**
     * @inheritDoc
     **/
    public void draw(Graphics gr, int x, int y, Component comp) {
        gr.drawImage(image, x, y, comp);
    }

    /**
     * @inheritDoc
     **/
    public void drawScaled(Graphics gr, Rectangle bounds, Component comp) {
	gr.drawImage(image, bounds.x, bounds.y, 
			    bounds.x+bounds.width, bounds.y+bounds.height,
			    0, 0, image.getWidth(null), image.getHeight(null), 
			    comp);
    }
    
    /**
     * @inheritDoc
     **/
    public void drawClipped(Graphics gr, int x, int y, Rectangle subsection, Component comp) {
	gr.drawImage(image, x, y, x+ subsection.width, y+subsection.height,
			    subsection.x, subsection.y, 
			    subsection.x+subsection.width, subsection.y+subsection.height, 
			    comp);
    }
    void destroy() {
	Image im = image;
	if (im != null) {
	    im.flush();	// Shouldn't be necessary, but doesn't hurt.
	}
    }
}

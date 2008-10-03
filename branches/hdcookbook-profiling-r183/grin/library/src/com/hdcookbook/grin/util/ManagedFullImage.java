
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
import java.awt.Graphics2D;

/**
 * A managed image that's loaded from its own image file (and not
 * as a part of a mosaic).
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ManagedFullImage extends ManagedImage {

    private String name;
    private int numReferences = 0;
    private int numPrepares = 0;
    Image image;		// Accessed by ManagedSubImage
    private ImageWaiter waiter;

    ManagedFullImage(String name) {
	this.name = name;
	this.image = AssetFinder.loadImage(name);
	this.waiter = new ImageWaiter(this.image);
    }

    public String getName() {
	return name;
    }

    public int getWidth() {
	if (Debug.ASSERT && numPrepares < 1) {
	    Debug.assertFail();
	}
	return image.getWidth(null);
    }
    
    public int getHeight() {
	if (Debug.ASSERT && numPrepares < 1) {
	    Debug.assertFail();
	}
	return image.getHeight(null);
    }

    public void addReference() {
	numReferences++;
    }

    public void removeReference() {
	numReferences--;
    }

    public boolean isReferenced() {
	return numReferences > 0;
    }

    /**
     * Prepare this image for display in the given component, or any
     * other component for the same graphics device.  This class reference
     * counts, so there can be multiple calls to prepare.
     *
     * @see #unprepare()
     **/
    public void prepare(Component comp) {
	int num;
	synchronized(this) {
	    numPrepares++;
	    num = numPrepares;
	    if (image == null) {
		image = AssetFinder.loadImage(name);
		waiter = new ImageWaiter(this.image);
	    }
	}
	if (num == 1) {
	    long tm = 0;
	    //
	    // The JDK seems to put the image fetching thread priority
	    // really high, which is the opposite of what we want.  By
	    // yielding, we increase the odds that higher-priority animation
	    // will be given a chance before our lower-priority setup thread
	    // grabs the CPU with the image fetching thread.
	    //
	    Thread.currentThread().yield();
	    if (!comp.prepareImage(image, waiter)) {
		waiter.waitForComplete();
	    }
	    if (Debug.LEVEL > 1) {
		Debug.println("Loaded image " + name);
	    }
	}
    }

    /**
     * @inheritDoc
     **/
    public synchronized boolean incrementIfPrepared() {
	if (numPrepares > 0) {
	    numPrepares++;
	    return true;
	} else {
	    return false;
	}
    }

    /** 
     * Undo a prepare.  We do reference counting; when the number of
     * active prepares hits zero, we flush the image.
     *
     * @see #prepare(java.awt.Component)
     **/
    public synchronized void unprepare() {
	numPrepares--;
	if (numPrepares == 0) {
	    image.flush();
	    image = null;
	    if (Debug.LEVEL > 1) {
		Debug.println("Unloaded image " + name);
	    }
	}
    }

    /**
     * Draw this image into the given graphics context
     **/
    public void draw(Graphics2D gr, int x, int y, Component comp) {
        gr.drawImage(image, x, y, comp);
    }

    void destroy() {
	Image im = image;
	if (im != null) {
	    im.flush();	// Shouldn't be necessary, but doesn't hurt.
	}
    }
}

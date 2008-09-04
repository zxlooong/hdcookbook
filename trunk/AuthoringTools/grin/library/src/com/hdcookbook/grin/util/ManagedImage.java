
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
import java.awt.Rectangle;

/**
 * An image that is managed by the GRIN utilities.  Managed images
 * have reference counts, and a count of how many clients have asked
 * it to be prepared.  This is used to flush images once they're
 * no longer needed.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
abstract public class ManagedImage {

    ManagedImage() {
    }

    abstract public String getName();

    public String toString() {
	return super.toString() + " : " + getName();
    }

    abstract public int getWidth();

    abstract public int getHeight();

    /**
     * Add one to the reference count of this image.  This is unrelated
     * to image loading and unloading.
     **/
    abstract public void addReference();

    /**
     * Remove one from the reference count of this image.  This is unrelated
     * to image loading and unloading.
     **/
    abstract public void removeReference();

    /**
     * Determine if this image is referenced, by consulting its reference
     * count.
     **/
    abstract public boolean isReferenced();

    /**
     * Prepare this image for display in the given component, or any
     * other component for the same graphics device.  This class reference
     * counts, so there can be multiple calls to prepare.
     * <p>
     * Calling prepare(null) is equivalent to calling
     * unprepare().  This is an implementation detail that is subject to change,
     * and should not be relied upon by client code.
     *
     * @param  comp	A component to use for loading the image.  Clients
     *			using ManagedImage should never pass in null.
     *
     * @see #unprepare()
     **/
    abstract public void prepare(Component comp);

    /** 
     * Undo a prepare.  We do reference counting; when the number of
     * active prepares hits zero, and the "sticky" count reaches zero,
     * we flush the image.
     *
     * @see #prepare(java.awt.Component)
     * @see #makeSticky()
     * @see #unmakeSticky()
     **/
    abstract public void unprepare();

    /**
     * Make this image "sticky".  An image that is sticky will be loaded the
     * normal way when prepare(Component) is called, but it will not be unloaded
     * when the count of active prepares reaches zero due to a call to
     * unprepare().  The calls to makeSticky() are themselves reference-counted;
     * an image is sticky until the sticky count reaches zero due to a call
     * to unmakeSticky().
     * <p>
     * If an image is a tile within a mosaic, the entire mosaic will be held
     * in memory as long as the mosaic tile is loaded.
     **/
    final public void makeSticky() {
	prepare(null);
    }

    /**
     * Undo the effects of one call to makeSticky().  This is described in
     * more detail under makeSticky().
     *
     * @see #makeSticky()
     * @see #unprepare()
     * @see #prepare(java.awt.Component)
     **/
    final public void unmakeSticky() {
	unprepare();
    }

    public boolean equals(Object other) {
	return this == other;
	// ImageManager canonicalizes ManagedImage instances
    }
    
    /**
     * Draw this image into the given graphics context
     **/
    abstract public void draw(Graphics2D gr, int x, int y, Component comp);

    /**
     * Draw this image into the given graphics context, scaled to fit within
     * the given bounds.
     **/
    abstract public void drawScaled(Graphics2D gr, Rectangle bounds,
    				    Component comp);

    abstract void destroy();
}

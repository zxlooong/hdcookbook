
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
 * no longer needed.  A ManagedImage instance is obtained from
 * ImageManager.
 *
 * <h2>ManagedImage contract - image loading and unloading</h2>
 *
 * The second level of reference counting is for tracking whether the 
 * underlying image asset is not loaded, loading, or loaded.  Every client
 * of an image that wants the image to be loaded should <code>prepare()</code>
 * the image, and each prepare call must eventually be balanced by a call 
 * to <code>unprepare()</code>
 * the image.  Because image loading is a time-consuming information, it's
 * useful to do as the GRIN scene graph does, and use a seperate thread
 * (like the SetupManager thread) to do the actual image loading.  However,
 * queueing a task for another thread is somewhat expensive, so if an image
 * is already loaded, it's good to skip that step.  This can be done with
 * the following client code:
 * <pre>
 *
 *        ManagedImage mi = ...
 *        mi.prepare();
 *        if (!mi.isLoaded()) {
 *            Queue a task for another thread to call load() on this image
 *        }
 *
 * </pre>
 * Eventually, when the client no longer wants the image to be loaded, 
 * it must call <code>unprepare()</code>.
 *
 * <h2>Sticky Images</h2>
 *
 * An image can be marked as "sticky" by calling <code>makeSticky()</code>,
 * and unmarked with <code>unmakeSticky()</code>.  A sticky image won't
 * be unloaded when the count of prepares reaches zero.
 * 
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
     * to image loading and unloading.  It's package-private, because it's
     * used by ImageManager only.
     **/
    abstract void addReference();

    /**
     * Remove one from the reference count of this image.  This is unrelated
     * to image loading and unloading.  It's package-private, because it's
     * used by ImageManager only.
     **/
    abstract void removeReference();

    /**
     * Determine if this image is referenced, by consulting its reference
     * count.  It's package-private, because it's
     * used by ImageManager only.
     **/
    abstract boolean isReferenced();

    /**
     * Prepare this image for display, by registering interest in having
     * the image be loaded.  In order to actually load the image,
     * <code>load(Component)</code> must be called.
     * for a call to <code>load(Component)</code>.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     *
     * @see #isLoaded()
     * @see #load(Component)
     * @see #unprepare()
     * @see ManagedImage
     **/
    abstract public void prepare();

    /**
     * Determine whether or not the image is currently loaded.  After a
     * call to prepare(), this method can be used to query whether or not
     * it's necessary to arrange for load(Component) to be called.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     * 
     * @see ManagedImage
     **/
    abstract public boolean isLoaded();

    /**
     * Load this image for display in the given component, or any
     * other component for the same graphics device.  The image will
     * only be loaded if an interest in loading this ManagedImage has
     * been registered by calling <code>prepare()</code>.  If no interest
     * has been registered, or if this image is already loaded, then this
     * method will return immediately.  If another thread is loading this
     * same image, this method will wait until that image load is complete
     * before it returns.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     *
     * @param  comp	A component to use for loading the image.  Clients
     *			using ManagedImage should never pass in null.
     *
     * @see #prepare()
     * @see #unprepare()
     * @see ManagedImage
     **/
    abstract public void load(Component comp);

    /** 
     * Undo a prepare.  We do reference counting; when the number of
     * active prepares hits zero, and the "sticky" count reaches zero,
     * we flush the image.
     * <p>
     * See ManagedImage's main class documentation under
     * "ManagedImage contract - image loading and unloading".
     *
     * @see #prepare()
     * @see #load(Component)
     * @see #makeSticky()
     * @see #unmakeSticky()
     * @see ManagedImage
     **/
    abstract public void unprepare();

    /**
     * Make this image "sticky".  An image that is sticky will be loaded the
     * normal way when prepare()/load() are called, but it will not be unloaded
     * when the count of active prepares reaches zero due to a call to
     * unprepare().  The calls to makeSticky() are themselves reference-counted;
     * an image is sticky until the sticky count reaches zero due to a call
     * to unmakeSticky().
     * <p>
     * If an image is a tile within a mosaic, the entire mosaic will be held
     * in memory as long as the mosaic tile is loaded.
     *
     * @see #unmakeSticky()
     * @see #unprepare()
     * @see #prepare()
     **/
    final public void makeSticky() {
	prepare();
    }

    /**
     * Undo the effects of one call to makeSticky().  This is described in
     * more detail under makeSticky().
     *
     * @see #makeSticky()
     * @see #unprepare()
     * @see #prepare()
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

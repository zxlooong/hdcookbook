
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


import java.awt.Component;
import java.awt.Graphics;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Hashtable;

import com.hdcookbook.grin.Show;

/**
 * This class manages a set of images.  It loads and flushes them as needed.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ImageManager {

    private static Hashtable images = new Hashtable();
    private static Hashtable imageMap = null;	
    	// Map of mosaic tile name to MosaicTile
    private static Object lock = new Object();

    private ImageManager() {
    }

    /**
     * Get an image.  Each call to getImage should be balanced
     * by a call to ungetImage when you no longer need the image.
     * Image instances are shared, so this class does reference counting.
     *
     * @see #ungetImage(com.hdcookbook.grin.util.ManagedImage)
     **/
    public static ManagedImage getImage(String name) {
	synchronized(lock) {
	    ManagedImage im = (ManagedImage) images.get(name);
	    if (im == null) {
		if (imageMap != null) {
		    MosaicTile t = (MosaicTile) imageMap.get(name);
		    if (t != null) {
			im = new ManagedSubImage(name, t.mosaicName, 
						 t.placement);
		    } else if (Debug.LEVEL > 0) {
			Debug.println(name + " not found in image map.");
		    }
		}
		if (im == null) {
		    im = new ManagedFullImage(name);
		}
		images.put(name, im);
	    }
	    im.addReference();
	    return im;
	}
    }

    /**
     * Called when an image acquired with getImage is no longer needed.
     *
     * @see #getImage(java.lang.String)
     **/
    public static void ungetImage(ManagedImage im) {
	synchronized(lock) {
	    im.removeReference();
	    if (!im.isReferenced()) {
		images.remove(im.getName());
		im.destroy();
	    }
	}
    }

    static void readImageMap(DataInputStream is) throws IOException {
	// Reads the file written by 
	// com.hdcookbook.grin.build.mosaic.MosaicMaker.makeMosaics()
	// This maps the original image file name to the name of a
	// mosaic image, and the position within that mosaic.

	synchronized(lock) {
	    if (Debug.ASSERT && imageMap != null) {
		Debug.assertFail();
	    }
	    imageMap = new Hashtable();
	    int n = is.readInt();
	    String[] mosaics = new String[n];
	    for (int i = 0; i < n; i++) {
		mosaics[i] = is.readUTF();
	    }

	    n = is.readInt();
	    for (int i = 0; i < n; i++) {
		String tileName = is.readUTF();
		MosaicTile t = new MosaicTile();
		t.mosaicName = mosaics[is.readInt()];
		t.placement = new Rectangle();
		t.placement.x = is.readInt();
		t.placement.y = is.readInt();
		t.placement.width = is.readInt();
		t.placement.height = is.readInt();
		imageMap.put(tileName, t);
	    }
	    if (Debug.ASSERT && is.read() != -1) {
		Debug.assertFail();
	    }
	}
    }
}

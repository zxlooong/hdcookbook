
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

import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.net.URL;
import java.io.File;
import java.io.BufferedInputStream;
import java.io.DataInputStream;


/**
 * This class serves two functions.  First, it has a bunch of
 * static methods that are used by GRIN and can be used by applications
 * to find various resources.  Secondly, an xlet (or other application)
 * can create an instance of AssetFinder and bind it to us with the method
 * setHelper.  If this is done, AssetFinder will try to locate assets
 * by first querying the helper.  This way, an Xlet can make decisions
 * about where to look for stuff.  For example, a signed BD-Live xlet
 * could set up a search path on the BUDA.
 **/
public class AssetFinder  {
    
    private static AssetFinder helper = null;
    private static Class theClass = AssetFinder.class;
    private static String[] appJarPath;
    private static File[] filePath;

    /**
     * See setHelper()
     **/
    protected AssetFinder() {
    }

    /**
     * An xlet can make an instance of a subclass of AssetFinder, and
     * connect it to us by calling this method.  Whenever the AssetFinder
     * is looking for something, it will first check with the helper,
     * by calling one of the helperXXX methods.
     **/
    public static void setHelper(AssetFinder helperArg) {
	helper = helperArg;
    }

    /**
     * @param  appJarPathArg  A list of paths within the classpath
     *			      of the app, for use by Class.getResource
     * @param  filePathArg    A list of paths in the filesystem,
     *                        e.g. from mounting a DSMCC carousel.
     **/
    public static void setSearchPath(String[] appJarPathArg, 
    				     File[] filePathArg) 
    {
	if (appJarPathArg == null) {
	    appJarPath = null;
	} else {
	    appJarPath = new String[appJarPathArg.length];
	    for (int i = 0; i < appJarPathArg.length; i++) {
		if (appJarPathArg[i].endsWith("/")) {
		    appJarPath[i] = appJarPathArg[i];
		} else {
		    appJarPath[i] = appJarPathArg[i] + "/";
		}
	    }
	}
	filePath = filePathArg;
    }
    
    /**
     * Sets the image map.  This is used for mosaic
     * images:  The image map translates a logical image name
     * into a tuple (mosaic image, rect within mosaic).  This
     * must be set after the search path, since the search path
     * is used to load the image map.
     **/
    public static void setImageMap(String imageMap) {
	DataInputStream dis = null;
	try {
	    URL u = getURL(imageMap);
	    if (u == null) {
		throw new IOException();
	    }
	    dis = new DataInputStream(new BufferedInputStream(u.openStream()));
	    ImageManager.readImageMap(dis);
	    // dis.close is in the finally block
	} catch (IOException ex) {
	    ex.printStackTrace();
	    if (Debug.ASSERT) {
		Debug.assertFail();
	    }
	} finally {
	    if (dis != null) {
		try {
		    dis.close();
		} catch (IOException ignored) {
		}
	    }
	}
    }

    /**
     * Get a URL to an asset
     *
     * @param path	A string, relative to the search path for assets
     *                  TODO: Maybe need to search locators, too
     **/
    public static URL getURL(String path) {
	URL u = tryURL(path);
        if (Debug.ASSERT && u == null) {
	    if (appJarPath != null) {
		for (int i = 0; i < appJarPath.length; i++) {
		    Debug.println("   Tried " + appJarPath[i] + path);
		}
	    }
	    if (filePath != null) {
		for (int i = 0; i < filePath.length; i++) {
		    Debug.println("   Tried " + new File(filePath[i], path));
		}
	    }
            Debug.println();
            Debug.println("****  Resource " + path + " does not exist!  ****");
            Debug.println();
	    Debug.assertFail();
        }
        return u;
    }

    /**
     * Try to get an asset that might not be there.  If it's not,
     * return null.
     **/
    public static URL tryURL(String path) {
	if (helper != null) {
	    URL u = helper.tryURLHelper(path);
	    if (u != null) {
		return u;
	    }
	}
	if (Debug.ASSERT && appJarPath == null && filePath == null) {
	    Debug.assertFail("Search path not set.");
	}
	if (appJarPath != null) {
	    for (int i = 0; i < appJarPath.length; i++) {
		URL u = theClass.getResource(appJarPath[i] + path);
		if (u != null) {
		    return u;
		}
	    }
	}
	if (filePath != null) {
	    for (int i = 0; i < filePath.length; i++) {
		File f = new File(filePath[i], path);
		if (f.exists()) {
		    try {
			return f.toURL();
		    } catch (Exception ex) {
			// This should never happen
			ex.printStackTrace();
		    }
		}
	    }
	}
        return null;
    }

    /**
     * Helper subclass can override this in order to search for
     * generic assets, like images.
     **/
    protected URL tryURLHelper(String path) {
	return null;
    }


    /**
     * Efficiently get a Color that's used within a Show.  This method
     * might someday share one Color instance for multiple calls
     * with the same rgba values.
     **/
    public static Color getColor(int r, int g, int b, int a) {
	Color c = new Color(r, g, b, a);
	return c;
	// We could consider canonicalizing Color instances for efficiency.  Not
	// sure if this should use weak refs, or just a static 
	// AssetFinder.clear() method.
    }

    /**
     * Get a Font that's used within a Show.  This method
     * might someday share one Font instance and any needed font
     * factories for multiple calls with the same specifications.
     *
     * @throws IOException if the font isn't found for some reason
     **/
    public static Font getFont(String fontName, int style, int size) {
	if (helper != null) {
	    Font f = helper.getFontHelper(fontName, style, size);
	    if (f != null) {
		return f;
	    }
	    if (Debug.LEVEL > 0) {
		Debug.println("*** Helper didn't find font " + fontName);
	    }
	}
	return new Font(fontName, style, size);
    }

    /**
     * Helper subclass can override this in order to search for
     * generic assets, like images.
     **/
    protected Font getFontHelper(String fontName, int style, int size) {
	return null;
    }
    
    /**
     * @param path should be an absolute path within the classpath.
     **/
    public static Image loadImage(String path) {
	if (helper != null) {
	    Image im = helper.loadImageHelper(path);
	    if (im != null) {
		return im;
	    }
	}

	Toolkit tk = Toolkit.getDefaultToolkit();
	Image result;
        URL url = getURL(path);
	return tk.getImage(url);
    }

    /**
     * Helper subclass can override this in order to search for
     * images specifically.  If the helper doesn't find one, the
     * default AssetFinder implementation will call tryURL(), which 
     * the helper can also override.  If you override tryURL() such
     * that images can be located, there's no reason to override
     * this method too.
     **/
    protected Image loadImageHelper(String path) {
	return null;
    }

    /**
     * Called when the disc playback should abort.  This should only
     * be called when there's a fatal error, like an assertion failure.
     * The expected behavior is immediate termination - like
     * System.exit(1) on big JDK, or ejecting the disc on a player.
     **/
    public static void abort() {
	if (helper != null) {
	    helper.abortHelper();
	}
	throw new RuntimeException("ABORT");
    }

    protected void abortHelper() {
    }
}

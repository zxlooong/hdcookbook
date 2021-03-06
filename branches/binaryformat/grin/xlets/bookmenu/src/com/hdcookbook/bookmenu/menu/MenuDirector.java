
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


package com.hdcookbook.bookmenu.menu;

import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import java.net.URL;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.input.RCKeyEvent;
import com.hdcookbook.grin.io.ExtensionsBuilder;
import com.hdcookbook.grin.io.text.ShowBuilder;
import com.hdcookbook.grin.io.text.ShowParser;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;


/**
 * This is a helper class that integrates GRIN into our xlet.
 * It includes some control logic for the overall xlet, as well.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class MenuDirector extends Director {

    private MenuXlet xlet;
    private boolean destroyed = false;
    private BioUpdater bioUpdater = null;
    private BookmarkManager bookmarkManager = null;
    private UserInputManager userInputManager = null;

    public MenuDirector(MenuXlet xlet) {
	this.xlet = xlet;
    }

    /**
     * Initialize this MenuDirector.  Called on xlet startup.
     **/
    public void init() {
	ChapterManager nullCM = new ChapterManager("");
	ChapterManager[] chapters = { nullCM };
	setup(0, chapters);
    }

    /**
     * Called by GRIN when it parses a show.  This is how we hook
     * in our extensions to the GRIN syntax.
     **/
    public ExtensionsBuilder getExtensionsBuilder() {
	return new MenuExtensionsBuilder(xlet);
    }

    /**
     * Called by the xlet, this creates the GRIN show file that defines
     * most of our menu UI.
     **/
    public Show createShow() {
        
        Show show = new Show(this);
        
        try {
            
            String showName = "menu.grin";
	    URL u = AssetFinder.getURL(showName);
            
            // The two lines below are equivalent of the 4 lines of code against the text based ShowParser
            // ====START-CODE====
            // BufferedReader rdr = new BufferedReader(new InputStreamReader(u.openStream(), "UTF-8"));
	    // ShowParser p = new ShowParser(rdr, showName, show);
	    // p.parse();
	    // rdr.close();
            // ====END-CODE====
            
 	    GrinBinaryReader reader = new GrinBinaryReader(this, u.openStream());
            show = reader.readShow();
            
	} catch (IOException ex) {
	    if (Debug.LEVEL > 0) {
		ex.printStackTrace();
		Debug.println();
		Debug.println("***  Fatal error:  Failed to parse show.");
		Debug.println("***  " + ex);
		Debug.println();
		AssetFinder.abort();
	    }
	}        
        
	return show;
    }

    private synchronized BioUpdater getBioUpdater() {
	if (!destroyed && bioUpdater == null) {
	    bioUpdater = new BioUpdater(xlet);
	    bioUpdater.start();
	}
	return bioUpdater;
    }

    /**
     * Called from the show via ActivateBioCommand, this sets up the UI
     * in the right state when the bio screen is activated.
     **/
    public void activateBio() {
	BioUpdater bu = getBioUpdater();
	if (bu != null) {
	    bu.activateRightSegment();
	}
    }

    /**
     * Called from the show via a DownloadBioCommand, this starts the
     * process of downloading a new bio image from the Internet.
     **/
    public void downloadBio() {
	BioUpdater bu = getBioUpdater();
	if (bu != null) {
	    bu.downloadBio();
	}
    }

    private synchronized BookmarkManager getBookmarkManager() {
	if (!destroyed && bookmarkManager == null) {
	    bookmarkManager = new BookmarkManager(xlet);
	    bookmarkManager.init();
	}
	return bookmarkManager;
    }

    /** 
     * Returns the media time of the given bookmark, or
     * Long.MIN_VALUE.
     *
     * @param bookmarkNum  Number of bookmark, 0 means "none"
     **/
    public long setBookmarkUIForScene(int bookmarkNum) {
	BookmarkManager mgr = getBookmarkManager();
	if (mgr != null) {
	    return mgr.updateUI(bookmarkNum);
	} else {
	    return Long.MIN_VALUE;
	}
    }

    /** 
     * Called from the show via a command, this creates a new
     * bookmark at the current position in the  main video.
     **/
    public void makeBookmark() {
	BookmarkManager mgr = getBookmarkManager();
	if (mgr != null) {
	    mgr.makeBookmark();
	}
    }

    /**
     * Called from the show via a command, this deletes the bookmark
     * that's currently selected in the UI.
     **/
    public void deleteCurrentBookmark() {
	BookmarkManager mgr = getBookmarkManager();
	if (mgr != null) {
	    mgr.deleteCurrentBookmark();
	}
    }

    private synchronized UserInputManager getUserInputManager() {
	if (!destroyed && userInputManager == null) {
	    userInputManager = new UserInputManager(xlet);
	    userInputManager.init();
	}
	return userInputManager;
    }

    /**
     * @param text  The uppercase letter to add, or one of the special 
     *		    values "-enter-" or "-init-".
     **/
    public void setUserInputText(String text) {
	UserInputManager m = getUserInputManager();
	if (m != null) {
	    m.setText(text);
	}
    }

    /**
     * Destroy this director.  Called on xlet termination.
     **/
    public void destroy() {
	synchronized(this) {
	    destroyed = true;
	    if (bioUpdater != null) {
		bioUpdater.destroy();
	    }
	    if (bookmarkManager != null) {
		bookmarkManager.destroy();
	    }
	    if (userInputManager != null) {
		userInputManager.destroy();
	    }
	}
    }

}

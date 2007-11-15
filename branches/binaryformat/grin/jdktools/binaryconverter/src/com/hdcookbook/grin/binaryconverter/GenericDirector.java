
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

package com.hdcookbook.grin.binaryconverter;


import java.net.URL;
import java.io.IOException;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.parser.ShowParser;
import com.hdcookbook.grin.parser.ShowBuilder;
import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.parser.ExtensionsParser;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;

/**
 * This is a subclass of the GRIN director class that fakes out
 * GRIN to accept any extensions of the GRIN syntax.  The extensions
 * are ignored, with default behavior put in.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class GenericDirector extends Director {
   
    private String showName;
    private ExtensionsParser parser;
    
    public GenericDirector(String showName, ExtensionsParser parser) {
	this.showName = showName;
        this.parser = parser;
        
	ChapterManager[] chapters = { new ChapterManager("init") };
	setup(0, chapters);
    }
    
    /**
     * See superclass definition.  The first time we're asked for a given
     * chapter manager, we just create it.  A real xlet might have named
     * chapter managers of different types, if it chooses to use the
     * state pattern.
     **/
    public ChapterManager getChapterManager(String name) {
        synchronized(getShow()) {
            ChapterManager result = super.getChapterManager(name);
            if (result == null) {
                result = new ChapterManager(name);
                addState(result);
            }
            return result;
        }
    }

    /**
     * See superclass definition.  This extensions parser will just
     * make a fake implementation of each extension.
     **/
    public ExtensionsParser getExtensionsParser() {
        if (parser == null) 
            parser = new GenericExtensionsParser(this);
        
        return parser;
    }

    /**
     * Create a show.  This is called by the main control class of
     * this debug tool.
     **/
    public Show createShow(ShowBuilder builder) {
	Show show = new Show(this);
	URL source = null;
	BufferedReader rdr = null;
	try {
	    source = AssetFinder.getURL(showName);
	    if (source == null) {
		throw new IOException("Can't find resource " + showName);
	    }
	    rdr = new BufferedReader(
			new InputStreamReader(source.openStream(), "UTF-8"));
	    ShowParser p = new ShowParser(rdr, showName, show, builder);
	    p.parse();
	    rdr.close();
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.out.println();
	    System.out.println(ex.getMessage());
	    System.out.println();
	    System.out.println("Error trying to parse " + showName);
            System.out.println("    URL:  " + source);
	    System.exit(1);
	} finally {
	    if (rdr != null) {
		try {
		    rdr.close();
		} catch (IOException ex) {
		}
	    }
	}
        return show;
    }

}

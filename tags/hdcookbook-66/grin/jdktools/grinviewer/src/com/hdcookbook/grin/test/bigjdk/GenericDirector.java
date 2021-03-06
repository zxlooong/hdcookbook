
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

package com.hdcookbook.grin.test.bigjdk;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.io.text.ShowParser;
import com.hdcookbook.grin.util.AssetFinder;

/**
 * This is a subclass of the GRIN director class that fakes out
 * GRIN to accept any extensions of the GRIN syntax.  The extensions
 * are ignored, with default behavior put in.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class GenericDirector extends Director {
   
    private String showName;

    public GenericDirector(String showName) {
	this.showName = showName;
    }
  
    /**
     * Create a show.  This is called by the main control class of
     * this debug tool.
     **/
    public SEShow createShow(ShowBuilder builder) {
	SEShow show = new SEShow(this);
	URL source = null;
	BufferedReader rdr = null;
        BufferedInputStream bis = null;
	try {
	    source = AssetFinder.getURL(showName);
	    if (source == null) {
		throw new IOException("Can't find resource " + showName);
	    }
            
            if (!showName.endsWith(".grin")) {
                rdr = new BufferedReader(
                        new InputStreamReader(source.openStream(), "UTF-8"));
                ShowParser p = new ShowParser(rdr, showName, show, builder);
                p.parse();
                rdr.close();
            } else {
                if (AssetFinder.tryURL("images.map") != null) {
                    System.out.println("Found images.map, using mosaic.");
                    AssetFinder.setImageMap("images.map");
                } else {
                    System.out.println("No images.map found");
                }
                bis = new BufferedInputStream(source.openStream());
 	        GrinBinaryReader reader = new GrinBinaryReader(bis);
                reader.readShow(show);
                bis.close();
            }   
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
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException ex) {
                }    
            }
	}
        return show;
    }

}


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

import java.io.IOException;

import org.bluray.net.BDLocator;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;

import com.hdcookbook.bookmenu.menu.commands.PlayVideoCommand;
import com.hdcookbook.grin.io.binary.ExtensionsReader;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;

/** 
 * This class parses small extensions to the GRIN syntax added
 * for our xlet.  It's how the "BOOK:" commands and feature get
 * parsed.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class MenuExtensionsReader implements ExtensionsReader {

    private MenuXlet xlet;
    private Show show;

    public MenuExtensionsReader(MenuXlet xlet, Show show) {
	this.xlet = xlet;
        this.show = show;
    }

    /** 
     * Called by the GRIN parser to parse an extension feature 
     * that's not a modifier
     **/
    public Feature readExtensionFeature(Show show, 
            String name, GrinDataInputStream in, int length)
		   throws IOException
    {
	return null;
    }

    /**
     * Called by the GRIN parser to parse a feature that is a modifier
     **/
    public Modifier readExtensionModifier(Show show, 
            String name, GrinDataInputStream in, int length)
		   throws IOException
    {
        String typeName = in.readUTF();
	if ("BOOK:bio_image".equals(typeName)) {
	    Modifier modifier = new BioImageFeature(show, name);   
            return modifier;
	} else {
	    return null;
	}
    }

    /**
     * Called by the GRIN parser to parse an extension command.
     **/
    public Command readExtensionCommand(Show show, 
            GrinDataInputStream in, int length)
		       throws IOException {
        
        String typeName = in.readUTF();
        String[] args = in.readStringArray();
                
	if ("BOOK:PlayVideo".equals(typeName)) {
	    BDLocator loc = null;
	    if ("menu".equals(args[0])) {
		loc = xlet.navigator.menuVideoStartPL;
	    } else if ("movie".equals(args[0])) {
		loc = xlet.navigator.movieVideoStartPL;
	    } else if ("scene_1".equals(args[0])) {
		loc = xlet.navigator.sceneVideoStartPL[0];
	    } else if ("scene_2".equals(args[0])) {
		loc = xlet.navigator.sceneVideoStartPL[1];
	    } else if ("scene_3".equals(args[0])) {
		loc = xlet.navigator.sceneVideoStartPL[2];
	    } else if ("scene_4".equals(args[0])) {
		loc = xlet.navigator.sceneVideoStartPL[3];
	    } else if ("scene_5".equals(args[0])) {
		loc = xlet.navigator.sceneVideoStartPL[4];
	    } else if ("nothing".equals(args[0])) {
		loc = null;
	    }
	    return new PlayVideoCommand(xlet, loc);
        }
	throw new IOException("Unrecognized command type  \"" + typeName + "\"");
    }
}

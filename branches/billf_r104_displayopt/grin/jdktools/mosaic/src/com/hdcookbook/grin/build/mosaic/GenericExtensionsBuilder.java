
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

package com.hdcookbook.grin.build.mosaic;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.text.Lexer;
import com.hdcookbook.grin.io.text.ShowParser;
import com.hdcookbook.grin.io.ExtensionsBuilder;
import com.hdcookbook.grin.input.RCHandler;

import java.io.IOException;
import java.util.LinkedList;

/**
 * This is an extensions parser for a GRIN show file.  Since we
 * don't know about any xlet-specific extensions, we just fake
 * them out:  We parse all the tokens, but we fill in generic
 * results rather than xlet-specific ones.  For building an image
 * mosaic, this works fine.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class GenericExtensionsBuilder implements ExtensionsBuilder {
   

    public GenericExtensionsBuilder() {
    }

    /** 
     * Parse an extension feature that's not a modifier.
     **/
    public Feature getFeature(Show show, String typeName,
                              String name, String arg)
    {
	// Some rules need to be set about the syntax of an extension
	// feature before we can deal with this.
        return null;
    }


    /**
     * Parse an extension command.
     * This assumes that all commands end with a semicolon, and have no
     * semicolons embedded in them.
     **/
    //public Command parseCommand(Show show, String typeName, Lexer lex,
    //			        ShowParser parser) 
    public Command getCommand(Show show, String typeName, String[] args) 
			throws IOException
    {
	return new GenericExtensionCommand(typeName, args);
    }

    /** 
     * Parse an extension feature that is a modifer.  Modifiers have
     * a fixed syntax, so we don't get passed the lexer or the parser.
     **/
    public Modifier getModifier(Show show, final String typeName, 
                                String name, String arg)
    { 
        return new Modifier(show, name) {
            public String toString() {
                return typeName;
            } 
        };
    }

    /** 
     * Called by the GRIN parser after the show is parsed
     **/
    public void finishBuilding(Show show) throws IOException {
    }

    /**
     * Called by the GRIN parser when it encounters a mosaic hint.
     * This gets overridden in an anonymous subclass in MosaicMaker.
     **/
    public void takeMosaicHint(String name, int width, int height, 
                               String[] images)
    {
    }
    
}

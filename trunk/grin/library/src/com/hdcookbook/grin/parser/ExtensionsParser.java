
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

package com.hdcookbook.grin.parser;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.parser.Lexer;
import com.hdcookbook.grin.Show;

import java.io.IOException;

/**
 * This class is used by an xlet to add new commands and features
 * to the syntax of its GRIN show file(s).
 * A Director can expand the set of features and commands
 * recognized in a show file.  It does this by providing an
 * implementation of ExtensionsParser to handle any extensions.
 *
 * @author @author Bill Foote (http://jovial.com)
 */
public interface ExtensionsParser {

    /**
     * Get a feature of the given type.  The type name will have a
     * colon in it.
     * <p>
     * The syntax of an extension feature is fixed at
     * <pre>
     *     "feature" "extension" namespace:type_name name string ";"
     * </pre>
     * where feature_name is given iff the feature is a Modifier.
     *
     * @param show      The show being parsed
     * @param typeName  The name of the feature's type.  This will always
     *                  contain a ":".
     * @param name      The name of this instance of feature
     *			a list of commands if needed.
     * @param arg	The argument string on the feature
     *
     * @throws      IOException if there's an error.
     *
     * @return	    A feature if one of the given type is known, null otherwise
     */
    public Feature getFeature(Show show, String typeName, 
    			      String name, String arg)
		       throws IOException;


    /**
     * Get a modifier feature of the given type.  The type name will have a
     * colon in it.  The sub-feature will automatically be set up for
     * you.
     * <p>
     * The syntax of an extension feature is fixed at
     * <pre>
     *     "feature" "modifier" namespace:type_name name feature_name string ";"
     * </pre>
     * where feature_name is given iff the feature is a Modifier.
     *
     * @param show      The show being parsed
     * @param typeName  The name of the feature's type.  This will always
     *                  contain a ":".
     * @param name      The name of this instance of feature
     *			a list of commands if needed.
     * @param arg	The argument string on the feature
     *
     * @throws      IOException if there's an error.
     *
     * @return	    A feature if one of the given type is known, null otherwise
     */
    public Modifier getModifier(Show show, String typeName, 
    			        String name, String arg)
		       throws IOException;

    /**
     * Parse a command of the given type.  
     * <p>
     * Commands are supposed to end with a ";" token.  This should be the
     * last token read by this method.
     *
     * @param show      The show being parsed
     * @param typeName  The name of the commands's type.  This will always
     *                  contain a ":".
     * @param lex       The lexer that's reading the input file
     * @param parser	The current parser.  We can use this to pass
     *			a list of commands if needed.
     *
     * @throws      IOException if there's any parsing error.  This can be
     *              generated with Lexer.reportError(String).     
     */    
    public Command parseCommand(Show show, String typeName, Lexer lex, 
    				ShowParser parser) 
			throws IOException;

    /**
     * Called after parsing is done, and all of the built-in objects
     * have been resolved.  This allows any final
     * initialization to be performed.  Note that GRIN's built-in
     * parser automatically calls Command.resolve() for all commands
     * in the show, including extension commands.
     **/
    public void finishBuilding(Show s) throws IOException;
    
    /**
     * Give a hint how an optimal mosaic could be built.
     **/
    public void takeMosaicHint(String name, int width, int height, 
    			       String[] images);
}

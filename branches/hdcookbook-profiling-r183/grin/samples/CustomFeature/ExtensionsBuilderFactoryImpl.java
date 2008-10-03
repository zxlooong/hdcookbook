/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.ExtensionsBuilderFactory;
import com.hdcookbook.grin.io.binary.ExtensionsWriter;
import com.hdcookbook.grin.io.binary.GrinDataOutputStream;
import com.hdcookbook.grin.io.text.ExtensionsParser;
import com.hdcookbook.grin.io.text.Lexer;
import com.hdcookbook.grin.util.AssetFinder;

import java.awt.Color;
import java.io.IOException;

public class ExtensionsBuilderFactoryImpl extends ExtensionsBuilderFactory {

    @Override
    public ExtensionsWriter getExtensionsWriter() {
        return new ExtensionsWriterImpl();
    }

    @Override
    public ExtensionsParser getExtensionsParser() {
        return new ExtensionsParserImpl();
    }

    class ExtensionsWriterImpl implements ExtensionsWriter {

        /**
         * Writes out a feature subclass to a given DataOutputStream.
         * 
         * @param out The OutputStream to write out the data to.
         * @param feature The user-defined Feature subclass to write out.
         * @throws java.io.IOException if IO error occurs.
         */
        public void writeExtensionFeature(GrinDataOutputStream out, Feature feature) throws IOException {
             if (feature instanceof Oval) {
                 Oval oval = (Oval) feature;
                 out.writeInt(oval.getX());
                 out.writeInt(oval.getY());
                 out.writeInt(oval.getWidth());
                 out.writeInt(oval.getHeight());
                 out.writeColor(oval.getColor());
             }
        }

        /**
         * Writes ou a modifier subclass to a given DataOutputStream.
         * @param out The OutputStream to write out the data to.
         * @param modifier The user-defined Modifier subclass to write out.
         * @throws java.io.IOException if IO error occurs.
         */
        public void writeExtensionModifier(GrinDataOutputStream out, Modifier modifier) throws IOException {
            // not used for this example
        }

        /**
         * Writes ou a command subclass to a given DataOutputStream.
         * @param out The OutputStream to write out the data to.
         * @param command The user-defined Command subclass to write out.
         * @throws java.io.IOException if IO error occurs.
         */
        public void writeExtensionCommand(GrinDataOutputStream out, Command command) throws IOException {
            // not used for this example
        }
        
    }
    
    class ExtensionsParserImpl implements ExtensionsParser {

        public Feature getFeature(Show show, String typeName, String name, Lexer lexer) throws IOException {

            if ("EXAMPLE:oval".equals(typeName)) {
                // arguments are - x, y, w, h, color_value ";", where color_value is "{" r g b a "}".
                int x = lexer.getInt();
                int y = lexer.getInt();
                int w = lexer.getInt();
                int h = lexer.getInt();

                lexer.parseExpected("{");
                int r = lexer.getInt();
                int g = lexer.getInt();
                int b = lexer.getInt();
                int a = lexer.getInt();
                lexer.parseExpected("}");

                lexer.parseExpected(";");

                Color color = AssetFinder.getColor(r, g, b, a);

                return new Oval(show, name, x, y, w, h, color);
            } 
            
            return null;
        }

        public Modifier getModifier(Show show, String typeName, String name, Lexer lexer) throws IOException {
            return null; // not used in this example
        }

        public Command getCommand(Show show, String typeName, Lexer lexer) throws IOException {
            return null; // not used in this example
        }
        
    }
}

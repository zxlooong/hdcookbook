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

package com.hdcookbook.grin.io.binary;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;
import java.io.IOException;

/**
 * ExtensionsReader defines the methods that handle the reading of the
 * custom (user-defined) GRIN features, modifiers and commands from an IO stream.  
 * These who are defining the custom GRIN subclasses should implement these methods accordingly.
 * 
 * @see GrinBinaryReader#GrinBinaryReader(java.io.InputStream, ExtensionsReader)
 * @see ExtensionsWriter
 */
public interface ExtensionsReader {
    
    /**
     * Reads in a feature subclass from a given DataInputStream.
     * 
     * @param show      The show that this feature belongs to.
     * @param name      The name of this feature.
     * @param in        The InputStream to read in the data from.
     * @param length    The number of bytes occupied in the InputStream to describe
     *                  the feature.
     *
     * @return Feature  A user-defined Feature subclass reconstructed from the data,
     *                  or null if none is found.
     * 
     * @throws java.io.IOException if IO error occurs.
     */
    public Feature readExtensionFeature(Show show, String name, 
            GrinDataInputStream in, int length) 
                              throws IOException;
    
    /**
     * Reads in a modifier subclass from a given DataInputStream.  Note that the child feature
     * this modifier will be working on is contructed before this method is invoked, and 
     * modifier.setup(Feature) will ve invoked after this method returns, hence the implementation
     * of this method does not have to deal with it.
     * 
     * @param show      The show that this modifier belongs to.
     * @param name      The name of this modifier.
     * @param in        The InputStream to read in the data from.
     * @param length    The number of bytes used in the InputStream to describe the modifier.
     * 
     * @return Modifier A user-defined Modifier subclass reconstructed from the data,
     *          or null if none is found.
     * 
     * @throws java.io.IOException if IO error occurs.
     */
    public Modifier readExtensionModifier(Show show, String name, 
            GrinDataInputStream in, int length) 
                                throws IOException;
    
    /**
     * Reads in a command subclass from a given DataInputStream.
     * 
     * @param show      The show that this command belongs to.
     * @param in        The InputStream to read in the data from.
     * @param length    The number of bytes used in the InputStream to describe this command.
     * 
     * @return Command  A user-defined Command subclass reconstructed from the data.
     *                  or null if none is found.
     * 
     * @throws java.io.IOException if IO error occurs.
     */    
    public Command readExtensionCommand(Show show,
            GrinDataInputStream in, int length) 
            throws IOException;

}

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

package net.java.bd.tools.logger;

import java.io.IOException;
import java.io.Writer;


/**
 * Writer to the harness log
 * 
 *
 */
public class LogWriter extends Writer {

    public final static int BUFFER_SIZE = 512;
    public final static String lineSep = System.getProperty("line.separator");
    
    private int lineSepCharCounter = 0; 
    private char[] buf = new char[BUFFER_SIZE];
    private int ptr = 0;
    
    public void close() throws IOException {
        flush();
    }

    public void flush() throws IOException {
        if (ptr > 0) {
            Logger.log(String.copyValueOf(buf, 0, ptr));
            ptr = 0;
        }
    }

    public void write(char[] cbuf, int off, int len) throws IOException {
        if (cbuf == null || off < 0 || len < 0 || off+len > cbuf.length) {
            return;
        }
        
        for(int i = 0; i < len; i++) {
            char c = cbuf[off + i];
            
            if (c == lineSep.charAt(lineSepCharCounter)) {
                if (lineSepCharCounter == lineSep.length()-1) {
                    flush();
                    lineSepCharCounter = 0;
                }
                else {
                    lineSepCharCounter++;
                }
            }
            else {
                if (lineSepCharCounter > 0) {
                    for(int j=0; j < lineSepCharCounter; j++) {
                        addToBuf(lineSep.charAt(j));
                    }
                
                    lineSepCharCounter = 0;
                }
                
                addToBuf(c);
            }
        }
    }

    
    private void addToBuf(char ch) throws IOException {
        buf[ptr++] = ch;
        if (ptr >= BUFFER_SIZE) {
            flush();
        }
    }
    
    
}

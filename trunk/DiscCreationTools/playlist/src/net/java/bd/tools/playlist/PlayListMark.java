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


package net.java.bd.tools.playlist;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import javax.xml.bind.annotation.XmlElement;

/**
 * BD-ROM 3-1 5.3.7 PlayListMark
 */
public class PlayListMark {
    
    private Mark[] marks = null;
    
    public void readObject(DataInputStream din) throws IOException {
        // 32 bit length
        // 16 bit number of playlist marks
        // for i = 0 ... N
        // Mark[i] (14 byte each)
        din.skipBytes(4);
        int length = din.readUnsignedShort();
        Mark[] lmarks = new Mark[length];
        for (int i = 0; i < length; i++) {
            lmarks[i] = new Mark(i);
            lmarks[i].readObject(din);
        }
        setMarks(lmarks);
    }
    public void writeObject(DataOutputStream dout) throws IOException {    
        Mark[] lmarks = getMarks();
  
        if (lmarks == null) {
           throw new IOException("There is a playlist without any mark.  There should be an entry mark at the beginning of the PlayList.");
        }
        
        dout.writeInt(14 * lmarks.length + 2);
        dout.writeShort(lmarks.length);
        for (int i = 0; i < lmarks.length; i++) {
            lmarks[i].writeObject(dout);
        }
    }

    @XmlElement(name="Mark")
    public Mark[] getMarks() {
        return marks;
    }

    public void setMarks(Mark[] marks) {
        for (int i = 0; i < marks.length; i++) {
            if (i != marks[i].getId()) {
                throw new IllegalArgumentException("id does not match the array order");
            }
        }         
        this.marks = marks;
    }
}

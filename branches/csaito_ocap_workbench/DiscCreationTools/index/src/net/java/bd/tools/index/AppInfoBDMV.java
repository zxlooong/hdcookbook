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

package net.java.bd.tools.index;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
//import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/*
 * BD-ROM Part 3-1 5.2.2 AppInfoBDMV
 */
public class AppInfoBDMV {
    
    byte[] contentProviderData = new byte[32];
    
    /**
     * Note: Commenting out to stop data represenatation to be in the xml file.
     * According to the spec, this field is for the Content Provider's use and
     * the field has no effect on BD-ROM player's behavior.
     * When we encounter an use case for the field, it can be added back.
     * 
    public void setContentProviderData(byte[] data) {
        if (data.length != 32) {
            throw new RuntimeException("AppInfo data is not 32 bytes " + data.length);
        }
        this.contentProviderData = data;
    }

    @XmlJavaTypeAdapter(HexStringBinaryAdapter.class)       
    public byte[] getContentProviderData() {
        return contentProviderData;
    }
    **/
    
    public void readObject(DataInputStream din) throws IOException {
        
        // 32 bit length (should be constant, 34)
        // 16 bit reserved
        // 8*32 user data
        
        din.skipBytes(6); // reserved
        din.read(contentProviderData);
    }
    
    public void writeObject(DataOutputStream dout) throws IOException {
        byte[] reserved = new byte[2];
        dout.writeInt(34);
        dout.write(reserved);
        dout.write(contentProviderData);
    }

}

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

package com.hdcookbook.grin.io.binary;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A convenience DatInputStream subclass that can handle certain Objects and 
 * Object arrays, including null.
 */
class GrinDataInputStream extends DataInputStream {
   
   public GrinDataInputStream(InputStream in) {
       super(in);
   }
   
   public Color readColor() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       int red = readInt();
       int green = readInt();
       int blue = readInt();
       int alpha = readInt();
       return new Color(red, green, blue, alpha);
   }
   
   public Rectangle readRectangle() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       double x = readDouble();
       double y = readDouble();
       double w = readDouble();
       double h = readDouble();
       return new Rectangle((int)x,(int)y,(int)w,(int)h);
   }
   
   public Rectangle[] readRectangleArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       Rectangle[] array = new Rectangle[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readRectangle();
       }
       return array;       
   }
   
   public Font readFont() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       String name = readUTF();
       int style = readInt();
       int size = readInt();
       return new Font(name, style, size);
   }  
   
   
   public int[] readIntArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       int[] array = new int[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readInt();
       }
       return array;
   }   


    public String readString() throws IOException {
	byte b = readByte();
	if (b == Constants.NULL) {
	    return null;
	} else {
	    return readUTF();
	}
    }

   public String[] readStringArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       String[] array = new String[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readUTF();
       }
       return array;
   }
}

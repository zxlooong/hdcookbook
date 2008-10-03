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

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.input.RCHandler;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * GrinDataOutputStream is a convenience DataOutputStream subclass 
 * that knows how to write out certain Objects and Object arrays, including null.
 * This class is used by the GrinBinaryWriter to write out information
 * about show nodes.
 * 
 * @see GrinBinaryWriter
 * @see GrinDataInputStream
 */

public class GrinDataOutputStream extends DataOutputStream {

   /**
    * An instance of the GrinBinaryWriter that this output stream
    * is working with.
    */
   private GrinBinaryWriter binaryWriter;
   
   /**
    * Constructs an instance of the GrinDataOutputStream that uses
    * the specified underlying output stream.
    * 
    * @param out The underlying OutputStream. 
    */
   GrinDataOutputStream(OutputStream out, GrinBinaryWriter writer) {
      super(out);
      
      this.binaryWriter = writer;
   }
   

   
   /**
    * Writes out a Color instance.
    * 
    * @param color  The color instance to write out.
    * @throws java.io.IOException if IO error occurs.
    */
   public void writeColor(Color color) throws IOException {
       if (color == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(color.getRed());
           writeInt(color.getGreen());
           writeInt(color.getBlue());
           writeInt(color.getAlpha()); 
       }    
   }
   
   /**
    * Writes out a Rectangle instance.
    * @param rect   The rectangle to write out.
    * @throws java.io.IOException if IO error occurs.
    */
   public void writeRectangle(Rectangle rect) throws IOException {
       if (rect == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);   
           writeDouble(rect.getX());
           writeDouble(rect.getY());
           writeDouble(rect.getWidth());
           writeDouble(rect.getHeight());
       }
   }
   
   /**
    * Writes out an array of Rectangles.
    * @param array An array of rectangles to write out.
    * @throws java.io.IOException if IO error occurs.
    */
   public void writeRectangleArray(Rectangle[] array) throws IOException {
       if (array == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(array.length);
           for (int i = 0; i < array.length; i++) {
               writeRectangle(array[i]);
           }    
       }       
   }
   
   /**
    * Writes out a Font instance.
    * @param font The font to write out.
    * @throws java.io.IOException if IO error occurs.
    */
   public void writeFont(Font font) throws IOException {
       if (font == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeUTF(font.getName());
           writeInt(font.getStyle());
           writeInt(font.getSize());   
       }    
   }  
   
   /**
    * Writes out an array of integers.
    * @param array An array of integers to write out.
    * @throws java.io.IOException if IO error occurs.
    */
   public void writeIntArray(int[] array) throws IOException {
       if (array == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(array.length);
           for (int i = 0; i < array.length; i++) {
               writeInt(array[i]);
           }    
       }
   }

   /**
    * Writes out a String instance.
    * @param string The String to write out.
    * @throws java.io.IOException if IO error occurs.
    */ 
   public void writeString(String string) throws IOException {
	if (string == null)  {
	    writeByte(Constants.NULL);
	} else {
	    writeByte(Constants.NON_NULL);
	    writeUTF(string);
	}
   }
  
   /**
    * Writes out an array of Strings.
    * @param array An array of Strings to write out.
    * @throws java.io.IOException if IO error occurs.
    */
   public void writeStringArray(String[] array) throws IOException {
       if (array == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(array.length);
           for (int i = 0; i < array.length; i++) {
	       if (array[i] == null) {
		   writeByte(Constants.NULL);
	       } else {
		   writeByte(Constants.NON_NULL);
		   writeUTF(array[i]);
	       }
           }
       }
   }

   /**
    * Writes out a reference of a Feature.  This method should be used
    * when the user is writing out an extension feature or command, and 
    * need to record about a feature that is referred by that extension.
    * 
    * @param feature The feature to write out.
    * @throws java.io.IOException if IO error occurs, or 
    *           if no such feature exists in the show that
    *           this GrinDataInputStream is working with.         
    */
   public void writeFeatureReference(Feature feature) throws IOException {
       
       int index = binaryWriter.getFeatureIndex(feature);      
       if (index < 0) {
	    throw new IOException("Invalid feature index");
       }
       
       writeInt(index);
   }
   
   /**
    * Writes out a reference of a segment.  This method should be used
    * when the user is writing out an extension feature or command, and 
    * need to record about a segment that is referred by that extension.
    * 
    * @param segment    The segment to write out.
    * @throws java.io.IOException if IO error occurs, or 
    *           if no such feature exists in the show that
    *           this GrinDataInputStream is working with.         
    */
   public void writeSegmentReference(Segment segment) throws IOException {
       
       int index = binaryWriter.getSegmentIndex(segment);      
       if (index < 0) {
	    throw new IOException("Invalid segment index");
       }
       
       writeInt(index);
   }   
   
   /**
    * Writes out a reference of an RCHandler.  This method should be used
    * when the user is writing out an extension feature or command, and 
    * need to record about an RCHandler that is referred by that extension.
    * 
    * @param  handler    The RCHandler to write out.
    * @throws java.io.IOException if IO error occurs, or 
    *           if no such RCHandler exists in the show that
    *           this GrinDataInputStream is working with.         
    */
   public void writeRCHandlerReference(RCHandler handler) throws IOException {
       
       int index = binaryWriter.getRCHandlerIndex(handler);      
       if (index < 0) {
	    throw new IOException("Invalid RCHandler index");
       }
       
       writeInt(index);
   }      
}

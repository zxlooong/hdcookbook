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

import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A tool that converts a text based GRIN script to the binary format.
 **/
public class BinaryConverter {
   
   /**
    * A driver method for the BinaryConverter.convert(String, String).
    * 
    * @param args  Arguments.  args[0] is  file name the text-based GRIN script to read.
    * 		    args[1] An optional file name for the binary-based GRIN script to write out.
    * @see #convert(String, String)
    **/
   public static void main(String[] args) {
       
        if (args == null || args.length == 0) {
            System.out.println("Missing an argument - need a grin script to parse");
            System.out.println("Syntax: com.hdcookbook.grin.io.binary.BinaryConverter text-based-grin-file [name-of-the-binary-file-to-create]");       
            return;
        }
        
        String textFile = args[0];
        String binaryFile;
        if (args.length > 1) {
            binaryFile = args[1];
        } else {
            binaryFile = null;
        }
        
        try {
           BinaryConverter.convert(textFile, binaryFile);
        } catch (IOException e) {           
            e.printStackTrace();
        }    
   }

   /**
    * Converts the text based GRIN script to a binary format. The GRIN script is searched from
    * the current directory and as an absolute path.
    *
    * @param textScriptName The GRIN text script file name to read in.  
    * @param binaryScriptName The GRIN binary script name to write out.
    *  If binaryScriptName is null or empty String, then the binary file will be named as 
    *  the textScriptName plus the ".grin" extension.
    */
   public static void convert(String textScriptName, String binaryScriptName) throws IOException {
       
       String fileName = null;
       
       try {
            
            GenericDirector director = new GenericDirector(textScriptName, null);
	    AssetFinder.setSearchPath(null, new File[]{new File("."), new File("")});
  
            SEShow show = director.createShow(null);
            
            if (binaryScriptName != null) {
               fileName = binaryScriptName;
            } else {   
               fileName = textScriptName;
               if (textScriptName.indexOf('.') != -1) {
                   fileName = textScriptName.substring(0, textScriptName.lastIndexOf('.'));
               }             
               fileName = fileName.concat(".grin");
            }   
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileName));
            
            GrinBinaryWriter out = new GrinBinaryWriter(show);
	    out.writeShow(dos);
            dos.close();
            
            if (Debug.ASSERT) {
               // A simple assertion test - check that the reader can read back
               // the binary file that just got generated without any error.
               DataInputStream in = new DataInputStream(new FileInputStream(fileName));
               GrinBinaryReader reader = new GrinBinaryReader(director, in);
	       Show recreatedShow = new Show(director);
               reader.readShow(recreatedShow);
            }
            
            return;
            
        } catch (IOException e) { 
            // failed on writing, delete the binary file
            if (fileName != null) {
                File file = new File(fileName);
                if (file.exists()) {
                   file.delete();
                }   
            }
            
            throw e;
        }
   }
}     

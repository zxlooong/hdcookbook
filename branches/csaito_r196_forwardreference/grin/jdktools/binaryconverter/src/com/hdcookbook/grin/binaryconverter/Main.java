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

package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.io.binary.*;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.text.ExtensionsParser;
import com.hdcookbook.grin.io.text.ShowParser;
import com.hdcookbook.grin.util.AssetFinder;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

/**
 * A tool that converts a text based GRIN script to the binary format.
 **/
public class Main {
   
   /**
    * A driver method for the Main.convert(String, String).
    * 
    * @param args   Arguments. Requires the name of the text-based GRIN script to read.
    * @see          #convert(String[], String, ExtensionsParser, boolean)
    **/
   public static void main(String[] args) {
       
        if (args == null || args.length == 0) {
            usage();
        }
        
        int index = 0;
        ArrayList assets = new ArrayList(); 
        assets.add(".");
        assets.add("");
        
        String textFile = null;
        String extensionParserName = null;
        boolean debug = false;
        
        while (index < args.length) {
            if ("-asset_dir".equals(args[index])) {
                index++;
                if (index == args.length) {
                    usage();
                }    
                assets.add(args[index]);
            } else if ("-debug".equals(args[index])){
                debug = true;
            } else if ("-extension_parser".equals(args[index])) {
                index++;
                if (index == args.length) {
                    usage();
                }                 
                extensionParserName = args[index];
            } else {
                if (textFile != null) {
                    usage();
                }
                textFile = args[index];
            }
            index++;
        }
        
        if (textFile == null) {
            usage();
        }
        
        ExtensionsParser extensionsParser = null;
        
        if (extensionParserName != null) {
            try {
                 extensionsParser = (ExtensionsParser) 
                         Class.forName(extensionParserName).newInstance();
            } catch (IllegalAccessException ex) {
                 ex.printStackTrace();
            } catch (InstantiationException ex) {
                 ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                 ex.printStackTrace();
            } 
        }
        
        try {
           Main.convert((String[])assets.toArray(new String[assets.size()]),
                                   textFile, extensionsParser, debug);
        } catch (IOException e) {           
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
   }

   /**
    * Converts the text based GRIN script to a binary format. 
    *
    * @param assetsDir  The asset directory to find the text script from.
    * @param textFile   The GRIN text script file name to read in.  
    * @param extensionsParser     The ExtensionsParser for writing
    * @param debug      If true, include debug information to generated
    * binary file
    */
   public static void convert(String[] assetsDir, String textFile, 
           ExtensionsParser extensionsParser, boolean debug) 
           throws IOException 
   {
       String fileNames[] = new String[3];
       
       String baseName = textFile;
       if (baseName.indexOf('.') != -1) {
           baseName = baseName.substring(0, baseName.lastIndexOf('.'));
       }
       
       try {
         
            ArrayList list = new ArrayList();
            for (int i = 0; i < assetsDir.length; i++) {      
                list.add(new File(assetsDir[i]));
            }
            
	    AssetFinder.setSearchPath(null, 
                    (File[])list.toArray(new File[list.size()]));
  
            SEShow show = createShow(textFile, extensionsParser);
              
            String fileName = baseName + ".grin";           
            fileNames[0] = fileName;
            
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(fileName));
            
            GrinBinaryWriter out = new GrinBinaryWriter(show, debug);
	    out.writeShow(dos);
            dos.close();
            
            fileNames[1] = baseName + ".xlet.java";
            out.writeCommandClass(show, true, fileNames[1]);
            
            fileNames[2] = baseName + ".grinview.java";
            out.writeCommandClass(show, false, fileNames[2]);
            
            return;
            
        } catch (IOException e) { 
            // failed on writing, delete the binary file
            for (int i = 0; i < fileNames.length; i++) {
                if (fileNames[i] != null) {
                    File file = new File(fileNames[i]);
                    if (file.exists()) {
                       file.delete();
                    }   
                }
            }
            
            throw e;
        }
   }
   
   private static SEShow createShow(String showName, ExtensionsParser extensionsParser) {
	SEShow show = new SEShow(null);
	URL source = null;
	BufferedReader rdr = null;
	try {
	    source = AssetFinder.getURL(showName);
	    if (source == null) {
		throw new IOException("Can't find resource " + showName);
	    }
	    rdr = new BufferedReader(
			new InputStreamReader(source.openStream(), "UTF-8"));
            ShowBuilder builder = new ShowBuilder();
            builder.setExtensionsParser(extensionsParser);
	    ShowParser p = new ShowParser(rdr, showName, show, builder);
	    p.parse();
	    rdr.close();
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.out.println();
	    System.out.println(ex.getMessage());
	    System.out.println();
	    System.out.println("Error trying to parse " + showName);
            System.out.println("    URL:  " + source);
	    System.exit(1);
	} finally {
	    if (rdr != null) {
		try {
		    rdr.close();
		} catch (IOException ex) {
		}
	    }
	}
        return show;
   }
       
   private static void usage() {
        System.out.println("Missing an argument - need a grin script to parse");
        System.out.println("Syntax: com.hdcookbook.grin.io.binary.Main " + "\n" +
                "\t[-asset_dir <directory>]\n\t[-extension_parser ExtensionsParserClassName]\n\t[-debug]\n\t" 
                + "text-based-grin-file");       
        System.exit(0);
   }
}     

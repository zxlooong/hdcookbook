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

import com.hdcookbook.grin.compiler.GrinCompiler;
import com.hdcookbook.grin.io.binary.*;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.text.ExtensionParser;
import com.hdcookbook.grin.io.text.ShowParser;
import com.hdcookbook.grin.util.AssetFinder;
import java.awt.Font;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A tool that converts a text based GRIN script to the binary format.
 **/
public class Main {
   
   /**
    * A driver method for the Main.convert(String, String).
    * 
    * @param args   Arguments. Requires the name of the text-based GRIN script to read.
    * @see          #convert(String[], File[], String, ExtensionParser, String, boolean, boolean)
    **/
   public static void main(String[] args) {
       
        if (args == null || args.length == 0) {
            usage();
        }
        
        int index = 0;
        LinkedList assetPathLL = new LinkedList();
        LinkedList assetDirsLL = new LinkedList();
        
        String textFile = null;
        String extensionParserName = null;
        String outputDir = null;
        boolean debug = false;
        boolean optimize = true;
        
        while (index < args.length) {
	    if ("-assets".equals(args[index])) {
		index++;
                String path = args[index];
                if (index == args.length) {
                    usage();
                }
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
		assetPathLL.add(path);
	    } else if ("-asset_dir".equals(args[index])) {
		index++;
                if (index == args.length) {
                    usage();
                }                
                String path = args[index];
		assetDirsLL.add(path);
            } else if ("-debug".equals(args[index])){
                debug = true;
            } else if ("-extension_parser".equals(args[index])) {
                index++;
                if (index == args.length) {
                    usage();
                }                 
                extensionParserName = args[index];
            } else if ("-out".equals(args[index])) {
                index++;
                if (index == args.length) {
                    usage();
                } 
                outputDir = args[index];
            } else if ("-avoid_optimization".equals(args[index])) {
                optimize = false;
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
        
        ExtensionParser extensionParser = null;
        
        if (extensionParserName != null && !"".equals(extensionParserName)) {
            try {
                 extensionParser = (ExtensionParser) 
                         Class.forName(extensionParserName).newInstance();
            } catch (IllegalAccessException ex) {
                 ex.printStackTrace();
            } catch (InstantiationException ex) {
                 ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                 ex.printStackTrace();
            } 
        }

        String[] assetPath = null;
        File[] assetDirs = null;
	if (assetDirsLL.size() > 0) {
	    assetDirs = new File[assetDirsLL.size()];
	    int i = 0;
	    for (Iterator it = assetDirsLL.iterator(); it.hasNext(); ) {
		File f = new File((String) it.next());
		assetDirs[i++] = f;
	    }
	}
        if (assetPathLL.isEmpty() && assetDirs.length == 0) {
            assetPath = new String[]{ "." }; // current dir
        } else {
            assetPath = (String[]) 
               assetPathLL.toArray(new String[assetPathLL.size()]);
        }
        
	AssetFinder.setHelper(new AssetFinder() {
	    protected void abortHelper() {
		System.exit(1);
	    }
            
            protected Font getFontHelper(String fontName, int style, int size) {
                // On JavaSE, one cannot easily load a custom font.
                // The font created here will have a different glyph from what's 
                // expected for the xlet runtime, but it will hold the correct
                // fontName, style, and size.
                return new Font(fontName, style, size);
            }
	});
       	AssetFinder.setSearchPath(assetPath, assetDirs);
        try {
           Main.convert(assetPath, assetDirs, textFile, 
                   extensionParser, outputDir, debug, optimize);
        } catch (IOException e) {           
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
   }

   /**
    * Converts the text based GRIN script to a binary format. 
    *
    * @param assets The path to the assets in a jarfile, which is used
    * as the first parameter to <code>AssetFinder.setSearchPath(String[], File[])</code>.
    * Could be null.
    * @param assetsDir  The path to the assets in the filesystem, which is 
    * used as the second parameter to  <code>AssetFinder.setSearchPath(String[], File[])</code>.
    * Could be null.
    * @param showFile   The GRIN text script file to read in.  
    * @param extensionParser     The ExtensionParser for handling extensions.
    * @param outputDir  The directory to output generated files.
    * @param debug      If true, include debug information to generated
    * binary file.
    * @param optimize   If true, apply optimization to the show object,
    * such as creating image mosaics.
    */
   public static void convert(String[] assets, File[] assetsDir, String showFile, 
           ExtensionParser extensionParser, String outputDir, 
           boolean debug, boolean optimize) 
           throws IOException 
   {   
       String baseName = showFile;
       if (baseName.indexOf('.') != -1) {
           baseName = baseName.substring(0, baseName.lastIndexOf('.'));
       }
       
       File[] files = new File[3];
       
       try {   
	    AssetFinder.setSearchPath(assets, assetsDir);
  
            ShowBuilder builder = new ShowBuilder();
            builder.setExtensionParser(extensionParser);         
            
            SEShow show = ShowParser.parseShow(showFile, null, builder);
            
            if (outputDir == null) {
                outputDir = "."; // current dir
            }
            
            if (optimize) {
               new GrinCompiler().optimizeShow(show, outputDir);
            }
            
            String fileName = baseName + ".grin";           
            files[0] = new File(outputDir, fileName);
            
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(files[0]));
            
            GrinBinaryWriter out = new GrinBinaryWriter(show, debug);
	    out.writeShow(dos);
            dos.close();
            
            files[1] = new File(outputDir, baseName + ".xlet.java");
            out.writeCommandClass(show, true, files[1]);

            files[2] = new File(outputDir, baseName + ".grinview.java");
            out.writeCommandClass(show, false, files[2]);
            
            return;
            
        } catch (IOException e) { 
            // failed on writing, delete the binary file
            for (File file: files) {
                if (file != null && file.exists()) {
                   file.delete();
                }
            }
            
            throw e;
        }
   }
   
   private static void usage() {
        System.out.println("Error in tools argument.\n");
        
        System.out.println("");
        System.out.println("This tool lets you create a binary show file " +
                "from a text show file, with possible compile time optimizations.");
        System.out.println("");
        System.out.println("Usage: com.hdcookbook.grin.io.binary.Main <options> <show file>");
        System.out.println("");
        System.out.println("\t<show file> should be a text based show file availale " +
                "in the assets search path.");
        System.out.println("");
        System.out.println("\t<options> can be:");
        System.out.println("\t\t-assets <directory>");
        System.out.println("\t\t-asset_dir <directory>");
        System.out.println("\t\t-extension_parser <a fully-qualified-classname>");
        System.out.println("\t\t-out <output_dir>");      
        System.out.println("\t\t-debug");
        System.out.println("\t\t-avoid_optimization");
        System.out.println("");
        System.out.println("\t-assets and -asset_dir may be repeated to form a search path.");
        System.out.println("\t-avoid_otimization prevents the conversion process from using " +
                "GrinCompiler methods.");
        System.out.println("\t-debug includes debugging information to the generated binary file.");
        
         
        System.exit(0);
   }
}     

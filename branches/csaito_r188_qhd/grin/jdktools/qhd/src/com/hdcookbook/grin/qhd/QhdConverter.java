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


package com.hdcookbook.grin.qhd;

import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.mosaic.MosaicMaker;
import com.hdcookbook.grin.io.ExtensionsBuilderFactory;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.binary.ExtensionsWriter;
import com.hdcookbook.grin.io.binary.GrinBinaryWriter;
import com.hdcookbook.grin.io.text.ShowParser;
import com.hdcookbook.grin.util.AssetFinder;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QhdConverter {
    
   private static String outputDir = null;
    
   public static void main(String[] args) {
        try {

            int index = 0;
            
            String showname = null;
            String asset_dir = null;
            String output_dir = null;
            String extensions = null;
            
            while (index < args.length) {
                if ("-asset_dir".equals(args[index])) {
                    index++;
                    asset_dir = args[index++];
                } else if ("-output_dir".equals(args[index])) {
                    index++;
                    output_dir = args[index++];
                } else if ("-extensions_factory".equals(args[index])) {
                    index++;
                    extensions = args[index++];
                } else {
                    showname = args[index++];
                }
            }

            ExtensionsBuilderFactory extensionsBuilderFactory = null;

            if (extensions != null) {
                try {
                    extensionsBuilderFactory = (ExtensionsBuilderFactory) Class.forName(extensions).newInstance();
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                } catch (InstantiationException ex) {
                    ex.printStackTrace();
                } catch (ClassNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
            
            if (asset_dir !=  null) {
                AssetFinder.setSearchPath(null, new File[]{new File(asset_dir)});
            }
            
            SEShow show = createShow(showname, extensionsBuilderFactory);

            QhdConverter converter = new QhdConverter(show, output_dir);
            show = converter.shrinknodes();

            writeShowInBinary(showname, show, extensionsBuilderFactory);

            return;
            
        } catch (IOException ex) {
            Logger.getLogger(QhdConverter.class.getName()).log(Level.SEVERE, null, ex);
        }
   }
   
   private SEShow show;
   
   public QhdConverter(SEShow show, String outputDir) {
       this.show = show;
       this.outputDir = outputDir;
   }
   
   public SEShow shrinknodes() throws IOException {

       MosaicMaker mm = new MosaicMaker(new SEShow[]{show}, new File(outputDir));
       
       show.accept(new QhdShowVisitor());
       
       mm.makeMosaics();
       mm.destroy();
       
       return show;
   }

   private static SEShow createShow(String showName, ExtensionsBuilderFactory extensionsFactory) {
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
            builder.setExtensionsBuilderFactory(extensionsFactory);
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
   
   private static void writeShowInBinary(
           String showName, SEShow show, 
           ExtensionsBuilderFactory extensionsFactory) 
           throws IOException {
       
       String baseName = showName;
       if (showName.indexOf('.') != -1) {
           baseName = showName.substring(0, showName.lastIndexOf('.'));
       }
       String fileName = baseName.concat(".grin");
       
       File outputFile = new File(outputDir, fileName);

       DataOutputStream dos = new DataOutputStream(new FileOutputStream(outputFile));

       ExtensionsWriter writer = null;
       if (extensionsFactory != null) {
           writer = extensionsFactory.getExtensionsWriter();
       }
       
       GrinBinaryWriter out = new GrinBinaryWriter(show, writer);
       out.writeShow(dos);
       out.writeCommandClass(show, true, new File(outputDir, baseName + ".xlet.java"));;
       out.writeCommandClass(show, false, new File(outputDir, baseName + ".grinview.java"));  
       dos.close();
   }   
}

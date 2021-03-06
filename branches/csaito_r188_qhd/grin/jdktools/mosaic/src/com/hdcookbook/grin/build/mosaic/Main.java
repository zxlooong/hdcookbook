
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

package com.hdcookbook.grin.build.mosaic;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.io.text.ShowParser;
import com.hdcookbook.grin.mosaic.MosaicMaker;
import com.hdcookbook.grin.util.AssetFinder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;

/**
 * This class has the main program for building the assets of a show
 * out to a mosaic.  Please see <a href="package.html">The package
 * documentation</a> for more details about this program.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
 public class Main {

    private static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.hdcookbook.grin.build.mosaic.Main \\");
	System.out.println("        -show <show_file> \\");
        System.out.println("        -assets <asset_dir>");
        System.out.println("        -out <output_dir>");
        System.out.println("");
        System.out.println("    -show and -assets may be repeated");
	System.out.println();
	System.exit(1);
    }
    
    public static void main(String[] args) {
        LinkedList assetPathLL = new LinkedList();
        LinkedList showLL = new LinkedList();
        File outputDir = null;
	int argsUsed = 0;
	while (argsUsed < args.length - 1) {
	    if ("-assets".equals(args[argsUsed])) {
		argsUsed++;
                String path = args[argsUsed];
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
		assetPathLL.add(path);
		argsUsed++;
	    } else if ("-show".equals(args[argsUsed])) {
		argsUsed++;
		showLL.add(args[argsUsed]);
		argsUsed++;
	    } else if ("-out".equals(args[argsUsed])) {
                if (outputDir != null) {
                    usage();
                }
		argsUsed++;
		outputDir = new File(args[argsUsed]);
		argsUsed++;
	    } else {
		break;
	    }
	}
        if (argsUsed != args.length) {
            usage();
        }
	String[] assetPath = (String[]) 
                    assetPathLL.toArray(new String[assetPathLL.size()]);
        String[] shows = (String[]) showLL.toArray(new String[showLL.size()]);
	if (outputDir == null || assetPath.length == 0 || shows.length == 0) {
	    usage();
	}
	if (!outputDir.isDirectory() && !outputDir.mkdir()) {
	    System.out.println("Couldn't create directory " + outputDir);
	    System.exit(1);
	}
        
	try {    
            SEShow[] showTrees = parseShow(assetPath, shows);
	    MosaicMaker mm = new MosaicMaker(showTrees, outputDir);
	    mm.makeMosaics();
            mm.destroy();
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
	System.exit(0);
    }
    
     
    public static SEShow[] parseShow(String[] assetPath, String[] shows) 
            throws IOException {
        
	ShowBuilder builder = new ShowBuilder();
        builder.setExtensionsBuilderFactory(new GenericExtensionsBuilderFactory());
	File [] fPath = new File[assetPath.length];
	for (int i = 0; i < fPath.length; i++) {
	    fPath[i] = new File(assetPath[i]);
	}
	AssetFinder.setSearchPath(null, fPath);
	SEShow[] seShows = new SEShow[shows.length];
        for (int i = 0; i < shows.length; i++) {
            Director director = new Director(){};
            seShows[i] = new SEShow(director);
            URL source = AssetFinder.getURL(shows[i]);
	    if (source == null) {
		throw new IOException("Can't find resource " + shows[i]);
	    }
	    BufferedReader rdr = null;
            try {
                rdr = new BufferedReader(
			new InputStreamReader(source.openStream(), "UTF-8"));
                ShowParser p = new ShowParser(rdr, shows[i], 
                                              seShows[i], builder);
                p.parse();
            } finally {
                if (rdr != null) {
                    rdr.close();
                }
            }
        }
        
        return seShows;

    }   
 }

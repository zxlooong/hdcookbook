
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

package com.hdcookbook.grin.test.bigjdk;

import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.Segment;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.IdentityHashMap;

import javax.swing.SwingUtilities;

/**
 * This is like GenericMain, and also includes a GUI to browse the
 * Show file and control show execution.  The GUI is equivalent to the
 * CLI provided by GenericMain.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class GuiGenericMain extends GenericMain {

    private IdentityHashMap lineNumberMap = new IdentityHashMap();
    private GuiGenericMainScreen screen;
    
    public GuiGenericMain() {
    }

    private void buildControlGUI(GuiShowBuilder builder, String showName) {
	screen = new GuiGenericMainScreen(this, builder.getShowTree(showName));
	screen.setNameText("GRIN show viewer:  " + showName);
	screen.setResultText("Double-click in the tree to activate a segment.");

	try {
	    String[] lines = readShowText(showName);
	    screen.setShowText(lines);
	} catch (IOException ex) {
	    System.out.println();
	    System.out.println("Error reading show:  " + ex);
	    System.out.println();
	    System.exit(1);
	}

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
		screen.pack();
                screen.setVisible(true);
		screen.setFpsText("" + getFps());
            }
        });
    }

    private String[] readShowText(String showName) throws IOException {
	URL source = AssetFinder.getURL(showName);
	if (source == null) {
	    throw new IOException("Can't find resource " + showName);
	}
	BufferedReader rdr 
	    = new BufferedReader(
		    new InputStreamReader(source.openStream(), "UTF-8"));
	LinkedList lines = new LinkedList();
	for (int lineNo = 1; ; lineNo++) {
	    String line = rdr.readLine();
	    if (line == null) {
		break;
	    }
	    String num;
	    if (lineNo < 10) {
		num = "    ";
	    } else if (lineNo < 100) {
		num = "   ";
	    } else if (lineNo < 1000) {
		num = "  ";
	    } else if (lineNo < 10000) {
		num = " ";
	    } else {
		num = "";
	    }
	    num = num + lineNo + ":   ";
	    lines.add(num + line);
	}
	rdr.close();
	return (String[]) lines.toArray(new String[lines.size()]);
    }

    void addLineNumber(Object obj, int line) {
	lineNumberMap.put(obj, new Integer(line));
    }

    int getLineNumber(Object o) {
	GuiShowBuilder.Node node = (GuiShowBuilder.Node) o;
	Object v = lineNumberMap.get(node.contents);
	if (v == null) {
	    return -1;
	} else {
	    return ((Integer) v).intValue();
	}
    }

    String getSegmentName(Object[] path) {
	for (int i = path.length - 1; i >= 0; i--) {
	    GuiShowBuilder.Node node = (GuiShowBuilder.Node) path[i];
	    if (node.contents instanceof Segment) {
		return ((Segment) node.contents).getName();
	    }
	}
	return null;
    }

    protected String setFps(float newFps) {
	String result = super.setFps(newFps);
	if (screen != null) {
	    screen.setFpsText("" + newFps);
	}
	return result;
    }


    private static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.hdcookbook.grin.test.bigjdk.GuiGenericMain \\");
	System.out.println("        -fps <number>");
        System.out.println("        -assets <asset_dir>");
        System.out.println("        -imagemap <mapfile>");
        System.out.println("        -background <image>");
        System.out.println("");
        System.out.println("    -assets may be repeated");
	System.out.println();
	System.exit(1);
    }

    public static void main(String[] args) {
        LinkedList assetPathLL = new LinkedList();
        String imageMap = null;
	String background = null;
	int argsUsed = 0;
	String fps = null;
	while (argsUsed < args.length - 1) {
	    if ("-fps".equals(args[argsUsed])) {
		argsUsed++;
                if (fps != null) {
                    usage();
                }
		fps = args[argsUsed];
		argsUsed++;
	    } else if ("-background".equals(args[argsUsed])) {
		argsUsed++;
                if (background != null) {
                    usage();
                }
		background = args[argsUsed];
		argsUsed++;
	    } else if ("-assets".equals(args[argsUsed])) {
		argsUsed++;
                String path = args[argsUsed];
                if (!path.endsWith("/")) {
                    path = path + "/";
                }
		assetPathLL.add(path);
		argsUsed++;
	    } else if ("-imagemap".equals(args[argsUsed])) {
                if (imageMap != null) {
                    usage();
                }
		argsUsed++;
		imageMap = args[argsUsed];
		argsUsed++;
	    } else {
		break;
	    }
	}
        if (argsUsed+1 != args.length) {
            usage();
        }
	String showFile = args[argsUsed++];
        String[] assetPath;
        if (assetPathLL.size() == 0) {
            assetPath = new String[] { "../test/assets/" };
        } else {
            assetPath = (String[]) 
                        assetPathLL.toArray(new String[assetPathLL.size()]);
        }
	AssetFinder.setHelper(new AssetFinder() {
	    protected void abortHelper() {
		System.exit(1);
	    }
	});
       	AssetFinder.setSearchPath(assetPath, null);
        if (imageMap != null) {
            AssetFinder.setImageMap(imageMap);
        }
	GuiGenericMain m = new GuiGenericMain();
	if (background != null) {
	    m.setBackground(background);
	}
	GuiShowBuilder builder = new GuiShowBuilder(m);
        m.init(showFile, builder);

	m.buildControlGUI(builder, showFile);
	if (fps != null) {
	    m.doKeyboardCommand("f " + fps); // set fps
	}

	m.inputLoop();
        
	System.exit(0);
    }
    
}

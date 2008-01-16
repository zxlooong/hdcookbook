
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

import com.hdcookbook.grin.io.ExtensionsBuilderFactory;
import com.hdcookbook.grin.io.binary.ExtensionsReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.IdentityHashMap;

import javax.swing.SwingUtilities;

/**
 * This is like GenericMain, and also includes a GUI to browse the
 * Show file and control show execution.  The GUI is equivalent to the
 * command line interface provided by GenericMain.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class GrinView extends GenericMain {

    private IdentityHashMap lineNumberMap = new IdentityHashMap();
    private GrinViewScreen screen;
    
    public GrinView() {
    }

    private void buildControlGUI(String showName) {
	screen = new GrinViewScreen(this, new ShowNode(show, showName));
	screen.setNameText("GRIN show viewer:  " + showName);
	screen.setResultText("Double-click in the tree to activate a segment.");

	try {
            String[] lines = readShowFile(showName);
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
		int x = 0;
		int y = getHeight() + FRAME_CHEAT;
		screen.setLocation(x, y);
                screen.setVisible(true);
		screen.setFpsText("" + getFps());
            }
        });
    }
    
    private String[] readShowFile(String showName) throws IOException {
        String[] lines;
        if (!showName.endsWith(".grin")) {
	    lines = readShowText(showName);
        } else {
            lines = readShowBinary(showName);
        }
        
        return lines;
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
    
    private String[] readShowBinary(String showName) throws IOException {
	URL source = AssetFinder.getURL(showName);
	if (source == null) {
	    throw new IOException("Can't find resource " + showName);
	}
        BufferedInputStream bis = new BufferedInputStream(source.openStream());
	LinkedList lines = new LinkedList();
        int ch;
        int count = 0;
        StringBuffer hexInts = new StringBuffer(); // Hex integer for each line
        StringBuffer content = new StringBuffer(); // hexInt in either char or '.' 
        for(;;) {
            ch = bis.read();
            int m = count % 16;
            if (m == 0) {
                if (ch == -1) {
                    break;
                }
                hexInts.append(toHex(count, 8) + ":  ");
            }
            if (m == 8) {
                hexInts.append(" ");
            }
            if (ch == -1) {
                hexInts.append("  ");
            } else {
                hexInts.append(toHex(ch, 2));
                if (ch >= 32 && ch < 127) {
                    content.append((char) ch);
                } else {
                    content.append('.');
                }
            }
            if (m == 15)  {
                hexInts.append("   ");
                hexInts.append(content);
                lines.add(hexInts.toString());
                hexInts = hexInts.delete(0, hexInts.length());
                content = content.delete(0, content.length());
            } else {
                hexInts.append(" ");
            }
            count++;
        }
              
	bis.close();
	return (String[]) lines.toArray(new String[lines.size()]);
    }

    private static String hexDigits = "0123456789abcdef";
    private static String toHex(int b, int digits) {
        if (digits <= 0) {
            throw new IllegalArgumentException();
        }
        String result = "";
        while (digits > 0 || b > 0) {
            result = hexDigits.charAt(b % 16) + result;
            b = b / 16;
            digits--;
        }
        return result;
    }
    
    int getLineNumber(Object o) {
	ShowNode node = (ShowNode) o;
	Object v = lineNumberMap.get(node.getContents());
	if (v == null) {
	    return -1;
	} else {
	    return ((Integer) v).intValue();
	}
    }

    String getSegmentName(Object[] path) {
	for (int i = path.length - 1; i >= 0; i--) {
	    ShowNode node = (ShowNode) path[i];
	    if (node.getContents() instanceof Segment) {
		return ((Segment) node.getContents()).getName();
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

    /**
     * @inheritDoc
     **/
    protected void waitForUser(String msg) {
	    // Make sure that there's no race condition where a button
	    // press happens after we set the button visible but before
	    // we get down into doWaitForUser
	synchronized(debugWaitingMonitor) {
	    screen.forceNextDrawButtonVisible(true);
	    screen.setResultText(msg);
	    doWaitForUser();
	}
    }

    /**
     * @inheritDoc
     **/
    public void debugDrawFrameDone() {
	if (getFps() == 0f) {
	    SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    advanceFrames(1);
		}
	    });
	}
    }

    private static void usage() {
	System.out.println();
	System.out.println("Usage:  java com.hdcookbook.grin.test.bigjdk.GrinView <option> <show file>\\");
	System.out.println("            <show file> can be a .grin binary file, or a text show file.");
	System.out.println("                        It is searched for in the asset search path.");

	System.out.println("            <options> can be:");
	System.out.println("                -fps <number>");
        System.out.println("                -assets <asset path in jar file>");
        System.out.println("                -asset_dir <directory in filesystem>");
        System.out.println("                -imagemap <mapfile>");
        System.out.println("                -background <image>");
        System.out.println("                -scale <number>");
        System.out.println("                -segment <segment name to activate>");
        System.out.println("                -extensions_factory <a fully qualified classname>");       
        System.out.println("                -extensions_reader <a fully qualified classname>");
        System.out.println("");
        System.out.println("            -assets and -asset_dir may be repeated to form a search path.lll");
	System.out.println();
	System.exit(1);
    }

    public static void main(String[] args) {
        LinkedList assetPathLL = new LinkedList();
        LinkedList assetDirsLL = new LinkedList();
        String imageMap = null;
	String background = null;
	int argsUsed = 0;
	String fps = null;
	String segment = null;
	String scaleDivisor = null;
        String extensionsFactoryName = null;
        String extensionsReaderName = null;
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
	    } else if ("-asset_dir".equals(args[argsUsed])) {
		argsUsed++;
                String path = args[argsUsed];
		assetDirsLL.add(path);
		argsUsed++;
	    } else if ("-imagemap".equals(args[argsUsed])) {
                if (imageMap != null) {
                    usage();
                }
		argsUsed++;
		imageMap = args[argsUsed];
		argsUsed++;
	    } else if ("-segment".equals(args[argsUsed])) {
		argsUsed++;
		if (segment != null) {
			usage();
		}
		segment = args[argsUsed];
		argsUsed++;
	    } else if ("-scale".equals(args[argsUsed])) {
		argsUsed++;
                if (scaleDivisor != null) {
                    usage();
                }
		scaleDivisor = args[argsUsed];
		argsUsed++;	
	    } else if ("-extensions_factory".equals(args[argsUsed])) {
                if (extensionsFactoryName != null) {
                    usage();
                }
		argsUsed++;
		extensionsFactoryName = args[argsUsed];
		argsUsed++;                
	    } else if ("-extensions_reader".equals(args[argsUsed])) {
                if (extensionsReaderName != null) {
                    usage();
                }
		argsUsed++;
		extensionsReaderName = args[argsUsed];
		argsUsed++; 
            } else {
		break;
	    }
	}
        if (argsUsed+1 != args.length) {
            usage();
        }
	String showFile = args[argsUsed++];
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
        if (assetPathLL.size() == 0) {
	    if (assetDirs == null) {
		assetPath = new String[] { "../test/assets/" };
	    }
        } else {
            assetPath = (String[]) 
                        assetPathLL.toArray(new String[assetPathLL.size()]);
        }
	AssetFinder.setHelper(new AssetFinder() {
	    protected void abortHelper() {
		System.exit(1);
	    }
	});
       	AssetFinder.setSearchPath(assetPath, assetDirs);
        if (imageMap != null) {
            AssetFinder.setImageMap(imageMap);
        }
	GrinView m = new GrinView();
	if (background != null) {
	    m.setBackground(background);
	}
	
	if (scaleDivisor != null) {
	    m.adjustScreenSize(scaleDivisor);
	}
        
        ExtensionsBuilderFactory factory = null;
        if (extensionsFactoryName != null) {
            if (showFile.endsWith(".grin")) {
                System.out.println("Warning: ExtensionsBuilderFactory " 
                        + extensionsFactoryName + " will not be used for " +
                        "displaying binary based GRIN show file.");
            }
            try {
                factory = (ExtensionsBuilderFactory)
                        Class.forName(extensionsFactoryName).newInstance();
            } catch (Exception e) {
                System.err.println("Error instanciating " + extensionsFactoryName);
                e.printStackTrace();
            }
        }
        
        ExtensionsReader reader = null;
        if (extensionsReaderName != null) {
            try {
                reader = (ExtensionsReader)
                        Class.forName(extensionsReaderName).newInstance();
            } catch (Exception e) {
                System.err.println("Error instanciating " + extensionsReaderName);
                e.printStackTrace();
            }
        }
        
        if (reader == null) {
            reader = new GenericExtensionsReader();
        }
        
	GuiShowBuilder builder = new GuiShowBuilder(m);
        builder.setExtensionsBuilderFactory(factory);
        builder.setExtensionsReader(reader);
        m.init(showFile, builder);

	m.buildControlGUI(showFile);
	if (fps != null) {
	    m.doKeyboardCommand("f " + fps); // set fps	 
	}
	if (segment != null) {
	    m.doKeyboardCommand("s " + segment); // activate segment
	}

	m.inputLoop();
        
	System.exit(0);
    }   
}

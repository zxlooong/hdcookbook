
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

import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Image;
import java.awt.Graphics2D; 
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.Transparency;
import java.awt.GraphicsConfiguration;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.AlphaComposite;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.net.URL;

import javax.swing.JFileChooser;
import javax.imageio.ImageIO;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.input.RCKeyEvent;
import com.hdcookbook.grin.test.RyanDirector;
import com.hdcookbook.grin.parser.ShowBuilder;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.ImageWaiter;

/**
 * This is a generic test program for exercising a show file.  It
 * accepts commands to boss the show around on stdin.  Probably
 * more interesting is the subclass of this, GrinView.
 * This command-line version came first, but I pretty quickly got
 * tired of it and made the GUI instead.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class GenericMain extends Frame implements Runnable {
    
    static int FRAME_CHEAT = 16;
    static int BUF_WIDTH = 1920;
    static int BUF_HEIGHT = 1080;

    protected Show show;
    private Scrollbar scrollbar;
    private GenericDirector director;
    
    private float fps = 24;	// Run at 24p by default
    private BufferedImage showBuffer = null;
    private Graphics2D showBufferGraphics = null;
    private BufferedImage paintBuffer = null;
    private BufferedImage nonTranslucentFix = null;
    private Graphics2D frameGraphics;
    private int frame;		// Current frame we're on
    private int skipToFrame = 0; // Frame to skip to as a result of user input
    private Object monitor = new Object();
    private long lastFrameTime;
    private Image background = null;
    
    private int scaleDivisor = 2;
    private int screenWidth= 1920 / scaleDivisor;
    private int screenHeight = 1080 / scaleDivisor;
    
    public GenericMain() {
    }

    protected void setBackground(String file) {
    	Toolkit tk = Toolkit.getDefaultToolkit();
	background = tk.createImage(file);
	ImageWaiter w = new ImageWaiter(background);
	if (!prepareImage(background, w)) {
	    w.waitForComplete();
	}
    }
    
    protected void adjustScreenSize(String scale) {
        try {
	   scaleDivisor = Integer.parseInt(scale);	
	} catch (NumberFormatException e) {
	   System.out.println("Could not reset the scaling factor " + scale);
	   return;
	}

        screenWidth= 1920 / scaleDivisor;
        screenHeight = 1080 / scaleDivisor;
    }
    
    
    protected void init(String showName, ShowBuilder builder) {

	director = new GenericDirector(showName);
	show = director.createShow(builder);
        show.initialize(this);

        int sbHeight = 0;
        scrollbar = new Scrollbar(Scrollbar.HORIZONTAL, 0, 10000, 0, 90000);
        sbHeight = scrollbar.getPreferredSize().height;
        if (sbHeight <= 0) {
            sbHeight = 14;
        }

        setBackground(Color.black);
        setLayout(null);
        setSize(screenWidth, screenHeight + FRAME_CHEAT + sbHeight);  
	// 720x576 is SD in Europe
        
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ex) {
                }
                System.exit(0);
            }
        });

        java.awt.event.KeyAdapter listener = new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                // ignored
            }
            public void keyPressed(java.awt.event.KeyEvent e) {
	    	int code = e.getKeyCode();
		// Translate F1..F4 into red/green/yellow/blue
		if (code >= e.VK_F1 && code <= e.VK_F4) {
		    code = code - e.VK_F1 + RCKeyEvent.KEY_RED.getKeyCode();
		} else if (code >= e.VK_NUMPAD0 && code <= e.VK_NUMPAD9) {
		    code = code - e.VK_NUMPAD0 + RCKeyEvent.KEY_0.getKeyCode();
		} else if (code == e.VK_F5) {
		    code =RCKeyEvent.KEY_POPUP_MENU.getKeyCode();
		}
		show.handleKeyPressed(code);
            }
            public void keyTyped(java.awt.event.KeyEvent e) {
            }
        };
        addKeyListener(listener);
	System.out.println("F1..F4 will generate red/green/yellow/blue, "
			   + "F5 popup_menu");
        if (scrollbar != null) {
            scrollbar.addKeyListener(listener);
        }
                MouseAdapter mouseL = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
		int x = e.getX() * scaleDivisor;
		int y = (e.getY() - FRAME_CHEAT) * scaleDivisor;
                show.handleMouseClicked(x, y);
            }
        };
        addMouseListener(mouseL);
        MouseMotionAdapter mouseM = new MouseMotionAdapter() {
            public void mouseMoved(MouseEvent e) {
		int x = e.getX() * scaleDivisor;
		int y = (e.getY() - FRAME_CHEAT) * scaleDivisor;
                show.handleMouseMoved(x, y);
            }
        };
        addMouseMotionListener(mouseM);
        setVisible(true);
    }

    protected float getFps() {
	synchronized(monitor) {
	    return fps;
	}
    }

    private void printHelpMessage() {
	System.out.println();
	System.out.println("Commands:  ");
	System.out.println("    f<number>    Set animation fps (0 is OK)");
	System.out.println("    s<segment>   Go to named segment");
	System.out.println("    +<number>    Advance that many frames");
	System.out.println("    +            Advance one frame");
	System.out.println("    ? or h       Get this help message");
	System.out.println();
	System.out.println("Currently displaying " + fps + " fps.");
	System.out.println();
    }

    protected void inputLoop() {
	try {
	    show.advanceToFrame(0);
	    BufferedReader in 
		= new BufferedReader(new InputStreamReader(System.in));
	    (new Thread(this)).start();
	    printHelpMessage();
	    for (;;) {
		String msg = null;
		String s = in.readLine();
		if (s == null) {
		    break;
		}
		msg = doKeyboardCommand(s);
		if (msg != null) {
		    System.out.println(msg);
		}
	    }
	} catch (InterruptedException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
    }

    public void snapshot() {
	BufferedImage snapshot;
	synchronized(monitor) {
	    snapshot = new BufferedImage(BUF_WIDTH, BUF_HEIGHT, 
				             BufferedImage.TYPE_INT_ARGB);
	    Graphics2D g = snapshot.createGraphics();
	    if (background == null) {
		g.setComposite(AlphaComposite.Src);
	    } else {
		g.setComposite(AlphaComposite.Src);
		g.drawImage(background, 0, 0, null);
		g.setComposite(AlphaComposite.SrcOver);
	    }
	    g.drawImage(showBuffer, 0, 0, null);
	}
	final BufferedImage snapshotF = snapshot;
	new Thread(new Runnable() {
	    public void run() {
		JFileChooser fc = new JFileChooser();
		fc.setDialogTitle("Save .png snapshot");
		int ret = fc.showSaveDialog(GenericMain.this);
		if (ret == JFileChooser.APPROVE_OPTION) {
		    File file = fc.getSelectedFile();
		    System.out.println("Saving PNG to " + file + "...");
		    try {
			boolean ok = ImageIO.write(snapshotF, "PNG", file);
			if (ok) {
			    System.out.println("Saved snapshot to " + file);
			} else {
			    System.out.println("**** Error writing to " + file);
			}
		    } catch (IOException ex) {
			ex.printStackTrace();
			System.out.println("**** Error writing to " + file);
		    }
		}
	    }
	}).start();
    }

    public String doKeyboardCommand (String s) {
	try {
	    if (s.startsWith("s")) {
		s = s.substring(1).trim();
		return gotoSegment(s);
	    } else if (s.startsWith("+")) {
		s = s.substring(1).trim();
		int num = 0;
		if ("".equals(s)) {
		    num = 1;
		} else {
		    try {
			num = Integer.parseInt(s);
		    } catch (NumberFormatException ex) {
			System.out.println(ex);
		    }
		}
		return advanceFrames(num);
	    } else if (s.startsWith("f")) {
		s = s.substring(1).trim();
		float newFps = 0f;
		try {
		    newFps = Float.parseFloat(s);
		} catch (NumberFormatException ex) {
		    System.out.println(ex);
		}
		return setFps(newFps);
	    } else {
		printHelpMessage();
		if ("?".equals(s) || "h".equals(s)) {
		    return null;
		} else {
		    return "Command \"" + s + "\" unrecognized.";
		}
	    }
	} catch (NumberFormatException ex) {
	    return ex.toString();
	}
    }

    protected String gotoSegment(String name) {
	Segment seg = show.getSegment(name);
	if (seg == null) {
	    return "No segment called \"" + name + "\".";
	} else {
	    show.activateSegment(seg);
	    return "Activating segment " + seg;
	}
    }
    
    protected String advanceFrames(int num) {
	synchronized(monitor) {
	    skipToFrame = frame - 1 + num;
	    monitor.notifyAll();
	    return "    Go from frame " + (frame-1) + " to " + skipToFrame;
	}
    }
    
    protected String setFps(float newFps) {
	synchronized(monitor) {
	    fps = newFps;
	    lastFrameTime = System.currentTimeMillis();
	    monitor.notifyAll();
	    return "    Set fps from " + fps + " to " + newFps;
	}
    }
    
    public void run() {
        try {
            doRun();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
    }

    private void doRun() throws InterruptedException {
	System.out.println("Starting frame pump...");
	GraphicsConfiguration con = getGraphicsConfiguration();
	if (con.getColorModel().getTransparency() != Transparency.TRANSLUCENT) {
	    nonTranslucentFix = con.createCompatibleImage(screenWidth, 
						    FRAME_CHEAT+screenHeight);
	}
	Graphics2D frameGr = (Graphics2D) getGraphics();
	BufferedImage sb = new BufferedImage(BUF_WIDTH, BUF_HEIGHT, 
					     BufferedImage.TYPE_INT_ARGB);
	Graphics2D bufGr = sb.createGraphics();
	Color transparentColor = new Color(0,0,0,0);
	bufGr.setColor(transparentColor);
	bufGr.fillRect(0,0,BUF_WIDTH, BUF_HEIGHT);
	frameGr.drawImage(showBuffer, 0, FRAME_CHEAT, 
			  screenWidth, FRAME_CHEAT + screenHeight,
			  0, 0, BUF_WIDTH, BUF_HEIGHT, this);
	Rectangle thisArea = new Rectangle();
	Rectangle lastArea = new Rectangle();
	Rectangle lastClip = new Rectangle();
	Rectangle showClip = new Rectangle(0, 0, BUF_WIDTH, BUF_HEIGHT);
	Font frameFont = Font.decode("Arial-BOLD-24");
	lastFrameTime = System.currentTimeMillis();
	frame = 0;
	int skippedFrames = 0;
	synchronized(monitor) {
	    showBuffer = sb;
	    showBufferGraphics = bufGr;
	    frameGraphics = frameGr;
	}
	for (;;) {
	    synchronized(monitor) {
		frame++;
		for (;;) {
		    if (Debug.LEVEL > 1 && frame % 100 == 0) {
			Debug.println(frame + " frames, " + skippedFrames
				      + " skipped.");
		    }
		    if (skipToFrame >= frame) {
			frame = skipToFrame;
			lastFrameTime = System.currentTimeMillis();
			break;
		    }
		    if (fps <= 0.0) {
			doRepaint();
			    // We do a repaint, in case fps was just
			    // set to 0.0.  Sometimes this will be
			    // gratituous, but we double-buffer
			    // so it'll look OK, and an extra repaint
			    // when we're stopped anyway is fine.
			monitor.wait();
		    } else {
			int msPerFrame = ((int) (0.5 + 1000.0 / fps));
			long delta = System.currentTimeMillis();
			delta = delta - lastFrameTime - msPerFrame;
			if (delta < 0) {
			    monitor.wait(-delta);
			    delta = System.currentTimeMillis();
			    delta = delta - lastFrameTime - msPerFrame;
			    if (delta >= 0) {
				lastFrameTime += msPerFrame;
				break;
			    }
			} else if (delta > msPerFrame) {
			    // We've fallen behind, skip a frame
			    skippedFrames++;
			    frame++;
			    lastFrameTime += msPerFrame;
			} else {
			    break;
			}
		    }
		}
		show.advanceToFrame(frame);
		synchronized(show) {
		    show.setDisplayArea(thisArea, lastArea, showClip);
		    if (thisArea.width > 0) {
			lastClip.setBounds(showClip);
			bufGr.getClipBounds(lastClip);
			bufGr.setClip(thisArea);
			bufGr.setComposite(AlphaComposite.Src);
			bufGr.setColor(transparentColor);
			bufGr.fillRect(thisArea.x, thisArea.y, 
				       thisArea.width, thisArea.height);
			bufGr.setComposite(AlphaComposite.SrcOver);	
			show.paintFrame(bufGr);
			bufGr.setClip(lastClip);
		    }
		}
		if (fps > 0.0) {
		    paint(frameGr);
		    // if fps is 0, doRepaint() will be called, above.
		}
		// This doesn't optimize drawing only the changed part
		// That's hard to do with scaling, and performance
		// isn't critical here anyway.!
	    }
	}
    }

    private void doRepaint() {
	if (isDoubleBuffered()) {
	    repaint();
	} else {
	    if (paintBuffer == null) {
		paintBuffer = getGraphicsConfiguration()
				.createCompatibleImage(screenWidth, 
					    FRAME_CHEAT+screenHeight);
	    }
	    synchronized(monitor) {
		if (frameGraphics == null) {
		    return;
		}
		Graphics2D g = paintBuffer.createGraphics();
		g.setComposite(AlphaComposite.Src);
		paint(g);
		g.dispose();
		frameGraphics.setComposite(AlphaComposite.Src);
		frameGraphics.drawImage(paintBuffer, 0, 0, this);
		Toolkit.getDefaultToolkit().sync();
	    }
	}
    }
    
    public void paint(Graphics gArg) {
	Graphics2D g = (Graphics2D) gArg;
	Graphics2D fixG = null;
	synchronized(monitor) {
	    if (showBuffer == null) {
		return;
	    }
	    if (background == null || fps > 0.0) {
		if (nonTranslucentFix == null) {
		    g.setComposite(AlphaComposite.Src);
		} else {
			// On windows, the graphics device doesn't
			// natively support a translucent color model.
			// This means that alpha-blended colors don't
			// show up properly, unless we SrcOver draw them
			// over a background.  That burns another
			// framebuffer:  nonTranslucentFix.
		    fixG = g;
		    g = nonTranslucentFix.createGraphics();
		    g.setComposite(AlphaComposite.Src);
		    g.setColor(Color.black);
		    g.fillRect(0, 0, nonTranslucentFix.getWidth(),
		    		     nonTranslucentFix.getHeight());
		    g.setComposite(AlphaComposite.SrcOver);
		}
	    } else {
		g.setComposite(AlphaComposite.Src);
		g.drawImage(background, 0, FRAME_CHEAT, 
				     screenWidth, FRAME_CHEAT+screenHeight,
				     0, 0, BUF_WIDTH, BUF_HEIGHT, this);
		g.setComposite(AlphaComposite.SrcOver);
	    }
	    g.drawImage(showBuffer, 0, FRAME_CHEAT, 
				 screenWidth, FRAME_CHEAT+screenHeight,
				 0, 0, BUF_WIDTH, BUF_HEIGHT, this);
	    if (fixG != null)  {
		g.dispose();
		g = fixG;
		g.drawImage(nonTranslucentFix, 0, 0, null);
	    }
	}
    }
    
    public static void main(String[] args) {
	String[] path = new String[] { "/assets/" };
	AssetFinder.setSearchPath(path, null);
	GenericMain m = new GenericMain();
        m.init(args[0], null);

	m.inputLoop();
        
	System.exit(0);
    }
    
}

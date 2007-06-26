
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


package com.hdcookbook.bookmenu.menu;

import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.Show;

import java.awt.Rectangle;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.AlphaComposite;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

/**
 * Singleton worker object.  This is the main control thread for the
 * xlet.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class MenuWorker implements Runnable {

    //
    // If our HScene is double-buffered, it might be advantageous
    // to do repaint draw.  This boolean lets us pick.
    //
    private final static boolean REPAINT_DRAW_ENABLED = false;

    private final static int FPS = 24;
    private static final int STATE_NOT_STARTED = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_STOPPING = 2;
    private static final int STATE_STOPPED = 3;
    private int state = STATE_NOT_STARTED;

    private MenuXlet xlet;
    private Show show = null;
    private int frame = 0;
    private long firstFrameTime = Long.MAX_VALUE;
    private int skippedFrames = 0;

    private Rectangle thisArea;
    private Rectangle lastArea;
    private Rectangle lastClip;
    private Rectangle showClip;
    private Graphics2D frameGr;
    private BufferedImage buffer = null;  // Null if our HScene is double buffered
    private Graphics2D bufGr;
    private Color transparent = new Color(0,0,0,0);

    public MenuWorker(MenuXlet xlet) {
	this.xlet = xlet;
    }

    /**
     * Main control loop of the xlet.
     **/
    public void run() {
	try {
	    setState(STATE_RUNNING);
	    init();
	    for (;;) {
		long currTime;
		for (;;) {
		    synchronized(this) {
			if (state != STATE_RUNNING) {
			    return;	// Does finally action
			} 
			long nextFrameTime 
				= firstFrameTime + ((frame*1000L) / FPS);
			currTime = System.currentTimeMillis();
			long delta = nextFrameTime - currTime;
			if (delta <= 0) {
			    break;
			} else {
			    wait(delta);
			}
		    }
		}
		advanceShow(currTime);
	    }
	} catch (InterruptedException ex) {
	    // Player is trying to terminate us or show has been destroyed
	    Thread.currentThread().interrupt();
	} finally {
	    // Make sure the screen gets erased when the xlet terminates
	    try {
		if (frameGr != null) {
		    frameGr.setColor(transparent);
		    frameGr.fillRect(0, 0, 1920, 1080);
		    Toolkit.getDefaultToolkit().sync();
		}
	    } catch (Throwable ignored) {
	    }
	    setState(STATE_STOPPED);
	}
    }

    /**
     * Called by the xlet when it's ready for us to start running
     * the show.
     **/
    public synchronized void runShow(Show show) {
	this.show = show;
	frame = 0;
	firstFrameTime = System.currentTimeMillis();
	thisArea = new Rectangle();
	lastArea = new Rectangle();
	lastClip = new Rectangle();
	showClip = new Rectangle(0, 0, 1920, 1080);
	frameGr = (Graphics2D) xlet.scene.getGraphics();

	if (REPAINT_DRAW_ENABLED && xlet.scene.isDoubleBuffered()) {
	    xlet.scene.repaint();
	} else {
	    buffer = xlet.scene.getGraphicsConfiguration()
			 .createCompatibleImage(1920, 1080);
	    bufGr = buffer.createGraphics();
	    frameGr.setColor(transparent);
	    frameGr.fillRect(0, 0, 1920, 1080);
	    Toolkit.getDefaultToolkit().sync();
	}
	notifyAll();
    }

    private void advanceShow(long currTime) throws InterruptedException {
	int nextFrame = (int)((currTime - firstFrameTime) * (long) FPS / 1000L);
	if (nextFrame < frame) {
	    if (Debug.LEVEL > 0) {
		Debug.println("*** Frame pump:  frame got ahead of time!");
		return;
	    }
	}
	if (Debug.LEVEL > 0) {
	    skippedFrames += (nextFrame - frame);
	}
	while (nextFrame >= frame) {
	    if (nextFrame == frame) {
		show.setCaughtUp();
	    }
	    show.advanceToFrame(frame);
	    frame++;
	    if (Debug.LEVEL > 0 && frame % 100 == 0) {
		Debug.println("Frame "+frame+", "+skippedFrames + " skipped.");
	    }
	}
	synchronized(show) {
	    show.setDisplayArea(thisArea, lastArea, showClip);
	    if (buffer != null && thisArea.width > 0) {
		lastClip.setBounds(showClip);
		bufGr.getClipBounds(lastClip);
		bufGr.setClip(thisArea);
		bufGr.setComposite(AlphaComposite.Src);
		bufGr.setColor(transparent);
		bufGr.fillRect(thisArea.x, thisArea.y, 
			       thisArea.width, thisArea.height);
		// Can set bufGr to SrcOver here if we want that in show
		show.paintFrame(bufGr);
		bufGr.setClip(lastClip);
	    }
	}
	Rectangle a = thisArea;
	if (buffer == null)  {
	    xlet.scene.repaint(a.x, a.y, a.width, a.height);
	} else {
	    frameGr.setComposite(AlphaComposite.Src);
	    frameGr.drawImage(buffer, a.x, a.y, a.x+a.width, a.y+a.height,
	                              a.x, a.y, a.x+a.width, a.y+a.height, null);
	    Toolkit.getDefaultToolkit().sync();
	}
    }

    /**
     * Paint the show as the result of an external request,
     * like from Component.paint() after a repaint.  Unless we're
     * doing repaint draw, this should happen rarely.  We should
     * only consider repaint draw when our HScene is double-buffered
     * for us.
     **/
    public void paintShow(Graphics2D g) throws InterruptedException {
	Show s;
	synchronized(this) {
	    s = show;
	}
	if (s != null) {
	    synchronized(s) {
		s.paintFrame(g);
	    }
	}
    }

    private void init() {
	xlet.initFromWorker();
    }

    private synchronized void setState(int s) {
	state = s;
	notifyAll();
	if (Debug.LEVEL > 1) {
	    Debug.println("****  Menu worker state set to " + s);
	}
    }

    /**
     * Wait until the show has reached the started state.
     **/
    public synchronized void waitUntilStarted() {
	if (Debug.ASSERT && state != STATE_RUNNING
		&& state != STATE_NOT_STARTED) 
	{
	    Debug.assertFail();
	}
	try {
	    while (state  == STATE_NOT_STARTED) {
		wait();
	    }
	} catch (InterruptedException ex) {
	    Thread.currentThread().interrupt();
	    // ignored - player must be trying to shut us down
	}
    }

    /**
     * Terminate the thread that's running us, and wait until it
     * has terminated.
     **/
    public synchronized void destroy() {
	if (state != STATE_STOPPED) {
	    if (Debug.ASSERT && state != STATE_RUNNING) {
		Debug.assertFail();
	    }
	    state = STATE_STOPPING;
	    notifyAll();
	}
	try {
	    while (state != STATE_STOPPED) {
		wait();
		// We could use Thread.join() instead.  It's really the
		// same thing.
	    }
	} catch (InterruptedException ex) {
	    Thread.currentThread().interrupt();
	    // ignored - player must be trying to shut us down
	}
    }
}

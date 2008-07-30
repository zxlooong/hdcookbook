
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

package com.hdcookbook.grin.animator;

import com.hdcookbook.grin.util.Debug;

/**
 * Abstract base class for a clock-based animation engine.  A clock-based
 * engine uses System.currentTimeMillis and Object.wait() to pace the
 * animation to real time.
 **/

public abstract class ClockBasedEngine extends AnimationEngine {

    private boolean paused = true;
    private int newFps = 24000;
    	// Used to change the frame rate - see setFps.
    private static int NEW_FPS_SKIP_REQUEST = -1;  // see skipFrames(int)
    private static int NEW_FPS_SKIPPED = -2;	   // see skipFrames(int)
    private static int INVALID_FPS = -3;	   // see skipFrames(int)

    /**
     * Create a new ClockBasedEngine
     **/
    protected ClockBasedEngine() {
    }

    /**
     * @inheritDoc
     **/
    public synchronized void start() {
	paused = false;
	notifyAll();
    }

    /**
     * @inheritDoc
     **/
    public synchronized void pause() {
	paused = true;
	notifyAll();
    }

    /**
     * Set the frame rate of the animation, in 1001sts of a second.
     * In other words, the following table applies:
     * <pre>
     *
     *     Desired framerate    fps value
     *     =================    =========
     *           23.976	 	  24,000
     *           24.000           24,024
     *           29.970		  30,000
     *           30.000		  30,030
     *           59.940           60,000
     *           60.000           60,060
     * 
     * </pre>
     * If you find yourself asking "why 1001," read the HD Handbook starting
     * from page 2-2 (ISBN 978-0-07-149585-1).  Long story short:  NTSC video's
     * frame rate is 30*1000/1001 frames/second, due to issues around the
     * transition from B&W to color.  Fun fact:  1001 isn't prime,
     * it's 7*11*13
     * <p>
     * The frame rate must be greater than 0 fps.  To stop the animation,
     * call pause().
     * <p>
     * If the animation is paused when this method is called, it will
     * stay paused.
     *
     * @throws IllegalArgumentException  if fps < 0.
     *
     * @see #skipFrames(int)
     **/
    public synchronized void setFps(int fps) {
	if (fps <= 0) {
	    throw new IllegalArgumentException();
	}
	newFps = fps;
	notifyAll();
    }

    /**
     * Skip ahead the given number of frames.  After skipping ahead, the
     * framerate will be restored.  If the animation is paused, 
     * the current
     * state of the model will be output as a frame.  If two threads call
     * this method concurrently on the same instance, one will bail out
     * early.  A concurrent call to setFps() will cause this method to bail 
     * out as well.
     * <p>
     * This method is probably only useful for debugging.
     *
     * @param num	The number of frames to skip
     **/
    public synchronized void skipFrames(int num) throws InterruptedException {
	int oldNewFps;
	synchronized(this) {
	    if (newFps == NEW_FPS_SKIP_REQUEST || newFps == NEW_FPS_SKIPPED) {
		return;
	    }
	    oldNewFps = newFps;
	    for (int i = 0; i < num; i++) {
		newFps = NEW_FPS_SKIP_REQUEST;
		notifyAll();
		while (newFps == NEW_FPS_SKIP_REQUEST) {
		    if (destroyRequested()) {
			return;
		    }
		    wait();
		}
		if (newFps != NEW_FPS_SKIPPED) {
		    return;
		}
	    }
	    newFps = oldNewFps;
	    notifyAll();
		// if num > 0, then fps is < 0.  oldNewFps is >= 0, so
		// we are sure that newFps != fps.
	}
    }


    /**
     * @inheritDoc
     **/
    protected void runAnimationLoop() throws InterruptedException {
	//
	// This is a little long for a single method, but that lets us keep
	// the variables local.
	//

	// The int frame number wraps after about 2.8 years, so we do
	// take care of this case.  For the time in miliseconds, which we
	// further multiply by 1001, 2^63 / (1000 * 1001) seconds is
	// 300,000 years, so we don't worry about that wrapping.
	// startFrame and skippedFrames might wrap, but they're just
	// for debugging messages, so we don't care.

	long startFrameTime = System.currentTimeMillis();
	long nextFrameTime = startFrameTime;
	int fps = newFps; 
	int frame = 0;
		// Adjusted for units, the following invariant always holds:
		//    nextFrameTime = startFrameTime + frame / fps,
	int startFrame = 0;    // frame # at startFrameTime, used only for debug
	int skippedFrames = 0; // used only for debug
	long currTime;
	boolean wasPaused = false;

	for (;;) {
	    checkNewClients();

	    synchronized_block:  synchronized(this) {

		// If we're being destroyed, bail out of the loop
		//
		if (destroyRequested()) {
		    return;	// Note the finally clause below
		}
		if (Thread.interrupted()) {
		    throw new InterruptedException();
		}

		// Handle the paused state
		if (paused && newFps != NEW_FPS_SKIP_REQUEST) {
		    wasPaused = true;
		    wait();
		    continue;
		}


		// Handle frame wrapping.  At 24fps, this comes after
		// 2.8 years of animation.
		//
		if (frame == Integer.MAX_VALUE) {
		    startFrameTime = nextFrameTime;
		    if (Debug.LEVEL > 0) {
			startFrame += frame;
		    }
		    frame = 0;
		    continue;
		}

		// Handle setting a new fps value, or coming out of
		// paused.  We do this by
		// resetting startFrameTime to the current time,
		// and resetting the frame count down to zero.
		//
		if (wasPaused || newFps != fps) {
		    startFrameTime = System.currentTimeMillis();
		    	// The next frame will come immediately, even if this
			// is a bit early.  Changing the frame rate is
			// rare, and is probably only done while debugging.
		    if (Debug.LEVEL > 0) {
			startFrame += frame;
		    }
		    if (newFps == NEW_FPS_SKIP_REQUEST) {
			    // Tell skipFrames that we're honoring request
			newFps = NEW_FPS_SKIPPED;
			notifyAll();
			while (newFps == NEW_FPS_SKIPPED) {
			    wait();
			}
			    // Now, skipFrames has restored newFps value
			fps = INVALID_FPS;   
			    // So after skipping, we'll re-set loop
			if (newFps == NEW_FPS_SKIP_REQUEST) {
			    currTime = Long.MAX_VALUE;  
			    	// force skipping of frame, no paint
			} else {
			    currTime = Long.MAX_VALUE - 1L;
			    	// force skipping of frame, but paint
			}
			break synchronized_block;
				// jump down to the end of the synchronized
				// block, so we can call advanceModel() after
				// the monitor is released.  See also
				// skipFrames().
		    } else {
			wasPaused = false;	// We know paused is false here
			fps = newFps;
			frame = 0;
			nextFrameTime = startFrameTime;
			continue;
		    }
		}

		currTime = System.currentTimeMillis();
		
		// If we're ahead, wait, and then go back to the
		// beginning of the loop to detect if our state has
		// changed (e.g. by a pending request to terminate the
		// thread, or set the fps value).
		//
		if (currTime < nextFrameTime) {
		    wait(nextFrameTime - currTime);	// can be interrupted
		    continue;
		}
	    }  // end of synchronized block

	    //
	    // Now, advance the model to the next frame.
	    //
	    advanceModel();
	    frame++;
	    if (fps <= 0) {
		nextFrameTime = Long.MAX_VALUE;
	    } else {
		nextFrameTime = startFrameTime + 
				    (frame * 1000L * 1001L) / ((long) fps);
		    // startFrameTime + frame / fps, adjusted for units
	    }
	    if (Debug.LEVEL > 0 && (frame % 100) == 0) {
		Debug.println("Frame " + (frame + startFrame) + ", "
			      + skippedFrames + " skipped.");
	    }

	    // 
	    // If we're behind, skip a frame
	    //
	    if (currTime >= nextFrameTime) {
		if (Debug.LEVEL > 0) {
		    skippedFrames++;
		}
		continue;
	    }

	    showFrame();
	}
    }

}

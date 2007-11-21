
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
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Abstract base class for an animation engine.
 * <p>
 * An animation engine runs the main animation loop, and manages
 * display of a set of animation clients to a component.  
 **/

public abstract class AnimationEngine implements Runnable {

    /**
     * A useful constant that's used extensively by engines
     **/
    public final static Color transparent = new Color(0, 0, 0, 0);

    /** 
     * The list of areas that were updated in the current frame
     * of animation.
     **/
    protected RenderArea[] targets;

    private AnimationClient[] clients;
    private Thread worker;

    private static final int STATE_NOT_STARTED = 0;
    private static final int STATE_RUNNING = 1;
    private static final int STATE_STOPPING = 2;
    private static final int STATE_STOPPED = 3;
    private int state = STATE_NOT_STARTED;

    private Object repaintLock = new Object();

    private AnimationContext context;	// see initialize(), start(), run()
    private Rectangle lastClip = new Rectangle(); // see paintFrame
    private Rectangle collapsed = new Rectangle(); // see collapseTargets()

    private boolean needsFullPaint = true;	// First frame painted fully
    private boolean paused = false;


    /**
     * Initialize a new AnimationEngine.
     * <p>
     * Note that the algorithm used to collapse render area targets
     * is cubic with the number of targets, so this number should be
     * kept low.  Between one and three would be reasonable, and five
     * is perhaps a workable maximum.
     **/
    protected AnimationEngine() {
	worker = new Thread(this, "Animation " + this);
	worker.setPriority(Thread.NORM_PRIORITY - 1);
    }

    /**
     * Initialize this engine with the animation clients it will be
     * managing.  This should be called exactly once, before 
     * AnimationContext.animationInitialize() completes.  It's fine
     * to call it before starting the animation thread, too.
     *
     * @param clients		The animation clients we'll support
     *
     * @see com.hdcookbook.grin.animator.AnimationContext#animationInitialize()
     **/
    public synchronized void initClients(AnimationClient[] clients) {
	this.clients = clients;
    }

    /**
     * Initialize this engine with the number of render area targets
     * that clients will want to use.
     * This should be called exactly once, before start() is called.
     *
     * @param numRenderAreas	Number of render areas needed by client
     **/
    public synchronized void initNumTargets(int numRenderAreas) {
	targets = new RenderArea[numRenderAreas];
	for (int i = 0; i < targets.length; i++) {
	    targets[i] = new RenderArea(this);
	}
    }


    /**
     * Set the priority of the thread that runs the animation loop.
     * The default values is 4, that is, Thread.NORM_PRIORITY-1.
     * The animation loop should run at less than NORM_PRIORITY, to
     * make sure that higher-priority activities, like remote control
     * keypresses, take precidence.
     **/
    public void setThreadPriority(int priority) {
	worker.setPriority(priority);
    }

    /**
     * Initialize this engine with its parent container and the
     * position and size of the engine within the container.
     * This should be called exactly once, before start() is called.
     * A good time to call this would be in the animationInitialize() call
     * within the context code passed to initialize().
     * <p>
     * The Component will be set to the specified bounds, and an internal
     * double buffer may be created to these bounds.  The container
     * is expected to have a null layout; if it doesn't, this might
     * change the widget's size to be different than the bounds passed
     * in (and therefore the double buffer).  The container must be
     * visible when this is called.
     *
     * @see #initialize(com.hdcookbook.grin.animator.AnimationContext)
     * @see com.hdcookbook.grin.animator.AnimationContext#animationInitialize()
     **/
    public abstract void initContainer(Container container, Rectangle bounds);

    /**
     * Get the width of the area we display over
     *
     * @return the width
     **/
    public abstract int getWidth();

    /**
     * Get the height of the area we display over
     *
     * @return the height 
     **/
    public abstract int getHeight();

    private synchronized void setState(int s) {
        state = s;
        notifyAll();
        if (Debug.LEVEL > 1) {
            Debug.println("****  Animation " + this + " state set to " + s);
        }
    }

    /**
     * Tells us if there has been a request to destroy this animation
     * manager.  Any activity in the animation thread that takes more
     * than about a tenth of a second should poll this method, and
     * bail out if needed.
     **/
    public synchronized boolean destroyRequested() {
	return state != STATE_RUNNING && state != STATE_NOT_STARTED;
    }

    /**
     * Throw an InterruptedException if we should bail out.  This will
     * happen if destroyRequested(), or if Thread.interrupted().  This
     * is a convenience method.  It's good to call this regularly
     * during time-consuming operations in the animatino thread.
     **/
    public void checkDestroy() throws InterruptedException {
	if (Thread.interrupted() || destroyRequested()) {
	    throw new InterruptedException();
	}
    }

    /**
     * Get the component that this animation engine renders into.  An
     * xlet using this framework must add this component into the
     * component hierarchy of the xlet and make it visible before
     * the context's animationInitialize() code passed to initialize() 
     * has completed.
     *
     * @return	the component, or null if initContainer hasn't been called yet.
     *
     * @see #initialize(com.hdcookbook.grin.animator.AnimationContext)
     * @see com.hdcookbook.grin.animator.AnimationContext#animationInitialize()
     **/
    public abstract Component getComponent();

    /**
     * Sets this engine so that the next frame will be painted in
     * its entirety, with no redraw optimization.  This can be needed
     * if the contents of the framebuffer was damaged somehow.
     **/
    public synchronized void paintNextFrameFully() {
	needsFullPaint = true;
    }

    /**
     * Initialize this animation manager.  The animation loop worker thread
     * will be started, and it will proceed in the following order:
     * <pre>
     *    context.animationInitialize();  
     *             // a good time to set up the HScene and Player
     *    for each client c
     *        c.initialize()
     *    context.animationFinishInitialization();  // perhaps set UI state
     *    for (frame = 0 to infinity) {
     *        wait until it's time for next frame
     *        for each client
     *            call advanceToFrame
     *         if animation isn't behind
     *             for each client
     *                 call setCaughtUp
     *                 call addDisplayAreas
     *             for each client
     *                 call paintFrame as many times as needed
     *    }
     * </pre>
     *
     * @param context   The context that the animation manager is running in.
     *		        This can be used for initialization that the client
     *			wants to do in the animation thread.  The context might
     *			use this to create an HScene, load a GRIN show, 
     *		        synchronously
     * 			start video playing, or do other time-consuming 
     *			operations.
     **/
    public void initialize(AnimationContext context) {
	if (Debug.ASSERT) {
	    synchronized(this) {
		if (state != STATE_NOT_STARTED) {
		    Debug.assertFail("Illegal state " + state + " in " + this);
		}
	    }
	}
	this.context = context;
	worker.start();
	//
	// Wait for the thread to start up.  This is good to do, because
	// it puts us in a known state before returning to the caller,
	// which is probably ultimately initXlet().  By doing this, we
	// know that the caller is OK to call destroy() on us after we're done.
	//
	synchronized(this) {
	    try {
		while (state == STATE_NOT_STARTED) {
		    wait();
		}
	    } catch (InterruptedException ex) {
		Thread.currentThread().interrupt();
		// ignored - player must be trying to shut us down
	    }
	}
    }

    /**
     * Start the current animations.  The animation will start
     * animating sometime after this method is called.  This can
     * be called while initialization is still underway in the
     * animation thread.
     * <p>
     * By default, the manager will assume that the contents of the
     * framebuffer wasn't changed while the manager was paused.  If
     * it might have been, it's advisable to call
     * paintNextFrameFully().
     * <p>
     * The animation manager starts in the paused state, so this
     * method will normally need to be called at least once.
     * 
     * @see #paintNextFrameFully()
     **/
    abstract public void start();

    /**
     * Pause the currently running animations.  The animation thread
     * will go into a wait state, until start() or destroy() are called.
     * <p>
     * This method will return immediately, even if the animation hasn't
     * reached the paused state yet.
     * <p>
     * When the animation framework is paused, no effort is made to output
     * one last frame with the current state of the model.
     **/
    abstract public void pause();

    /**
     * Destroy this animation manager, by terminating the animation thread.
     * It's OK to call this more than once.  It should be called while
     * the animation manager's component is still visible, and a child
     * of a visible HScene.
     * <p>
     * This method will not return until the animation thread has
     * terminated.  
     **/
    public void destroy() {
	synchronized(this) {
	    if (state != STATE_STOPPED) {
		if (Debug.ASSERT && state != STATE_RUNNING) {
		    Debug.assertFail();
		}
		setState(STATE_STOPPING);
	    }
	    try {
		//
		// Wait until it's really terminated, so that things are
		// in a known state when this method returns.
		//
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


    /**
     * Called from RenderArea to cause an area to be cleared in the
     * current frame.
     **/
    protected abstract void clearArea(int x, int y, int width, int height);

    /**
     * Tell us whether or not this style of animation requires a full
     * redraw of everything on the screen in each pass through the animation
     * loop.  This is called from the animation loop.
     **/
    protected abstract boolean needsFullRedrawInAnimationLoop();

    /**
     * Paint the current frame into the right graphics buffer.
     * The subclass implementation of this method should call
     * paintFrame(Graphics2D).
     *
     * @param numTargets  Number of render area targets that need to
     *		          be painted.
     *
     * @see #paintFrame(java.awt.Graphics2D)
     **/
    protected abstract void callPaintFrame(int numTargets) 
    	throws InterruptedException;

    /**
     * Called when the engine is finished drawing the current frame.
     * This should flush the results to the screen, if needed.  The
     * framework guarantees that each call to callPaintFram,()
     * will be followed by a call to finishedFrame(), even
     * if the thread is interrupted or there's a RuntimeException
     * that terminates the thread.
     **/
    protected abstract void finishedFrame();

    /**
     * Erase the screen because the AnimationManager is terminating.
     * This is called in the animation thread as a result of a call
     * to destroy(), just before the animation thread terminates.
     **/
    protected abstract void terminatingEraseScreen();


    /**
     * This is the main loop for animation.  It's for internal use
     * only; users of this framework should not call this method
     * directly.
     **/
    public void run() {
	setState(STATE_RUNNING);
	try {
	    context.animationInitialize();
	    if (Debug.ASSERT) {
		synchronized(this) {
		    if (getComponent() == null) {
			Debug.assertFail("initContainer() not called");
		    }
		    if (targets == null) {
			Debug.assertFail("initNumTargets() not called");
		    }
		    if (clients == null) {
			Debug.assertFail("initClients() not called");
		    }
		}
	    }
	    for (int i = 0; i < clients.length; i++) {
		clients[i].initialize(getComponent());
	    }
	    context.animationFinishInitialization();
	    runAnimationLoop();

	} catch (InterruptedException ex) {
	    // Player is trying to terminate us 
	    Thread.currentThread().interrupt();
	} finally {
	    AnimationClient[] c = clients;
	    if (c != null) {
		for (int i = 0; i < c.length; i++) {
		    try {
			c[i].destroy();
		    } catch (Throwable t) {
			if (Debug.LEVEL > 1) {
			    Debug.println("****  Exception in destroy:  " + t);
			}
		    }
		}
	    }
	    terminatingEraseScreen();
	}
	setState(STATE_STOPPED);  // Allows the call to destroy() to return
    }

    /**
     * The inner loop for the animation thread.  This implementation uses
     * System.currentTimeMillis() and wait() to keep a steady frame
     * rate.  This method can be overridden for animation styles that
     * have a different synch mechanism, like SFAA.  If this is done,
     * be sure to look inside this method, and obey the same semantic
     * contract.
     * <p>
     * This method must check destroyRequested() at least once per frame,
     * and if it's true, bail out of the loop.  
     * To advance the model by one frame, it should call
     * advanceModel().  When it wants to show a frame (and not skip it), it 
     * should call showFrame(), which will cause callPaintFrame() to 
     * be called.
     * <p>
     * Of course, the animation loop should also check Thread.interrupted()
     * regularly.
     *
     * @see #destroyRequested()
     * @see #advanceModel()
     * @see #showFrame()
     * @see #callPaintFrame(int)
     **/
    abstract protected void runAnimationLoop() throws InterruptedException;
    
    protected final void advanceModel() throws InterruptedException {
	synchronized(repaintLock) {
	    for (int i = 0; i < clients.length; i++)  {
		clients[i].nextFrame();
	    }
	} 
    }

    /**
     * Tell the model we're caught up, and show the current frame.  This
     * calls callPaintFrame.
     *
     * @see #callPaintFrame(int)
     **/
    protected final void showFrame() throws InterruptedException {
	for (int i = 0; i < clients.length; i++) {
	    clients[i].setCaughtUp();
	}
	for (int i = 0; i < targets.length; i++) {
	    targets[i].setEmpty();
	}
	boolean fullPaint;
	synchronized(this) {
	    fullPaint = needsFullPaint || needsFullRedrawInAnimationLoop();
	    needsFullPaint = false;
	}
	if (fullPaint) {
	    targets[0].addArea(0, 0, getWidth(), getHeight());
	    // We still add the display areas, because that's also where
	    // the client does any necessary erasing.
	}
	for (int i = 0; i < clients.length; i++) {
	    clients[i].addDisplayAreas(targets);
	}
	int n = collapseTargets();
	try {
	    callPaintFrame(n);
	} finally {
	    finishedFrame();
	}
    }

    /**
     * Paint the current frame into the given graphics object.  This
     * should be called by the subclass within callPaintFrame().
     * <p>
     * The Graphics2D instance must have a clip area that is set to
     * the extent of the area being managed by this animation manager.
     * The instance will have an unmodified clip area
     * after this method is called, but other attributes, like the
     * drawing mode and the foreground color, might be changed.  However,
     * the Graphics2D instance will be suitable for re-use in a subsequent
     * paintFrame operation.
     * <p>
     * This method indicates which targets were painted to.  This might
     * be useful to limit the areas of the double buffer that need to be
     * flushed to the framebuffer.
     *
     * @param g		A graphics context with a clip area covering
     *			the full extent of the screen area under the
     *			control of this animation manager. 
     * @param numTargets  Number of render area targets that need to
     *		          be painted.
     *
     * @see #callPaintFrame(int)
     **/
    protected final void paintFrame(Graphics2D g, int numTargets) 
	    throws InterruptedException 
    {
	g.setComposite(AlphaComposite.Src);
	lastClip.width = 0;
	g.getClipBounds(lastClip);
	for (int i = 0; i < numTargets; i++) {
	    g.setClip(targets[i].bounds);
	    for (int j = 0; j < clients.length; j++) {
		clients[j].paintFrame(g);
	    }
	}
	if (lastClip.width == 0) {
	    g.setClip(null);
	} else {
	    g.setClip(lastClip);
	}
    }

    /**
     * Paint the current frame into the given graphics object.  This
     * variant is for use when the full clip area of g should be
     * painted to, rather than the set of render area gargets.
     *
     * @param g		A graphics context with a clip area covering the 
     *			area that needs to be painted to
     *
     * @see #paintFrame(java.awt.Graphics2D, int)
     **/
    protected final void paintFrame(Graphics2D g) throws InterruptedException {
	g.setComposite(AlphaComposite.Src);
	for (int j = 0; j < clients.length; j++) {
	    clients[j].paintFrame(g);
	}
    }


    //
    // Collapse the render areas into an optimal set.  Return the number
    // of RenderArea instances that need to be drawn; targets[0..n-1]
    // will need to be drawn.  If no paint is needed, n will be 0.
    //
    private final int collapseTargets() {

	int n = targets.length - 1;

		// First, the easy part:  Put all of the empty targets
		// at the end of the list

	while (n >= 0 && targets[n].isEmpty()) {
	    n--;
	}
	// Now, targets[n] is non-empty, or n is -1

	for (int i = 0; i < n; ) {
	    if (targets[i].isEmpty()) {
		RenderArea a = targets[n];
		targets[n] = targets[i];
		targets[i] = a;
		n--;
	    } else {
		i++;
	    }
	}
	// Now, targets[0..n] are non-empty

		// Next, figure out which areas should be collapsed.
		// As a SWAG, we collapse areas when combining them
		// at most doubles the area of the screen drawn to.
		//
		// This is an area where it would be worth measuring what
		// is optimal, and perhaps even using different heuristics
		// based on player.
		//
		// Note that this algorithm is O(n^3) on the number of
		// targets.

    collapse: 
	for (;;) {
	    for (int i = 0; i < n; ) {
		for (int j = i+1; j <= n; j++) {
		    collapsed.setBounds(targets[i].bounds);
		    collapsed.add(targets[j].bounds);
		    int ac = collapsed.width * collapsed.height;
		    int a = targets[i].bounds.width * targets[i].bounds.height
		    	   + targets[j].bounds.width * targets[j].bounds.height;
		    if (ac <= 2*a) {
			// combine them
			targets[i].bounds.setBounds(collapsed);
			if (j < n) {
			    RenderArea ra = targets[j];
			    targets[j] = targets[n];
			    targets[n] = ra;
			    targets[n].setEmpty();  
			    	// setEmpty() is not strictly necessary,
				// but it's fast and makes the code a bit
				// more robust.
			}
			n--;
			continue collapse;   // yay goto!
		    }
		}
	    }
	    break collapse;
	}

	// At this point, targets[0..n] represents an optimal set of
	// the areas we need to display.  Add one to get the length of
	// the list of targets.

	return n+1;
    }

    /**
     * Repaint the current frame.  This can be called if the
     * current frame needs to be painted to a graphics context outside
     * of the animation loop.  This should be extremely rare in practice,
     * but in theory it can at least happen with repaint animation that
     * uses platform-supplied double buffering, and it might be possible
     * with SFAA when the UI is repainted.  
     * <p>
     * This can also be used for things like capturing a screenshot
     * to a buffered image.
     **/
    public void repaintFrame(Graphics2D g) throws InterruptedException {
	if (destroyRequested()) {
	    return;
	}
	synchronized(repaintLock) {
	    // Hold lock so that no model updates happen during repaint.
	    // We should be safe from deadlock, because the only contention
	    // for model updates happen from the animation thread, which
	    // acquires no other locks before acquiring repaintLock.
	    paintFrame(g);
	} 
    }
}

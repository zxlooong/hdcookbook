
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

package com.hdcookbook.grin;

import com.hdcookbook.grin.animator.AnimationClient;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.SetTarget;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.SetupManager;
import com.hdcookbook.grin.util.SetupClient;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.Queue;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.RCKeyEvent;

import java.util.Hashtable;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.io.IOException;


/**
 * Represents a show.  A show is the top-level node in a scene graph.
 * It is composed of a number of segments.  A show progresses by moving
 * through segments; a show has exactly one active segment while it is
 * running.  A segment is composed of a number of visual features, plus
 * a set of remote control handlers.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Show implements AnimationClient {

    private Director director;
	// Never null

    /**
     * Our helper that calls into us to load images and such.  This is
     * for internal use only, and is public so that GRIN features in
     * other packages can access it efficiently.
     **/
    public SetupManager setupManager;

    /**
     * The component we're presented within.  This can be needed
     * for things like loading images via Component.prepareImage().
     * This is for internal use only, and is public so that GRIN
     * classes in other packages can access it efficiently.
     **/
    public Component component;

    /**
     * An object used to hold state during initializaition of a show.
     * This is nulled out after the show is initialized.
     * This is for internal use only, and is public so that GRIN
     * classes in other packages can access it efficiently.
     **/
    public ShowInitializer initializer = new ShowInitializer();

    protected Segment[] segments;
    protected Feature[] features;
    protected RCHandler[] rcHandlers;

    protected Hashtable publicSegments;
    protected Hashtable publicFeatures;
    protected Hashtable publicRCHandlers;

    /**
     * This is the set of images that are "sticky".  Sticky images get
     * loaded as normal (that is, by a feature that uses the image being 
     * included in the setup clause of a segment), but they are never
     * unloaded.  Thus, features that use sticky images will be
     * prepared instantly if subsequently re-used.
     * <p>
     * This array may be null.
     **/
    protected ManagedImage[] stickyImages = null;

    private Segment currentSegment = null;
    private Segment[] segmentStack = new Segment[0];  // For push/pop
    private int segmentStackPos = 0;
    private String[] drawTargets;
    private int defaultDrawTarget = 0;
    private ActivateSegmentCommand popSegmentCommand = null;
    private GrinXHelper syncDisplayCommand = null;
    private GrinXHelper segmentDoneCommand = null;

    private boolean initialized = false;
    private boolean destroyed = false;

    private Queue pendingCommands = new Queue(32);
    private boolean deferringPendingCommands = false;
    private int numTargets = 1;	  // number of RenderContext targets needed 
    				  // by this show
    private boolean inputOK = true;	
	// Condition variable on this instance of show
    private int inputOKWaiting = 0;
    	// # of threads waiting on inputOK

    /** 
     * Create a new show.
     *
     * @param director  A Director helper class the xlet can use to control
     *			the show.  May be null, in which case a default
     *			direct instance of Director will be assigned.
     **/
    public Show(Director director) {
	if (director == null) {
	    director = new Director();
	}
	this.director = director;
	director.setShow(this);
    }

    /**
     * Get this show's director.  If the show was created without a director,
     * a default one is created, and this will be returned.
     *
     * @return our Director
     **/
    public Director getDirector() {
	return director;
    }

    /**
     * This is called to build the show.  This needs to be done before
     * initialize is called.  Normally, clients of the GRIN framework
     * shouldn't call this.
     *
     * @throws IOException if anything goes wrong.
     **/
    public void buildShow(Segment[] segments, Feature[] features, 
    		          RCHandler[] rcHandlers, String[] stickyImages,
		          Hashtable publicSegments, Hashtable publicFeatures,
		          Hashtable publicRCHandlers)
	    throws IOException 
    {
	this.segments = segments;
	this.features = features;
	this.rcHandlers = rcHandlers;
	if (stickyImages != null) {
	    this.stickyImages = new ManagedImage[stickyImages.length];
	    for (int i = 0; i < stickyImages.length; i++) {
		this.stickyImages[i] = ImageManager.getImage(stickyImages[i]);
		    // This will never be null, even if the path doesn't refer
		    // to a real image.  The library doesn't try to load images
		    // until a feature that uses the image is prepared, so the
		    // framework has no way of knowing if the image exists or
		    // not at this time.
	    }
	}
	this.publicSegments = publicSegments;
	this.publicFeatures = publicFeatures;
	this.publicRCHandlers = publicRCHandlers;
	
	for (int i = 0; i < rcHandlers.length; i++) {
	    rcHandlers[i].setShow(this);
	}
	for (int i = 0; i < segments.length; i++) {
	    segments[i].setShow(this);
	}
    }

    /**
     * @inheritDoc
     * <p>
     * This will  be called by the animation framework; clients of the GRIN
     * framework should ensure a show has been built before handing it off
     * to the animation framework.
     *
     * @param  component The component this show will eventually be displayed
     *                   in.  It's used for things like 
     *                   Component.prepareImage().
     **/
    public void initialize(Component component) {
	if (Debug.ASSERT && initialized) {
	    Debug.assertFail("Initizlize called twice");
	}
	initialized = true;
	this.component = component;
	if (stickyImages != null) {
	    for (int i = 0; i < stickyImages.length; i++) {
		this.stickyImages[i].makeSticky();
		    // This always succeeds, even if the actual image
		    // file doesn't exist, because no attempt is made to
		    // load the image here.
	    }
	}
	popSegmentCommand = new ActivateSegmentCommand(this, false, true);
	{
	    int num = 0;
	    for (int i = 0; i < features.length; i++) {
		if (features[i] instanceof SetupClient) {
		    num++;
		}
	    }
	    setupManager = new SetupManager(num);
	}
	setupManager.start();
	for (int i = 0; i < segments.length; i++) {
	    segments[i].initialize();
	}
	for (int i = 0; i < features.length; i++) {
	    features[i].initialize();
	}
	initializer = null;
    }

    /**
     * @inheritDoc
     * <p>
     * Destroy a show.  This will be called by the animation framework
     * when the animation engine is destroyed, or when this show is removed
     * from the engine's list of shows.
     **/
    public synchronized void destroy() {
	if (Debug.ASSERT && !initialized) {
	    Debug.assertFail("Destroy of uninitialized show");
	}
	if (currentSegment != null) {
	    currentSegment.deactivate();
	    currentSegment = null;
	}
	for (int i = 0; i < features.length; i++) {
	    features[i].destroy();
	}
	if (stickyImages != null) {
	    for (int i = 0; i < stickyImages.length; i++) {
		this.stickyImages[i].unmakeSticky();
	    }
	}
	destroyed = true;
	setupManager.stop();
	director.notifyDestroyed();
	notifyAll();
    }

    /** 
     * Used to build the show.  Clients of the GRIN framework should not
     * call this method directly.
     **/
    public void setSegmentStackDepth(int depth) {
	segmentStack = new Segment[depth];
    }

    /**
     * @inheritDoc
     *
     * Used to build the show, or to reinitialize it.  This method is
     * called by the animation framework, and should not be directly called
     * by client code.
     **/
    public void setDrawTargets(String[] drawTargets) {
	this.drawTargets = drawTargets;
    }

    /**
     * Get the set of draw target names.  The numerical draw targets
     * in features (e.g. the SetTarget feature) don't necessarily correspond
     * to indicies in this array, because when AnimationClient instances
     * are set up in an AnimationEngine, a master list of all of the unique
     * draw targets is built.
     *
     * @see com.hdcookbook.grin.animator.RenderContext#setTarget(int)
     * @see #mapDrawTargets(Hashtable)
     * @see com.hdcookbook.grin.features.SetTarget
     **/
    public String[] getDrawTargets() {
	return drawTargets;
    }

    /**
     * @inheritDoc
     **/
    public void mapDrawTargets(Hashtable targetMap) {
	defaultDrawTarget 
	    = ((Integer) targetMap.get(drawTargets[0])).intValue();
	for (int i = 0; i < features.length; i++) {
	    Feature f = features[i];
	    if (f instanceof SetTarget) {
		((SetTarget) f).mapDrawTarget(targetMap);
	    }
	}
    }

    /**
     * Look up the given public feature.
     *
     * @return feature, or null if not found
     **/
    public Feature getFeature(String name) {
	return (Feature) publicFeatures.get(name);
    }
   
    /**
     * Get a public RC handler by name.
     *
     * @return rc handler, or null if not found
     **/
    public RCHandler getRCHandler(String name) {
	return (RCHandler) publicRCHandlers.get(name);
    }
    
    /**
     * Look up a public segment.  This is done without taking out the show lock.
     * It's OK to call this before the show is initialized.
     *
     * @return segment, or null if not found.  
     * 	
     **/
    public Segment getSegment(String name) {
	return (Segment) publicSegments.get(name);
    }

    /**
     * Get the depth of the segment stack.  This is how many times you can
     * push a segment without an old value falling off the end of the stack.
     * It's set in the show file.
     **/
    public int getSegmentStackDepth() {
	return segmentStack.length;
    }


    /**
     * Set the current segment.  This is the main way an application
     * controls what is being displayed on the screen.  The new segment
     * will become current when we advance to the next frame.
     * <p>
     * This can be called from any thread; it does not take out the show
     * lock or any other global locks.  If the show has been destroyed, 
     * calling this method has no effect.  It's OK to call this before the
     * show has been initialized.
     * <p>
     * The current segment is not pushed onto the segment activation stack.
     *
     * @param   seg  The segment to activate, or null to pop the
     *               segment activation stack;
     **/
    public void activateSegment(Segment seg) {
	activateSegment(seg, false);
    }

    /**
     * Set the current segment.  This is the main way an application
     * controls what is being displayed on the screen.  The new segment
     * will become current when we advance to the next frame.
     * <p>
     * This can be called from any thread; it does not take out the show
     * lock or any other global locks.  If the show has been destroyed, 
     * calling this method has no effect.
     *
     * @param   seg  The segment to activate, or null to pop the
     *               segment activation stack.
     *
     * @param   push When true and when the segment is non-null, the 
     *               current segment will be pushed onto the
     *		     segment activation stack as the show transitions to
     *		     the new segment.
     **/
    public void activateSegment(Segment seg, boolean push) {
	if (seg == null) {
	    runCommand(popSegmentCommand);
	} else {
	    runCommand(seg.getCommandToActivate(push));
	}
    }

    /**
     * Used by the activate segment command.  Clients of the GRIN framework
     * should never call this method directly.
     **/
    public synchronized void pushCurrentSegment() {
	segmentStack[segmentStackPos] = currentSegment;
	segmentStackPos = (segmentStackPos + 1) % segmentStack.length;
    }

    /**
     * Used by the activate segment command.  Clients of the GRIN framework
     * should never call this method directly.
     **/
    public synchronized Segment popSegmentStack() {
	segmentStackPos--;
	if (segmentStackPos < 0) {
	    segmentStackPos = segmentStack.length - 1;
	}
	Segment result = segmentStack[segmentStackPos];
	segmentStack[segmentStackPos] = null;
	return result;
    }

    /**
     * Synchronize the display to the current show state.  This works by
     * queueing a show command.  When executed, this command will prevent
     * the execution of any other show commands, until the display has
     * caught up to the internal program state.  This is a good thing
     * to do before a command that is potentially time-consuming, like
     * selecting a playlist.  To flesh out this example, 
     * on some players, while a playlist is being
     * selected, the Java runtime does not get a chance to run and update
     * the screen, so it's good to make sure the screen is up-to-date before
     * starting the playlist selection.
     * <p>
     * Note carefully the difference between syncDisplay() and 
     * deferNextCommands().  If you're about to queue a command that does
     * something time-consuming, then you should call syncDisplay() first,
     * so the display is guaranteed to be updated.  If, however, you're
     * executing within the body of the command and you want to make sure the
     * display gets synchronized before the next command in the queue (which
     * was already there), then you want to call deferNextCommands().
     * <p>
     * This is equivalent to the <code>sync_display</code> GRIN command.
     *
     * @see #deferNextCommands()
     **/
    public void syncDisplay() {
	if (syncDisplayCommand == null) {
	    GrinXHelper cmd = new GrinXHelper(this);
	    cmd.setCommandNumber(GrinXHelper.SYNC_DISPLAY);
	    syncDisplayCommand = cmd;
	}
	runCommand(syncDisplayCommand);
    }

    /**
     * Signal to the show that the current segment is done.  This causes
     * the segment to execute its "next" command block, which in most
     * shows should contain a command to move to a different segment.
     * <p>
     * This is equivalent to the <code>segment_done</code> GRIN command.
     **/
    public void segmentDone() {
	if (segmentDoneCommand == null) {
	    GrinXHelper cmd = new GrinXHelper(this);
	    cmd.setCommandNumber(GrinXHelper.SEGMENT_DONE);
	    segmentDoneCommand = cmd;
	}
	runCommand(segmentDoneCommand);
    }

    /**
     * Run the given command when we advance to the next frame.
     * If the show has been destroyed, this has no effect. 
     * <p>
     * This can be called from any thread; it does not take out the show
     * lock or any other global locks.  If the show has been destroyed, 
     * calling this method has no effect.  It's OK to call this before
     * the show has been initialized.
     *
     * @see #runCommands(Command[])
     **/
    public void runCommand(Command cmd) {
	pendingCommands.add(cmd);
    }

    /**
     * Run the given commands when we advance to the next frame.
     * If the show has been destroyed, this has no effect. 
     * <p>
     * This can be called from any thread; it does not take out the show
     * lock or any other global locks.  If the show has been destroyed, 
     * calling this method has no effect.  It's OK to call this before
     * the show has been initialized.
     *
     * @see #runCommand(Command)
     **/
    public void runCommands(Command[] cmds) {
	if (cmds == null || cmds.length == 0) {
	    return;
	}
	synchronized(pendingCommands) {
	    for (int i = 0; i < cmds.length; i++) {
		pendingCommands.add(cmds[i]);
	    }
	}
    }

    /**
     * @inheritDoc
     * <p>
     * The animation framework calls this method just before calling 
     * nextFrame() if
     * the animation loop is caught up.  From time to time, pending commands
     * will be deferred until animation has caught up - this is done by the
     * sync_display command.  GRIN knows we've
     * caught up when we paint a frame, but calling this method can let
     * it know one frame earlier.
     *
     * @see #nextFrame()
     **/
    public synchronized void setCaughtUp() {
	deferringPendingCommands = false;
    }

    /**
     * @inheritDoc
     *
     * @throws	InterruptedException	if the show has been destroyed
     *
     * @see #setCaughtUp()
     **/
    public synchronized void nextFrame() throws InterruptedException {
	if (currentSegment != null) {
	    currentSegment.nextFrame();
	}
	director.notifyNextFrame();
	    // It's in Director's contract that this be called with the
	    // show lock held.
    }

    // 
    // Called from Director.notifyNextFrame()
    //
    synchronized void runPendingCommands() {
	while (!deferringPendingCommands && !pendingCommands.isEmpty()) {
	    Command c = (Command) pendingCommands.remove();
	    if (c != null) {
		c.execute();	// Can call deferNextCommands()
	    }
	}
    }

    /**
     * This method, which should ONLY be called from the execute() method
     * of a command, suspends processing of further queued commands until
     * the display of the show has caught up with the screen.  This is done
     * from the sync_display command, but can be done from any other command
     * too.  It causes an effect like Toolkit.sync() or the old X-Windows
     * XSync() command, whereby the screen is guaranteed to be up-to-date
     * before further commands are processed.
     * <p>
     * Doing this can be useful, for example, just before an operation that
     * is time-consuming and CPU-bound on some players, like JMF selection.
     *
     * @see #syncDisplay()
     **/
    public synchronized void deferNextCommands() {
	deferringPendingCommands = true;
    }

    /**
     * This is called from ActivateSegmentCommand, and should not be
     * called from anywhere else.
     **/
    public synchronized void doActivateSegment(Segment newS) {
	// We know the lock is being held, and a command is being executed
	Segment old = currentSegment;
	currentSegment = newS;
	currentSegment.activate(old);
	director.notifySegmentActivated(newS, old);
    }

    /**
     * This is called from SegmentDoneCommand, and should not be
     * called from anywhere else.
     **/
    // We know the lock is being held, and a command is being executed
    public void doSegmentDone() {
	currentSegment.doSegmentDone();
    }


    /**
     * Get the current segment.  The caller may wish to
     * synchronize on the show when using this, so that the
     * current segment doesn't change right after the call.
     **/
    public synchronized Segment getCurrentSegment() {
	return currentSegment;
    }
    

    /**
     * @inheritDoc
     **/
    public synchronized void addDisplayAreas(RenderContext context) 
				throws InterruptedException 
    {
	while (inputOKWaiting > 0) {
	    // If input is pending, let it in first
	    wait();	// can throw InterruptedException
	}
	inputOK = false;
	    // We could call notifyAll() here, but nobody waits for
	    // inputOK to turn false, so there's no need.
	if (currentSegment != null) {
	    int old = context.setTarget(defaultDrawTarget);
	    currentSegment.addDisplayAreas(context);
	    context.setTarget(old);
	}
    }

    /**
     * @inheritDoc
     * <p>
     * Paint the current state of the enhancement.  This should be
     * called by the xlet, usually via the animation framework.  
     **/
    public synchronized void paintFrame(Graphics2D gr)
    	throws InterruptedException 
    {
	if (Thread.interrupted() || destroyed) {
	    throw new InterruptedException();
	}
	if (currentSegment != null) {
	    currentSegment.paintFrame(gr);
	}
	if (Debug.ASSERT && deferringPendingCommands) {
	    Debug.assertFail();
	}
    }

    /**
     * @inheritDoc
     **/
    public synchronized void paintDone() {
	inputOK = true;
	notifyAll();	// Remote control input might be waiting on inputOK
    }
    //
    // Wait until it's safe to receive input.  Returns true if 
    // it's OK to proceed, or false if we've been interrupted 
    // and should bail out.
    //
    // This is needed because the remote control key handlers execute
    // show commands synchronously, in the AWT thread.  This means that
    // the show must be in a safe state for the execution of commands
    // before the keypress can be allowed in.
    //
    // Experiments were done with queuing the commands that result from
    // keypresses to the command, but that caused problems, because 
    // it meant that a command keypress might be processed resulting
    // in a state-changing command (like activate_segment), and then
    // another keypress might come in before that command runs.
    //
    private synchronized boolean waitForInputOK() {
	if ((!inputOK) && (!destroyed)) {
	    inputOKWaiting++;
	    try {
		while ((!inputOK) && (!destroyed)) {
		    wait();
		}
	    } catch (InterruptedException ex) {
		Thread.currentThread().interrupt();
		return false;
	    } finally {
		inputOKWaiting--;
		notifyAll();
	    }
	}
	return !destroyed;
    }

    /**
     * Called by the xlet when a key press is received.
     *
     * @return true	If the key press is handled
     **/
    public synchronized boolean handleKeyPressed(int vkCode) {
	if (currentSegment == null) {
	    return false;
	}
	if (!waitForInputOK()) {
	    return false;
	}
	RCKeyEvent re = RCKeyEvent.getKeyByEventCode(vkCode);
	if (re == null) {
	    return false;
	}
	return currentSegment.handleKeyPressed(re);
    }

    /**
     * Called by the xlet when a key release is received.  Note that not
     * all devices generate a key released.
     *
     * @return true	If the keypress is handled
     **/
    public synchronized boolean handleKeyReleased(int vkCode) {
	if (currentSegment == null) {
	    return false;
	}
	if (!waitForInputOK()) {
	    return false;
	}
	RCKeyEvent re = RCKeyEvent.getKeyByEventCode(vkCode);
	if (re == null) {
	    return false;
	}
	return currentSegment.handleKeyReleased(re);
    }

    /**
     * Called by the xlet when the mouse moves.  This should be called
     * when a mouse moved event or a mouse dragged event is received.
     **/
    public synchronized void handleMouseMoved(int x, int y) {
	boolean used = false;
        if (currentSegment != null) {
	    if (!waitForInputOK()) {
		return;
	    }
            used = currentSegment.handleMouse(x, y, false);
        }
	Cursor c = used ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
			: Cursor.getDefaultCursor();
	if (component != null && c != component.getCursor()) {
	    component.setCursor(c);
	}
    }
   
    /**
     * Called by the xlet when the mouse is clicked.
     **/
    public synchronized void handleMouseClicked(int x, int y) {
        if (currentSegment != null) {
	    if (!waitForInputOK()) {
		return;
	    }
            currentSegment.handleMouse(x, y, true);
        }
	Cursor c = Cursor.getDefaultCursor();
	if (component != null && c != component.getCursor()) {
	    component.setCursor(c);
	}
    }
}


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
import com.hdcookbook.grin.animator.RenderArea;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.ImageManager;
import com.hdcookbook.grin.util.SetupManager;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.Queue;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.RCKeyEvent;

import java.util.Hashtable;
import java.util.Enumeration;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;


/**
 * Represents a show.  A show is the top-level node in an enhancement.
 * It is composed of a number of segments.  A show progresses by moving
 * through segments; a show has exactly one active segment while it is
 * running.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Show implements AnimationClient {

    private Director director;

    /**
     * Our helper that calls into us to load images and such
     **/
    public SetupManager setupManager;

    /**
     * The component we're presented within.  This can be needed
     * for things like loading images via Component.prepareImage().
     **/
    public Component component;

    /**
     * An object used to hold state during initializaition of a show.
     * This is nulled out after the show is initialized.
     **/
    public ShowInitializer initializer = new ShowInitializer();

    private Hashtable features = new Hashtable();
    private Hashtable rcHandlers = new Hashtable();
    private Hashtable segments = new Hashtable();
    private Hashtable controllers = new Hashtable();

    private Segment currentSegment = null;
    private Segment[] segmentStack = new Segment[0];  // For push/pop
    private int segmentStackPos = 0;
    private ActivateSegmentCommand popSegmentCommand;

    private boolean initialized = false;
    private boolean destroyed = false;

    private Queue pendingCommands = new Queue(32);
    private boolean deferringPendingCommands = false;
    private int currentFrame = -1;	// The current frame number

    /** 
     * Create a new show.
     *
     * @param director  A Director helper class the xlet can use to control
     *			the show.
     **/
    public Show(Director director) {
	this.director = director;
	director.setShow(this);
    }

    /**
     * Get this show's director, if it has one.
     *
     * @return our Director, or null
     **/
    public Director getDirector() {
	return director;
    }

    /**
     * @inheritDoc
     * <p>
     * This should be called after the show has been built.
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
	popSegmentCommand = new ActivateSegmentCommand(this, false, true);
    	setupManager = new SetupManager(features.size());
	setupManager.start();
	for (Enumeration e = segments.elements(); e.hasMoreElements() ;) {
	    Segment seg = (Segment) e.nextElement();
	    seg.initialize(this);
	}
	for (Enumeration e = features.elements(); e.hasMoreElements() ;) {
	    Feature f = (Feature) e.nextElement();
	    f.initialize();
	}
	initializer = null;
    }

    /**
     * @inheritDoc
     * <p>
     * Destroy a show.  This should be called when the Xlet is done with
     * this show.
     **/
    public synchronized void destroy() {
	if (Debug.ASSERT && !initialized) {
	    Debug.assertFail("Destroy of uninitialized show");
	}
	for (Enumeration e = segments.elements(); e.hasMoreElements() ;) {
	    Segment s = (Segment) e.nextElement();
	    s.destroy();
	}
	for (Enumeration e = features.elements(); e.hasMoreElements() ;) {
	    Feature f = (Feature) e.nextElement();
	    f.destroy();
	}
	destroyed = true;
	setupManager.stop();
    }

    /** 
     * Used by the parser
     **/
    public void setSegmentStackDepth(int depth) {
	segmentStack = new Segment[depth];
    }

    /**
     * Used by the parser
     **/
    public int getSegmentStackDepth() {
	return segmentStack.length;
    }

    /**
     * Used by the parser
     **/
    public void addFeature(String name, Feature f) throws IOException {
	if (features.get(name) != null) {
	    throw new IOException("Feature named \"" + name 
	    			   + "\" already exists.");
	}
	features.put(name, f);
    }

    /**
     * Look up the given feature.
     *
     * @return feature, or null if not found
     **/
    public Feature getFeature(String name) {
	return (Feature) features.get(name);
    }
   
    /** 
     * Get all of the features in this show
     **/
    public Enumeration getFeatures() {
        return features.elements();
    }


    /**
     * Used by the parser
     **/
    public void addRCHandler(String name, RCHandler h) throws IOException {
	if (rcHandlers.get(name) != null) {
	    throw new IOException("RC handler named \"" + name 
	    			   + "\" already exists.");
	}
	rcHandlers.put(name, h);
	h.setShow(this);
    }

    /**
     * @return rc handler, or null if not found
     **/
    public RCHandler getRCHandler(String name) {
	return (RCHandler) rcHandlers.get(name);
    }

    /**
     * Look up a segment.  This is done without taking out the show lock.
     *
     * @return segment, or null if not found.  
     * 	
     **/
    public Segment getSegment(String name) {
	return (Segment) segments.get(name);
    }


    /**
     * Used by the parser
     **/
    public void addSegment(String name, Segment f) {
	segments.put(name, f);
    }

    /**
     * Set the current segment.  This is the main way an application
     * controls what is being displayed on the screen.  The new segment
     * will become current when we advance to the next frame.
     * <p>
     * This can be called from any thread; it does not take out the show
     * lock or any other global locks.  If the show has been destroyed, 
     * calling this method has no effect.
     * <p>
     * @param   seg  The segment to activate, or null to pop the
     *               segment activation stack;
     **/
    public void activateSegment(Segment seg) {
	if (seg == null) {
	    runCommand(popSegmentCommand);
	} else {
	    runCommand(seg.cmdToActivate);
	}
    }

    /**
     * Used by the activate segment command.
     **/
    public synchronized void pushCurrentSegment() {
	segmentStack[segmentStackPos] = currentSegment;
	segmentStackPos = (segmentStackPos + 1) % segmentStack.length;
    }

    /**
     * Used by the activate segment command
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
     * Run the given command when we advance to the next frame.
     * If the show has been destroyed, this has no effect. 
     * <p>
     * This can be called from any thread; it does not take out the show
     * lock or any other global locks.  If the show has been destroyed, 
     * calling this method has no effect.
     **/
    public void runCommand(Command cmd) {
	pendingCommands.add(cmd);
    }

    /**
     * @inheritDoc
     * <p>
     * An xlet can call this method just before calling advanceToFrame if
     * the animation loop is caught up.  From time to time, pending commands
     * will be deferred until animation has caught up.  GRIN knows we've
     * caught up when we paint a frame, but calling this method can let
     * it know one frame earlier.
     *
     * @see #nextFrame()
     **/
    public synchronized void setCaughtUp() {
	deferringPendingCommands = false;
    }

    /**
     * Advance the state of the show to the given frame.  Frame numbers
     * monotonically increase; in the presence of trick play, they
     * do not track the media time of the underlying video.
     * <p>
     * A show starts with the current frame set to -1, so the first frame is
     * 0.  When the show is advanced to frame 0, there might be a significant
     * number of features that start initializing, e.g. by loading images.
     * For this reason, an application might choose to use frame 0 as
     * a special "initializing" frame that's never displayed, and that
     * last for a fairly long time.
     *
     * @throws 	IllegalArgumentException if newFrame < getCurrentFrame()
     * @throws	InterruptedException	if the show has been destroyed
     *
     * @see #setCaughtUp()
     *
     * @deprecated  With the new animation framework, this is being
     *		    replaced by nextFrame().  The method advanceToFrame()
     *		    will soon be removed from Show.
     **/
    public synchronized void advanceToFrame(int newFrame) 
    	throws InterruptedException 
    {
	if (Thread.interrupted() || destroyed) {
	    throw new InterruptedException();
	}
	if (newFrame <= currentFrame) {
	    if (newFrame == currentFrame) {
		return;
	    }
	    throw new IllegalArgumentException();
	}
	boolean advanced = false;
	while (newFrame > currentFrame) {
	    currentFrame++;
	    advanced = true;
	    runPendingCommands();
	    if (currentSegment != null) {
		currentSegment.advanceToFrame(currentFrame);
		runPendingCommands();
	    }
	}
	if (advanced) {
	    notifyAll();
	}
    }

    // Inherited from AnimationClient
    /**
     * @inheritDoc
     **/
    public synchronized void nextFrame() throws InterruptedException {
	// @@ TODO:  Eliminate the old, deprecated method entirely.  When
	//	     doing this, also change the Feature API to get the
	//	     int frame number out of the calls - it's misleading,
	//	     given that each feature is called once per frame anyway...
	//	     the frame number is just confusing, and it wraps to
	//	     a negative value in only 2.8 years at 24 fps.
	advanceToFrame(currentFrame + 1);
    }

    /**
     * This is useful for an animation that wants to synchronize on
     * certain frames in a show, using this pattern:
     * <pre>
     *      Show show = ...;
     *      int wantedFrame = ...;
     *      synchronized(show) {
     *          show.waitForFrame(wantedFrame);
     *          ...  make stateful changes to the show;
     *      }
     * </pre>
     *
     * @deprecated	Cute, but not useful.  Use a command instead
     **/
    // @@ TODO:  Eliminate this.
    public synchronized void waitForFrame(int wanted) 
    		throws InterruptedException 
    {
	for (;;) {
	    if (Thread.interrupted() || destroyed) {
		throw new InterruptedException();
	    }
	    if (currentFrame >= wanted) {
		break;
	    }
	    wait();
	}
    }

    synchronized void runPendingCommands() {
	while (!deferringPendingCommands && !pendingCommands.isEmpty()) {
	    Command c = (Command) pendingCommands.remove();
	    if (c != null) {
		c.execute();
		deferringPendingCommands 
		    = deferringPendingCommands || c.deferNextCommands();
	    }
	}
    }

    /**
     * Get the current frame number of our underlying mode.
     *
     * @see #advanceToFrame(int)
     *
     * @deprecated   Really only useful for features, with the old
     *		     frame number-based way of keeping track of state
     **/
    // @@ TODO:  Get rid of this
    public synchronized int getCurrentFrame() {
	return currentFrame;
    }


    /**
     * This is called from ActivateSegmentCommand, and should not be
     * called from anywhere else.
     **/
    public void doActivateSegment(Segment newS) {
	// We know the lock is being held, and a command is being executed
	Segment old = currentSegment;
	currentSegment = newS;
	currentSegment.activate(old);
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
     * Get the current segment.  The caller should probably be
     * synchronizing on the show when using this, so that the
     * current segment doesn't change right after the call.
     **/
    public synchronized Segment getCurrentSegment() {
	return currentSegment;
    }

    /**
     * Set thisArea to the union of lastArea and what will be
     * displayed this time.  Set lastArea to what will be displayed
     * this time.  A width of 0 indicates that nothing is drawn.
     * <p>
     * Usage:
     * <pre>
     *     Rectangle thisARea = new Rectangle();
     *     Rectangle lastArea = new Rectangle();
     *     Rectangle showClip = the bounding Rectangle of the show
     *     Rectlangle lastClip = new Rectangle();
     *     Show show = ...;
     *     for (frame = 1; ...; frame++) {
     *        show.setDisplayArea(thisArea. lastArea, showClip);
     *        if (thisArea.width > 0) {
     *            Graphics2D gr = the graphics to draw into;
     *            clear thisArea using gr.fillRect();
     *            Set gr's clip rect to thisArea;
     *            show.paintFrame(gr);
     *            Restore gr's clip area to what it was;
     *        }
     *    }
     * </pre>
     * Doing this will optimize display by reducing the area cleared, and
     * the amount of acutal drawing the show does.  A concrete example of
     * this pattern is in com.hdcookbook.grin.test.bigjdk.GrinTestRyan.
     *
     * @param thisArea 	The union of lastArea and what will be drawn this time
     * @param lastArea  What was drawn last time.  Will be set to what will be
     *			drawn this time.
     * @param clip	The bounds of this show.  The result that lastArea
     *			is set to is clipped to these bounds.
     *
     * @throws	InterruptedException	if the show has been destroyed
     *
     * @deprecated	This was an incremental step toward what the
     *			animation framework can do better.
     **/
    // @@ TODO:  Get rid of this with transition to animation framework
    public synchronized void setDisplayArea(Rectangle thisArea, 
    					    Rectangle lastArea,
					    Rectangle clip)
    	throws InterruptedException 
    {
	if (Thread.interrupted() || destroyed) {
	    throw new InterruptedException();
	}
	thisArea.setBounds(lastArea);
	lastArea.x = lastArea.y = lastArea.width = lastArea.height = 0;
	if (currentSegment != null) {
	    currentSegment.addDisplayArea(lastArea);
	}
	int d = clip.x - lastArea.x;
	if (d > 0) {
	    lastArea.x = clip.x;
	    lastArea.width -= d;
	}
	d = clip.y - lastArea.y;
	if (d > 0) {
	    lastArea.y = clip.y;
	    lastArea.height -= d;
	}
	d = (lastArea.x + lastArea.width) - (clip.x + clip.width);
	if (d > 0) {
	    lastArea.width -= d;
	}
	d = (lastArea.y + lastArea.height) - (clip.y + clip.height);
	if (d > 0) {
	    lastArea.height -= d;
	}
	if (lastArea.width <= 0 || lastArea.height <= 0) {
	    lastArea.width = 0;
	    lastArea.height = 0;
	} else {
	    if (thisArea.width > 0) {
		thisArea.add(lastArea);
	    } else {
		thisArea.setBounds(lastArea);
	    }
	}
    }

    // @@ Some temporary adaptation of the new animation f/w to the
    // @@ older, less-optimized way of drawing.  This will go away
    // @@ within a week.
    private Rectangle tempThisArea = new Rectangle();
    private Rectangle tempLastArea = new Rectangle();
    private Rectangle tempShowClip = new Rectangle();

    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderArea[] targets) 
    	    throws InterruptedException 
    {
    	// @@ TODO:  Make an optimized version of this that works with
	//  @@	     the features.
	tempShowClip.width = component.getWidth();
	tempShowClip.height = component.getHeight();
	setDisplayArea(tempThisArea, tempLastArea, tempShowClip);
	targets[0].clearAndAddArea(tempThisArea);
	targets[0].addArea(tempLastArea);
    }

    /**
     * @inheritDoc
     * <p>
     * Paint the current state of the enhancement.  This should be
     * called by the xlet.  This way, the xlet can decide to use
     * whatever animation style it wants:  direct draw, repaint
     * draw, SFAA, or anything else.
     **/
    // @@ TODO:  Think about checking clip rect in features
    public synchronized void paintFrame(Graphics2D gr)
    	throws InterruptedException 
    {
	if (Thread.interrupted() || destroyed) {
	    throw new InterruptedException();
	}
	if (currentSegment != null) {
	    currentSegment.paintFrame(gr);
	}
	deferringPendingCommands = false; 	
	    // If we've paint a frame, we're definitely caught up.
    }

    /**
     * Called by the xlet when a keypress is received.
     *
     * @return true	If the keypress is handled
     **/
    public synchronized boolean handleKeyPressed(int vkCode) {
	if (currentSegment == null) {
	    return false;
	}
	RCKeyEvent re = RCKeyEvent.getKeyByEventCode(vkCode);
	if (re == null) {
	    return false;
	}
	if (Debug.LEVEL > 1) {
	    System.out.println("RC event:  " + re.getName());
	}
	return currentSegment.handleRCEvent(re);
    }

    /**
     * Called by the xlet when the mouse moves.  This should be called
     * when a mouse moved event or a mouse dragged event is received.
     **/
    public synchronized void handleMouseMoved(int x, int y) {
	boolean used = false;
        if (currentSegment != null) {
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
            currentSegment.handleMouse(x, y, true);
        }
	Cursor c = Cursor.getDefaultCursor();
	if (component != null && c != component.getCursor()) {
	    component.setCursor(c);
	}
    }
}

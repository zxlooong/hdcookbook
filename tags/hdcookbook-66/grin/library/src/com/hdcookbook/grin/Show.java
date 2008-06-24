
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
import com.hdcookbook.grin.util.SetupManager;
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

    protected Segment[] segments;
    protected Feature[] features;
    protected RCHandler[] rcHandlers;

    protected Hashtable publicSegments;
    protected Hashtable publicFeatures;
    protected Hashtable publicRCHandlers;

    private Segment currentSegment = null;
    private Segment[] segmentStack = new Segment[0];  // For push/pop
    private int segmentStackPos = 0;
    private String[] drawTargets;
    private int defaultDrawTarget = 0;
    private ActivateSegmentCommand popSegmentCommand;

    private boolean initialized = false;
    private boolean destroyed = false;

    private Queue pendingCommands = new Queue(32);
    private boolean deferringPendingCommands = false;
    private int numTargets = 1;	  // number of RenderContext targets needed 
    				  // by this show

    /** 
     * Create a new show.
     *
     * @param director  A Director helper class the xlet can use to control
     *			the show.
     **/
    public Show(Director director) {
	this.director = director;
        if (director != null) {
	   director.setShow(this);
        }   
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
     * This is called to build the show.  This needs to be done before
     * initialize is called.
     *
     * @throws IOException if anything goes wrong.
     **/
    public void buildShow(Segment[] segments, Feature[] features, 
    		          RCHandler[] rcHandlers,
		          Hashtable publicSegments, Hashtable publicFeatures,
		          Hashtable publicRCHandlers)
	    throws IOException 
    {
	this.segments = segments;
	this.features = features;
	this.rcHandlers = rcHandlers;
	this.publicSegments = publicSegments;
	this.publicFeatures = publicFeatures;
	this.publicRCHandlers = publicRCHandlers;
	
	for (int i = 0; i < rcHandlers.length; i++) {
	    rcHandlers[i].setShow(this);
	}
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
    	setupManager = new SetupManager(features.length);
	setupManager.start();
	for (int i = 0; i < segments.length; i++) {
	    segments[i].initialize(this);
	}
	for (int i = 0; i < features.length; i++) {
	    features[i].initialize();
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
	for (int i = 0; i < segments.length; i++) {
	    segments[i].destroy();
	}
	for (int i = 0; i < features.length; i++) {
	    features[i].destroy();
	}
	destroyed = true;
	setupManager.stop();
    }

    /** 
     * Used to build the show
     **/
    public void setSegmentStackDepth(int depth) {
	segmentStack = new Segment[depth];
    }

    /**
     * Used to build the show
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
     * calling this method has no effect.
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
     * An xlet can call this method just before calling nextFrame() if
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
     * @inheritDoc
     *
     * @throws	InterruptedException	if the show has been destroyed
     *
     * @see #setCaughtUp()
     **/
    public synchronized void nextFrame() throws InterruptedException {
	if (currentSegment == null) {
	    runPendingCommands();
	} else {
	    currentSegment.nextFrame();
	    runPendingCommands();
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
     * Returns true if the node passed in is recorded as an public element
     * in this show, false otherwise.
     * 
     * @throws RuntimeException if node is neither an instance of 
     * Feature, RCHandler, nor Segment.
     */
    public boolean isPublic(Node node) {
        if (node instanceof Feature) {
            return publicFeatures.contains(node);
        } else if (node instanceof RCHandler) {
            return publicRCHandlers.contains(node);
        } else if (node instanceof Segment) {
            return publicSegments.contains(node);
        } else {
            throw new RuntimeException("Unknown node type " + node);
        }
    }

    /**
     * @inheritDoc
     **/
    public synchronized void addDisplayAreas(RenderContext context) 
				throws InterruptedException 
    {
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
	deferringPendingCommands = false; 	
	    // If we've painted a frame, we're definitely caught up.
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


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

import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.RCKeyEvent;
import com.hdcookbook.grin.util.Debug;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.IOException;

/**
 * A segment within a show.  A show is composed of segments, and at all
 * times exactly one segment is active.  When a segment is active, its
 * features are showing, and its remote control handlers receive events.
 * When a new feature is activated, any features that are active in both
 * segments are not re-initialized, so that animations will just continue,
 * for example.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class Segment {

    String name;
    private Show show;
    private Feature[] activeFeatures;
    private boolean[] featureWasActivated;
    private Feature[] settingUpFeatures;
    protected ChapterManager ourChapterManager;
    private boolean nextOnSetupDone;
    private Command[] nextCommands;
    private RCHandler[] rcHandlers;
    private boolean active = false;
    private boolean nextCommandSent;

    ActivateSegmentCommand cmdToActivate;

    public Segment(String name, Feature[] active, Feature[] setup,
    		 ChapterManager chapterManager, RCHandler[] rcHandlers,
		 boolean nextOnSetupDone, Command[] nextCommands) 
	    throws IOException 
    {
	this.name = name;	// for debugging
	this.activeFeatures = active;
	featureWasActivated = new boolean[active.length];
	for (int i = 0; i < featureWasActivated.length; i++) {
	    featureWasActivated[i] = false;
	}
	this.settingUpFeatures = setup;
	this.ourChapterManager = chapterManager;
	this.nextOnSetupDone = nextOnSetupDone;
	this.nextCommands = nextCommands;
	this.rcHandlers = rcHandlers;
    }

    public String toString() {
	return "Segment(" + name + ")";
    }

    public String getName() {
	return name;
    }

    public Show getShow() {
	return show;
    }

    public Feature[] getActiveFeatures() {
	return activeFeatures;
    }

    public Feature[] getSetupFeatures() {
	return settingUpFeatures;
    }

    /**
     * Do we trigger the commands in our next clause when all of the
     * features in our setup clause have finished loading? 
     *
     * @return the answer to that question.
     **/

    public boolean getNextOnSetupDone() {
	return nextOnSetupDone;
    }

    /**
     * Give the commands in our next clause.  This can be triggered
     * by setup being done, or by a segment_done command.
     *
     * @see #getNextOnSetupDone()
     * @see com.hdcookbook.grin.commands.SegmentDoneCommand
     **/
    public Command[] getNextCommands() {
	return nextCommands;
    }

    /**
     * Give the set of remote control handlers for this segment
     **/
    public RCHandler[] getRCHandlers() {
	return rcHandlers;
    }

    /**
     * Initialize up this segment.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the segments.
     **/
    public void initialize(Show show) {
	this.show = show;

	// For use by Show.activateSegment()
	cmdToActivate = new ActivateSegmentCommand(show);
	cmdToActivate.setup(this);
    }

    /**
     * Free any resources held by this segment.  It is the opposite of
     * setup; each call to setup() shall be balanced by
     * a call to unsetup(), and they shall *not* be nested.  
     * <p>
     * It's possible an active segment may be destroyed.  For example,
     * the last segment a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty segment
     * too!).
     **/
    public void destroy() {
    }

    /**
     * Activate this segment, that is, cause it to start presenting.
     * This will not take long; all real work is deferred
     * to worker threads.
     * <p>
     * This call is synchronized by the Show.
     *
     * @param	lastSegment	The last segment we're coming from.
     **/
    public void activate(Segment lastSegment) {
	if (Debug.LEVEL > 1) {
	    Debug.println("Going from segment " + lastSegment + " to " + this);
	}
	if (lastSegment == this) {
	    return;
	}
	active = true;
	nextCommandSent = false;
	for (int i = 0; i < activeFeatures.length; i++) {
	    boolean wasNeeded = activeFeatures[i].setup();
	    if (Debug.LEVEL > 0 
	        && (wasNeeded || activeFeatures[i].needsMoreSetup())) 
	    {
		Debug.println("WARNING:  Feature " + activeFeatures[i]
			      + " in segment " + name 
			      + " wasn't set up on time.");
	    }
	    if (!activeFeatures[i].needsMoreSetup()) {
		activeFeatures[i].activate();
		featureWasActivated[i] = true;
	    }
	}
	for (int i = 0; i < settingUpFeatures.length; i++) {
	    settingUpFeatures[i].setup();
	}
	if (lastSegment != null) {
	    lastSegment.active = false;
	    for (int i = 0; i < lastSegment.activeFeatures.length; i++) {
		if (lastSegment.featureWasActivated[i]) {
		    lastSegment.activeFeatures[i].deactivate();
		    lastSegment.featureWasActivated[i] = false;
		}
		lastSegment.activeFeatures[i].unsetup();
	    }
	    for (int i = 0; i < lastSegment.settingUpFeatures.length; i++) {
		lastSegment.settingUpFeatures[i].unsetup();
	    }
	}
	if (rcHandlers != null) {
	    for (int i = 0; i < rcHandlers.length; i++) {
		rcHandlers[i].activate(this);
	    }
	}
	runFeatureSetup();
    }

    //
    // When a feature is setup, we get this call.  We have to be a
    // little conservative; it's possible that a feature from a
    // previous, stale segment could finish its setup after we
    // become the current segment, so this call really means "one
    // of our features probably finished setup, but we'd better
    // check to be sure."
    //
    // This is externally synchronized by show, and must be.
    //
    void runFeatureSetup() {
	if (!active) {
	    return;
	}
	for (int i = 0; i < activeFeatures.length; i++) {
	    if (!featureWasActivated[i] && !activeFeatures[i].needsMoreSetup())
	    {
		activeFeatures[i].activate();
		featureWasActivated[i] = true;
	    }
	}
	// Now check to see if
	// it's time to move to the next segment.  It is if we have
	// no active features, and all of our features are set up.
	if (nextCommands == null || 
	    (!nextOnSetupDone && activeFeatures.length > 0)) 
	{
	    return;
	}
	for (int i = 0; i < settingUpFeatures.length; i++) {
	    if (settingUpFeatures[i].needsMoreSetup()) {
		return;
	    }
	}
	if (nextCommandSent) {
	    return;
	}
	for (int i = 0; i < nextCommands.length; i++) {
	    show.runCommand(nextCommands[i]);
	}

	nextCommandSent = true;
    }

    void doSegmentDone() {
	// We don't need to consult nextCommandSent here, because
	// the "segment done" command is sent from a feature within
	// the model update loop; if the next command moves us to
	// a new segment, that will prevent us from getting a second
	// one.  If it *doesn't* move us to a new segment, then maybe
	// the show author means to send the next command more than
	// once.
	if (nextCommands != null) {
	    for (int i = 0; i < nextCommands.length; i++) {
		show.runCommand(nextCommands[i]);
	    }
	}
    }

    //
    // Called from Show with the Show lock held
    //
    void paintFrame(Graphics2D gr) {
	for (int i = 0; i < activeFeatures.length; i++) {
	    activeFeatures[i].paintFrame(gr);
	}
    }

    //
    // Called from Show with the Show lock held
    //
    void  addDisplayArea(Rectangle area) {
	for (int i = 0; i < activeFeatures.length; i++) {
	    activeFeatures[i].addDisplayArea(area);
	}
    }

    //
    // Called from Show with the Show lock held
    //
    void advanceToFrame(int newFrame) {
	for (int i = 0; i < activeFeatures.length; i++) {
	    activeFeatures[i].advanceToFrame(newFrame);
	}
	if (rcHandlers != null) {
	    for (int i = 0; i < rcHandlers.length; i++) {
		rcHandlers[i].advanceToFrame(newFrame);
	    }
	}
    }

    //
    // Called from Show with the Show lock held
    //
    boolean handleRCEvent(RCKeyEvent re) {
	if (rcHandlers == null) {
	    return false;
	}
	for (int i = 0; i < rcHandlers.length; i++) {
	    if (rcHandlers[i].handleRCEvent(re)) {
		return true;
	    }
	}
	return false;
    }
    
    // Called from show with show lock held
    boolean handleMouse(int x, int y, boolean activate) {
        if (rcHandlers == null) {
            return false;
        }
	boolean handled = false;
        for (int i = 0; i < rcHandlers.length; i++) {
	    if (rcHandlers[i].handleMouse(x, y, activate)) {
		handled = true;
	    }
	}
	return handled;
    }
}

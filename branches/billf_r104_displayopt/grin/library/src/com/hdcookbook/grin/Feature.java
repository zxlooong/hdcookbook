
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

import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.SetupClient;

import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Represents a feature.  A feature is a thing that presents some sort
 * of UI.  A phase presents some number of features, and features can
 * be shared between phases.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public abstract class Feature implements SetupClient {

    protected Show show;
    private String name;

    private int activateCount = 0;
    private int setupCount = 0;

    boolean activated = false;	// Used by show, not by subfeatures

    protected Feature(Show show, String name) {
	this.show = show;
	this.name = name;
    }

    /**
     * @return the name of this feature
     **/
    public String getName() {
	return name;
    }

    /** 
     * @return a developer-friendly description of this feature, for debugging
     **/
    public String toString() {
	String nm = getClass().getName();
	int i = nm.lastIndexOf('.');
	if (i >= 0) {
	    nm = nm.substring(i+1, nm.length());
	}
	return nm + "(" + name + ")";
    }

    /**
      * Give the x coordinate of the upper-left hand corner of this
      * feature's bounding box at frame 0 of any translations it might
      * contain.
      **/
    public abstract int getStartX();

    /**
      * Give the y coordinate of the upper-left hand corner of this
      * feature's bounding box at frame 0 of any translations it might
      * contain.
      **/
    public abstract int getStartY();

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    abstract public void initialize();

    /**
     * Free any resources held by this feature.  It is the opposite of
     * setup; each call to setup() shall be balanced by
     * a call to unsetup(), and they shall *not* be nested.  
     * <p>
     * It's possible an active phase may be destroyed.  For example,
     * the last phase a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty phase
     * too!).
     **/
    abstract public void destroy();


    /**
     * Change the setup mode of this feature.  The new mode will always
     * be different than the old.
     **/
    abstract protected void setSetupMode(boolean mode);

    /**
     * Change the activated mode of this feature.  The new mode will
     * always be different than the old.
     **/
    abstract protected void setActivateMode(boolean mode);

    /**
     * Do some setup work.  This is called from the SetupManager thread,
     * and is where time-consuming setup (like image loading) should
     * happen.
     **/
    abstract public void doSomeSetup();

    /**
     * This is where the feaure says whether or not it needs more
     * setup.  Calls to this are synchronized within the init manager
     * to avoid race conditions.  The implementation of this method
     * must not call outside code or call any animation manager
     * methods.
     **/
    abstract public boolean needsMoreSetup();

    /**
     * Called by the show when it's time to begin setting up this
     * feature.  This can be called multiple times; each call will
     * eventually be matched by a call to unsetup().
     *
     * @see #unsetup()
     *
     * @return true if this call started setup being done
     **/
    public boolean setup() {
	setupCount++;
	if (setupCount == 1) {
	    setSetupMode(true);
	    return true;
	} else {
	    return false;
	}
    }

    /**
     * Called by the show when this feature is no longer needed
     * by whatever contains it.  When the last call to setup() has been
     * matched by a call to unsetup(), it's time to unload this feature's
     * assets.
     *
     * @see #setup()
     **/
    public void unsetup() {
	setupCount--;
	if (setupCount == 0) {
	    setSetupMode(false);
	}
    }

    /**
     * Called by the show when this feature becomes activated, that is,
     * when it starts presenting.  These nest, so this can be called
     * mutliple times.  When the last call to activate() is undone by
     * a call to deactivate(), that means this feature is no longer
     * being shown.
     *
     * @see #deactivate()
     **/
    final public void activate() {
	activateCount++;
	if (activateCount == 1) {
	    setActivateMode(true);
	}
    }

    /**
     * Called by the show when this feature is no longer being presented
     * by whatever contains it.
     *
     * @see #activate()
     **/
    final public void deactivate() {
	activateCount--;
	if (activateCount == 0) {
	    setActivateMode(false);
	}
    }

    // Used by sendFeatureSetup():
    private Command featureSetupCommand 
        = new Command() {
	    public void execute() {
		Segment s = show.getCurrentSegment();
		if (s != null) {
		    s.runFeatureSetup();
		}
	    }
	};
    /**
     * When a feature finishes its setup, it should call this to
     * tell the show about it.  This happens in the setup thread.
     **/
    protected void sendFeatureSetup() {
	show.runCommand(featureSetupCommand);
	show.runPendingCommands();
	    // It's safe to run the pending commands, because we're
	    // in the setup thread...  If a frame is being painted,
	    // we can wait.
    }

    /**
     * Add areas that need to be erased for the frame that's about
     * to be drawn.  If this feature is inactive, add the area that 
     * it painted to the
     * last time it was displayed.  In either case, add to the erase 
     * set of one of the render areas passed in the context.
     * <p>
     * This method might be called more than once for a given
     * frame of animation (e.g. in a Translator, once for the 
     * previous position, and once for the new position).
     * 
     * @param	context	The context for tracking rendering state
     * @param	srcOver	true if the feature is drawn in SrcOver mode,
     *			as opposed to Src.
     * @param   envChanged  True if the drawing environment changed,
     *			    e.g. the feature moved or was faded.
     **/
    abstract public void addEraseAreas(RenderContext context, boolean srcOver,
    			  	       boolean envChanged);

    /**
     * Add all of the areas that need to be painted with the current
     * frame.  Only areas that changed since the last frame
     * need to be drawn.
     * <p>
     * This will be called exactly once per frame displayed.  Thus,
     * it's a good place to track state, like what's displayed
     * in this frame for reference when we get to the next frame.
     * 
     * @param	context	The context for tracking rendering state
     * @param   envChanged  True if the drawing environment changed,
     *			    e.g. the feature moved or was faded.
     **/
    abstract public void addDrawAreas(RenderContext context,
    			  	       boolean envChanged);



    /**
     * Paint the current state of this feature to gr
     *
     * @param gr  The place to paint to.
     **/
    public abstract void paintFrame(Graphics2D gr);

    /**
     * Called from Segment with the Show lock held, to advance us to
     * the state we should be in for the next frame.
     **/
    public abstract void nextFrame();
}

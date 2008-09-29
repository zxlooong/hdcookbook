
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.SetupClient;

import java.awt.Graphics2D;

/**
 * Represents a feature.  A feature is a thing that presents some sort
 * of UI.  A phase presents some number of features, and features can
 * be shared between phases.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public abstract class Feature implements SetupClient {

    protected Show show;
    
    /**
     * The name of this feature within the show.  This is set to null for
     * private features in the binary file.
     */
    protected String name = null;

    private int activateCount = 0;
    private int setupCount = 0;

    /**
     * @param show	The show this feature is attached to.  The value
     *			can be null, as long as it's set to a real value
     *			before the feature is used.
     **/
    protected Feature(Show show) {
	this.show = show;
    }
    
    /**
     * Sets a name for this feature.  All public features have a name.  
     * Private features might or might
     * not have a name; if they do, it's just for debugging.  This method
     * should only be called when a feature is first created.
     **/    
    public void setName(String name) {
        this.name = name;
    }

    /**
     * All public features have a name.  Private features might or might
     * not have a name; if they do, it's just for debugging.
     *
     * @return the name of this feature, or null if it is not known.
     **/
    public String getName() {
	return name;
    }

    /**
     * Get the upper-left hand corner of this feature as presently displayed.
     * Return Integer.MAX_VALUE if this feature has no visible representation.
     * 
     * @return the x coordinate
     **/
    abstract public int getX();

    /**
     * Get the upper-left hand corner of this feature as presently displayed
     * Return Integer.MAX_VALUE if this feature has no visible representation.
     * 
     * @return the y coordinate
     **/
    abstract public int getY();

    /** 
     * @return a developer-friendly description of this feature, for debugging
     **/
    public String toString() {
        if (Debug.LEVEL > 0) {
            String nm = getClass().getName();
            int i = nm.lastIndexOf('.');
            if (i >= 0) {
                nm = nm.substring(i+1, nm.length());
            }
            return nm + "(" + name + ")";
        } else {
            return super.toString();
        }
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the segments.  Clients of the GRIN framework should never call this
     * method directly.  Custom feature extensions must implement this method.
     **/
    abstract public void initialize();

    /**
     * Free any resources held by this feature.  It is the opposite of
     * initialize().  
     * <p>
     * It's possible an active segment may be destroyed.  For example,
     * the last segment a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty segment
     * too!).
     * <p>
     * Clients of the GRIN framework should never call this method directly.
     * Custom feature extensions must implement this method.
     **/
    abstract public void destroy();


    /**
     * Change the setup mode of this feature.  The new mode will always
     * be different than the old.
     * Clients of the GRIN framework should never call this method directly.
     * Custom feature extensions must implement this method.
     **/
    abstract protected void setSetupMode(boolean mode);

    /**
     * Change the activated mode of this feature.  The new mode will
     * always be different than the old.
     * Clients of the GRIN framework should never call this method directly.
     * Custom feature extensions must implement this method.
     **/
    abstract protected void setActivateMode(boolean mode);

    /**
     * Do some setup work.  This is called from the SetupManager thread,
     * and is where time-consuming setup (like image loading) should
     * happen.
     * Clients of the GRIN framework should never call this method directly.
     * Custom feature extensions must implement this method.
     **/
    abstract public void doSomeSetup();

    /**
     * This is where the feaure says whether or not it needs more
     * setup.  Calls to this are synchronized within the init manager
     * to avoid race conditions.  The implementation of this method
     * must not call outside code or call any animation manager
     * methods.
     * Clients of the GRIN framework should never call this method directly.
     * Custom feature extensions must implement this method.
     **/
    abstract public boolean needsMoreSetup();

    /**
     * Called by the show when it's time to begin setting up this
     * feature.  This might be called from the show multiple times; each call 
     * will eventually be matched by a call to unsetup().
     * Clients of the GRIN framework should never call this method directly,
     * and it should not be overridden.
     * 
     * @see #unsetup()
     *
     * @return true if this call started setup being done
     **/
    final public boolean setup() {
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
    final public void unsetup() {
	setupCount--;
	if (setupCount == 0) {
	    setSetupMode(false);
	}
    }

    /**
     * Check to see if this feature has been set up
     **/
    final public boolean isSetup() {
	return setupCount > 0;
    }

    /**
     * Called by the show when this feature becomes activated, that is,
     * when it starts presenting.  These nest, so this can be called
     * mutliple times.  When the last call to activate() is undone by
     * a call to deactivate(), that means this feature is no longer
     * being shown.
     * Clients of the GRIN framework should never call this method directly,
     * and it should not be overridden.
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
     * Clients of the GRIN framework should never call this method directly,
     * and it should not be overridden.
     *
     * @see #activate()
     **/
    final public void deactivate() {
	activateCount--;
	if (activateCount == 0) {
	    setActivateMode(false);
	}
    }

    private Command featureSetupCommand = null;
    
    /**
     * When a feature finishes its setup, it should call this to
     * tell the show about it.  This happens in the setup thread.
     **/
    protected void sendFeatureSetup() {
        if (featureSetupCommand == null) {
            GrinXHelper c = new GrinXHelper(show);
            c.setCommandNumber(GrinXHelper.FEATURE_SETUP);
            featureSetupCommand = c;
        }
	show.runCommand(featureSetupCommand);
    }

    /**
     * Add all of the areas that are displayed for this feature with the
     * current frame.  This will be called exactly once per frame
     * displayed on each activated feature.
     * <p>
     * A feature that displays something needs to maintain a record
     * of it in a DrawRecord.  The animation framework uses this to
     * track what needs to be erased and drawn from frame to frame.
     * <p>
     * Clients of the GRIN framework should not call this method directly.
     * Feature subclasses must implement this method.
     * 
     * @param	context	The context for tracking rendering state
     *
     * @see com.hdcookbook.grin.animator.DrawRecord
     **/
    abstract public void addDisplayAreas(RenderContext context);


    /**
     * Paint the current state of this feature to gr.
     * Clients of the GRIN framework should not call this method directly.
     * Feature subclasses must implement this method.
     *
     * @param gr  The place to paint to.
     **/
    public abstract void paintFrame(Graphics2D gr);

    /**
     * Called from Segment with the Show lock held, to advance us to
     * the state we should be in for the next frame.
     **/
    public abstract void nextFrame();

    /**
     * Called from the ResetFeatureCommand, this should reset the internal
     * state of the feature to what it was when first activated.
     **/
    public final void resetFeature() {
	if (activateCount > 0) {
	    setActivateMode(false);
	    setActivateMode(true);
	}
    }

    /**
     * Clone the subgraph of a scene graph, rooted at this feature.  This
     * node, any child nodes, and other children below this node in the
     * scene graph are copied.  The original feature must have been initialized
     * and setup must have completed, and it must not currently be activated
     * (that is, visible).  The cloned feature will not be subject
     * to the setup/unsetup lifecycle.  The feature being cloned might be
     * activated or not.
     * <p>
     * The resulting cloned feature will already be initialized, but the
     * caller is responsible for ensuring that destroyClonedSubgraph() is 
     * called on the top node of all cloned feature subgraphs.
     * <p>
     * A cloned feature can be used by adding it to the set of visible
     * features in a group using Group.resetVisibleFeatures().
     * <p>
     * A scene graph can contain a reference to another feature, either by
     * a direct reference, like the model attached to a Translator, or
     * with a command that refers to a feature, like an activate_part command
     * directed to an assembly.  In either case, if the reffered-to feature
     * is in the subgraph being cloned, then the clone is what will be referred
     * to.  If the referred-to feature is outside the subgraph being cloned,
     * then the original feature will be referred to.  It's the caller's
     * responsibility to ensure that this results in a scene graph that gives
     * valid behavior; usually this means that any referred-to feature that
     * carries state (like a translation's model) should be in the set of
     * features being cloned.
     *
     * @param	map	A map from original feature to its clone, which should
     *			initially be empty.  When this method completes it
     *			will be populated with all cloned features, including
     *			the top node of the cloned subgraph.  You can use
     *			this map to locate the clones of features in your 
     *			graph, e.g. named features that you've previously looked
     *			up.  This must not be null.  The key of each entry
     *			is the original feature, and the value is its clone.
     *
     * @throws UnsupportedOperationException if this feature's class doesn't
     *	 	implement cloneFeature(), or if any features or commands this
     *		feature refers to doesn't implement cloning.  All built-in 
     *		GRIN commands and features features do
     *		implement cloning, but extension subclasses might not.
     *		The implementation of cloneFeature() in the Feature superclass
     *		always throws this exception.
     *
     * @throws IllegalStateException may be thrown if the feature has not been 
     *		initialized and set up, if the feature is currently activated
     *	        (that is, visible), or if the feature has been destroyed.  
     *
     * @see com.hdcookbook.grin.features.Group#resetVisibleParts(com.hdcookbook.grin.Feature[])
     * @see #initialize()
     * @see #destroyClonedSubgraph()
     **/
    final public Feature cloneSubgraph(HashMap clones) {
	if (Debug.ASSERT && !clones.isEmpty()) {
	    Debug.assertFail();
	}
	Feature result = makeNewClone(clones);
	clones.put(this, result);
	for (Iterator it = clones.keySet().iterator(); it.hasNext(); ) {
	    Feature key = (Feature) it.next();
	    Feature value = (Feature) clones.get(key);
	    value.initializeClone(key, clones);
	}
	return result;
    }

    /**
     * This is an implementation method
     * that should not be called direction by applications; applications 
     * should call cloneSubgraph().  New subclasses of Feature may override
     * this method.
     * <p>
     * Make a new clone of this feature.  This method creates a new instance
     * of this feature, and creates new instances of any sub-features, but it
     * does not initialize the feature.  This is an implementation method
     * that should not be called direction by applications; applications 
     * should call cloneSubgraph().
     * <p>
     * See the documentation of cloneSubgraph() for a list
     * of runtime exceptions this method can throw.  Subclasses that wish
     * to support cloning must override this method.
     * <p>
     * Whenever you call this method, be sure to add the new clone
     * to the clones map.
     *
     * @param	clones	A map from original feature to cloned feature.  Entries
     *			are added by the <i>caller</i>.
     *
     * @throws UnsupportedOperationException as specified in 
     *		Feature.cloneSubgraph()
     *
     * @throws IllegalStateException as specified in 
     *		Feature.cloneSubgraph()
     *
     * @see #cloneSubgraph(java.util.HashMap)
     **/
    public Feature makeNewClone(HashMap clones) {
	throw new UnsupportedOperationException(getClass().getName()
						    + ".makeNewClone()");
    }

    /**
     * This is an implementation method
     * that should not be called direction by applications; it is called
     * from cloneSubgraph().  New subclasses of Feature may override
     * this method.
     * <p>
     * Initialize this cloned feature from its original.  This is called after
     * the entire subgraph has been cloned, so the HashMap containing the set
     * of clones will be complete.
     * See the documentation of cloneSubgraph() for a list
     * of runtime exceptions this method can throw.  Subclasses that wish
     * to support cloning must override this method.
     * <p>
     * If this feature doesn't need initialization it's OK for a feature
     * to not implement it; the default version of this method does nothing.
     * Typically, you only need to implement this for features that have
     * references to other features that aren't sub-features, or that have
     * commands that might have references to other features.
     *
     * @see #cloneSubgraph(java.util.HashMap)
     **/
    protected void initializeClone(Feature original, HashMap clones) {
    }

    /**
     * Destroy a subgraph of features that was created with
     * cloneSubgraph().  Application code that creates new features
     * using cloneSubgraph() must destroy the set of cloned features
     * by calling this method on the top node of each cloned subgraph.
     * In other words, every call to cloneSubgraph() should be balanced
     * by a call to destroyClonedSubgraph().  This should be done when
     * the cloned subgraph is not part of any group, but before the
     * containing show is destroyed.
     *
     * @see #cloneSubgraph(java.util.HashMap)
     **/
    public final void destroyClonedSubgraph() {
	HashSet set = new HashSet();
	addSubgraph(set);
	for (Iterator it = set.iterator(); it.hasNext(); ) {
	    Feature f = (Feature) it.next();
	    f.destroy();
	}
    }

    /**
     * This is an implementation method
     * that is not intended to be called direction by applications.
     * New subclasses of Feature may override
     * this method.
     * <p>
     * Add this node and all of its descendent nodes to the given set.
     * The superclass definition of this method adds the current node.
     * Any node types that have children should override this method to
     * call the superclass version, then recursively invoke this method
     * on each child.
     **/
    public void addSubgraph(HashSet set) {
	set.add(this);
    }


    /**
     * Return a reference to the feature f for use within a reference.
     * If some other feature contains a reference to this feature (other
     * than an "owning" parent-child reference), then this method will
     * give a reference to a clone of the feature being called, if
     * one was made, or if not, to this original feature.
     *
     * @param f		The feature reference.  This may be null.
     **/
    protected static Feature clonedReference(Feature f, HashMap clones) {
	if (f == null) {
	    return null;
	}
	Object result = clones.get(f);
	if (result == null) {
	    return f;
	} else {
	    return (Feature) result;
	}
    }

    /**
     * Clone a command array within a feature.  This is used by some features'
     * implementation of makeNewClone().
     *
     *  @param commands		The array to clone.  May be null.
     *  @param	clones		A map from original feature to their clones.
     *
     * @throws UnsupportedOperationException as specified in 
     *		Feature.cloneSubgraph()
     *
     * @throws IllegalStateException as specified in 
     *		Feature.cloneSubgraph()
     **/
    protected static Command[] cloneCommands(Command[] commands, HashMap clones)
    {
	if (commands == null || commands.length == 0) {
	    return commands;
	}
	Command[] result = new Command[commands.length];
	boolean changed = false;
	for (int i = 0; i < commands.length; i++) {
	    result[i] = commands[i].cloneIfNeeded(clones);
	    changed = changed || result[i] != commands[i];
	}
	if (!changed) {
	    result = commands;
	}
	return result;
    }
}

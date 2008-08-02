
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

import com.hdcookbook.grin.features.Assembly;

/**
 * This class is a supertype that xlets can subclass to interact with a show.
 * The java_command commands can access the directory and do a downcast as
 * a way of getting into the xlet.  Director also defines various protected
 * methods to notify the xlet of Show state changes, and to allow the xlet to
 * insert itself into the animation loop.  Shows that are created without a
 * director will be given a default director that is a direct instance of
 * this class.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public class Director {

    private Show show;

    /**
     * Create a new Director.
     **/
    public Director()  {
    }
    
    //
    // Called from Show constructor
    //
    void setShow(Show show) {
	this.show = show;
    }

    /**
     * Get the show we're managing.
     **/
    public Show getShow() {
	return show;
    }

    /**
     * Notify the director that the model is moving to the next frame.
     * Subclasses that override this method must call super.notifyNextFrame()
     * at least once.  The implementation of this method in Director causes
     * the show to execute all pending commands, which can result in model
     * state changes, such as selecting new segments, changing the selected
     * part in an assembly, etc.  Usually, you'll probably want to call
     * super.notifyNextFrame() first thing, but it may be useful in some
     * circumstances to do some compation before, e.g. something that might
     * result in posting a command to the show.
     * <p>
     * This method is called after the scene graph's model has moved to the 
     * upcoming frame's state.  Xlets that override this method may wish to
     * update user-programmable node values in the body of this method.  This
     * method and the execute() body of a command are the only safe times
     * for user code to update show nodes.
     * <p>
     * This method is called with the show lock held, that is, within a
     * synchronized block in this director's show.  It's essential to hold
     * the show lock when updating the scene graph, e.g. so that changes
     * from remote control keypresses do not happen at the same time.
     * Xlets that override notifyNextFrame() should be careful that any
     * xlet state they access is done in a thread-safe way.
     * <p>
     * If you want to run some Java code to update the scene graph for every
     * frame when the show is in certain states, it might be easier to make
     * a timer that goes off every frame, invoking a java_command.
     **/
    public void notifyNextFrame() {
	show.runPendingCommands();
    }

    /**
     * Notify the director that a new segment has been activated. 
     * <p>
     * This method is called with the show lock held, that is, within a
     * synchronized block in this director's show.  It's essential to hold
     * the show lock when updating the scene graph, e.g. so that changes
     * from remote control keypresses do not happen at the same time.
     * Xlets that override this method should be careful that any
     * xlet state they access is done in a thread-safe way.
     * <p>
     * The default implementation of this method does nothing,
     * so there is no need to call super.notifySegmentActivated().
     *
     * @param newSegment	The new segment that was activated
     * @param oldSegment	The old segment that was previously active
     **/
    public void 
    notifySegmentActivated(Segment newSegment, Segment oldSegment) {
    }


    /**
     * Notify the director that a new part has been selected within an assembly.
     * <p>
     * This method is called with the show lock held, that is, within a
     * synchronized block on this director's show.  It's essential to hold
     * the show lock when updating the scene graph, e.g. so that changes
     * from remote control keypresses do not happen at the same time.
     * Xlets that override this method should be careful that any
     * xlet state they access is done in a thread-safe way.
     * <p>
     * The default implementation of this method does nothing,
     * so there is no need to call super.notifyAssemblyPartSelected().
     *
     * @param assembly		The assembly within which a new part was
     *				selected
     * @param newPart		The new part that's now selected
     * @param oldPart		The old part that used to be selected.
     * @param assemblyIsActive	True if assembly is currently active, that
     *				is, being displayed.
     **/
    public void notifyAssemblyPartSelected(Assembly assembly, 
					   Feature newPart, Feature oldPart,
					   boolean assemblyIsActive) 
    {
    }

}


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
import java.awt.Graphics2D;
import java.awt.Component;

/**
 * An AnimationClient is a client of an AnimationEngine.  When it's
 * set up, an AnimationEngine calls an AnimationClient to do some
 * amount of drawing, once per frame.  An AnimationEngine may have 
 * multiple AnimationClient instances that it is managing.
 **/
public interface AnimationClient {

    /**
     * Initialize the animation client.  This is called from the
     * animation worker thread before starting the animation loop.
     *
     * @param  component The component this show will eventually be displayed
     *                   in.  It's used for things like
     *                   Component.prepareImage().
     **/
    public void initialize(Component component)
	throws InterruptedException;


    /**
     * Destroy this animatin client.  This is called from the animation
     * worker thread before ending the animation loop.
     **/
    public void destroy() throws InterruptedException;

    /**
     * Advance the state of the show to the next frame.  This is
     * called once per frame; the first time it is called can be
     * considered "frame 0", and can monotonically increase from there.
     * <p>
     * This method can be called multiple times before any attempt is
     * made to display the UI state.  This happens when animation falls
     * behind; the engine catches up by skipping frames.  Animation
     * clients should perform only quick updates in this method; any
     * more time-consuming calculations should be deferred until
     * an object is first painted for a given frame.
     *
     * @throws	InterruptedException	if the thread has been interrupted
     *					(e.g. because the xlet is being killed)
     **/
    public void nextFrame() throws InterruptedException;

    /**
     * Indicate to the animation client that we're not behind in the
     * animation, so that the current frame will actually be displayed.
     * Clients shouldn't make any changes to the model in this call; all
     * such changes need to happen in nextFrame()
     *
     * @throws	InterruptedException	if the thread has been interrupted
     *					(e.g. because the xlet is being killed)
     *
     * @see #nextFrame()
     **/
    public void setCaughtUp() throws InterruptedException;

    /**
     * Tell the animation manager what areas need to be drawn to for the
     * next frame.  This is called just after setCaughtUp(), and just
     * before paintFrame().  
     * <p>
     * In this call, the client must indicate where it intends to draw
     * by calling methods on RenderContext.  Internally, a RenderContext
     * keeps a number of RenderArea targets.  Each RenderArea will
     * keep track of a bounding rectangle of all of the indended drawing
     * operations within that area.  When the call to addDisplayAreas
     * is complete, the animation manager may merge some render areas,
     * or may leave them seperate; it will then call paintFrame() as
     * many times as it needs to, with a different clip rect each time.
     * <p>
     * The number of RenderArea targets is set up when an AnimationEngine
     * is created.  Each AnimationClient is passed the same set of targets.
     * If there are multiple AnimationClient instances attached to an
     * AnimationEngine, it is up to the programmer to decide which RenderArea
     * targets should be shared between clients so as to optimize drawing
     * performance.
     * <p>
     * Often, an AnimationClient only needs to erase or draw objects
     * that have changed.  However, under certain circumstances, the
     * AnimationClient will be asked to redraw everything.  This will
     * happen on the first frame drawn, and possibly on others.  For
     * example, with repaint draw and platform double-buffering, the
     * platform erases the buffer for each frame, so a full redraw is
     * required.  When this happens, the animation enginer automatically
     * adds the full extent of the component to one of the targets.
     * In this case, it still calls this method, so that items we draw
     * have the opportunity to erase themselves if needed.
     * <p>
     * This method will be called exactly once for each frame displayed.
     * Because paintFrame can be called multiple times per frame, any state 
     * maintained by the animation client to optimize display areas should 
     * be updated in this method, and not in paintFrame().
     *
     * @param targets		The set of targets the client
     *				can draw to.
     *
     * @throws	InterruptedException	if the thread has been interrupted
     *					(e.g. because the xlet is being killed)
     **/
    public void addDisplayAreas(RenderContext targets)
	throws InterruptedException;

    /**
     * Paint the current frame of the animation.  This is called after
     * addDisplayAreas(), as the last step in a cycle through the animation
     * loop.  This might be called multiple times for a given frame
     * of animation, with a different clip rect set each time.  It also
     * might be called zero times, if no display areas were added.  The callee
     * should leave the graphics context in the same state as it was found in
     * initially, notably:
     * <ul>
     *     <li> Src drawing mode
     *     <li> clip rect set to the full extent of this drawing operation
     * </ul>
     * <p>
     * The animation client shouldn't erase screen areas in this call.  That
     * can be handled more efficiently (for some drawing styles) via
     * <code>RenderArea.clearAndAddArea()</code>
     *
     * @param gr	The graphics context to draw to, set to Src drawing mode
     *
     * @throws	InterruptedException	if the thread has been interrupted
     *					(e.g. because the xlet is being killed)
     *
     * @see RenderArea#clearAndAddArea(int, int, int, int)
     **/
    public void paintFrame(Graphics2D gr)
    	throws InterruptedException ;
}

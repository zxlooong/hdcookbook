/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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


/** 
 * This is an exension feature for a Playlist.  Note that this class
 * depends on GEM APIs to start and stop video.  For GrinView use,
 * there's an SE version of this class, with the same classname.  It's
 * important that the public methods of this class be kept in sync with
 * the SE version.
 * <p>
 * This feature is designed to be used in conjunction with java_command
 * commands.  There are no built-in commands to start and stop video,
 * but you can make a java_command that calls the relevant public
 * methods.  GrinBunny demonstrates this technique - see
 * F:BackgroundVideo.
 * <p>
 * Generally speaking, you call methods on a playlist, and the right
 * thing happens.  So, if you <code>start()</code> a playlist, then
 * it will find the global JMF player, stop any currently playing
 * video, start the new video, and register a listener for notification
 * when that video actually starts.  Callbacks when the state of playing
 * changes are done via GRIN commands sent to the show.
 * <p>
 * In order to use a playlist, you must initialize and destroy the
 * PlayerWrangler singleton.  
 *
 * @see PlayerWrangler
 */

package com.hdcookbook.grin.media;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import org.bluray.net.BDLocator;

import java.awt.Graphics2D;
import java.io.IOException;


public class Playlist extends Feature implements Node {

    private BDLocator locator;
    protected Command[] onActivate;
    protected Command[] onMediaStart;
    protected Command[] onMediaEnd;
    protected Command[] onDeactivate;
    protected boolean autoStart;
    protected boolean autoStop;

    private boolean activated;

    public Playlist(Show show) {
	super(show);
    }

    protected void setLocator(String locator) {
	PlayerWrangler wrangler = PlayerWrangler.getInstance();
	this.locator = wrangler.createLocator(locator);
    }

    protected String getLocator() {
	return locator.toString();
    }

    /**
     * Start video playback.  The playlist finds the main player, stops
     * anything else that's playing, and starts video playback.  When the
     * video actually starts, the onMediaStart commands are triggered.
     * Starting any other playlist will grab playback away from this
     * playlist, and cease the triggering of any commands from this
     * playlist.
     * <p>
     * This may be called on a deactivated player.  A deactivated
     * player won't react to media events (by posting the onXXX commands).
     **/
    public void start() {
	PlayerWrangler wrangler = PlayerWrangler.getInstance();
	wrangler.start(this, locator);
    }

    //
    // Called from PlayerWrangler, with a lock held.  We mustn't do
    // anything here that requires a non-local lock.
    //
    void notifyMediaStart() {
	synchronized(this) {
	    if (!activated) {
		return;
	    }
	}
	show.runCommands(onMediaStart);
    }

    /**
     * Stop video playback.  If this playlist is currently playing, this
     * halts playback, and does not replace the currently playing video with
     * anything else.  The onMediaEnd commands are not triggered when this
     * method is called, and will not be triggered under any circumstances
     * after stop() has been called, unless playback is first re-started.
     * <p>
     * This may be called on a deactivated player.  A deactivated
     * player won't react to media events (by posting the onXXX commands).
     **/
    public void stop() {
	PlayerWrangler wrangler = PlayerWrangler.getInstance();
	wrangler.stop(this);
    }

    //
    // Called from PlayerWrangler, with a lock held.  We mustn't do
    // anything here that requires a non-local lock.
    //
    void notifyMediaEnd() {
	synchronized(this) {
	    if (!activated) {
		return;
	    }
	}
	show.runCommands(onMediaEnd);
    }

    /**
     * Reset the locator.  Re-setting the locator does not change any
     * running playback, but does change the video that will be shown
     * when video is next started.
     **/
    public void resetLocator(String locator) {
	setLocator(locator);
    }

    /** 
     * {@inheritDoc}
     **/
    public int getX() {
	return 0;
    }

    /** 
     * {@inheritDoc}
     **/
    public int getY() {
	return 0;
    }

    /** 
     * {@inheritDoc}
     **/
    public void initialize() {
    }

    /** 
     * {@inheritDoc}
     **/
    public void destroy() {
    }

    /** 
     * {@inheritDoc}
     **/
    protected int setSetupMode(boolean mode) {
	return 0;
    }

    /** 
     * {@inheritDoc}
     **/
    protected void setActivateMode(boolean mode) {
	synchronized(this) {
	    activated = mode;
	}
	if (mode) {
	    show.runCommands(onActivate);
	    if (autoStart) {
		start();
	    }
	} else {
	    show.runCommands(onDeactivate);
	    if (autoStop) {
		stop();
	    }
	}
    }

    /** 
     * {@inheritDoc}
     **/
    public boolean needsMoreSetup() {
	return false;
    }

    /** 
     * {@inheritDoc}
     **/
    public void addDisplayAreas(RenderContext context) {
    }

    /** 
     * {@inheritDoc}
     **/
    public void markDisplayAreasChanged() {
    }

    /** 
     * {@inheritDoc}
     **/
    public void paintFrame(Graphics2D gr) {
    }

    /** 
     * {@inheritDoc}
     **/
    public void nextFrame() {
    }

    public void readInstanceData(GrinDataInputStream in, int length)
                throws IOException
    {
	in.readSuperClassData(this);

	setLocator(in.readString());
	onActivate = in.readCommands();
	onMediaStart = in.readCommands();
	onMediaEnd = in.readCommands();
	onDeactivate = in.readCommands();
	autoStart = in.readBoolean();
	autoStop = in.readBoolean();
    }
}

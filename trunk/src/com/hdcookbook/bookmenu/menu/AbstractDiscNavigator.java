
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


package com.hdcookbook.bookmenu.menu;

import com.hdcookbook.grin.util.Debug;

import java.io.IOException;
import java.util.Enumeration;

import javax.media.Control;
import javax.media.Player;
import javax.media.Time;
import javax.media.Manager;
import javax.media.ControllerListener;
import javax.media.ControllerEvent;
import javax.media.protocol.DataSource;
import javax.tv.locator.InvalidLocatorException;
import javax.tv.service.SIManager;
import javax.tv.service.selection.ServiceContext;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContentHandler;
import javax.tv.service.selection.ServiceMediaHandler;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.xlet.XletContext;

import org.davic.media.MediaLocator;
import org.davic.media.MediaTimePositionControl;
import org.havi.ui.HSound;

import org.dvb.application.AppAttributes;
import org.dvb.application.AppID;
import org.dvb.application.AppProxy;
import org.dvb.application.AppsDatabase;
import org.dvb.application.CurrentServiceFilter;

import org.bluray.net.BDLocator;
import org.bluray.media.InvalidPlayListException;
import org.bluray.media.PlayListChangeControl;
import org.bluray.media.PlaybackControl;
import org.bluray.media.PlaybackListener;
import org.bluray.media.PlaybackMarkEvent;
import org.bluray.media.PlaybackPlayItemEvent;
import org.bluray.media.StreamNotAvailableException;
import org.bluray.media.SubtitlingControl;
import org.bluray.ti.Title;
import org.bluray.ti.selection.TitleContext;

/**
 * Navigate the disc.  This is an abstract superclass for a
 * singleton is used by an xlet
 * to seek to different parts of the disc, change subtitles,
 * and stuff like that.  Basically, anything involving a locator.
 * <p>
 * This class assumes that when a title is selected, there is no
 * autoplay video.  After a title starts, the controlling Xlet should 
 * start video using gotoPlaylistInCurrentTitle().  In addition to
 * starting the video, this ensures that a Player under our control
 * has been created and is managing playback.  Title selection using
 * a ServiceContext can take that control away from our Player, so
 * going to a playlist ensures that it's restored.
 **/


public abstract class AbstractDiscNavigator 
		implements PlaybackListener, ControllerListener
{
    private XletContext xletContext;
    /**
     * The player for the main A/V content.  This will be set
     * as soon as we navigate to our first  playlist.
     **/
    protected Player mainPlayer;
    private PlayListChangeControl playlistControl;
    private MediaTimePositionControl timePositionControl;
    private PlaybackControl playbackControl;
    private SubtitlingControl subtitlingControl;
    private TitleContext titleContext;

    private static SIManager siManager;

    protected AbstractDiscNavigator(XletContext xletContext) {
	this.xletContext = xletContext;
    }

    protected static BDLocator makeBDLocator(String ls) {
	if (Debug.LEVEL > 1) {
	    Debug.println("Making BD locator " + ls);
	}
	try {
	    return new BDLocator(ls);
	} catch (Exception ex) {
	    ex.printStackTrace();
	    return null;
	}
    }

    protected static MediaLocator makeMediaLocator(String ls) {
	return new MediaLocator(makeBDLocator(ls));
    }

    protected static HSound makeSound(String ls) {
	try {
	    MediaLocator ml = makeMediaLocator(ls);
	    HSound hs = new HSound();
	    hs.load(ml.getURL());
	    return hs;
	} catch (Throwable t) {
	    t.printStackTrace();
	    if (Debug.LEVEL > 0) {
		Debug.println();
		Debug.println("****  Failed to load sound " + ls + "  *****");
		Debug.println(t);
		Debug.println();
	    }
	    return null;
	}
    }

    protected static Title makeTitle(String ls) {
	BDLocator loc = makeBDLocator(ls);
	try {
	    if (siManager == null) {
		siManager = SIManager.createInstance();
	    }
	    return (Title) siManager.getService(loc);
	} catch (InvalidLocatorException ignored) {
	    ignored.printStackTrace();
	    return null;
	} catch (SecurityException ex) {
	    ex.printStackTrace();
	    if (Debug.LEVEL > 0) {
		Debug.println();
		Debug.println("*** Permission denied for creating Title "+loc);
		Debug.println("*** Only signed xlets can do this.");
		Debug.println();
	    }
	    return null;
	}
    }


    public synchronized void selectTitle(Title title) {
	if (titleContext == null) {
	    try {
		ServiceContextFactory scf = ServiceContextFactory.getInstance();
		titleContext = (TitleContext)scf.getServiceContext(xletContext);
	    } catch (ServiceContextException ignored) {
		ignored.printStackTrace();
		if (Debug.ASSERT) {
		    Debug.assertFail();
		}
	    }
	}
	if (Debug.LEVEL > 0) {
	    Debug.println("*** Changing title to " + title.getLocator()+" ***");
	}
	titleContext.start(title, false);
    }

    public synchronized void gotoPlaylistInCurrentTitle(BDLocator loc) {
	if (mainPlayer == null) {
	    try {
		MediaLocator ml = new MediaLocator(loc);
		mainPlayer  = Manager.createPlayer(ml);
	    } catch (Exception ignored) {
		ignored.printStackTrace();
		if (Debug.ASSERT) {
		    Debug.assertFail();
		}
	    }
	    mainPlayer.addControllerListener(this);
	    mainPlayer.prefetch();
	    Control[] controls = mainPlayer.getControls();
	    for (int i = 0; i < controls.length; i++) {
		if (controls[i] instanceof PlayListChangeControl) {
		    playlistControl = (PlayListChangeControl) controls[i];
		} else if (controls[i] instanceof PlaybackControl) {
		    playbackControl = (PlaybackControl) controls[i];
		} else if (controls[i] instanceof SubtitlingControl) {
		    subtitlingControl = (SubtitlingControl) controls[i];
		} else if (controls[i] instanceof MediaTimePositionControl) {
		    timePositionControl = (MediaTimePositionControl)controls[i];
		}
	    }
	    if (Debug.LEVEL > 1) {
		Debug.println("Playback control:  " + playbackControl);
		Debug.println("Playlist control:  " + playlistControl);
		Debug.println("Subtitling control:  " + subtitlingControl);
	    }
	    if (Debug.ASSERT && 
		 (playbackControl == null || playlistControl == null
		  || subtitlingControl == null || timePositionControl == null))
	    {
		Debug.assertFail();
	    }
	    playbackControl.addPlaybackControlListener(this);
	} else {
	    // We had already created the player, so we can use
	    // org.bluray.media.PlayListChangeControl
	    mainPlayer.stop();
	    try {
		playlistControl.selectPlayList(loc);
	    } catch (Exception ignored) {
		ignored.printStackTrace();
	    }
	}
	if (Debug.LEVEL > 0) {
	    Debug.println("*** Changing playlist to " + loc + " ***");
	}
	mainPlayer.start();
	setupVideoForPlaylist(loc);
    }


    /**
     * After a playlist is selected, this method is called to let
     * the subclass do any other setup, like subtitles or audio
     * stream.
     **/
    protected abstract void setupVideoForPlaylist(BDLocator loc);

    /**
     * Navigate to the given time in the video determined by the playlist
     **/
    public synchronized void gotoMediaTime(BDLocator playlist, long mediaTime) {
	gotoPlaylistInCurrentTitle(playlist);
	if (waitForStarted(2000)) {
	    timePositionControl.setMediaTimePosition(new Time(mediaTime));
	}
    }

    public synchronized void selectSubtitles(boolean on, int streamNum) {
	if (subtitlingControl != null) {
	    if (Debug.LEVEL > 0) {
		Debug.println("Subtitles set " + on + ", " + streamNum);
	    }
	    subtitlingControl.setSubtitling(on);
	    if (on) {
		try {
		    subtitlingControl.selectStreamNumber(streamNum);
		} catch (StreamNotAvailableException ignored) {
		    if (Debug.LEVEL > 0) {
			Debug.println("*** Subtitles stream " + streamNum 
				      + " not available.");
		    }
		}
	    }
	}
    }

    /**
     * @return the current media time, or Long.MIN_VALUE if it can't
     *         be determined.
     **/
    public synchronized long getMediaTime() {
	if (timePositionControl == null) {
	    // Should never happen
	    return Long.MIN_VALUE;
	}
	Time t = timePositionControl.getMediaTimePosition();
	if (t == null) {
	    return Long.MIN_VALUE;
	}
	return t.getNanoseconds();
    }

    public synchronized void destroy() {
	if (playbackControl != null) {
	    playbackControl.removePlaybackControlListener(this);
	}
	if (mainPlayer != null) {
	    mainPlayer.removeControllerListener(this);
	}
    }

    /**
     * From ControllerListener
     **/
    public synchronized void controllerUpdate(ControllerEvent event) {
	notifyAll();
	// Rather than parse the controllerEvent, we just 
	// poll the player status in a wait loop.  See waitForStarted(),
	// for example.
    }

    //
    // Wait until the state of our player is "started".
    //
    private synchronized boolean waitForStarted(long timeout) {
	long tm = 0;
	if (timeout > 0) {
	    tm = System.currentTimeMillis();
	}
	for (;;) {
	    if (mainPlayer.getState() == Player.Started) {
		return true;
	    }
	    if (!waitWithTimeout(tm, timeout)) {
		return false;
	    }
	}
    }

    //
    // Wait a bit.  Return true if we got notified, false if we timed out
    // or were interrupted.
    private boolean waitWithTimeout(long startTime, long timeout) {
	try {
	    if (timeout <= 0) {
		wait();
		return true;
	    } else {
		long t = timeout - (System.currentTimeMillis() - startTime);
		if (t <= 0) {
		    return true;
		}
		wait(t);
		t = timeout - (System.currentTimeMillis() - startTime);
		return t > 0;
	    }
	} catch (InterruptedException ex) {
	    Thread.currentThread().interrupt();
	    return false;
	}
    }
}

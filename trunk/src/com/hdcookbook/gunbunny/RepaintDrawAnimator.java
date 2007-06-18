

package com.hdcookbook.gunbunny;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import org.bluray.ui.SyncFrameAccurateAnimation;
import org.bluray.ui.AnimationParameters;

import com.hdcookbook.gunbunny.util.Debug;
import com.hdcookbook.gunbunny.util.ImageUtil;

/**
 * An Animator using the repaint draw drawing model
 **/

public class RepaintDrawAnimator extends Animator {

    private Rectangle position;
    private Container container;
    private long startTime;
    private int startFrame;

    public RepaintDrawAnimator() {
    }

    public Rectangle getPosition() {
	return position;
    }

    /**
     * Called to initialize an animator to start presenting at the
     * given frame.
     **/
    public synchronized void initAtFrame(int frame, Container container, 
    					 Rectangle position) 
    {
	this.container = container;
	this.position = position;
	this.startFrame = frame;
	this.startTime = System.currentTimeMillis();
    }

    public void destroy() {
    }

    /**
     * Get a buffer for double-buffered drawing.  If one is not needed
     * for this style of animation, return null.
     **/
    public synchronized BufferedImage getDoubleBuffer(int width, int height) {
	return null;
    }

    /**
     * Get the graphics for drawing into the buffer returned by the last
     * call to getDoubleBuffer().  If one is not
     * needed for this style of animation, return null.  The drawing
     * mode of the graphics will be set to AlphaComposite.Src.
     **/
    public Graphics2D getDoubleBufferGraphics() {
	return null;
    }

    /**
     * Return true if this animator needs the sprites to erase themselves.
     **/
    public boolean needsErase() {
	return false;
    }

    /**
     * Called by the main loop once per frame.
     **/
    public void animateGame(int frame, Game game) throws InterruptedException {
	if (Debug.LEVEL > 0 && frame % 100 == 0) {
	    Debug.println("Frame " + (frame - startFrame));
	}
	long now = System.currentTimeMillis();
	long fTime = ((frame - startFrame) * 1000L) / 24L + startTime;
	if (now < fTime) {	// We're ahead
	    Thread.sleep(fTime - now);
	} else {
	    long nextF = ((frame + 1 - startFrame) * 1000L) / 24L + startTime;
	    if (now >= nextF) {
	    	// We're behind.  However, if we drop a frame it's more likely
		// to happen because we don't get a repaint call.
		return;
	    }
	}
	synchronized(game) {
	    game.advanceToFrame(frame);
	}
	// We could make this more efficient by giving the game the
	// means to give us a bounding box
	container.repaint(position.x, position.y, 
			  position.width, position.height);
    }
}

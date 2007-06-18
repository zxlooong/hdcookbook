

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
 * An Animator using the sync frame accurate animation drawing model
 **/

public class SFAAAnimator extends Animator {

    private Rectangle position;
    private boolean firstFrame;
    private Container container;
    private SyncFrameAccurateAnimation sfaa;
    private int startFrame;
    private int framesDropped;
    private boolean destroyed = false;

    public SFAAAnimator() {
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
	this.firstFrame = true;
	this.startFrame = frame;
	Dimension sz = new Dimension(position.width, position.height);
	AnimationParameters ap = new AnimationParameters();
	ap.threadPriority = Thread.NORM_PRIORITY - 1;
	SyncFrameAccurateAnimation.setDefaultFrameRate(
		SyncFrameAccurateAnimation.FRAME_RATE_24);
	this.sfaa = SyncFrameAccurateAnimation.getInstance(sz, 1, ap);
	container.add(this.sfaa);
	this.sfaa.setLocation(position.x, position.y);
	this.sfaa.setVisible(true);
	this.sfaa.start();
    }

    public void destroy() {
	synchronized(this) {
	    destroyed = true;
	}
	container.remove(sfaa);
	sfaa.stop();
	sfaa.destroy();
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
	return true;
    }

    /**
     * Called by the main loop once per frame.
     **/
    public void animateGame(int frame, Game game) throws InterruptedException {
	synchronized(this) {
	    if (destroyed) {
		return;
	    }
	}
	Graphics2D g = sfaa.startDrawing(frame - startFrame);
	if (g == null) {
	    framesDropped++;
	} else {
	    if (firstFrame) {
		g.setColor(ImageUtil.colorTransparent);
		g.fillRect(0, 0, position.width, position.height);
	    }
	    synchronized(game) {
		game.advanceToFrame(frame);
		game.paintFrame(g, firstFrame, this);
	    }
	    firstFrame = false;
	    synchronized(this) {
		if (destroyed) {
		    return;
		}
	    }
	    sfaa.finishDrawing(frame - startFrame);
	}
	if (Debug.LEVEL > 0 && frame % 100 == 0) {
	    Debug.println("Frame " + (frame - startFrame) + ", " 
	    		    + framesDropped + " frames dropped.");
	}
    }
}

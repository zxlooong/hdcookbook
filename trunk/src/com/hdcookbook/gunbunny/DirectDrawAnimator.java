

package com.hdcookbook.gunbunny;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import com.hdcookbook.gunbunny.util.Debug;
import com.hdcookbook.gunbunny.util.ImageUtil;

/**
 * An Animator using the direct draw drawing model
 **/

public class DirectDrawAnimator extends Animator {

    private Rectangle position;
    private boolean firstFrame;
    private Container component;
    private BufferedImage buffer;
    private Graphics2D bufferG;
    private Graphics2D componentG;
    private long startTime;
    private int startFrame;
    private int framesDropped;

    public DirectDrawAnimator() {
    }

    public Rectangle getPosition() {
	return position;
    }

    /**
     * Called to initialize an animator to start presenting at the
     * given frame.
     **/
    public synchronized void initAtFrame(int frame, Container component, 
    					 Rectangle position) 
    {
	this.component = component;
	this.position = position;
	this.firstFrame = true;
	createNewBuffer(256, 256);
		// Pick a big enough default size so that growth
		// is unlikley
	componentG = (Graphics2D) component.getGraphics();
	componentG.translate(position.x, position.y);
	componentG.setClip(0, 0, position.width, position.height);
	componentG.setComposite(AlphaComposite.Src);
	this.startFrame = frame;
	this.startTime = System.currentTimeMillis();
    }

    private synchronized void createNewBuffer(int width, int height) {
	buffer = component.getGraphicsConfiguration()
			.createCompatibleImage(width, height);
	bufferG = buffer.createGraphics();
	bufferG.setComposite(AlphaComposite.Src);
    }

    public void destroy() {
	// nothing needed for a DirectDrawAnimator
    }

    /**
     * Get a buffer for double-buffered drawing.  If one is not needed
     * for this style of animation, return null.
     **/
    public synchronized BufferedImage getDoubleBuffer(int width, int height) {
	if (width > buffer.getWidth() || height > buffer.getHeight()) {
	    if (buffer.getWidth() > width) {
		width = buffer.getWidth();
	    }
	    if (buffer.getHeight() > height) {
		height = buffer.getHeight();
	    }
	    createNewBuffer(width, height);
	}
	return buffer;
    }

    /**
     * Get the graphics for drawing into the buffer returned by the last
     * call to getDoubleBuffer().  If one is not
     * needed for this style of animation, return null.  The drawing
     * mode of the graphics will be set to AlphaComposite.Src.
     **/
    public Graphics2D getDoubleBufferGraphics() {
	return bufferG;
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
	if (Debug.LEVEL > 0 && frame % 100 == 0) {
	    Debug.println("Frame " + (frame - startFrame) + ", " 
	    		    + framesDropped + " frames dropped.");
	}
	long now = System.currentTimeMillis();
	long fTime = ((frame - startFrame) * 1000L) / 24L + startTime;
	if (now < fTime) {	// We're ahead
	    Thread.sleep(fTime - now);
	} else {
	    long nextF = ((frame + 1 - startFrame) * 1000L) / 24L + startTime;
	    if (now >= nextF) {
		framesDropped++;
		return;
	    }
	}
	if (firstFrame) {
	    componentG.setColor(ImageUtil.colorTransparent);
	    componentG.fillRect(0, 0, position.width, position.height);
	}
	synchronized(game) {
	    game.advanceToFrame(frame);
	    game.paintFrame(componentG, firstFrame, this);
	}
	Toolkit.getDefaultToolkit().sync();
	firstFrame = false;
    }
}

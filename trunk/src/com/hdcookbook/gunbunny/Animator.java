

package com.hdcookbook.gunbunny;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * An Animator is something that repeatedly repaints a set of sprites in
 * order to animate a game.
 **/

public abstract class Animator {

    private Rectangle scratchRectangle = new Rectangle();

    /**
     * Get the x,y position and the width and height we're animating over
     **/
    public abstract Rectangle getPosition();
    /**
     * Called to initialize an animator to start presenting at the
     * given frame.
     **/
    public abstract void initAtFrame(int frame, Container container,
    				     Rectangle position);

    public abstract void destroy();

    /**
     * Get a buffer for double-buffered drawing.  If one is not needed
     * for this style of animation, return null.
     **/
    public abstract BufferedImage getDoubleBuffer(int width, int height);

    /**
     * Get the graphics for drawing into the buffer returned by the last
     * call to getDoubleBuffer().  If one is not
     * mode of the graphics will be set to AlphaComposite.Src.
     **/
    public abstract Graphics2D getDoubleBufferGraphics();

    public Rectangle getScratchRectangle() {
	return scratchRectangle;
    }

    /**
     * Return true if this animator needs the sprites to erase themselves.
     **/
    public abstract boolean needsErase();


    /**
     * Called by the main loop once per frame.
     **/
    public abstract void animateGame(int frame, Game game) 
    		throws InterruptedException;
}

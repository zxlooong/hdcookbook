package com.hdcookbook.gunbunny;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.util.Random;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 *
 */
public class TurtleSaucer {
    private Image turtleSaucerBlam;
    private Image turtleSaucer;
    private Rectangle ourBounds = new Rectangle();
    private ImageSprite sprite;
    private boolean active = false;
    private boolean startBlam = false;
    private int blamFramesLeft = -1;	// -1 means not blamming
    private int dx;
    private int xloc;
    private int yloc = 80;
    private Random random = new Random();
    
    public TurtleSaucer(Image turtleSaucer, Image turtleSaucerBlam) {
        this.turtleSaucer = turtleSaucer;
        this.turtleSaucerBlam = turtleSaucerBlam;
	this.sprite = new ImageSprite(turtleSaucer);
	this.sprite.initImageOff();
    }

    public void nextFrame(int numFrames) {
	for (int i = 0; i < numFrames; i++) {
	    if (startBlam) {
		startBlam = false;
		blamFramesLeft = 14;
	    } 
	    if (blamFramesLeft >= 0) {
		xloc += dx;
		blamFramesLeft--;
	    } else if (!active && random.nextInt(24 * 5) == 7) {
		// Make a saucer every 5 seconds or so
		active = true;
		if (random.nextInt(2) == 0)  {
		    xloc = -10;
		    dx = 15;
		} else {
		    xloc = 1840;
		    dx = -15;
		}
	    } else if (active) {
		xloc += dx;
		if (xloc > 1840 || xloc < -10) {
		    active = false;
		}
	    }
	}
	if (active) {
	    sprite.nextFrame(xloc, yloc, turtleSaucer);
	} else if (blamFramesLeft >= 0) {
	    sprite.nextFrame(xloc, yloc, turtleSaucerBlam);
	} else {
	    sprite.nextFrameOff();
	}
    }

    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
	sprite.paintFrame(g, paintAll, animator);
    }

    public boolean hitBy(Rectangle hitRect) {
	// This hit calculation isn't quite right in the case where
	// we skip frames, because it doesn't account for the horizontal
	// motion.  The easiest fix would be to go frame-by-frame.
	// I'm on a book deadline, so I didn't do that; it rarely
	// skips frames anyway, and when it does a user won't be so
	// surprised by a near miss, I'm betting.
	sprite.getBounds(ourBounds);
	if (active && ourBounds.intersects(hitRect)) {
	    if (Debug.LEVEL > 0) {
		Debug.println("Saucer hit!");
		active = false;
		startBlam = true;
	    }
	    return true;
	}
	return false;
    }

}

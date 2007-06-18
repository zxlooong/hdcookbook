package com.hdcookbook.gunbunny;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 *
 */
public class TurtleTrooper {

    private Image turtleTrooperImg;
    private Image turtleTrooperBlamImg;
    private ImageSprite sprite;
    private Rectangle ourBounds = new Rectangle();
    private boolean startBlam = false;
    private int blamFramesLeft = -1;	// -1 means not blamming
    
    private int xloc;
    private int yloc = 240;
    
    private boolean active = true;
    
    public TurtleTrooper(int x, Image turtleTrooperImg, 
    			 Image turtleTrooperBlamImg)
    {
        this.turtleTrooperImg = turtleTrooperImg; 
        this.turtleTrooperBlamImg = turtleTrooperBlamImg;
        this.xloc = x;
	this.sprite = new ImageSprite(turtleTrooperImg);
	this.sprite.initPosition(this.xloc, this.yloc);
	this.sprite.getBounds(ourBounds);
    }

    public void nextFrame(int numFrames) {
	if (startBlam) {
	    startBlam = false;
	    blamFramesLeft = 10;
	    sprite.nextFrame(turtleTrooperBlamImg);
	} else if (blamFramesLeft >= 0) {
	    blamFramesLeft -= numFrames;
	    if (blamFramesLeft <= 0) {
		blamFramesLeft = -1;
		sprite.nextFrameOff();
	    } else {
		sprite.nextFrame(turtleTrooperBlamImg);
	    }
	} else if (active) {
	    sprite.nextFrame(turtleTrooperImg);
	} else {
	    sprite.nextFrameOff();
	}
    }

    public boolean isVisible() {
	return active || blamFramesLeft >= 0;
    }

    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
	sprite.paintFrame(g, paintAll, animator);
    }

    public boolean hitBy(Rectangle hitRect) {
	if (active && ourBounds.intersects(hitRect)) {
	    if (Debug.LEVEL > 0) {
		Debug.println("Trooper hit!");
	    }
	    return true;
	}
	return false;
    }

    public void startBlamWithNextFrame() {
	active = false;
	startBlam = true;
    }

    public void awakenWithNextFrame() {
	active = true;
	startBlam = false;
	blamFramesLeft = -1;
    }

}

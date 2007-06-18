package com.hdcookbook.gunbunny;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

import com.hdcookbook.gunbunny.util.ImageUtil;

/**
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 */
public class Bunny {
    ImageSprite bunnySprite;
    ImageSprite carrotSprite;
    
    TurtleTrooperSquad squad;
    TurtleSaucer saucer;
    
    int xloc = 872;
    int yloc = 775;
    int xSpeed = 0;
    int bunnyW = 0;
    boolean shooting = false;
    boolean startShot = false;
    int carrotY;
    int carrotX;
    
    int trooperHits = 0;
    int saucerHits = 0;

    private Rectangle hitRect = new Rectangle();
    
    
    public Bunny(Image bunnyImg, Image carrotBullet, TurtleTrooperSquad squad, 
    		 TurtleSaucer saucer)
    {
        this.bunnySprite = new ImageSprite(bunnyImg);
	bunnySprite.initPosition(xloc, yloc);
        this.carrotSprite = new ImageSprite(carrotBullet);
	this.carrotSprite.initImageOff();
	this.shooting = false;
        this.squad = squad;
        this.saucer = saucer;
        
        bunnyW = bunnyImg.getWidth(null);
    }

    public synchronized void setXSpeed(int speed) {
	xSpeed = speed;
    }

    public synchronized void fire() {
	if (shooting) {
	    return;
	}
	startShot = true;
	carrotY = yloc + 10;
	carrotX = xloc + (bunnySprite.getWidth() - carrotSprite.getWidth()) / 2;
    }

    public void nextFrame(int numFrames) {
	if (xSpeed == 0) {
	    bunnySprite.nextFrame();
	} else {
	    xloc += xSpeed * numFrames;
	    if (xloc < 10) {
		xloc = 10;
	    } else if (xloc > 1650) {
		xloc = 1650;
	    }
	    bunnySprite.nextFrame(xloc, yloc);
	}
	if (startShot) {
	    shooting = true;
	    startShot = false;
	} else if (shooting) {
	    int dy = -22 * numFrames;
	    carrotSprite.getBounds(hitRect);
	    hitRect.height -= dy;
	    hitRect.y += dy;
	    boolean hit = squad.hitBy(hitRect);
	    if (hit) {
		trooperHits++;
	    } else if (saucer.hitBy(hitRect)) {
		hit = true;
		saucerHits++;
	    }
	    if (hit) {
		shooting = false;
	    } 
	}
	if (shooting) {
	    carrotY -= 22 * numFrames;
	    if (carrotY < 24) {
		shooting = false;
		carrotSprite.nextFrameOff();
	    } else {
		carrotSprite.nextFrame(carrotX, carrotY);
	    }
	} else {
	    carrotSprite.nextFrameOff();
	}
    }

    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
	bunnySprite.paintFrame(g, paintAll, animator);
	carrotSprite.paintFrame(g, paintAll, animator);
    }
}

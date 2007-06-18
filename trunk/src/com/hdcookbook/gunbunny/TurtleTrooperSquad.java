package com.hdcookbook.gunbunny;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;

/**
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 *
 */
public class TurtleTrooperSquad {
    Image turtleTrooperImg;
    Image turtleTrooperBlamImg;
    
    int[] xLoc = new int[] { 162, 364, 566, 768, 970, 1172, 1374, 1576 };
    TurtleTrooper[] squad = new TurtleTrooper[xLoc.length];
    
    public TurtleTrooperSquad(Image turtleTrooperImg, 
    			      Image turtleTrooperBlamImg)
    {
        this.turtleTrooperImg = turtleTrooperImg; 
        this.turtleTrooperBlamImg = turtleTrooperBlamImg;
    }

    public void assemble() {
        for(int i=0; i < squad.length; i++) {
            squad[i] = new TurtleTrooper(xLoc[i], turtleTrooperImg, 
	    				 turtleTrooperBlamImg);
        }
    }
    public void nextFrame(int numFrames) {
	boolean allGone = true;
	for (int i = 0; i < squad.length; i++) {
	    squad[i].nextFrame(numFrames);
	    allGone = allGone && !squad[i].isVisible();
	}
	if (allGone) {
	    for (int i = 0; i < squad.length; i++) {
		squad[i].awakenWithNextFrame();
	    }
	}
    }

    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
	for (int i = 0; i < squad.length; i++) {
	    squad[i].paintFrame(g, paintAll, animator);
	}
    }

    public boolean hitBy(Rectangle hitRect) {
	for (int i = 0; i < squad.length; i++) {
	    if (squad[i].hitBy(hitRect)) {
		squad[i].startBlamWithNextFrame();
		return true;
	    }
	}
	return false;
    }
    
}

package com.hdcookbook.gunbunny;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.MediaTracker;

import org.dvb.ui.FontNotAvailableException;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * This class manages the game's state
 * 
 * @author Bill Foote
 */
public class Game {

    private static int gameDuration = 24 * 60;	// game duration in frames
    private int gameStartFrame;

    private final static int STATE_INITIAL = 0;
    private final static int STATE_PLAYING = 1;
    private final static int STATE_GAME_OVER = 2;
    private int state = STATE_INITIAL;
    
    private long gameStartTime = 0;
    private long gameCurrentTime = 0;
    
    private GunBunnyXlet xlet;

    private Bunny bunny;
    private TurtleTrooperSquad squad;
    private TurtleSaucer saucer;

    private ImageSprite gameTitle;
    private TextSprite gameMessage;
    private ImageSprite gameOverTitle;
    private TextSprite gameOverMessage;
    private TextSprite timeSprite;
    private TextSprite scoreSprite;

    private boolean needsClear = false;
    private int lastFrame = 0;

    public Game() {
    }

    public void initialize(GunBunnyXlet xlet) throws InterruptedException {
	this.xlet = xlet;

	if (Debug.LEVEL > 0) {
	    Debug.println("Loading assets...");
	}
	MediaTracker tracker = new MediaTracker(xlet);
	Image turtleSaucerBlam = ImageUtil.getImage(
			"assets/images/turtle_saucer_blam.png", tracker);
	Image turtleSaucer = ImageUtil.getImage(
			"assets/images/turtle_saucer.png", tracker);
	Image turtleTrooper = ImageUtil.getImage(
			"assets/images/turtle_trooper.png", tracker);
	Image turtleTrooperBlam = ImageUtil.getImage(
			"assets/images/turtle_trooper_blam.png", tracker);
	Image bunnyImg = ImageUtil.getImage(
			"assets/images/bunny_00.png", tracker);
	Image carrotBullet = ImageUtil.getImage(
			"assets/images/carrot_bullet_01.png", tracker);
	Image gameTitleImg = ImageUtil.getImage(
			"assets/images/text_title.png", tracker);
	Image gameOverTitleImg = ImageUtil.getImage(
			"assets/images/text_title_gameover.png", tracker);
	tracker.waitForAll();	// Might throw InterruptedException
	if (Debug.LEVEL > 0) {
	    Debug.println("Assets loaded.");
	}
            
	squad = new TurtleTrooperSquad(turtleTrooper, turtleTrooperBlam);
	squad.assemble();
	saucer = new TurtleSaucer(turtleSaucer, turtleSaucerBlam);
	bunny = new Bunny(bunnyImg, carrotBullet, squad, saucer);            

	gameTitle = new ImageSprite(gameTitleImg);
	gameTitle.initPositionCentered(0, 0, xlet.width, xlet.height);
	Font lisa;
	try {
	    lisa = xlet.fontFactory.createFont("Lisa", Font.PLAIN, 64);
	} catch (Exception ex) {
	    // Shouldn't happen, unless we're built with a bad font.
	    lisa = new Font("SansSerif", Font.PLAIN, 60);
	}
	Color messageColor = new Color(240, 0, 0);
	gameMessage = new TextSprite("Hit Enter to begin!", lisa, messageColor,
				     xlet, 600, 870);
	gameOverTitle = new ImageSprite(gameOverTitleImg);
	gameOverTitle.initPositionCentered(0, 0, xlet.width, xlet.height);
	gameOverMessage = new TextSprite(
		"Hit Enter to go to main menu.",
		lisa, messageColor, xlet, 300, 910);
	timeSprite = new TextSprite("", lisa, messageColor, xlet, 20, 10);
	scoreSprite = new TextSprite("", lisa, messageColor, xlet, 1410, 10);
    }


    public void advanceToFrame(int frame) {
	int numFrames = frame - lastFrame;
	lastFrame = frame;
	switch(state) {
	    case STATE_INITIAL:
		gameTitle.nextFrame();
		gameMessage.nextFrame();
		break;
	    case STATE_PLAYING: {
		int framesLeft = gameDuration - (frame - gameStartFrame);
		if (framesLeft > 0) {
		    bunny.nextFrame(numFrames);
		    squad.nextFrame(numFrames);
		    saucer.nextFrame(numFrames);
		    timeSprite.nextFrame("Time: " + (framesLeft / 24));
		    int score = bunny.trooperHits * 50
		    		+ bunny.saucerHits * 350;
		    scoreSprite.nextFrame("Score: " + score);
		    break;
		} else {
		    state = STATE_GAME_OVER;
		    needsClear = true;
		    // fall through to case STATE_GAME_OVER
		}
	    }
	    case STATE_GAME_OVER:
		gameOverTitle.nextFrame();
		gameOverMessage.nextFrame();
		break;
	}
    }

    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
	if (needsClear && animator != null) {
	    paintAll = true;
	    g.setColor(ImageUtil.colorTransparent);
	    Rectangle r = animator.getPosition();
	    g.fillRect(0, 0, r.width, r.height);
	    needsClear = false;
	}
	switch(state) {
	    case STATE_INITIAL:
		gameTitle.paintFrame(g, paintAll, animator);
		gameMessage.paintFrame(g, paintAll, animator);
		break;
	    case STATE_PLAYING:
		bunny.paintFrame(g, paintAll, animator);
		squad.paintFrame(g, paintAll, animator);
		saucer.paintFrame(g, paintAll, animator);
		timeSprite.paintFrame(g, paintAll, animator);
		scoreSprite.paintFrame(g, paintAll, animator);
		break;
	    case STATE_GAME_OVER:
		gameOverTitle.paintFrame(g, paintAll, animator);
		gameOverMessage.paintFrame(g, paintAll, animator);
		break;
	}
    }

    private void resetGameTime(int currFrame) {
	gameStartFrame = currFrame;
    }
    
    public void handleEnter() {
	boolean destroy = false;
	synchronized(this) {
	    switch(state) {
		case STATE_INITIAL:
		    state = STATE_PLAYING;
		    needsClear = true;
		    resetGameTime(xlet.frame);
		    break;
		case STATE_PLAYING:
		    bunny.fire();
		    break;
		case STATE_GAME_OVER:
		    destroy = true;
		    break;
	    }
	}
	if (destroy) {
	    xlet.destroySelf();
	}
    }

    public synchronized void handleRight() {
	if (state == STATE_PLAYING) {
	    bunny.setXSpeed(16);
	}
    }

    public synchronized void handleLeft() {
	if (state == STATE_PLAYING) {
	    bunny.setXSpeed(-16);
	}
    }

    public synchronized void handleUp() {
	if (state == STATE_PLAYING) {
	    bunny.fire();
	}
    }

    public synchronized void handleDown() {
	if (state == STATE_PLAYING) {
	    bunny.setXSpeed(0);
	}
    }
}



package com.hdcookbook.gunbunny;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import javax.tv.xlet.XletStateChangeException;

import org.dvb.ui.FontFactory;

import com.hdcookbook.gunbunny.util.ImageUtil;
import com.hdcookbook.gunbunny.util.Debug;

/**
 * 
 * @author Shant Mardigian
 * @author Bill Foote
 *
 */
public class GunBunnyXlet extends BaseXlet {

    private static int frameWidth = 40;
    private Color frameColor = new Color(0, 0, 128, 255);

    private Game game = null;
    private Animator animator = null;
    private Rectangle animPos;
    int frame;
    public FontFactory fontFactory;

    public void paint(Graphics gArg) {
	Graphics2D g = (Graphics2D) gArg;
	g.setComposite(AlphaComposite.Src);
	g.setColor(frameColor);
	g.fillRect(0, 0, width, frameWidth);
	g.fillRect(0, 0, frameWidth, height);
	g.fillRect(width - frameWidth, 0, frameWidth, height);
	g.fillRect(0, height-frameWidth, width, frameWidth);

	Game gm;
	synchronized (this) {
	    gm = game;
	}
	if (gm != null) {
	    g.translate(animPos.x, animPos.y);
	    g.setClip(0, 0, animPos.width, animPos.height);
	    gm.paintFrame(g, true, null);
	}
    }



    protected String getVideoLocator() {
	return "bd://0.PLAYLIST:00003.MARK:00000";
    }

    protected void doXletLoop() throws InterruptedException {
	try {
	    fontFactory = new FontFactory();
	} catch (Exception ex) {
	    if (Debug.ASSERT) {
		ex.printStackTrace();
		Debug.assertFail(ex.toString());
	    }
	}
	Game gm = new Game();
	gm.initialize(this);
	frame = 0;
	animPos = new Rectangle();
	animPos.x = frameWidth + 10;
	animPos.y = frameWidth + 10;
	animPos.width = width - 2*animPos.x;
	animPos.height = height - 2*animPos.y;
	setAnimator(new DirectDrawAnimator());
	synchronized(this) {
	    game = gm;
	}
	try {
	    for (;;) {
		if (getDestroyed()) {
		    return;		// End xlet
		} else if (Thread.interrupted()) {
		    throw new InterruptedException();
		}
		Animator a;
		int f;
		synchronized(this) {
		    a = animator;
		}
		animator.animateGame(frame, game);
		synchronized(this) {
		    frame++;
		}
	    }
	} finally {
	    animator.destroy();
	}
    }

    private synchronized void setAnimator(Animator newAnimator) {
	if (Debug.LEVEL > 0) {
	    Debug.println("Setting animator to " 
	    		   + newAnimator.getClass().getName());
	}
	synchronized(this) {
	    if (animator != null) {
		animator.destroy();
	    }
	    animator = newAnimator;
	    animator.initAtFrame(frame, scene, animPos);
	}
    }

    public void destroySelf() {
	try {
	    destroyXlet(true);
	} catch (XletStateChangeException ignored) {
	}
	xletContext.notifyDestroyed();
    }

    
    protected void numberKeyPressed(int value) {
	Animator newAnimator = null;
	if (Debug.LEVEL > 0) {
	    Debug.println("NUMBER KEY:  " + value);
	}
	if (value == 1 && !(animator instanceof DirectDrawAnimator)) {
	    setAnimator(new DirectDrawAnimator());
	} else if (value == 2 && !(animator instanceof SFAAAnimator)) {
	    setAnimator(new SFAAAnimator());
	} else if (value == 3 && !(animator instanceof RepaintDrawAnimator)) {
	    setAnimator(new RepaintDrawAnimator());
	}
    }
    
    protected void colorKeyPressed(int value){
    }
    
    protected void popupKeyPressed(){
    }
    
    protected void enterKeyPressed(){
	Game g = game;
	if (g != null) {
	    g.handleEnter();
	}
    }
        
    protected void arrowLeftKeyPressed(){
	Game g = game;
	if (g != null) {
	    g.handleLeft();
	}
    }
    
    protected void arrowRightPressed(){
	Game g = game;
	if (g != null) {
	    g.handleRight();
	}
    }
    
    protected void arrowUpPressed(){
	Game g = game;
	if (g != null) {
	    g.handleUp();
	}
    }
    
    protected void arrowDownPressed(){
	Game g = game;
	if (g != null) {
	    g.handleDown();
	}
    }    
}

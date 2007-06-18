

package com.hdcookbook.gunbunny;

import java.awt.AlphaComposite;
import java.awt.Component;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Image;
import java.awt.image.BufferedImage;

import com.hdcookbook.gunbunny.util.ImageUtil;

/**
 * A text sprite is a sprite that holds some text.
 * This class does not handle overlapping sprites.
 **/

public class TextSprite extends Sprite {

    private String text;
    private String lastText;
    private Font font;
    private Component comp;
    private Color color;

    private int lastFrame = -1;
    private Rectangle pos = new Rectangle();
    private Rectangle lastPos = new Rectangle();
    private int ascent;
    private int descent;

    /** 
     * Create a TextSprite.
     **/
    public TextSprite(String text, Font font, Color color,
    		      Component comp, int x, int y) {
	this.text = text;
	this.font = font;
	this.color = color;
	this.comp = comp;
	calculateMetrics(pos, x, y);
    }

    private void calculateMetrics(Rectangle r, int x, int y) {
	r.x = x;
	r.y = y;
	FontMetrics fm = comp.getFontMetrics(font);
	r.width = fm.stringWidth(text);
	ascent = fm.getMaxAscent();
	descent = fm.getMaxDescent();
	r.height = ascent + descent + 1;
    }


    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.
     **/
    public void nextFrame() {
	lastPos.setBounds(pos);
	lastText = text;
    }

    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.
     **/
    public void nextFrame(String newText) {
	lastPos.setBounds(pos);
	lastText = text;
	text = newText;
	calculateMetrics(pos, pos.x, pos.y);
    }



    /**
     * Called to paint this sprite
     *
     * @param g		The place to paint the sprite
     * @param paintAll	true if we should paint everything, even if it's
     *			identical to what was painted in the last frame
     * @param animator  The animator that's animating us, or null if
     *			this is just a call from repaint.
     **/
    public void paintFrame(Graphics2D g, boolean paintAll, Animator animator) {
	boolean moved = lastText !=  text || !pos.equals(lastPos);
	if (moved && animator != null && animator.needsErase()) {
	    Rectangle r = animator.getScratchRectangle();
	    r.setBounds(pos);
	    r.add(lastPos.x, lastPos.y);
	    r.add(lastPos.x+lastPos.width, lastPos.y+lastPos.height);
	    BufferedImage buf = animator.getDoubleBuffer(r.width, r.height);
	    if (buf == null) {
		g.setColor(ImageUtil.colorTransparent);
		g.fillRect(r.x, r.y, r.width, r.height);
		g.setColor(color);
		g.setFont(font);
		g.drawString(text, pos.x, pos.y + ascent);
	    } else {
		Graphics2D bufG = animator.getDoubleBufferGraphics();
		bufG.setColor(ImageUtil.colorTransparent);
		bufG.fillRect(0, 0, r.width, r.height);
		bufG.setColor(color);
		bufG.setFont(font);
		bufG.drawString(text,
			        (pos.x <= lastPos.x ? 0 : pos.x - lastPos.x),
			        ascent + (pos.y <= lastPos.y 
			       			? 0 : pos.y - lastPos.y));
		g.drawImage(buf, r.x, r.y, r.x+r.width, r.y+r.height,
				   0,   0,     r.width,     r.height, null);
	    }
	} else if (moved || paintAll) {
	    g.setColor(color);
	    g.setFont(font);
	    g.drawString(text, pos.x, pos.y + ascent);
	}
    }
}

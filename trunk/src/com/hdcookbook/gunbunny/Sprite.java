

package com.hdcookbook.gunbunny;

import java.awt.Graphics2D;

/**
 * A sprite represents a graphical object that can move around the screen.
 * It knows how to efficiently paint itself.  In this game, all sprites
 * are non-overlapping, which simplifies things.
 **/

public abstract class Sprite {

    /**
     * Advance the state of the sprite to the given frame.  A sprite
     * may only change position during a call to one of the nextFrame
     * methods.  Subclasses may add this method with different signatures
     * to create version that change the state of the sprite.
     * <p>
     * What's tricky is you have to arrange to call nextFrame() exactly
     * once per paint cycle.  If you call it a second time, then it might
     * not erase the right stuff when it comes time to paint.
     **/
    abstract public void nextFrame();


    /**
     * Called to paint this sprite.
     *
     * @param g		The place to paint the sprite
     * @param paintAll	true if we should paint everything, even if it's
     *			identical to what was painted in the last frame
     * @param animator  The animator that's animating us, or null if
     *			this is just a call from repaint.
     **/
    abstract public void paintFrame(Graphics2D g, boolean paintAll, 
    				    Animator animator);
}

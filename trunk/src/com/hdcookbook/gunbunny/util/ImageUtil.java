package com.hdcookbook.gunbunny.util;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * 
 * @author Shant Mardigian
 * @version 1.0 April 16, 2007
 */
public class ImageUtil {

    public final static Color colorTransparent = new Color(0, 0, 0, 0);

    private static Class theClass = ImageUtil.class;
    private static LinkedList imagesList = new LinkedList();

    public static Image getImage(String path, MediaTracker tracker) {
        URL url = theClass.getResource("../" + path);
        Image img = Toolkit.getDefaultToolkit().createImage(url);
        if(tracker != null){
            tracker.addImage(img, 0);
        }
	synchronized(theClass) {
	    imagesList.add(img);
	}
        return img;
    }

    /**
     * Discard all of the images loaded so far, and discard the
     * list that records these images.
     **/
    public static void discardImages() {
	LinkedList ll;
	synchronized(theClass) {
	    ll = imagesList;
	    imagesList = new LinkedList();
	}
	for (Iterator it = ll.iterator(); it.hasNext();) {
	    Image im = (Image) it.next();
	    im.flush();
	}
    }
    

}

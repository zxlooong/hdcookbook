/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of Sun Microsystems nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 *  Note:  In order to comply with the binary form redistribution 
 *         requirement in the above license, the licensee may include 
 *         a URL reference to a copy of the required copyright notice, 
 *         the list of conditions and the disclaimer in a human readable 
 *         file with the binary form of the code that is subject to the
 *         above license.  For example, such file could be put on a 
 *         Blu-ray disc containing the binary form of the code or could 
 *         be put in a JAR file that is broadcast via a digital television 
 *         broadcast medium.  In any event, you must include in any end 
 *         user licenses governing any code that includes the code subject 
 *         to the above license (in source and/or binary form) a disclaimer 
 *         that is at least as protective of Sun as the disclaimers in the 
 *         above license.
 * 
 *         A copy of the required copyright notice, the list of conditions and
 *         the disclaimer will be maintained at 
 *         https://hdcookbook.dev.java.net/misc/license.html .
 *         Thus, licensees may comply with the binary form redistribution
 *         requirement with a text file that contains the following text:
 * 
 *             A copy of the license(s) governing this code is located
 *             at https://hdcookbook.dev.java.net/misc/license.html
 */

package com.hdcookbook.genericgame;


import java.awt.Rectangle;
import java.io.IOException;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.graphics.TVContainer;

import java.awt.Container;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.animator.AnimationClient; 
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.animator.DirectDrawEngine;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.dvb.ui.DVBBufferedImage;
import org.dvb.ui.FontFactory;

import org.bluray.ui.event.HRcEvent;

/** 
 * The xlet class for the GrinBunny project
 **/

public class GameXlet
	implements Xlet, AnimationContext, UserEventListener
{
    public Show show;
    Container rootContainer;
    FontFactory fontFactory;
    DirectDrawEngine animationEngine;
    Director director;
    XletContext context;

    private String showFileName;
    private String showInitialSegment;
    private String showDirectorName;
    private boolean definesFonts;

    boolean sendKeyUp = true;

    // A small show we use to manage a debug screen accessed with
    // the popup menu key
    private Show xletShow;
    private XletDirector xletDirector;
	
    public void initXlet(XletContext context) {
        this.context = context;

	String[] args = (String[]) context.getXletProperty(context.ARGS);
	if (Debug.ASSERT && args.length != 4) {
	    Debug.assertFail("Parameters:  <grin file> <initial segment> <director> <fontflag>\n    fontflag is -fonts or -nofonts");
	}
	showFileName = args[0];
	showInitialSegment = args[1];
	showDirectorName = args[2];
	definesFonts = "-fonts".equals(args[3]);
       
        rootContainer = TVContainer.getRootContainer(context);			
        rootContainer.setSize(1920, 1080);
        
        animationEngine = new DirectDrawEngine();
        animationEngine.setFps(24000);
        animationEngine.initialize(this);
    }
    
    public void startXlet() {
       rootContainer.setVisible(true);
       animationEngine.start(); 	   
    }
    
    public void pauseXlet() {
       rootContainer.setVisible(false);
       animationEngine.pause();
    }
    
    public void destroyXlet(boolean unconditional) {
       rootContainer = null;
	if (Debug.LEVEL > 0) {
	    System.out.println("Destroying animation engine...");
	}
       animationEngine.destroy();
       EventManager.getInstance().removeUserEventListener(this);          
	if (Debug.LEVEL > 0) {
	    System.out.println("destroyXlet() completes successfully.");
	    System.out.println();
	    System.out.println();
	    System.out.println();
	    System.out.println();
	}
    }
    
    public void animationInitialize() throws InterruptedException {

	try {
	    director = (Director) Class.forName(showDirectorName).newInstance();
	} catch (Throwable t) {
	    Debug.assertFail("Can't create director:  " + t);
	}
	xletDirector = new XletDirector(this);
	if (definesFonts) {
	    try {
	       fontFactory = new FontFactory();
	    } catch (Exception ex) {
		ex.printStackTrace();
		if (Debug.ASSERT) {
		    Debug.assertFail();
		}
	    }
	} else {
	    fontFactory = null;
	}

	try {
	    // Set up AssetFinder so we use DVBBufferedImage.
	    // See http://wiki.java.net/bin/view/Mobileandembedded/BDJImageMemoryManagement
	    AssetFinder.setHelper(new AssetFinder() {
		protected Image createCompatibleImageBufferHelper
				    (Component c, int width, int height) 
		{
		    return new DVBBufferedImage(width, height);
		}
		protected Graphics2D createGraphicsFromImageBufferHelper
				    (Image buffer) 
		{
		    Object g = ((DVBBufferedImage) buffer).createGraphics();
		    return (Graphics2D) g;
		}
		protected void destroyImageBufferHelper(Image buffer) {
		    ((DVBBufferedImage) buffer).dispose();
		}
		protected Font getFontHelper(String name, int style, int size)
		{
		    if (fontFactory != null) {
			try {
			    return fontFactory.createFont(name, style, size);
			} catch (Exception ex) {
			    if (Debug.LEVEL > 0) {
				ex.printStackTrace();
				Debug.println("***  Font " + name 
					      + " not found.  ***");
			    }
			}
		    }
		    return null;
		}
	    });

	   AssetFinder.setSearchPath(new String[]{""}, null);      
	   if (AssetFinder.tryURL("images.map") != null) {
	       AssetFinder.setImageMap("images.map");
	       if (Debug.LEVEL > 0) {
		   Debug.println("Found images.map, using mosaic.");
	       }
	    } else {
	       if (Debug.LEVEL > 0) {
		   Debug.println("No images.map, not using mosaic.");
	       }
	    }

	   GrinBinaryReader reader = 
		   new GrinBinaryReader(AssetFinder.getURL(
			    showFileName).openStream());
	   show = new Show(director);
	   reader.readShow(show);

	   reader = new GrinBinaryReader(AssetFinder.getURL(
			    "xlet_show.grin").openStream());
	   xletShow = new Show(xletDirector);
	   reader.readShow(xletShow);

	} catch (IOException e) {
	   e.printStackTrace();
	   System.err.println("Error in reading the show file");
	   throw new InterruptedException();
	}

	animationEngine.checkDestroy();
	animationEngine.initClients(new AnimationClient[] { show, xletShow });
	animationEngine.initContainer(rootContainer, 
				     new Rectangle(0,0,1920,1080));
       
    } 
    
    public void animationFinishInitialization() {
	show.activateSegment(show.getSegment(showInitialSegment));	
	xletShow.activateSegment(xletShow.getSegment("S:Initialize"));	
       
	UserEventRepository userEventRepo = new UserEventRepository("x");
	userEventRepo.addAllArrowKeys();
	userEventRepo.addAllColourKeys();
	userEventRepo.addAllNumericKeys();
	userEventRepo.addKey(HRcEvent.VK_ENTER);
	userEventRepo.addKey(HRcEvent.VK_POPUP_MENU);

	EventManager.getInstance().addUserEventListener(this, userEventRepo);          
	rootContainer.requestFocus();          
    }

    /**
     * A remote control event that is coming in via
     * org.dvb.event.UserEventListener
     **/
    public void userEventReceived(UserEvent e) {
	int type = e.getType();
        if (type == HRcEvent.KEY_PRESSED) {
	    int code = e.getCode();
	    boolean handled =
		xletShow.handleKeyPressed(code) || show.handleKeyPressed(code);
        } else if (sendKeyUp && type == HRcEvent.KEY_RELEASED) {
	    int code = e.getCode();
	    boolean handled =
		xletShow.handleKeyReleased(code) 
		|| show.handleKeyReleased(code);
		    // Note that Gun Bunny doesn't do anything on key_released,
		    // or at least it didn't when this comment was written.
		    // Because of this setting sendKeyUp false doesn't have
		    // a visible effect on this particular game, but this xlet
		    // can trivially be adapted for use as a generic game xlet.
		    // If you do this, then being able to turn off key up events
		    // is really useful.
	}
    }	

}

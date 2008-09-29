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

import java.awt.Rectangle;
import java.io.IOException;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.graphics.TVContainer;

import java.awt.AlphaComposite;
import java.awt.Container;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.util.Random;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.animator.AnimationClient; 
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;

import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;
import org.bluray.ui.event.HRcEvent;
import org.dvb.ui.DVBBufferedImage;

/** 
 * The main class for the Playground project
 */

public class MainDirector extends Director {

    private AnimationEngine engine;

    private InterpolatedModel scaler;
    private InterpolatedModel boxPos;
    private Text myText;
    private Fade boxedStuffFade;
    private Random random;

    private float fadeGoal = 1.0f;
    private float fadeAlpha = 1.0f;

    public MainDirector(AnimationEngine engine) {
	this.engine = engine;
    }

    public void restoreNormalMenu() {
	AnimationClient[] clients = new AnimationClient[] { getShow() };
	engine.resetAnimationClients(clients);
    }

    public void putNewShowOnTopOfMenu() {
	    // First we print out the old clients.  This is only done
	    // as a minimal test of engine.getAnimationClients()
	AnimationClient[] clients = engine.getAnimationClients();
	System.out.println();
	System.out.println("Old animation clients:");
	for (int i = 0; i < clients.length; i++) {
	    System.out.println("    [" + i + "]:  " + clients[i]);
	}
	System.out.println();

	    // Now we create a new Show object, and set it to the
	    // first segment.  It's OK to call activateSegment() before
	    // the animation engine initializes the show.
	Show newShow = null;
	try {
	    GrinBinaryReader reader = 
	       new GrinBinaryReader(AssetFinder.getURL(
			"second_show.grin").openStream());
	    newShow = new Show(null);
	    reader.readShow(newShow);
	} catch (IOException e) {
	    e.printStackTrace();
	    System.err.println("Error in reading the show file");
	    return;
	}
	newShow.activateSegment(newShow.getSegment("S:Initialize"));	

	    // Finally, we get the animation engine to reset its list of
	    // clients.  This won't take effect until the current frame of
	    // animation is complete.
	clients = new AnimationClient[] { getShow(), newShow };
	engine.resetAnimationClients(clients);
    }


    /**
     * This method is alled from main_show.txt before each frame when the
     * S:S:ProgrammaticSceneGraphControl segment is showing.  This
     * method does most of the control of the show, but a little bit
     * of Java scripting is also done in-line in the show file, just
     * to show what it looks like.  Note, however, that the Director
     * is the natural place to store the state information you'll
     * need for whatever you're doing, so it's probably easier to just
     * call a method on your director most of the time.
     **/
    public void programmaticallyChageSceneGraph() {
	if (scaler == null) {
	    Show show = getShow();
	    scaler = (InterpolatedModel) show.getFeature("F:MainScaler");
	    boxPos = (InterpolatedModel)show.getFeature("F:BoxedStuffPosition");
	    myText = (Text) show.getFeature("F:EnterText");
	    boxedStuffFade = (Fade) show.getFeature("F:BoxedStuffFade");
	    random = new Random();
	}

		// Mess around with the X,Y center of scaling, and the
		// scale factors.

	if (random.nextInt(88) == 42) {
	    scaler.setField(scaler.SCALE_X_FIELD, 780 + random.nextInt(400));
	}
	if (random.nextInt(88) == 42) {
	    scaler.setField(scaler.SCALE_Y_FIELD, 410 + random.nextInt(300));
	}
	if (random.nextInt(88) == 42) {
	    scaler.setField(scaler.SCALE_X_FACTOR_FIELD, 
	    			500 + random.nextInt(1000));
	}
	if (random.nextInt(88) == 42) {
	    scaler.setField(scaler.SCALE_Y_FACTOR_FIELD, 
	    			500 + random.nextInt(1000));
	}

		// Randomly change the translation of the boxed stuff

	if (random.nextInt(88) == 42) {
	    boxPos.setField(Translator.X_FIELD, -600 + random.nextInt(700));
	}
	if (random.nextInt(88) == 42) {
	    boxPos.setField(Translator.Y_FIELD, -300 + random.nextInt(400));
	}

		// Scramble the text in entertaining ways

	if (random.nextInt(50) == 21) {
	    String[] result = new String[2];
	    String[] src = { "Press", "enter", "to", "return" };
	    result[0] = scrambleText(src, 4, result[0]);
	    result[0] = scrambleText(src, 3, result[0]);
	    result[1] = scrambleText(src, 2, result[1]);
	    result[1] = scrambleText(src, 1, result[1]);
	    myText.setText(result);
	}

		// Make the boxed stuff fade in and out, but mostly
		// in, and make it smooth.
	if (random.nextInt(100) == 42) {
	    fadeGoal = random.nextFloat();
	    fadeGoal = 1.0f - fadeGoal * fadeGoal;  // Bias toward visibility
	}
	fadeAlpha = 0.95f * fadeAlpha + 0.05f * fadeGoal;
	if (fadeAlpha < 0f) {
	    fadeAlpha = 0f;
	} else if (fadeAlpha > 1f) {
	    fadeAlpha = 1f;
	}
	AlphaComposite ac 
	    = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeAlpha);
	boxedStuffFade.setAlpha(ac);
    }

    private String scrambleText(String[] src, int srcLen, String line) {
	int i = random.nextInt(srcLen);
	String tmp = src[i];
	src[i] = src[srcLen-1];
	src[srcLen-1] = null;
	if (line == null) {
	    return tmp;
	} else {
	    return line + " " + tmp;
	}
    }
}

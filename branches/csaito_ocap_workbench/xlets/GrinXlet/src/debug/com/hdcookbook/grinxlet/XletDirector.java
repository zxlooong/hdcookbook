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

package com.hdcookbook.grinxlet;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Translator;

//import java.util.ListIterator;
import java.util.Enumeration;


/** 
 * The director for the small xlet UI that manages test state using the
 * popup menu.
 **/

public class XletDirector extends Director {
	
    public GrinXlet xlet;

    Assembly F_KeyUpState;
    Feature F_KeyUpState_enabled;
    Feature F_KeyUpState_disabled;
    Assembly F_DebugDraw;
    Feature F_DebugDraw_enabled;
    Feature F_DebugDraw_disabled;
    Text F_Framerate;
    Text F_DebugLog_LineCount;
    Text F_DebugLog_LineNumbers;
    Text F_DebugLog_Lines;
    InterpolatedModel F_DebugLog_Scroller;
    
    final int MAX_X = 640;  // 1920 for bd-j
    final int MAX_Y = 480;  // 1080 for bd-j


    private int currFramerate = 10;	// Default to 23.976
    private static int[] framerates = {
    	31, 62, 125, 250, 500, 1001, 2002, 3003, 6006, 12012, 24000,
	24024, 30000, 30030, 60000, 60060, 120000, 120120
    };
    private boolean debugDraw = false;

    private int debugLogTop = 0;	// Top line visible in debug log
    private int lastDebugLogTop = Integer.MIN_VALUE;
    private boolean debugLogFollow = false;	
    	// If we're in "follow" mode, always showing the last line (like tail -f)
    private String[] debugLogEntries;
    private String[] debugLogLineNumbers;
    private String[] debugLogLineCount;

    public XletDirector(GrinXlet xlet) {
	this.xlet = xlet;
	DebugLog.startDebugListener();
    }

    /** 
     * @inheritDoc
     **/
    public void notifyDestroyed() {
	DebugLog.shutdownDebugListener();
    }


    public void initialize() {
	F_KeyUpState = (Assembly) getFeature("F:KeyUpState");
	F_KeyUpState_enabled = getPart(F_KeyUpState, "enabled");
	F_KeyUpState_disabled = getPart(F_KeyUpState, "disabled");
	F_Framerate = (Text) getFeature("F:Framerate");
	F_DebugDraw = (Assembly) getFeature("F:DebugDraw");
	F_DebugDraw_enabled = getPart(F_DebugDraw, "enabled");
	F_DebugDraw_disabled = getPart(F_DebugDraw, "disabled");
	F_DebugLog_LineCount = (Text) getFeature("F:DebugLog.LineCount");
	F_DebugLog_LineNumbers = (Text) getFeature("F:DebugLog.LineNumbers");
	F_DebugLog_Lines = (Text) getFeature("F:DebugLog.Lines");
	F_DebugLog_Scroller = (InterpolatedModel) getFeature("F:DebugLog.Scroller");
	int lines = (MAX_Y - 80*2) / F_DebugLog_Lines.getLineHeight();
	debugLogEntries = new String[lines];
	debugLogLineNumbers = new String[lines];
	debugLogLineCount = new String[1];
    }

    /**
     * Sets the UI state to match the model.
     **/
    public void setUIState() {
	if (xlet.sendKeyUp) {
	    F_KeyUpState.setCurrentFeature(F_KeyUpState_enabled);
	} else {
	    F_KeyUpState.setCurrentFeature(F_KeyUpState_disabled);
	}
	if (debugDraw) {
	    F_DebugDraw.setCurrentFeature(F_DebugDraw_enabled);
	} else {
	    F_DebugDraw.setCurrentFeature(F_DebugDraw_disabled);
	}
	setFramerate();
    }

    public void setFramerate() {
	int fpts = framerates[currFramerate];	// frames / 1001ths second
	float fps = ((float) fpts) / 1001f;
	String[] arr = new String[] { "Frames per second:  " + fps };
	F_Framerate.setText(arr);
	xlet.animationEngine.setFps(fpts);
    }

    /**
     * Increases the framerate
     **/
    public void framerateUp() {
	currFramerate++;
	if (currFramerate >= framerates.length) {
	    currFramerate = framerates.length - 1;
	}
	setFramerate();
    }

    /**
     * Decreases the framerate
     **/
    public void framerateDown() {
	currFramerate--;
	if (currFramerate < 0) {
	    currFramerate = 0;
	}
	setFramerate();
    }

    public boolean getDebugDraw() {
	return debugDraw;
    }

    public void setDebugDraw(boolean value) {
	debugDraw = value;
	xlet.animationEngine.setDebugDraw(value);
    }

    private void displayDebugLog() {
	synchronized(DebugLog.LOCK) {
	    DebugLog.changed = false;
	    int lines = DebugLog.log.size() + DebugLog.linesRemoved;
	    debugLogLineCount[0] = "" + lines + " lines";
	    F_DebugLog_LineCount.setText(debugLogLineCount);
	    int maxTop = lines - debugLogEntries.length;
	    if (debugLogTop >= maxTop) {
		debugLogFollow = true;
	    }
	    if (debugLogFollow) {
		debugLogTop = maxTop;
	    }
	    if (debugLogTop == lastDebugLogTop && debugLogTop != maxTop) {
		return;
	    }
	    if (debugLogTop < DebugLog.linesRemoved) {
		debugLogTop = DebugLog.linesRemoved;
	    }
	    //ListIterator iter = DebugLog.log.listIterator(
            Enumeration iter = DebugLog.log.elements();
	    				//debugLogTop - DebugLog.linesRemoved);
	    for (int i = 0; i < debugLogEntries.length; i++) {
		//if (!iter.hasNext()) {
                if (!iter.hasMoreElements()) {
		    debugLogEntries[i] = "";
		    debugLogLineNumbers[i] = "";
		} else {
		    //debugLogEntries[i] = (String) iter.next();
		    debugLogEntries[i] = (String) iter.nextElement();
		    int line = debugLogTop + i + 1;
		    debugLogLineNumbers[i] = "" + line + ":";
		}
	    }
	    F_DebugLog_LineNumbers.setText(debugLogLineNumbers);
	    F_DebugLog_Lines.setText(debugLogEntries);
	    lastDebugLogTop = debugLogTop;
	}
    }

    public void debugLogHeartbeat() {
	synchronized(DebugLog.LOCK) {
	    if (DebugLog.changed) {
		displayDebugLog();
	    }
	}
    }

    public void skipDebugLogTo(int pos) {
	F_DebugLog_Scroller.setField(Translator.X_FIELD, 0);
	synchronized(DebugLog.LOCK) {
	    int lines = DebugLog.log.size();   
		    // not including DebugLog.linesRemoved.
	    lines -= debugLogEntries.length;
	    if (lines <= 0) {
		debugLogFollow = true;
	    } else {
		debugLogFollow = false;
		pos--;
		if (pos < 0) {
		    pos = 0;
		} else if (pos > 8) {
		    pos = 8;
		}
		debugLogTop = (pos * 100 * lines) / 800;
		debugLogTop += DebugLog.linesRemoved;
	    }
	    displayDebugLog();
	}
    }

    public void moveDebugLogLeft() {
	int x = F_DebugLog_Scroller.getField(Translator.X_FIELD);
	x += MAX_X/2;
	if (x > 0) {
	    x = 0;
	}
	F_DebugLog_Scroller.setField(Translator.X_FIELD, x);
    }

    public void moveDebugLogRight() {
	int x = F_DebugLog_Scroller.getField(Translator.X_FIELD);
	x -= MAX_X/2;
	F_DebugLog_Scroller.setField(Translator.X_FIELD, x);
    }

    public void moveDebugLogUp() {
	debugLogTop -= 10;
	debugLogFollow = false;
	displayDebugLog();
    }

    public void moveDebugLogDown() {
	debugLogTop += 10;
	displayDebugLog();
    }

}


/*  
 * Copyright (c) 2007, Sun Microsystems, Inc.
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

package com.hdcookbook.grin.input;

import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.util.Debug;

import java.util.Hashtable;
import java.awt.event.KeyEvent;


/**
 * This class is used to manage constants related to the remote
 * control keys.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class RCKeyEvent {
    
    public static RCKeyEvent KEY_0;
    public static RCKeyEvent KEY_1;
    public static RCKeyEvent KEY_2;
    public static RCKeyEvent KEY_3;
    public static RCKeyEvent KEY_4;
    public static RCKeyEvent KEY_5;
    public static RCKeyEvent KEY_6;
    public static RCKeyEvent KEY_7;
    public static RCKeyEvent KEY_8;
    public static RCKeyEvent KEY_9;
    public static RCKeyEvent KEY_RIGHT;
    public static RCKeyEvent KEY_LEFT;
    public static RCKeyEvent KEY_UP;
    public static RCKeyEvent KEY_DOWN;
    public static RCKeyEvent KEY_ENTER;
    public static RCKeyEvent KEY_POPUP_MENU;

    //  NOTE:  If you add a new key, check the note at the end of
    //         generatePerfectHashOfEventCodes().

    /**
     * VK_COLOURED_KEY_0 from HAVi.  It might not be red in
     * some markets, but this is the standard from UK teletext,
     * and seems to be common in the US.
     **/
    public static RCKeyEvent KEY_RED;

    // @@ Set these up for real, as outlined in book

    /**
     * VK_COLOURED_KEY_1 from HAVi.  It might not be red in
     * some markets, but this is the standard from UK teletext,
     * and seems to be common in the US.
     **/
    public static RCKeyEvent KEY_GREEN;

    /**
     * VK_COLOURED_KEY_2 from HAVi.  It might not be red in
     * some markets, but this is the standard from UK teletext,
     * and seems to be common in the US.
     **/
    public static RCKeyEvent KEY_YELLOW;

    /**
     * VK_COLOURED_KEY_3 from HAVi.  It might not be red in
     * some markets, but this is the standard from UK teletext,
     * and seems to be common in the US.
     **/
    public static RCKeyEvent KEY_BLUE;

    private static Hashtable keyByName;
    private static RCKeyEvent[] keyByEventCode; // see getKeyByEventCode()

    static {
	keyByName = new Hashtable();
	KEY_0 = new RCKeyEvent("0", KeyEvent.VK_0, 0x00001);
	KEY_1 = new RCKeyEvent("1", KeyEvent.VK_1, 0x00002);
	KEY_2 = new RCKeyEvent("2", KeyEvent.VK_2, 0x00004);
	KEY_3 = new RCKeyEvent("3", KeyEvent.VK_3, 0x00008);
	KEY_4 = new RCKeyEvent("4", KeyEvent.VK_4, 0x00010);
	KEY_5 = new RCKeyEvent("5", KeyEvent.VK_5, 0x00020);
	KEY_6 = new RCKeyEvent("6", KeyEvent.VK_6, 0x00040);
	KEY_7 = new RCKeyEvent("7", KeyEvent.VK_7, 0x00080);
	KEY_8 = new RCKeyEvent("8", KeyEvent.VK_8, 0x00100);
	KEY_9 = new RCKeyEvent("9", KeyEvent.VK_9, 0x00200);
	KEY_RIGHT = new RCKeyEvent("right", KeyEvent.VK_RIGHT, 0x00400);
	KEY_LEFT = new RCKeyEvent("left", KeyEvent.VK_LEFT, 0x00800);
	KEY_UP = new RCKeyEvent("up", KeyEvent.VK_UP, 0x01000);
	KEY_DOWN = new RCKeyEvent("down", KeyEvent.VK_DOWN, 0x02000);
	KEY_ENTER = new RCKeyEvent("enter", KeyEvent.VK_ENTER, 0x04000);

	// For the color keys, I just lifted the constants out of the
	// HAVi stubs.  This avoids a compilation dependency on HAVi.
	KEY_RED = new RCKeyEvent("red", 403, 0x08000);
	KEY_GREEN = new RCKeyEvent("green", 404, 0x10000);
	KEY_YELLOW = new RCKeyEvent("yellow", 405, 0x20000);
	KEY_BLUE = new RCKeyEvent("blue", 406, 0x40000);

	// For the popup key, I lifted the constant out of the BD-J spec.
	KEY_POPUP_MENU = new RCKeyEvent("popup_menu", 461, 0x80000);

	RCKeyEvent[] keys = new RCKeyEvent[] {
	    KEY_0, KEY_1, KEY_2, KEY_3, KEY_4, 
	    KEY_5, KEY_6, KEY_7, KEY_8, KEY_9,
	    KEY_RIGHT, KEY_LEFT, KEY_UP, KEY_DOWN,
	    KEY_ENTER, KEY_RED, KEY_GREEN, KEY_YELLOW, KEY_BLUE,
	    KEY_POPUP_MENU
	};
	keyByEventCode = generatePerfectHashOfEventCodes(keys);
    }
    
    private String name;    // human-readable name, used in script file
    private int keyCode;    // java.awt.event.KeyEvent.getKeyCode()
    private int mask;       // Mask value that we assign
    
    private RCKeyEvent(String name, int keyCode, int mask) {
        this.name = name;
        this.keyCode = keyCode;
        this.mask = mask;
        keyByName.put(name, this);
    }

    public String getName() {
	return name;
    }

    /**
     * @return the VK_ key code corresponding to this key
     **/
    public int getKeyCode() {
	return keyCode;
    }

    /**
     * A bitmask value is assigned to each remote control key.  Since
     * there are 19 keys we consider and 32 bits in an int, there's plenty
     * of room in an int.  Using a bitmask lets us check a key against
     * a set of expected keys very efficiently.
     * 
     * @return The mask for this key.
     **/
     public int getBitMask() {
	return mask;
    }

    /**
     * Look up the RCKeyEvent corresponding to a VK_ key code.
     * This method is very fast and creates no garbage.  It
     * uses a perfect hash function, an idea I lifted from
     * http://www.onjava.com/pub/a/onjava/2001/01/25/hash_functions.html
     *
     * @return The RCKeyEvent, or null if there's no corresponding event
     **/
    public static RCKeyEvent getKeyByEventCode(int key) {
	RCKeyEvent result = keyByEventCode[key % keyByEventCode.length];
	if (result != null && result.keyCode == key) {
	    return result;
	} else {
	    return null;
	}
    }

    /**
     * Look up the RCKeyEvent by its logical name.
     *
     * @return the RCKeyEvent, or null if there's no corresponding event.
     **/
    public static RCKeyEvent getKeyByName(String name) {
	return (RCKeyEvent) keyByName.get(name);
    }

    private static RCKeyEvent[] 
            generatePerfectHashOfEventCodes(RCKeyEvent[] keys) 
    {
	int remainder = keys.length;
	for (;;) {
	    boolean ok = true;
	    RCKeyEvent[] result = new RCKeyEvent[remainder];
	    for (int i = 0; i < keys.length; i++) {
		int x = keys[i].keyCode % remainder;
		if (result[x] != null) {
		    ok = false;
		    break;
		} else {
		    result[x] = keys[i];
		}
	    }
	    if (ok) {
		return result;
	    }
	    remainder++;
		// This might look a little crazy, but it terminates
		// after eleven iterations with the 20 keys we have
		// defined (as of 12/19/06).  Given what I saw in
		// http://www.onjava.com/pub/a/onjava/2001/01/25/hash_functions.html ,
		// this is a safe thing to do.  Still, if a key is added,
		// it wouldn't hurt to check that this terminates quickly.
	    if (Debug.ASSERT && remainder > 40) {
		Debug.assertFail("Find a better algorithm!");
	    }
	}
    }
}

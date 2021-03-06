
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


package com.hdcookbook.grin;

import java.awt.event.KeyEvent;


/**
 * A ChapterManager manages the segments in a chapter.  "chapter"
 * doesn't exist as a class, but it is in the show file - it's
 * used to set up the right ChapterManager instances.  A Director
 * can be implemented using different ChapterManager instances, by
 * putting the state related to a chapter in its ChapterManager.  This
 * provides a simple state machine that applications can use to track
 * the coarse-grained state of presentation to the user.
 * <p>
 * One way this can be used, if you're interested, is with the GoF state
 * pattern.  If you do this, you'd have a different subclass for each state.
 * Various behaviors of your xlet could be declared abstract on the common
 * ChapterManager superclass, and you could pass them through the current
 * chapter manager.  In this way, you could hook into the state transition
 * that a show gives you.
 * <p>
 * Many xlets will be to simple to really benefit from this kind of state
 * pattern.  In that case, just have one default chapter manager, and never
 * mention a chapter in your show file, and you'll be fine.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ChapterManager {

    private String name;
    private Director director;

    public ChapterManager(String name) {
	this.name = name;
    }

    void setDirector(Director director) {
	this.director = director;
    }

    /**
     * @return the name of this chapter
     **/
    public String getName() {
	return name;
    }

    public String toString() {
	return super.toString() + "(" + name + ")";
    }

    /** 
     * Get our director, e.g. to send it to a new state.
     **/
    protected Director getDirector() {
	return director;
    }

    /**
     * Called when we enter a state
     **/
    protected void enter() {
    }


    /**
     * Process a keyboard event.  This is synchronized by the
     * director.  This method must not sleep, because it is
     * called with locks held.
     *
     * @param e		The event
     **/
    protected boolean doKeyEvent(KeyEvent e) {
        return false;
    }
}

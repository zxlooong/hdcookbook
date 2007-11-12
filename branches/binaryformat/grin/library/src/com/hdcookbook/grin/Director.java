
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

import com.hdcookbook.grin.parser.ExtensionsParser;

import java.util.Hashtable;
import java.awt.event.KeyEvent;


/**
 * This class is a generic facade for the control entity that
 * manages a show.  It receives input from triggers embedded
 * in a show, positioning due to trick play, and remote control
 * key events.
 * <p>
 * A director manages itself by its state.  A director also keeps a
 * list of all states, indexed by name.  Most of the actual work
 * of the director is done by its states.
 * <p>
 * A director provides the controler that's needed by a Show
 * so that applications don't need to.
 * <p>
 * See also the Facade pattern (GoF page 185) and the State pattern
 * (GoF page 305).
 *
 *   @author     Bill Foote (http://jovial.com)
 **/

public abstract class Director {

    private com.hdcookbook.grin.ChapterManager theController;
    private Hashtable states = new Hashtable();
    private Show show;
    private ChapterManager initialState;

    private ChapterManager currentState;		// never null

    /**
     * Create a new Director.
     **/
    public Director()  {
    }
    
    /**
     * Initialize this director.
     * 
     * @param initialState	The initial state of the show
     * @param states		All states of the show known at initialization
     *			        time (must include the initial state)
     **/
    protected void setup(int initialState, ChapterManager[] states) {
	this.initialState = states[initialState];
	this.currentState = this.initialState;
	for (int i = 0; i < states.length; i++) {
	    states[i].setDirector(this);
	    this.states.put(states[i].getName(), states[i]);
	}
    }
   
    /**
     * Add a new state (called a "chapter") to the set of states managed
     * by this director.  This extends the set passed into the setup
     * method.
     **/
    protected void addState(ChapterManager state) {
        state.setDirector(this);
        states.put(state.getName(), state);
    }

    //
    // Called from Show constructor
    //
    void setShow(Show show) {
	this.show = show;
    }

    /**
     * Get the show we're managing.
     **/
    public Show getShow() {
	return show;
    }
   
    /** 
     * Returns the ChapterManager for a given state.  Xlets
     * can override this, but if they do the parameter to
     * the constructor becomes meaningless.
     **/
    public ChapterManager getChapterManager(String name) {
        return (ChapterManager) states.get(name);
    }

    /**
     * Give the ExtensionsParser that will parse any new
     * features or commands we've extended the framework with.
     * If no extensions are returned, it's OK to return null.
     *
     * @return The extensions parser
     **/
    abstract public ExtensionsParser getExtensionsParser();

}

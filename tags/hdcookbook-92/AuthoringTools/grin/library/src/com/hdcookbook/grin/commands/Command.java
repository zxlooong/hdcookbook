
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

package com.hdcookbook.grin.commands;

import com.hdcookbook.grin.Show;

/**
 * Common base class of all GRIN commands.  GRIN defers anything that
 * can change the state of a show to a command.  In this way, the
 * synchronization model is kept very simple.  Commands are executed
 * from within Show.nextFrame, with the show lock held.
 *
 * @see com.hdcookbook.grin.Show#nextFrame()
 *
 * @author Bill Foote (http://jovial.com)
 */
public abstract class Command {
    
    protected Show show;
    
    protected Command(Show show) {
        this.show = show;
    }
 
    /**
     * Execute the command.  This causes the command to take
     * whatever action it was created to do.
     **/
    public abstract void execute();

    /**
     * This method is obsolete, and should not be overridden by any
     * subclass.  It has been kept and marked final so that a compilation
     * error will be provoked if any subclass overrides it.
     * <p>
     * This method used to be defined so that returning true would have
     * the effect of a sync_display command.  Now that there's a sync_display
     * command, there's no need (see GrinXHelper.SYNC_DISPLAY).  Additionally,
     * any command that wants to defer command execution until the display
     * is caught up can call Show.deferNextCommands() in the body of the
     * command's execute method.
     *
     * @deprecated
     * @see com.hdcookbook.grin.Show.deferNextCommands()
     **/
    final public boolean deferNextCommands() {
	return false;
    }

    /**
     * Return a user-friendly string for this command for debugging
     * purposes.
     **/
    public String toString() {
	String nm = getClass().getName();
	int i = nm.lastIndexOf('.');
	if (i >= 0) {
	    nm = nm.substring(i+1, nm.length());
	}
	return nm;
    }
    
}

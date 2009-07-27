
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

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;

import java.io.IOException;

/**
 * A command to make a new segment be the active one within a show.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class ActivateSegmentCommand extends Command implements Node {
 
    /**
     * The segment to activate.  In the case of pop, this will be null.
     **/
    protected Segment segment;
    protected boolean push;
    protected boolean pop;

    public ActivateSegmentCommand(Show show, 
            boolean push, boolean pop) {
        super(show);
	this.push = push;
	this.pop = pop;
    }

    public ActivateSegmentCommand(Show show) {
	this(show, false, false);
    }
    
    public boolean getPush() {
        return push;
    }
    
    public boolean getPop() {
        return pop;
    }
    
    public Segment getSegment() {
        return segment;
    }
    
    public void setup(Segment segment) {
	this.segment = segment;
    }    

    public void execute() {
	if (push) {
	    show.pushCurrentSegment();
	}
	Segment seg = segment;
	if (pop) {
	    seg = show.popSegmentStack();
	    if (Debug.LEVEL > 0 && seg == null) {
		Debug.println("****  Error:  Popping segment stack gets null ***");
	    }
	}
	if (seg != null) {
	    show.doActivateSegment(seg);
	}
    }

    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException { 
                
        in.readSuperClassData(this);
        
        this.push = in.readBoolean();
        this.pop = in.readBoolean();
        this.segment = in.readSegmentReference();
    }
}

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

import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.RCKeyEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;


/**
 * Represents a show, with extra data that's useful for tools that run
 * on SE (big JDK), including a record of things like the names of private
 * features, and a list of all of the features and segments.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class SEShow extends Show {

    private Map<String, Segment> privateSegments = null;
    private Object internalMonitor = new Object();

    /**
     * Create a new SEShow.
     *
     * @param director	A Director helper class that can be used to
     *			control the show.
     **/
    public SEShow(Director director) {
	super(director);
    }

    /**
     * Get all of the segments in the show
     **/
    public Segment[] getSegments() {
	return segments;
    }

    /**
     * Get all of the features in the show
     **/
    public Feature[] getFeatures() {
	return features;
    }

    /**
     * Get all of the remote control handlers in the show
     **/
    public RCHandler[] getRCHandlers() {
	return rcHandlers;
    }

    /**
     * Look up a private segment.  This only works if the show had
     * names for the private segments, of course, but for debugging
     * reasons we expect even binary files will do this.
     **/
    public Segment getPrivateSegment(String name) {
	synchronized(internalMonitor) {
	    if (privateSegments == null) {
		privateSegments = new HashMap<String, Segment>();
		for (int i = 0; i < segments.length; i++) {
		    if (segments[i].getName() != null) {
			privateSegments.put(segments[i].getName(), segments[i]);
		    }
		}
	    }
	    return privateSegments.get(name);
	}
    }

    /**
     * Determine if the given Segment is public
     **/
    public boolean isPublic(Segment seg) {
	if (seg.getName() == null) {
	    return false;
	} else {
	    return publicSegments.get(seg.getName()) != null;
	}
    }


    /**
     * Determine if the given Feature is public
     **/
    public boolean isPublic(Feature f) {
	if (f.getName() == null) {
	    return false;
	} else {
	    return publicFeatures.get(f.getName()) != null;
	}
    }


    /**
     * Determine if the given RCHandler is public
     **/
    public boolean isPublic(RCHandler hand) {
	if (hand.getName() == null) {
	    return false;
	} else {
	    return publicRCHandlers.get(hand.getName()) != null;
	}
    }

}

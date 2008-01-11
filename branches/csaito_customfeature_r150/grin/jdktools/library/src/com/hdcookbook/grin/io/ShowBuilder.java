
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

package com.hdcookbook.grin.io;


import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.io.binary.ExtensionsWriter;
import com.hdcookbook.grin.io.builders.DeferredBuilder;
import com.hdcookbook.grin.io.text.ExtensionsParser;
import com.hdcookbook.grin.util.Debug;

import java.io.Reader;
import java.io.IOException;
import java.awt.Font;
import java.awt.Color;

import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.regex.Pattern;


/**
 * A helper class for parsing a show.  Clients of the parser can
 * subclass this to intercept items as they are encountered.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class ShowBuilder {
   
    protected SEShow show;

    private Map<String, Segment> namedSegments = new HashMap<String, Segment>();
    private Map<String, Feature> namedFeatures = new HashMap<String, Feature>();
    private Map<String, RCHandler> namedRCHandlers 
    		= new HashMap<String, RCHandler>();

    private List<Segment> allSegments = new ArrayList<Segment>();
    private List<Feature> allFeatures = new ArrayList<Feature>();
    private List<RCHandler> allRCHandlers = new ArrayList<RCHandler>();

    private List<String> exportedSegments = null;
    private List<String> exportedFeatures = null;
    private List<String> exportedRCHandlers = null;

    private List<DeferredBuilder> deferredBuilders
    	= new ArrayList<DeferredBuilder>();

    private ExtensionsBuilderFactory factory;
    
    public ShowBuilder() {
    }

    public void init(SEShow show) {
        this.show = show;
    }    

    public void setExtensionsBuilderFactory(ExtensionsBuilderFactory factory) {
        this.factory = factory;
    }
    
    /**
     * Returns an instance of ExtensionsParser that this Builder is working with,
     * or null if it is not set.
     */
    public ExtensionsParser getExtensionsParser() {
        if (factory == null) {
            return null;
        }
        return factory.getExtensionsParser();
    }
 
    /**
     * Returns an instance of ExtensionsWriter that this Builder is working with,
     * or null if it is not set.
     */
    public ExtensionsWriter getExtensionsWriter() {
        if (factory == null) {
            return null;
        }
        return factory.getExtensionsWriter();
    }
    
    /** 
     * Called when a new feature is encountered.
     **/
    public void addFeature(String name, int line, Feature f) throws IOException
    {
	if (name != null) {
	    if (namedFeatures.get(name) != null) {
		throw new IOException("Feature named \"" + name
				       + "\" already exists.");
	    }
	    namedFeatures.put(name, f);
	}
	allFeatures.add(f);
    }

    /**
     * Called when a new segment is encountered.
     **/
    public void addSegment(String name, int line, Segment s) throws IOException
    {
	if (name != null) {
	    if (namedSegments.get(name) != null) {
		throw new IOException("Segment named \"" + name
				       + "\" already exists.");
	    }
	    namedSegments.put(name, s);
	}
	allSegments.add(s);
    }

    /**
     * Called when a new command is encountered.
     **/
    public void addCommand(Command command, int line) {
	// At xlet runtime, commands are just part of other things, so we
	// don't need to record them.
    }

    /**
     * Called when a new remote control handler is encountered.
     **/
    public void addRCHandler(String name, int line, RCHandler hand) 
			throws IOException
    {
	if (name != null) {
	    if (namedRCHandlers.get(name) != null) {
		throw new IOException("RC Handler named \"" + name
				       + "\" already exists.");
	    }
	    namedRCHandlers.put(name, hand);
	}
	allRCHandlers.add(hand);
    }

    /**
     * Called when a DeferrredBuilder is created.  It will be called
     * after the show is populated with all of its parts, in the
     * finishBuilding method.
     *
     * @see #finishBuilding()
     **/
    public void addDeferredBuilder(DeferredBuilder builder) {
	deferredBuilders.add(builder);
    }

    /**
     * Called when the exported clause is encountered.  This is optional;
     * if it's not called, then everything defaults to public visibility.
     * <p>
     * The segments, features and handlers may contain the wildcard
     * character "*", which cannot be escaped.
     **/
    public void setExported(String[] segments, String[] features, 
    			    String[] handlers) 
		throws IOException
    {
	if (exportedSegments != null) {
	    throw new IOException("Multiple exported clauses");
	}

	exportedSegments = Arrays.asList(segments);
	exportedFeatures = Arrays.asList(features);
	exportedRCHandlers = Arrays.asList(handlers);
    }

    /** 
     * Look up a segment in the list of all named segments.
     **/
    public Segment getNamedSegment(String name) {
	return namedSegments.get(name);
    }

    /** 
     * Look up a feature in the list of all named features.
     **/
    public Feature getNamedFeature(String name) {
	return namedFeatures.get(name);
    }

    /** 
     * Look up an RC handler in the list of all named handlers.
     **/
    public RCHandler getNamedRCHandler(String name) {
	return namedRCHandlers.get(name);
    }

    /**
     * Called when the show has finished parsing and all forward references
     * have been resolved.  Any DeferredBuider instances are processed here.
     **/
    public void finishBuilding() throws IOException {
	Segment[] segments 
	    = allSegments.toArray(new Segment[allSegments.size()]);
	Feature[] features
	    = allFeatures.toArray(new Feature[allFeatures.size()]);
	RCHandler[] rcHandlers
	    = allRCHandlers.toArray(new RCHandler[allRCHandlers.size()]);
	Hashtable publicSegments 
		= findPublic(namedSegments, namedSegments.keySet(),
		             exportedSegments, "Segment");
	Hashtable publicFeatures 
		= findPublic(namedFeatures, namedFeatures.keySet(),
			     exportedFeatures, "Feature");
	Hashtable publicRCHandlers 
		= findPublic(namedRCHandlers, namedRCHandlers.keySet(),
			     exportedRCHandlers, "RC Handler");
	show.buildShow(segments, features, rcHandlers,
		       publicSegments, publicFeatures, publicRCHandlers);
	for (DeferredBuilder builder : deferredBuilders) {
	    builder.finishBuilding(show);
	}
    }

    private Hashtable findPublic(Map namedThings, Set<String> names,
    			         List<String> exportedThings, String thingName) 
		throws IOException 
    {
	Hashtable result = new Hashtable();
	if (exportedThings == null) {
	    result.putAll(namedThings);
	} else {
	    List<Pattern> patterns = new ArrayList<Pattern>();
	    for (String pat : exportedThings) {
		patterns.add(convertWildcard(pat));
	    }
	    for (String key : names) {
		boolean found = false;
		for (Pattern pat : patterns) {
		    if (pat.matcher(key).matches()) {
			found = true;
			break;
		    }
		}
		if (found) {
		    Object value = namedThings.get(key);
		    assert value != null;
		    result.put(key, value);
		}
	    }
	}
	return result;
    }

    private Pattern convertWildcard(String wildcard) {
	StringBuilder pat = new StringBuilder();
	for (int i = 0; i < wildcard.length(); i++) {
	    char ch = wildcard.charAt(i);
	    if (ch == '*') {
		pat.append(".*");
	    } else if (ch == '?') {
		pat.append(".");
	    } else if (ch == '[') {
		int pos = wildcard.indexOf("]", i);
		String range = null;
		if (pos > -1) {
		    range = wildcard.substring(i, pos + 1);
		}
		if (range.indexOf("[", 1) != -1) {
		    // Can't handle '[' inside a '[' ']' pair)
		    pos = -1;
		}
		if (pos == -1)  {
		    // If we don't have a valid set of characters
		    pat.append(Pattern.quote("" + ch));
		} else {
		    pat.append(range);
		    i += range.length() - 1;
		}
	    } else {
		pat.append(Pattern.quote("" + ch));
	    }
	}
	return Pattern.compile(pat.toString());
    }
}

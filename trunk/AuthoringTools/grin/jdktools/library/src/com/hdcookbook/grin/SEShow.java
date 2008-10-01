
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

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Hashtable;


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
    private SEShowCommands showCommands = new SEShowCommands(this);

    // For mosaic building.
    private ArrayList<MosaicSpec> mosaicSpecs = new ArrayList();
    private boolean noShowFile = false;

    private String binaryGrinFileName = null;	// null means "use default"
    
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
     * Set a flag to indicate that no binary show file should be
     * generated.  This is used for the synthetic show file that is
     * used to collect mosaic definitions.
     **/
    public void setNoShowFile(boolean val) {
	noShowFile = val;
    }

    /**
     * Get a flag to indicate that no binary show file should be
     * generated.  This is used for the synthetic show file that is
     * used to collect mosaic definitions.
     **/
    public boolean getNoShowFile() {
	return noShowFile;
    }

    /**
     * Get the list of sticky images as a string array of the image file names.
     **/
    public String[] getStickyImages() {
	if (stickyImages == null) {
	    return null;
	}
	String[] result = new String[stickyImages.length];
	for (int i = 0; i < result.length; i++) {
	    result[i] = stickyImages[i].getName();
	}
	return result;
    }
    
    /**
     * Get the object that represents the commands defined for this show.
     * If no show commands class has been defined, this will return an
     * unpopulated SEShowCommands object.
     */
    public SEShowCommands getShowCommands() {
        return showCommands;
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

    /**
     * @inheritDoc
     * <p>
     * This adds internal structure checking to the superclass version of
     * this method.
     **/
    @Override
    public void buildShow(Segment[] segments, Feature[] features, 
    		          RCHandler[] rcHandlers, String[] stickyImages,
		          Hashtable publicSegments, Hashtable publicFeatures,
		          Hashtable publicRCHandlers)
	    throws IOException
    {
	super.buildShow(segments, features, rcHandlers, stickyImages,
		        publicSegments, publicFeatures, publicRCHandlers);
	SEDoubleUseChecker checker = new SEDoubleUseChecker();
	accept(checker);
	checker.reportAnyProblems();
    }

    /**
     * Sets the name of the file that the GRIN compiler should generate,
     * if you want to override the default.
     *
     * @param name  The file name, including any extension
     **/
    public void setBinaryGrinFileName(String name) {
	binaryGrinFileName = name;
    }

    /**
     * Gets the name of the file that the GRIN compiler should generate.
     * Gives null if the default file name should be used.
     **/
    public String getBinaryGrinFileName() {
	return binaryGrinFileName;
    }

    /**
     * Called by the ShowParser when a new mosaic or mosaic_hint is found.
     *
     * @throws	IOException if name is a duplicate
     */
    public MosaicSpec newMosaicSpec(String name) throws IOException {
	for (MosaicSpec s : mosaicSpecs) {
	    if (name.equals(s.name)) {
		throw new IOException("Duplicate mosaic \"" + name + "\".");
	    }
	}
	MosaicSpec ms = new MosaicSpec(name);
        mosaicSpecs.add(ms);
	return ms;
    }

    /**
     * Returns an array of MosaicSpec instances associated with this show, or
     * an zero-length array if none is found.
     */
    public MosaicSpec[] getMosaicSpecs() {
        return mosaicSpecs.toArray(new MosaicSpec[mosaicSpecs.size()]);
    }
    

    /**
     * Visit a SEShow with a SEShowVisitor.  This will call
     * visitShow on the given visitor; it's up to the visitor to
     * call SEShow.accept(xxx) for any children it wants to visit.
     **/
    public void accept(SEShowVisitor visitor) {
	visitor.visitShow(this);
    }

    /**
     * Visit a list of segments with a SEShowVisitor.  This will call
     * visitSegment on each segment.  
     **/
    public static void acceptSegments(SEShowVisitor visitor, Segment[] segments)
    {
	for (Segment e : segments) {
            if (e instanceof SENode) {
                ((SENode)e).accept(visitor);
            }
        }
    }

    /**
     * Visit a list of features with a SEShowVisitor.  This will call
     * acceptFeature() on each of the features.
     **/
    public static void acceptFeatures(SEShowVisitor visitor, Feature[] features)
    {
	for (Feature e : features) {
	    acceptFeature(visitor, e);
	}
    }

    /**
     * Visit a list of RC handlers with a SEShowVisitor.  This will
     * call acceptRCHandler() on each of the handlers.
     **/
    public static void acceptRCHandlers(SEShowVisitor visitor, 
    					RCHandler[] rcHandlers) 
    {
	for (RCHandler e : rcHandlers) {
	    acceptRCHandler(visitor, e);
	}
    }

    /**
     * Accept a feature of a show.  This will call the appropriate
     * visitXXX method on the visitor, according to the subtype of
     * feature passed in.
     **/
    public static void acceptFeature(SEShowVisitor visitor, Feature feature) {
	if (feature instanceof SENode) {
            ((SENode)feature).accept(visitor);
	} else {
	    visitor.visitUserDefinedFeature(feature);
	}
    }
    
    /**
     * Accept an RC handler from a show.  This will call the appropriate
     * visitXXX method on the visitor, according  to the subtype of the
     * handler passed in.
     **/
    public static void acceptRCHandler(SEShowVisitor visitor, RCHandler handler)
    {
	if (handler instanceof SENode) {
	    ((SENode)handler).accept(visitor);
	} else {
	    assert false;
	}
    }

    /**
     * Visit a list of commands with a SEShowVisitor.  This will
     * call acceptCommand() on each of the commands.
     **/
    public static void acceptCommands(SEShowVisitor visitor, 
    					Command[] commands) 
    {
	for (Command e : commands) {
	    acceptCommand(visitor, e);
	}
    }

    /**
     * Accept a command from a show.  This will call the appropriate
     * visitXXX method on the visitor, according  to the subtype of the
     * command passed in.
     **/
    public static void acceptCommand(SEShowVisitor visitor, Command command)
    {
	if (command instanceof SENode) {
	    ((SENode)command).accept(visitor);
	} else {
	    visitor.visitUserDefinedCommand(command);
	}
    }
    
    /**
     * Returns true if the node passed in is recorded as an public element
     * in this show, false otherwise.
     * 
     * @throws RuntimeException if node is neither an instance of 
     * Feature, RCHandler, nor Segment.
     */
    public boolean isPublic(SENode node) {
        if (node instanceof Feature) {
            return publicFeatures.contains(node);
        } else if (node instanceof RCHandler) {
            return publicRCHandlers.contains(node);
        } else if (node instanceof Segment) {
            return publicSegments.contains(node);
        } else {
            throw new RuntimeException("Unknown node type " + node);
        }
    }
    
    public void printContent(PrintStream out) {
        out.println("Features");
        for (int i = 0; i < features.length; i++) {
            out.println(i + " : " + features[i]);
        }
        
        out.println("\nSegments");
        for (int i = 0; i < segments.length; i++) {
            out.println(i + " : " + segments[i]);   
        }
        
        out.println("\nRCHandlers");
        for (int i = 0; i < rcHandlers.length; i++) {
            out.println(i + " : " + rcHandlers[i]);   
        }        
        
        out.println();        
    }
}

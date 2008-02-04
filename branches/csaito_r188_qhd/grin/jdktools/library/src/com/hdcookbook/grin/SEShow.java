
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

import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.Clipped;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.GuaranteeFill;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.SetTarget;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Timer;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.TranslatorModel;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    private ArrayList<MosaicHint> mosaicHints;
    
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
     * Get the object that represents the commands defined for this show
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
    		          RCHandler[] rcHandlers,
		          Hashtable publicSegments, Hashtable publicFeatures,
		          Hashtable publicRCHandlers)
	    throws IOException
    {
	super.buildShow(segments, features, rcHandlers,
		        publicSegments, publicFeatures, publicRCHandlers);
	SEDoubleUseChecker checker = new SEDoubleUseChecker();
	accept(checker);
	checker.reportAnyProblems();
    }

    /**
     * Called by the ShowParser when the mosaic_hint element is encountered.
     */
    public void takeMosaicHint(String name, int width, 
			 int height, String[] images) 
    {    
        if (mosaicHints == null) {
            mosaicHints = new ArrayList();       
        }  
        
        mosaicHints.add(new MosaicHint(name, width, height, images));        
    }

    /**
     * Returns an array of MosaicHints associated with this show, or
     * an zero-length array if none is found.
     */
    public MosaicHint[] getMosaicHints() {
        if (mosaicHints == null) {
            return new MosaicHint[0];
        }    
        return mosaicHints.toArray(new MosaicHint[mosaicHints.size()]);
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
	for (Segment e : Arrays.asList(segments)) {
	    visitor.visitSegment(e);
	}
    }

    /**
     * Visit a list of features with a SEShowVisitor.  This will call
     * acceptFeature() on each of the features.
     **/
    public static void acceptFeatures(SEShowVisitor visitor, Feature[] features)
    {
	for (Feature e : Arrays.asList(features)) {
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
	for (RCHandler e : Arrays.asList(rcHandlers)) {
	    acceptRCHandler(visitor, e);
	}
    }

    /**
     * Accept a feature of a show.  This will call the appropriate
     * visitXXX method on the visitor, according to the subtype of
     * feature passed in.
     **/
    public static void acceptFeature(SEShowVisitor visitor, Feature feature) {
        
        Class featureClazz = feature.getClass();
        
	if (featureClazz == Assembly.class) {
	    visitor.visitAssembly((Assembly) feature);
	} else if (featureClazz == Box.class) {
	    visitor.visitBox((Box) feature);
	} else if (featureClazz == Clipped.class) {
	    visitor.visitClipped((Clipped) feature);
	} else if (featureClazz == Fade.class) {
	    visitor.visitFade((Fade) feature);
	} else if (featureClazz == FixedImage.class) {
	    visitor.visitFixedImage((FixedImage) feature);
	} else if (featureClazz == Group.class) {
	    visitor.visitGroup((Group) feature);
	} else if (featureClazz == GuaranteeFill.class) {
	    visitor.visitGuaranteeFill((GuaranteeFill) feature);
	} else if (featureClazz == ImageSequence.class) {
	    visitor.visitImageSequence((ImageSequence) feature);
	} else if (featureClazz == SetTarget.class) {
	    visitor.visitSetTarget((SetTarget) feature);
	} else if (featureClazz == SrcOver.class) {
	    visitor.visitSrcOver((SrcOver) feature);
	} else if (featureClazz == Text.class) {
	    visitor.visitText((Text) feature);
	} else if (featureClazz == Timer.class) {
	    visitor.visitTimer((Timer) feature);
	} else if (featureClazz == Translator.class) {
	    visitor.visitTranslator((Translator) feature);
	} else if (featureClazz == TranslatorModel.class) {
	    visitor.visitTranslatorModel((TranslatorModel) feature);
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
	if (handler instanceof CommandRCHandler) {
	    visitor.visitCommandRCHandler((CommandRCHandler) handler);
	} else if (handler instanceof VisualRCHandler) {
	    visitor.visitVisualRCHandler((VisualRCHandler) handler);
	} else {
	    assert false;
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

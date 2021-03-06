
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

import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.Clipped;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.GuaranteeFill;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.features.SetTarget;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Timer;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.TranslatorModel;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;
import com.hdcookbook.grin.input.RCKeyEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;


/**
 * Utility class used by SEShow to check a constraint on the show graph.
 * This constraint is that it shall never be possible for a feature to
 * be visible twice simulataneously.  This is checked on the active features
 * of each segment, with special handling for assemblies.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class SEDoubleUseChecker implements SEShowVisitor {

    private ArrayList<String> errors = new ArrayList<String>();
    private Set<Feature> activeFeatures;

    /**
     * Report any problems we've seen visiting the show graph.  Errors
     * are reported by throwing an IOException.
     **/
    public void reportAnyProblems() throws IOException {
	if (errors.size() > 0) {
	    String message = "Feature(s) simultaneously visible > 1 times:  ";
	    System.out.println(message);
	    for (String error : errors) {
		System.out.println("    " + error);
		message += error;
		message += "  ";
	    }
	   System.out.println();
	   throw new IOException(message);
	}
    }

    public void visitShow(SEShow show) {
	// We only visit the segments, and then recurse down the active features
	// for each segment.
	SEShow.acceptSegments(this, show.getSegments());
    }

    public void visitSegment(Segment segment) {
	activeFeatures = new HashSet<Feature>();
	SEShow.acceptFeatures(this, segment.getActiveFeatures());
    }

    private void addActive(Feature feature) {
	if (activeFeatures.contains(feature)) {
	    errors.add("" + feature.getName());
	    	// The name shouldn't be null, because it should be impossible
		// to have an un-named feature appear twice in the show graph,
		// but with automatically generated show graphs we can't rule
		// this out.  This makes it prudent to append to "".
	}
	activeFeatures.add(feature);
    }


    public void visitAssembly(Assembly feature) {
	addActive(feature);
	//
	// An assembly is the tricky case.  The parts of an assembly can
	// contain the same features, so we have to construct a new set
	// for each of our parts.  However,  for the other features in
	// the segment, we need to assume that any feature in any part
	// of this assembly might be active, so we need to add the
	// union of our children to activeFeatures.
	//
	Set originalSet = activeFeatures;
	Set unionSet = new HashSet(originalSet);
	for (Feature part : Arrays.asList(feature.getParts())) {
	    activeFeatures = new HashSet(originalSet);
	    SEShow.acceptFeature(this, part);
	    unionSet.addAll(activeFeatures);
	}
	activeFeatures = unionSet;
    }

    public void visitBox(Box feature) {
	addActive(feature);
    }

    public void visitClipped(Clipped feature) {
	addActive(feature);
	SEShow.acceptFeature(this, feature.getPart());
    }

    public void visitFade(Fade feature) {
	addActive(feature);
	SEShow.acceptFeature(this, feature.getPart());
    }

    public void visitFixedImage(FixedImage feature) {
	addActive(feature);
    }

    public void visitGroup(Group feature) {
	Feature[] parts = feature.getParts();
	if (parts.length != 0) {
	    addActive(feature);
	    // A group with no children is a special case:  Because it does
	    // nothing, it's OK for it to be active more than once.  An empty
	    // group is used as an idiom for "Nothing" or "Null feature", so
	    // this does happen in practice.
	}
	for (Feature part : Arrays.asList(parts)) {
	    SEShow.acceptFeature(this, part);
	}
    }

    public void visitGuaranteeFill(GuaranteeFill feature) {
	addActive(feature);
	SEShow.acceptFeature(this, feature.getPart());
    }

    public void visitImageSequence(ImageSequence feature) {
	addActive(feature);
    }

    public void visitUserDefinedFeature(Feature feature) {
	addActive(feature);
        if (feature instanceof Modifier) {
	   SEShow.acceptFeature(this, ((Modifier)feature).getPart());
        }   
    }

    public void visitSetTarget(SetTarget feature) {
	addActive(feature);
	SEShow.acceptFeature(this, feature.getPart());
    }

    public void visitSrcOver(SrcOver feature) {
	addActive(feature);
	SEShow.acceptFeature(this, feature.getPart());
    }

    public void visitText(Text feature) {
	addActive(feature);
    }

    public void visitTimer(Timer feature) {
	addActive(feature);
    }

    public void visitTranslator(Translator feature) {
	addActive(feature);
	SEShow.acceptFeature(this, feature.getPart());
    }

    public void visitTranslatorModel(TranslatorModel feature) {
	addActive(feature);
    }

    public void visitCommandRCHandler(CommandRCHandler handler) {
	// do nothing
    }

    public void visitVisualRCHandler(VisualRCHandler handler) {
	// do nothing
    }
}

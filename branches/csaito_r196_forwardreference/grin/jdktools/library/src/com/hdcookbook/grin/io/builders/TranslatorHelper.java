
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

package com.hdcookbook.grin.io.builders;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.SEShowVisitor;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.util.Debug;
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
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.SETranslator;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;
import com.hdcookbook.grin.input.RCKeyEvent;


import java.io.IOException;


/**
 * A helper class for creating a Translation object.  This handles
 * the difference between the absolute "linear" Translation features
 * and the newer, more sensible "linear-relative" Translation type.  Only
 * linear-relative is supported at runtime.
 * <p>
 * To use this, build a normal Translator feature and add it to the
 * show, then make a TranslatorHelper for that feature.  Add the
 * TranslatorHelper to the ShowBuilder using addDeferredBuilder().
 * Then, the ShowBuilder will call back to the TranslatorHelper, which
 * will finish up the needed initialization of the translator.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class TranslatorHelper implements DeferredBuilder {

    private SETranslator translator;
    private int line;
    private int xCoord;
    private int yCoord;


    public TranslatorHelper(SETranslator translator, int line) {
	this.translator = translator;
	this.line = line;
    }

    /** 
     * @inheritDoc
     * <p>
     * Since the show is built, we can calculate the upper-left hand
     * corner of our translator's child.  If our model uses
     * absolute coordinates, we use this calculation to transform it
     * into relative coordinates.
     * <p>
     * Note that using relative coordinates on a translation is less
     * confusing, and is to be encouraged.  Absolute coordinates exist
     * mostly for old show files, created before relative coordinates were
     * supported.
     **/
    public void finishBuilding(SEShow show) throws IOException {
	if (translator.getModelIsRelative()) {
	    return;
	}

	//
	// This code is a little non-obvious.  What we're doing is
	// starting at the translator, and doing a depth-first traversal
	// of its children, looking for the minimum x and y coordinates.
	// That lets us calculate the x,y offset needed to make our children
	// land at the absolute xx,y coordinates of the TranslationModel.
	// This applies even if we have a child that's a translation.  We
	// always assume a child translation starts out at a 0,0 offset for the
	// purposes of this calculation, which is about the only reasonable
	// thing for us to do.
	//
	SEShowVisitor visitor = new SEShowVisitor() {
	    public void visitShow(SEShow show) { }
	    public void visitSegment(Segment segment) { }

	    public void visitAssembly(Assembly feature) { 
		SEShow.acceptFeatures(this, feature.getParts());
	    }
	    public void visitBox(Box feature) { 
		check(feature.getX(), feature.getY());
	    }
	    public void visitClipped(Clipped feature) { 
		SEShow.acceptFeature(this, feature.getPart());
	    }
	    public void visitFade(Fade feature) { 
		SEShow.acceptFeature(this, feature.getPart());
	    }
	    public void visitFixedImage(FixedImage feature) { 
		check(feature.getX(), feature.getY());
	    }
	    public void visitGroup(Group feature) {
		SEShow.acceptFeatures(this, feature.getParts());
	    }
	    public void visitGuaranteeFill(GuaranteeFill feature) { 
		SEShow.acceptFeature(this, feature.getPart());
	    }
	    public void visitImageSequence(ImageSequence feature) {
		check(feature.getX(), feature.getY());
	    }
	    public void visitUserDefinedFeature(Feature feature) {
                if (feature instanceof Modifier) {
                    SEShow.acceptFeature(this, ((Modifier)feature).getPart());
                } else {
                    int x = feature.getX();
                    int y = feature.getY();
                    if (x != Integer.MAX_VALUE && y != Integer.MAX_VALUE) {
                        check(x, y);
                    }
                }
	    }
	    public void visitSetTarget(SetTarget feature) {
		SEShow.acceptFeature(this, feature.getPart());
	    }
	    public void visitSrcOver(SrcOver feature) {
		SEShow.acceptFeature(this, feature.getPart());
	    }
	    public void visitText(Text feature) {
		check(feature.getX(), feature.getY());
	    }
	    public void visitTranslator(Translator feature) {
		SEShow.acceptFeature(this, feature.getPart());
	    }
	    public void visitInterpolatedModel(InterpolatedModel feature) { }

	    public void visitCommandRCHandler(CommandRCHandler handler) { }
	    public void visitVisualRCHandler(VisualRCHandler handler) { }
	};

	    // This does the actual depth-first traversal...
	xCoord = Integer.MAX_VALUE;
	yCoord = Integer.MAX_VALUE;
	SEShow.acceptFeature(visitor, translator.getPart());

	    // Now, xCoord and yCoord contain the upper-left corner of the
	    // bounding rectangle of our children.
	if (xCoord == Integer.MAX_VALUE || yCoord == Integer.MAX_VALUE) {
	    throw new IOException("Can't determine upper-left hand coordinates "
	    		          + " of tranlator's child at line " + line);
	}
	translator.setupAbsoluteXOffset(xCoord);
	translator.setupAbsoluteYOffset(yCoord);
    }

    private void check(int x, int y) {
	if (x < xCoord) {
	    xCoord = x;
	}
	if (y < yCoord) {
	    yCoord = y;
	}
    }
}

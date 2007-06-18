
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


package com.hdcookbook.grin.features;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;

import java.io.IOException;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class Assembly extends Feature {

    private String[] partNames;
    private Feature[] parts;
    private Feature currentFeature = null;
    private boolean activated = false;

    public Assembly(Show show, String name) throws IOException {
	super(show, name);
    }

    /**
     * Called from parser
     **/
    public void setParts(String[] partNames, Feature[] parts) { 
	this.partNames = partNames;
	this.parts = parts;
	currentFeature = parts[0];
    }

    public String[] getPartNames() {
	return partNames;
    }

    public Feature[] getParts() {
	return parts;
    }

    /**
     * Initialize this feature.  This is called on show initialization.
     * A show will initialize all of its features after it initializes
     * the phases.
     **/
    public void initialize() {
	// The show will initialize our sub-features, so we don't
	// need to do anything here.
    }

    public int getStartX() {
	int x = Integer.MAX_VALUE;
	for (int i = 0; i < parts.length; i++) {
	    int xi = parts[i].getStartX();
	    if (xi < x) {
		x = xi;
	    }
	}
	return x;
    }

    public int getStartY() {
	int y = Integer.MAX_VALUE;
	for (int i = 0; i < parts.length; i++) {
	    int yi = parts[i].getStartY();
	    if (yi < y) {
		y = yi;
	    }
	}
	return y;
    }

    /**
     * Free any resources held by this feature.  It is the opposite of
     * setup; each call to setup() shall be balanced by
     * a call to unsetup(), and they shall *not* be nested.  
     * <p>
     * It's possible an active phase may be destroyed.  For example,
     * the last phase a show is in when the show is destroyed will
     * probably be active (and it will probably be an empty phase
     * too!).
     **/
    public void destroy() {
	// The show will destroy our sub-features, so we don't
	// need to do anything here.
    }

    //
    // This is synchronized to only occur within model updates.
    //
    protected void setActivateMode(boolean mode) {
	activated = mode;
	if (mode) {
	    currentFeature.activate();
	} else {
	    currentFeature.deactivate();
	}
    }

    /**
     * This should not be directly called by clients of the GRIN
     * framework.  Calls are synchronized to only occur within
     * model updates, with the show lock held.  The current feature
     * can be set with:  <pre>
     *     show.runCommand(new ActivatePartCommand(assembly, part))
     * </pre>
     **/
    public void setCurrentFeature(Feature feature) {
	if (currentFeature == feature) {
	    return;
	}
	if (activated) {
	    feature.activate();
	    currentFeature.deactivate();
	}
	currentFeature = feature;
    }

    protected void setSetupMode(boolean mode) {
	if (mode) {
	    for (int i = 0; i < parts.length; i++) {
		parts[i].setup();
	    }
	} else {
	    for (int i = 0; i < parts.length; i++) {
		parts[i].unsetup();
	    }
	}
    }

    public void doSomeSetup() {
	for (int i = 0; i < parts.length; i++) {
	    if (parts[i].needsMoreSetup()) {
		parts[i].doSomeSetup();
		return;
	    }
	}
	// None needed
    }

    public boolean needsMoreSetup() {
	for (int i = 0; i < parts.length; i++) {
	    if (parts[i].needsMoreSetup()) {
		return true;
	    }
	}
	return false;
    }

    public Feature findPart(String name) {
	for (int i = 0; i < parts.length; i++) {
	    if (partNames[i].equals(name)) {
		return parts[i];
	    }
	}
        return null;
    }

    public Feature getCurrentPart() {
	return currentFeature;
    }

    public void  addDisplayArea(Rectangle area) {
	currentFeature.addDisplayArea(area);
    }

    public void paintFrame(Graphics2D gr) {
	currentFeature.paintFrame(gr);
    }

    public void advanceToFrame(int newFrame) {
	currentFeature.advanceToFrame(newFrame);
    }
}

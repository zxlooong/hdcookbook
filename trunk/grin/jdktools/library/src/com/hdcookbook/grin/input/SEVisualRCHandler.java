/*  
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

package com.hdcookbook.grin.input;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.SENode;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.SEShowVisitor;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.io.binary.GrinDataOutputStream;
import com.hdcookbook.grin.io.builders.VisualRCHandlerHelper;
import java.awt.Rectangle;
import java.io.IOException;

public class SEVisualRCHandler extends VisualRCHandler implements SENode {
    private VisualRCHandlerHelper helper;

    public SEVisualRCHandler(SEShow show, VisualRCHandlerHelper helper) {
        super();
        setShow(show);
        this.helper = helper;
    }

    public SEVisualRCHandler(String name, String[] stateNames,
			   int[] upDown, int[] rightLeft,
			   Command[][] selectCommands, 
			   Command[][] activateCommands, 
			   Rectangle[] mouseRects, int[] mouseRectStates,
			   int timeout, Command[] timeoutCommands,
			   VisualRCHandlerHelper helper) 
    {
        super();
	this.name = name;
	this.stateNames = stateNames;
	this.upDown = upDown;
	this.rightLeft = rightLeft;
	this.selectCommands = selectCommands;
	this.activateCommands = activateCommands;
	this.mouseRects = mouseRects;
	this.mouseRectStates = mouseRectStates;
	this.timeout = timeout;
	this.timeoutCommands = timeoutCommands;
        this.helper = helper;
    }

    public VisualRCHandlerHelper getHelper() {
        return helper;
    }

    /** used by the binaryconverter */  
    public int[] getUpDown() {
        return upDown;
    }

    /** used by the binaryconverter */  
    public int[] getRightLeft() {
        return rightLeft;
    }

    /** used by the binaryconverter */  
    public String[] getStateNames() {
        return stateNames;
    }
    
    /** used by the binaryconverter */  
    public Command[][] getSelectCommands() {
        return selectCommands;
    }
    
    /** used by the binaryconverter */  
    public Command[][] getActivateCommands() {
        return activateCommands;
    }

    /** used by the binaryconverter */  
    public Rectangle[] getMouseRects() {
        return mouseRects;
    }
    
    /** used by the binaryconverter */  
    public int[] getMouseRectStates() {
        return mouseRectStates;
    }
    
    /** used by the binaryconverter */  
    public int getTimeout() {
        return timeout;
    }
    
    /** used by the binaryconverter */  
    public Command[] getTimeoutCommands() {
        return timeoutCommands;
    }
    
    /* used by the binaryconverter */  
    public Assembly getAssembly() {
        return assembly;
    }

    /* used by the binaryconverter */      
    public Feature[] getSelectFeatures() {
        return selectFeatures;
    }
    
    /* used by the binaryconverter */  
    public Feature[] getActivateFeatures() {
        return activateFeatures;
    }

    /** used by the binaryconverter */  
    public void setUpDown(int[] upDown) {
        this.upDown = upDown;
    }

    /** used by the binaryconverter */  
    public void setRightLeft(int[] rightLeft) {
        this.rightLeft = rightLeft;
    }

    /** used by the binaryconverter */  
    public void setStateNames(String[] stateNames) {
        this.stateNames = stateNames;
    }
    
    /** used by the binaryconverter */  
    public void setSelectCommands(Command[][] selectCommands) {
        this.selectCommands = selectCommands;
    }
    
    /** used by the binaryconverter */  
    public void setActivateCommands(Command[][] activeCommands) {
        this.activateCommands = activeCommands;
    }

    /** used by the binaryconverter */  
    public void setMouseRects(Rectangle[] mouseRects) {
        this.mouseRects = mouseRects;
    }
    
    /** used by the binaryconverter */  
    public void setMouseRectStates(int[] mouseRectStates) {
        this.mouseRectStates = mouseRectStates;
    }
    
    /** used by the binaryconverter */  
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    /** used by the binaryconverter */  
    public void setTimeoutCommands(Command[] timeoutCommands) {
        this.timeoutCommands = timeoutCommands;
    }
    
    /* used by the binaryconverter */  
    public void setAssembly(Assembly assembly) {
        this.assembly = assembly;
    }

    /* used by the binaryconverter */      
    public void setSelectFeatures(Feature[] selectFeatures) {
        this.selectFeatures = selectFeatures;
    }
    
    /* used by the binaryconverter */  
    public void setActivateFeatures(Feature[] activateFeatures) {
        this.activateFeatures = activateFeatures;
    }
    
    /**
     * Called from the parser
     **/
    public void setup(Assembly assembly, Feature[] selectFeatures, 
    		      Feature[] activateFeatures)
    {
	this.assembly = assembly;
	this.selectFeatures = selectFeatures;
	this.activateFeatures = activateFeatures;
	// activating this handler can change its state
    }
        
    public void writeInstanceData(GrinDataOutputStream out) 
            throws IOException {
        
        out.writeSuperClassData(this);
        
        out.writeIntArray(getUpDown());
        out.writeIntArray(getRightLeft());
        out.writeStringArray(getStateNames());
        if (selectCommands == null) {
            out.writeNull();
        } else {
            out.writeNonNull();
            out.writeInt(selectCommands.length);
            for (int i = 0; i < selectCommands.length; i++) {
                out.writeCommands(selectCommands[i]);
            }
        }
        if (activateCommands == null) {
            out.writeNull();
        } else {
            out.writeNonNull();
            out.writeInt(activateCommands.length);
            for (int i = 0; i < activateCommands.length; i++) {
                out.writeCommands(activateCommands[i]);
            }
        }
        
        out.writeRectangleArray(getMouseRects());
        out.writeIntArray(getMouseRectStates());
        out.writeInt(getTimeout());
        out.writeCommands(getTimeoutCommands());
       
        out.writeBoolean(assembly != null);
	if (assembly != null) {
	    out.writeFeatureReference(assembly);
	}
        
        out.writeFeaturesArrayReference(getSelectFeatures());
        out.writeFeaturesArrayReference(getActivateFeatures());
    }

    public String getRuntimeClassName() {
        return VisualRCHandler.class.getName();
    }
    
    public void accept(SEShowVisitor visitor) {
        visitor.visitVisualRCHandler(this);
    }
}

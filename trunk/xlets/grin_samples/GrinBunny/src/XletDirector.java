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

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.util.Debug;


/** 
 * The director for the small xlet UI that manages test state using the
 * popup menu.
 **/

public class XletDirector extends Director {
	
    public GrinBunnyXlet xlet;
    private boolean destroyed = false;

    Assembly F_KeyUpState;
    Feature F_KeyUpState_enabled;
    Feature F_KeyUpState_disabled;


    public XletDirector(GrinBunnyXlet xlet) {
	this.xlet = xlet;
    }

    public void initialize() {
	F_KeyUpState = (Assembly) getFeature("F:KeyUpState");
	F_KeyUpState_enabled = getPart(F_KeyUpState, "enabled");
	F_KeyUpState_disabled = getPart(F_KeyUpState, "disabled");
    }

    /**
     * Sets the UI state to match the model.
     **/
    public void setUIState() {
	if (xlet.sendKeyUp) {
	    F_KeyUpState.setCurrentFeature(F_KeyUpState_enabled);
	} else {
	    F_KeyUpState.setCurrentFeature(F_KeyUpState_disabled);
	}
    }


    /**
     * Called by a java_command in the show when it's time to destroy
     * the show. When the xlet wants to shut
     * down, it should send the show to S:Finished, and then call
     * waitForShowDestroyed().  Navigating to S:Finished will cause
     * destroyShow() to be called in the animation thread.  So, the
     * proper shutdown sequence for an xlet is:
     * <pre>
     *		Send the show to S:Finished
     *		Call waitForShowDestroyed()
     *		destroy the animation engine
     * </pre>
     *
     * @see #waitForShowDestroyed();
     **/
    public void destroyShow() {
	synchronized(this) {
	    destroyed = true;
	    notifyAll();
	}
    }

    /**
     * Wait for the show to be destroyed.  This should be called from the
     * xlet when the xlet is being destroyed to wait until the show is shut
     * down.
     *
     * @see #destroyShow()
     **/
    public synchronized void waitForShowDestroyed() throws InterruptedException
    {
	while (!destroyed) {
	    wait();
	}
    }
}


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


package com.hdcookbook.grin.util;

public class SetupManager implements Runnable {

    private SetupClient[] settingUp;
    int first;	     // index into setting up.  Wraps.
    int lastPlusOne; // index into setting up.  Wraps.
    private Runnable runnable;		// avoids exposing run method
    private Thread worker;
    private Object monitor = new Object();
    private boolean running = false;

    public SetupManager(int numFeatures) {
	settingUp = new SetupClient[numFeatures + 1];
	first = 0;
	lastPlusOne = 0;
    }

    public void start() {
            // @@ TODO:  Make it one thread for all setup managers
	synchronized(monitor) {
	    worker = new Thread(this, "SetupManager");
	    worker.setDaemon(true);
	    worker.setPriority(3);
	    worker.start();
	    running = true;
	}
    }

    public void stop() {
	synchronized(monitor) {
	    if (running) {
		running = false;
		monitor.notifyAll();
	    }
	}
    }

    public void scheduleSetup(SetupClient f) {
	synchronized(monitor) {
	    int i = first;
	    while (i != lastPlusOne) {
		if (f == settingUp[i]) {
		    return;	// Already on queue
		}
		i = next(i);
	    }
	    settingUp[lastPlusOne] = f;
	    lastPlusOne = next(lastPlusOne);
	    monitor.notifyAll();
	}
    }

    private int next(int i) {
	i++;
	if (i >= settingUp.length) {
	    return 0;
	} else {
	    return i;
	}
    }

    /**
     * This isn't really public; it's only called by our worker thread.
     **/
    public void run() {
	for (;;) {
	    int work = -1;
	    synchronized(monitor) {
		if (!running) {
		    return;
		}
		if (first != lastPlusOne) {
		    work = first;
		}
	    }
	    if (work == -1) {
		synchronized(monitor) {
		    if (first == lastPlusOne) {  // That is, if it still is
			try {
			    monitor.wait();
			} catch (InterruptedException ex) {
			    return;
			}
		    }
		}
	    } else {	// work != -1
		settingUp[work].doSomeSetup();
		synchronized(monitor) {
		    if (!settingUp[work].needsMoreSetup()) {
			// There's a slight chance of a race condition
			// where this won't be true.
			settingUp[work] = null;
			first = next(first);
		    }
		}
	    }
	}
    }
}



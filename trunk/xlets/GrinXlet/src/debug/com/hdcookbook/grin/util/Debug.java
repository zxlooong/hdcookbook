
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
package com.hdcookbook.grin.util;

import com.hdcookbook.grinxlet.DebugLog;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.UnsupportedEncodingException;

/**
 * Debugging support.  This overrides the version in the GRIN library, to redirect
 * debug output to the log maintained by GenericGame.
 *
 * @author Bill Foote (http://jovial.com)
 */
public class Debug {

    /**
     * Variable to say that assertions are enabled.  If
     * set false, then javac should strip all assertions
     * out of the generated code.
     * <p>
     * Usage:
     * <pre>
     *     if (Debug.ASSERT && some condition that should be false) {
     *         Debug.println(something interesting);
     *     }
     * </pre>
     * <p>
     * Note that JDK 1.4's assertion facility can't be used
     * for Blu-Ray, since PBP 1.0 is based on JDK 1.3.
     **/
    public final static boolean ASSERT = true;
    /**
     * Debug level.  2 = noisy, 1 = some debug, 0 = none.
     **/
    public final static int LEVEL = 2;

    /**
     * Variable to say if time profiling is enabled.
     * <p>
     * Usage:
     * <pre>
     *     private static byte[] PROFILE_TIMER_1;
     *     static {
     *          if (Debug.PROFILE) {
     *              PROFILE_TIMER_1 = Debug.makeProfileTimer("My animation");
     *          }
     *      }
     *      <...>
     *      public void myMethod() {
     *          int token;
     *     	    if (Debug.PROFILE) {
     *     	    	Debug.initProfiler(2000, "127.0.0.1");
     *              token = Debug.startTimer(PROFILE_TIMER_1);
     *          }
     *          doTheThingIWantMeasured();
     *          if (Debug.PROFILE) {
     *          	Debug.stopTimer(token);
     *          	Debug.doneProfiling();
     *          }
     *      }
     * </pre>
     **/
    public final static boolean PROFILE = true;
    public final static byte TIMER_START = 0;
    public final static byte TIMER_STOP = 1;
    private static DatagramSocket socket;
    private static DatagramPacket packet;
    private static byte[] stopBuf = new byte[5];
    private static int token = 0;

    private Debug() {
    }

    public static void println() {
        if (LEVEL > 0) {
            println("");
        }
    }

    public static void println(Object o) {
        if (LEVEL > 0) {
            System.err.println(o);
            if (o == null) {
                DebugLog.println("" + null);
            } else {
                DebugLog.println(o.toString());
            }
        }
    }

    /**
     * Called on assertion failure.  This is a useful during development:  When
     * you detect a condition that should be impossible, you can trigger an
     * assertion failure.  That means you've found a bug.  When an assertion
     * failure is detected, you basically want to shut everything down,
     * so that the developer notices immediately, and sees the message.
     **/
    public static void assertFail(String msg) {
        if (ASSERT) {
            try {
                throw new RuntimeException("\n***  Assertion failure:  " + msg + "  ***\n");
            } catch (RuntimeException ex) {
                printStackTrace(ex);
            }
            AssetFinder.abort();
        }
    }

    /**
     * Called on assertion failure.  This is a useful during development:  When
     * you detect a condition that should be impossible, you can trigger an
     * assertion failure.  That means you've found a bug.  When an assertion
     * failure is detected, you basically want to shut everything down,
     * so that the developer notices immediately, and sees the message.
     **/
    public static void assertFail() {
        if (ASSERT) {
            assertFail("");
        }
    }

    /**'
     * Print a stack trace to the debug log, if Debug.LEVEL > 0.  Note that you can
     * also easily use this for the equivalent of <code>Thread.dumpStack()</code> using this
     * bit of code:
     * <pre>
     *      try {
     *          throw new RuntimeException("STACK BACKTRACE");
     *      } catch (RuntimeException ex) {
     *		Debug.printStackTrace(ex);
     *      }
     * </pre>
     **/
    public static void printStackTrace(Throwable t) {
        if (LEVEL > 0) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            println(sw.toString());
        }
    }

    /**
     * Initializes this class with the network address of the
     * remote computer where profiling is done.
     *
     * @param port The UDP port on which the remote computer is waiting for
     *             data
     * @param host The hostname or the IP address of the remote computer
     */
    public static void initProfiler(int port, String host) {
        InetAddress addr = null;
        try {
            // get the inet address from the string
            addr = InetAddress.getByName(host);
            socket = new DatagramSocket();
            packet = new DatagramPacket(stopBuf, stopBuf.length,
                                        addr, port);
        } catch (IOException e) {
            printStackTrace(e);
        } 
    }

    /**
     * Allocates buffer and returns UTF-8 bytes for the string representing
     * profile information. This method is meant to be called by the application
     * during class loading:
     * Usage:
     * <p>
     * <pre>
     *     private static byte[] PROFILE_TIMER_1;
     *     static {
     *          if (Debug.PROFILE) {
     *              PROFILE_TIMER_1 = Debug.makeProfileTimer("my animation");
     *          }
     *     }
     * </pre>
     * @param description of the task that is being profiled.
     * @return A UTF-8 encoded byte array representing the description.
     */
    public static byte[] makeProfileTimer(String description) {
        byte[] utf8Buf = null;
        try {
            utf8Buf = description.getBytes("UTF-8");
         } catch (UnsupportedEncodingException e) {
            printStackTrace(e);
         }
         byte[] retBuf = new byte[(utf8Buf.length + 5)];
         System.arraycopy(utf8Buf, 0,
                         retBuf, 5, utf8Buf.length);
         utf8Buf = null;
         return retBuf;
    }

    /**
     * Indicates profiling is over, releases the network resources.
     */
    public static synchronized void doneProfiling() {
        socket.close();
    }

    /**
     * Signals starting the timer on the remote computer.
     *
     * @param description Description of the task that is timed
     * @return Returns the token for the task that is timed
     */
    public static synchronized int startTimer(byte[] startBuf) {
        token++;
        startBuf[0] = (byte) TIMER_START;
        startBuf[1] = (byte) ((token >> 24) & 0xff);
        startBuf[2] = (byte) ((token >> 16) & 0xff);
        startBuf[3] = (byte) ((token >> 8) & 0xff);
        startBuf[4] = (byte) (token & 0xff);
        try{
            packet.setData(startBuf);
            socket.send(packet);
        } catch (IOException e) {
            Debug.printStackTrace(e);
        }
        return token;
    }

    /**
     * Signals stopping the timer on the remote computer.
     *
     * @param token Token for the task that is done.
     */
    public synchronized static void stopTimer(int tk) {
        stopBuf[0] = (byte) TIMER_STOP;
        stopBuf[1] = (byte) ((tk >> 24) & 0xff);
        stopBuf[2] = (byte) ((tk >> 16) & 0xff);
        stopBuf[3] = (byte) ((tk >> 8) & 0xff);
        stopBuf[4] = (byte) (tk & 0xff);
        try{
            packet.setData(stopBuf);
            socket.send(packet);
        } catch (IOException e) {
            Debug.printStackTrace(e);
        }
    }
}

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
package net.java.bd.tools.logger;

import java.awt.Container;
import java.io.File;
import net.java.bd.tools.logger.Logger.Observer;
import org.bluray.ui.event.HRcEvent;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

/**
 * Provide static methods useful for general xlet logging need.
 */

public class XletLogger {
    
    private static boolean initialized = false;
    private static int screenToggleKey = HRcEvent.VK_COLORED_KEY_0;
    private static BDJUserEventListener listener;
    
    /**
     * Logs the string passed in.  The log goes to System.out and to the
     * XletLogDialog screen by default, and optionally to a file in the 
     * filesystem specified by the setLogFile(String) call.
     * 
     * @param s The string to log.
     */
    public static void log(String s) {
        checkInitialized();
        Logger.log(s);
    }
    
    /**
     * Logs the string and the stacktrace of the throwable passed in.
     * 
     * @see #log(String)
     */
    public static void log(String s, Throwable t) {
        checkInitialized();
        Logger.log(s, t);
    }
    
    /**
     * Sets the root xlet container.  The container passed in will be
     * added to the HScene obtained by calling 
     * HSceneFactory.getInstance().getDefaultHScene() and resized to be
     * 1920x1080.  One can switch the display between this xlet container
     * and the logging screen by sending the key event specified by calling 
     * #setToggleKey(int).
     * 
     * @param gui the root xlet container to be added to the default HScene 
     * instance.
     */
    public static void setXletContainer(Container gui) {
        checkInitialized();  
        Screen.setCurrentComponent(gui);
        Screen.setShowLogMode(false);  
    }
    
    /**
     * Sets the HRcEvent type used to toggle between the xlet screen 
     * and the logging screen.  If this method is not called, the default
     * key for togging is HRcEvent.VK_COLORED_KEY_0.
     * 
     * The method has no effect if xlet container is not set.
     * 
     * @param type the HRcEvent type used to toggle the screen display.
     */
    public static void setToggleKey(int type) {
        screenToggleKey = type;
    }
    
    /**
     * Sets the log file in the filesystem to store the logging info.
     * For example, it can be the file in application data area or 
     * binding unit data area.
     * 
     * @param logfile the absolute path of the file to store log info to.
     */
    public static void setLogFile(String logfile) {
        checkInitialized();
        Logger.setLogFile(logfile);
    }
    
    /**
     * Returns the File instance used to store logging info.
     * 
     * @return File the logfile for the current xlet session, or null
     * if none is set.
     */
    public static File getLogFile() { 
        checkInitialized();
        return Logger.getLogFiles()[0];
    }
    
    /**
     * Calls HScene.setVisible(boolean) to the default HScene
     * to show/hide the components on the screen. 
     */
    public static void setVisible(boolean b) {
        checkInitialized();
   
        // Add a keylistener to toggle between log screen and xlet screen
        // if the xlet screen is not null
        if (Screen.getCurrentComponent() != null) {
            synchronized(XletLogger.class) {
                if (listener == null) {
                    listener = new BDJUserEventListener();
                    Logger.log("Setting the screen toggle key to " + screenToggleKey);
                    listener.setListener(screenToggleKey);
                }
            }
        }
        
        Screen.setVisible(b);
    }
    
    private static void checkInitialized() {
        synchronized(XletLogger.class) {
            if (!initialized) {
                initialize();
                initialized = true;
            }
        }
    }
    
    private static void initialize() {
        HScene scene = HSceneFactory.getInstance().getDefaultHScene(); 
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
 
    	// initiate LogDialog component to display log on the screen
        Screen.setRootContainer(scene);  
	XletLogDialog logDialog = new XletLogDialog();
        logDialog.compose();
        Logger.addObserver(logDialog);
        Logger.addObserver(new SystemOutLogObserver());
        
        Screen.setLogComponent(logDialog);
        Screen.setShowLogMode(true);         
    }
    
    static class SystemOutLogObserver implements Observer {

        public void output(String s) {
            System.out.println(s);
        }

        public void clearLog() {
            // Nothing to do
        }
    }
}

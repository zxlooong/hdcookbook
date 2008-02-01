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
 * 
 */

package storage;

import java.io.*;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.BorderLayout;

import org.dvb.ui.FontFactory;
import org.havi.ui.HSceneFactory;

/**
 * A test xlet that accesses persistant storage.
 * Need to be signed to work.
 */
public class WriteToStorageXlet extends Container implements Xlet {
	
    Container rootContainer = null;
    String status = "Not yet run";
    XletContext context;
    Font font;
    
    static final int WINDOW_WIDTH = 1920;
    static final int WINDOW_HEIGHT = 1080;

    public void initXlet(XletContext context) {       
	this.context = context;
	
        rootContainer = HSceneFactory.getInstance().getDefaultHScene();
        rootContainer.add(this, BorderLayout.CENTER);
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        rootContainer.validate();
        rootContainer.setVisible(true);
    }
    
    public void startXlet() {	    
	setVisible(true);
        accessPersistantStorage();
	repaint();
    }
    
    public void pauseXlet() {
	setVisible(false);   
    }
    public void destroyXlet(boolean unconditional) {
	    rootContainer.remove(this);
    }
    
    public void paint(Graphics g) {

	if (font == null) {
	   try {
              font = new FontFactory().createFont("Arial", Font.BOLD, 64);
	   } catch (Exception e) {
	      e.printStackTrace();
	      font = g.getFont();
      	   }
	}
	g.setFont(font);
	g.setColor(new Color(100,100,10));
        g.fillRect(20,20,getWidth()-40,getHeight()-40);
	g.setColor(new Color(245,245,0));   
    	int message_width = g.getFontMetrics().stringWidth(status);
    	g.drawString(status, (WINDOW_WIDTH-message_width)/2, 500);
	
    }
    
    public void accessPersistantStorage() {
        String root = System.getProperty("dvb.persistent.root");
	String filename = System.getProperty("dvb.persistent.root")
	       + "/" + context.getXletProperty("dvb.org.id")
	       + "/" + context.getXletProperty("dvb.app.id")
	       + "/tmp.txt";
	
	System.out.println("FileName = " + filename);
	FileOutputStream os = null;
	try {
          /* BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
           String writable = "Hello BD-J!";
           bw.write(writable, 0, writable.length());
           bw.close(); */
           os = new FileOutputStream(filename);
	   for (int i = 0; i < 10; i++) {
		os.write(i);
           }
	   os.close();
           status = "WRITER Test passed, accessed filesystem without SecurityException";
	   System.out.println("Test passed, accessed filesystem without SecurityException");
	} catch (SecurityException ex) {
		ex.printStackTrace();
		System.out.println();
		System.out.println("***  No permission to write to the persistant storage ***");
		System.out.println();
		status = "Test Failed, SecurityException";	    
	} catch (IOException ex) {
		ex.printStackTrace();  
		status = "Test Failed with IOException";
        } finally {
	    if (os != null) {
		try {
		    os.close();
		} catch (Throwable ignored) {
		}
	    }
	}
    }
    
    public static void main(String[] args) {
	    // just to fool netbeans...
    }
}

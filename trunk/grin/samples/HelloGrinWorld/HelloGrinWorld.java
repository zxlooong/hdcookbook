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

import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.parser.ShowParser;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import javax.tv.graphics.TVContainer;

import java.awt.Container;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.parser.ExtensionsParser;
	
/** 
 * An xlet example that displays GRIN script helloworld.txt
 * content at the startup.
 */

public class HelloGrinWorld implements Xlet, Runnable {
	
	public Show show;
	Container rootContainer;
	String grinScriptName = "helloworld.txt";
	boolean running = false;
	
	public void initXlet(XletContext context) {
		
	   rootContainer = TVContainer.getRootContainer(context);			
	   rootContainer.setSize(1920, 1080);
	   
	   SimpleDirector director = new SimpleDirector();
	   show = director.createShow();
	   show.initialize(rootContainer);
	}
	
	public void startXlet() {
	   rootContainer.setVisible(true);	
	   show.activateSegment(show.getSegment("S:Initialize"));
	   running = true;
	   
	   new Thread(this).start();
	}
	
	public void pauseXlet() {
	   running = false;
	   rootContainer.setVisible(false);	
	}
	
	public void destroyXlet(boolean unconditional) {
	   running = false;
	   rootContainer = null;
	   show.destroy();
	}
	
	// The actual display sequence.
	public void run() {
			   
	   //int i = 0;
	   try {
	      show.advanceToFrame(1);
	   } catch (InterruptedException e) { return; }
	   
	   while (running) {
	      try {
		 Thread.currentThread().sleep(1000);
		 
	         //show.advanceToFrame(i++);
	         show.paintFrame((Graphics2D)rootContainer.getGraphics());
	         Toolkit.getDefaultToolkit().sync();
	      } catch (InterruptedException e) { 
		 e.printStackTrace(); 
		 running = false;
	      }
	   }
	}
	
	class SimpleDirector extends Director {
		
	   public ExtensionsParser getExtensionsParser() {
		     return null;
	   }
	   
	   public Show createShow() {
	      
	      setup(0, new ChapterManager[]{new ChapterManager("")});
	      
	      Show show = new Show(this);
	      String showName = grinScriptName;
	      try {
		 AssetFinder.setSearchPath(new String[]{""}, null);
	         URL u = AssetFinder.getURL(showName);
	         if (u == null) {
		    throw new IOException("Can't find " + showName + " in assets");
	         }
	         BufferedReader rdr = new BufferedReader(
	         new InputStreamReader(u.openStream(), "UTF-8"));
	         ShowParser p = new ShowParser(rdr, showName, show);
	         p.parse();
	         rdr.close();
	      } catch (IOException ex) {
		ex.printStackTrace();
		AssetFinder.abort();
	      }
	      
	      return show;
	}
     }
}

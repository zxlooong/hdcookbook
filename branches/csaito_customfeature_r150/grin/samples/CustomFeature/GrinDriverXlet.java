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

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;
import java.awt.Graphics2D;
import java.awt.Rectangle;
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
import com.hdcookbook.grin.animator.AnimationClient; 
import com.hdcookbook.grin.animator.AnimationEngine;
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.animator.DirectDrawEngine;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.io.ExtensionsBuilder;
import com.hdcookbook.grin.util.AssetFinder;
import java.awt.Color;
	
/** 
 * An xlet example that displays GRIN script.
 */

public class GrinDriverXlet implements Xlet, AnimationContext {
	
	public Show show;
	Container rootContainer;
	DirectDrawEngine animationEngine;
	XletContext context;
	String grinScriptName = "custom-feature-example.grin";
	
	public void initXlet(XletContext context) {
		
	    this.context = context;
	   
	    rootContainer = TVContainer.getRootContainer(context);			
	    rootContainer.setSize(1920, 1080);
	   
	    animationEngine = new DirectDrawEngine();
	    animationEngine.setFps(24000);
	    animationEngine.initialize(this);
	   
	}
	
	public void startXlet() {
	    rootContainer.setVisible(true);
	    animationEngine.start(); 	   
	}
	
	public void pauseXlet() {
	    rootContainer.setVisible(false);
	    animationEngine.pause();
	}
	
	public void destroyXlet(boolean unconditional) {
	    rootContainer = null;
	    animationEngine.destroy();
	}
	
	public void animationInitialize() throws InterruptedException {

	    SimpleDirector director = new SimpleDirector();
           
            try {
               
                AssetFinder.setSearchPath(new String[]{""}, null);      
	        GrinBinaryReader reader = new GrinBinaryReader(director, AssetFinder.getURL(grinScriptName).openStream());
                show = new Show(director);
	        reader.readShow(show);
               
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Error in reading the show file");
                throw new InterruptedException();
            }
           
	    animationEngine.checkDestroy();
	    animationEngine.initClients(new AnimationClient[]{show});
	    animationEngine.initContainer(rootContainer, new Rectangle(0,0,1920,1080));
	   
	} 
	
	public void animationFinishInitialization() {
	    show.activateSegment(show.getSegment("S:Initialize"));		
	}
	
	class SimpleDirector extends Director {
		
	   public ExtensionsBuilder getExtensionsBuilder() {
               return new SimpleExtensionsBuilder();
	   }
        }
        
        class SimpleExtensionsBuilder implements ExtensionsBuilder {

            public Feature getFeature(Show show, String typeName, String name, String arg) throws IOException {
                if ("EXAMPLE:oval".equals(typeName)) {
                    return new Oval(show, name, 10, 10, 100, 100, Color.LIGHT_GRAY);
                }
                return null;
            }

            public Modifier getModifier(Show show, String typeName, String name, String arg) throws IOException { 
                return null; 
            }

            public Command getCommand(Show show, String typeName, String[] args) throws IOException {
                return null;
            } 

            public void finishBuilding(Show s) throws IOException {
            }

            public void takeMosaicHint(String name, int width, int height, String[] images) { 
            }   
        }
}

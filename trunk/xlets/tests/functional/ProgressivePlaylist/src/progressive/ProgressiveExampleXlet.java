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

package progressive;

import javax.tv.service.selection.ServiceContextException;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.EndOfMediaEvent;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.tv.service.selection.ServiceContextFactory;
import org.bluray.net.BDLocator;
import org.bluray.system.RegisterAccess;
import org.bluray.ti.DiscManager;
import org.bluray.ti.Title;
import org.bluray.ti.selection.TitleContext;
import org.bluray.vfs.PreparingFailedException;
import org.bluray.vfs.VFSManager;
import org.davic.media.MediaLocator;
import org.davic.net.InvalidLocatorException;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

/**
 * An example of a progressive playlist.
 * 
 * This is a simple example where all the data for progressive playlist is
 * provided as a jar file in the optical disc structure.  The xlet unzips
 * the jar content to it's binding unit data area, performs the VFS update,
 * enables the clip that's listed as a progressive playlist asset in the
 * binding unit manifest file used for the VFS update, and then finally
 * plays back the video clip in the progressive playlist.
 * 
 * In real use case, most likely the video clip is downloaded to the player.
 */
public class ProgressiveExampleXlet implements Xlet, Runnable {

    private HScene scene;
    private Container gui;
    private static String message = "Progressive Playlist Example";
    private String bindingUnitDir = null;
    private XletContext context;
    private char sep = File.separatorChar;

    public void initXlet(XletContext context) {

        this.context = context;
        final Font font = new Font(null, Font.PLAIN, 48);
        scene = HSceneFactory.getInstance().getDefaultHScene();
        scene.setBackgroundMode(HScene.BACKGROUND_FILL);
        gui = new Container() {
            public void paint(Graphics g) {
                g.setFont(font);
                g.setColor(new Color(245, 245, 245));
                int message_width = g.getFontMetrics().stringWidth(message);
                g.drawString(message, (getWidth() - message_width) / 2, 500);
            }
        };

        gui.setSize(1920, 1080);  // BD screen size
        scene.add(gui, BorderLayout.CENTER);
        scene.validate();

        String root = System.getProperty("bluray.bindingunit.root");
        String orgID = (String) context.getXletProperty("dvb.org.id");
        String discID = DiscManager.getDiscManager().getCurrentDisc().getId();
        bindingUnitDir = root + File.separator + orgID + File.separator + discID;        
    }

    public void startXlet() {
        gui.setVisible(true);
        scene.setVisible(true);
        new Thread(this).start();  // start the test.
    }

    public void pauseXlet() {
        gui.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) {
        scene.remove(gui);
        scene = null;
    }
    
    /**
     * Starts the test.
     */
    public void run() {
        // Use the General Purpose Register to flag whether to do the 
        // VFS update or the clip playback, since the VFS update
        // restarts this xlet.
        RegisterAccess register = RegisterAccess.getInstance();
        if (register.getGPR(0) == 0) {
            try {
               prepareForVFSUpdate();
               register.setGPR(0, 1);
               doVFSUpdate();
            } catch (Exception e) {
                setMessage("Exception in VFS update: " + e.toString());
                e.printStackTrace();
                register.setGPR(0,0);
            }
        } else {
            try {
               startProgressivePlaylist(); 
            } catch (Exception e) {
               setMessage("Exception in playback: " + e.toString());
               e.printStackTrace();                
            }
            register.setGPR(0,0);
        }  
    }

    /**
     * Unzips the files that are needed for the VFS update to 
     * the binding unit data area.
     * The files are - bumf.xml, bumf.sf, and CLIPINF, PLAYLIST, and STREAM
     * directories where each dir contains just one file.
     * The m2ts file under the STREAM dir is listed as a progressvie playlist.
     */
    void prepareForVFSUpdate() throws IOException {
        setMessage("Preparing for the VFS update.");
        
        // Simply unzip the content of "vfs.jar" to the binding unit data area.
        // In real use case, these files are likely to be downloaded to the player.
        String root = System.getProperty("bluray.vfs.root");        
        File file = new File(root + sep + "BDMV" + sep + "JAR" + sep + "vfs.jar");
        ZipInputStream zin = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(file)));
        ZipEntry e;
        while ((e = zin.getNextEntry()) != null) {
            unzip(zin, e, bindingUnitDir);    
        }
        zin.close();
    }
    
    /**
     * Unzip a specific ZipEntry from the InputStream to the output directory.
     */
    private void unzip(InputStream zin, ZipEntry e, String dir)
            throws IOException {

        if (e.isDirectory()) 
            return;
 
        String s = e.getName();       
        File file = new File(dir, s);
        if (!file.exists()) {
           file.getParentFile().mkdirs();
        }
  
        byte[] b = new byte[1024];
        int len = 0;              
        FileOutputStream out = new FileOutputStream(file);
        
        while ((len = zin.read(b)) != -1) {
            if (out != null) {              
               out.write(b, 0, len);
            }
        }
        
        out.flush();
        out.close();
    }   
    
    /**
     * Performs a VFS update.
     */
    void doVFSUpdate() throws IOException, PreparingFailedException, 
            SecurityException, ServiceContextException {
        setMessage("Restarting the title.");
        
        VFSManager.getInstance().requestUpdating(
                    bindingUnitDir + sep + "sample.xml",
                    bindingUnitDir + sep + "sample.sf",
                    true);       

        // restart this title.
        TitleContext titleContext = 
                (TitleContext) ServiceContextFactory.getInstance().getServiceContext(context);
        Title title = (Title) titleContext.getService();
        titleContext.start(title, true); // this will restart this xlet.           
    }
    
    /**
     * Enable the progressive playlist clip and play it back.
     * Since this xlet unzips the m2ts file to the buda before performing VFS update,
     * the method can safely assume that the m2ts file is already in the buda.
     */
    void startProgressivePlaylist() throws InvalidLocatorException, IOException, NoPlayerException {    
        setMessage("Playing back the progressive playlist.");

        // First, enable the progressive playlist asset.
        VFSManager manager = VFSManager.getInstance();
        if (!manager.isEnabledClip("00001")) {
            manager.enableClip(bindingUnitDir + sep + "BDMV" + sep + "STREAM" + sep + "00001.m2ts");
        }
        // Then, playback the video.  Note that playlist 00000 points to clipinf and stream 00001.
        BDLocator bdLocator = new BDLocator("bd://0.PLAYLIST:00000");
        MediaLocator locator = new MediaLocator(bdLocator);
        final Player thePlayer = Manager.createPlayer(locator);
        // Let's loop the video, since it's short.
        thePlayer.addControllerListener(new ControllerListener() {
            public void controllerUpdate(ControllerEvent e) {
                if (e instanceof EndOfMediaEvent) {
                    thePlayer.start();
                }
            }
        });
        thePlayer.start();
    }

    /**
     * Display the message on the screen.
     */
    private void setMessage(String s) {
        message = s;
        if (scene != null)
           scene.repaint();
    }
       
}

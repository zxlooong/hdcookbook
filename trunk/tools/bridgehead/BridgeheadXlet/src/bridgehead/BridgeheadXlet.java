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

package bridgehead;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.tv.service.selection.ServiceContextFactory;
import javax.tv.service.selection.ServiceContextException;
import javax.tv.xlet.XletContext;
import org.bluray.ti.DiscManager;
import org.bluray.ti.Title;
import org.bluray.ti.selection.TitleContext;
import org.bluray.vfs.VFSManager;
import org.bluray.vfs.PreparingFailedException;

import net.java.bd.tools.logger.XletLogger;

/**
 * A bootstrap xlet that opens up a ServerSocket,
 * downloads a zipfile containing a new BDMV structure,
 * and performs VFS update with the new disc image.
 */

public class BridgeheadXlet implements javax.tv.xlet.Xlet, Runnable {

    public static final int PORT = 4444;
    
    private XletContext  context;
    private String       bindingUnitDir;    
    private Thread       thread;
    private ServerSocket ssocket;
    
    public void initXlet(XletContext context) {
        this.context = context;
        
        String root = System.getProperty("bluray.bindingunit.root");
        String orgID = (String) context.getXletProperty("dvb.org.id");
        String appID = (String) context.getXletProperty("dvb.app.id");
        String discID = DiscManager.getDiscManager().getCurrentDisc().getId();

        bindingUnitDir = root + "/" + orgID + "/" + discID;
        
        String ada = System.getProperty("dvb.persistent.root")
	       + "/" + orgID + "/" + appID;
      
        //Set the logging output file
        XletLogger.setLogFile(ada + "/" + "log.txt");
        XletLogger.log("BindingRoot: " + bindingUnitDir);
    } 
    
    public void startXlet() {
        
        XletLogger.log("Starting the xlet...");        
        XletLogger.setVisible(true); 
        
        // If the player doesn't support VFS, stop.
        if (!isPlayerCompatible()) {
            return;
        }
        
        thread = new Thread(this);
        thread.start();
    }
    
    public void run() {        
        
        try {
            
            XletLogger.log("Waiting for the client connect.");
            doDownload(bindingUnitDir);
          
            XletLogger.log("Calling VFS update");
            doVFSUpdate(bindingUnitDir + "/" + "sample.xml", 
                        bindingUnitDir + "/" + "sample.sf");
            
            XletLogger.log("Restarting the current title");
            doTitleRestart();
            
        } catch (IOException ie) {
            XletLogger.log("", ie); 
        } catch (PreparingFailedException pfe) {
            XletLogger.log("", pfe);
        } catch (ServiceContextException sce) {
            XletLogger.log("", sce);
        } 
    }
    
    public void pauseXlet() {
        XletLogger.setVisible(false);
    }
    
    public void destroyXlet(boolean unconditional) {
        if (thread != null) {
            thread.interrupt(); 
            thread = null;
        }
        if (ssocket != null) {
            try {
              ssocket.close();  
            } catch (IOException e) {             
            }
        }
    }

    // Check that this player is supporting VFS.
    private boolean isPlayerCompatible() {
        String lsLevel = System.getProperty("bluray.localstorage.level");
        if (lsLevel.equals("-1")) {
            XletLogger.log("VFS is not supported, bluray.localstorage.level=" + lsLevel);
            return false;
        } else if (lsLevel.equals("0")) {
            XletLogger.log("No storage device, bluray.localstorage.level=" + lsLevel);
            return false;
        }
        // Check that this player supports network access.
        if (!"YES".equals(System.getProperty("bluray.profile.2"))) {
            XletLogger.log("Not a profile 2 player");
            return false;
        } 
        
        return true;
    }
    
    public String getHostIP() {
        try {
           return InetAddress.getLocalHost().getHostAddress(); 
        } catch (UnknownHostException e) { 
            XletLogger.log("", e);
            return null;
        }
    }
    
    public void doDownload(String downloadDir) throws IOException {

        ssocket = new ServerSocket(PORT);

        XletLogger.log("*** Host IP is " + getHostIP() + ", listening on port " + PORT);

        Socket clientSocket = ssocket.accept();
        XletLogger.log("Accepted connection, start downloading");

        ZipInputStream zin = new ZipInputStream(
                new BufferedInputStream(clientSocket.getInputStream()));

        ZipEntry e;

        while ((e = zin.getNextEntry()) != null) {
            XletLogger.log(e.getName());
            unzip(zin, e, downloadDir);
        }
        zin.close();

        clientSocket.close();
        ssocket.close();

    }

    private void unzip(InputStream zin, ZipEntry e, String dir)
            throws IOException {

        if (e.isDirectory()) 
            return;
 
        String s = e.getName();       
        File file = new File(dir, s);
        if (!file.exists()) {
           file.getParentFile().mkdirs();
        }
  
        byte[] b = new byte[512];
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
    
    public void doVFSUpdate(String xmlFile, String sigFile) 
            throws PreparingFailedException {
     
        VFSManager.getInstance().requestUpdating(xmlFile, sigFile, true);           
    }
    
    public void doTitleRestart() 
            throws ServiceContextException {
        
        try {
            ServiceContextFactory factory = ServiceContextFactory.getInstance(); 
            TitleContext titleContext =(TitleContext) factory.getServiceContext(context);
            Title title = (Title) titleContext.getService();
            titleContext.start(title, true);        
        } catch (SecurityException ex) {
            XletLogger.log("Can't get TitleContext", ex);
            return;
        } 
    }
}

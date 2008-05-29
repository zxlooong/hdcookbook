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

package bumfgenerator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import net.java.bd.tools.bumf.AssetType;
import net.java.bd.tools.bumf.AssetsType;
import net.java.bd.tools.bumf.FileType;
import net.java.bd.tools.bumf.Manifest;
import net.java.bd.tools.bumf.ObjectFactory;
import net.java.bd.tools.bumf.NamespacePrefixMapperImpl;
import net.java.bd.tools.id.IdReader;
import net.java.bd.tools.id.Id;

/**
 * This tool generates a binding unit manifest file which can be used with
 * the bridgehead xlet.
 */

public class BumfGenerator {
    
   public static void main(String[] args) throws Exception {
       if (args.length < 3) {
           usage();
       }
       
       String idFile = args[0];
       String input  = args[1];
       String output = args[2];
       
       if ("id.bdmv".equalsIgnoreCase(idFile) || !new File(idFile).exists()) {
           System.out.println("id.bdmv file not found, " + idFile);
           usage();
       }
       
       if (!new File(input).exists()) {
           System.out.println("Input needs to be an existing BDMV directory " + input);
           usage();
       }
       
       BumfGenerator budagen = new BumfGenerator();
       Manifest manifest = budagen.constructManifest(idFile, input);
       budagen.writeXml(manifest, output);
    
   }
   
   public static void usage() {
       System.out.println("\n\nUsage:\n");
       System.out.println("" + BumfGenerator.class.getName() + " id.bdmv bdmv-dir output-xml-file");
       System.exit(0);
   }

   private Manifest constructManifest(String idFile, String input) 
       throws Exception {   
       
        Manifest m = new Manifest();
        
        FileInputStream fin = new FileInputStream(idFile);
        DataInputStream din = new DataInputStream(new BufferedInputStream(fin)); 
        Id idObject = new IdReader().readId(din);  
        String discId = new BigInteger(idObject.getDiscId()).toString(16);
        String orgId  = Integer.toHexString(idObject.getOrgId());
        m.setDiscID("0x" + discId);
        m.setOrgID("0x"+ orgId);
    
        int index = input.indexOf("BDMV");      
        File[] fs = new File[]{ new File(input) };
        ArrayList<File> list = new ArrayList<File>();
        findFiles(fs, list);
        ObjectFactory factory = new ObjectFactory();
        AssetsType assetsType = factory.createAssetsType();
        for(int i = 0; i < list.size(); i++) {
            String filename = list.get(i).getPath().substring(index).replace('\\', '/');
            String buFilename = orgId + '/' + discId + '/' + filename;
            AssetType assetType = factory.createAssetType();
            FileType fileType  = factory.createFileType();
            assetType.setVPFilename(filename);
            fileType.setName(buFilename);
            assetType.setBUDAFile(fileType);
            assetsType.getAsset().add(assetType);
        } 
        
        m.setAssets(assetsType);
        
        return m;     
   }
   
   private void findFiles(File[] fs, ArrayList<File> v) {
        for (File f: fs) {
           if (!f.isDirectory()) {
              v.add(f);
           } else {
              findFiles(f.listFiles(), v);
           }
        }
   }    

   private void writeXml(Manifest manifest, String output) throws Exception {       
       JAXBContext jc = JAXBContext.newInstance("net.java.bd.tools.bumf");
       Marshaller m = jc.createMarshaller();
       m.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", 
                     new NamespacePrefixMapperImpl());
       m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
       OutputStream os = new BufferedOutputStream(new FileOutputStream(output));
       m.marshal( manifest, os );
       os.flush();
       os.close();
   }
}

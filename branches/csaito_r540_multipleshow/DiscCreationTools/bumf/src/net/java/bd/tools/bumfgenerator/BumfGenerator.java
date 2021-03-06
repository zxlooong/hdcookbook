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

package net.java.bd.tools.bumfgenerator;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
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
import net.java.bd.tools.bumf.ProgressiveAssetType;
import net.java.bd.tools.bumf.ProgressiveType;
import net.java.bd.tools.id.IdReader;
import net.java.bd.tools.id.Id;

/**
 * This tool generates a binding unit manifest file (xml-based file)
 * that can be used for a VFS update.
 */

public class BumfGenerator {
    
   private static final String MANIFEST_ID = "0x00000001";

   private int nextFile = 1;
   private byte[] buffer = new byte[16384];

   public static class Entry {
       public File src;
       public File dest;
   }
    
   public static void main(String[] args) {
       if (args.length < 3) {
           usage();
       }
       
       int index = 0;
       String idFile = args[index++];
       String input  = args[index++];
       String[] progressives = null;
       if ("-progressive".equals(args[index])) {
           index++;
           String progressiveList = args[index++];
           progressives = progressiveList.split(",");
           for (int i = 0; i < progressives.length; i++) {
               progressives[i] = progressives[i] + ".m2ts";
           }
       }
       String output = args[index];
       
       if ("id.bdmv".equalsIgnoreCase(idFile) || !new File(idFile).exists()) {
           System.out.println("id.bdmv file not found, " + idFile);
           usage();
       }
       
       File inputDir = new File(input);
       File outputDir = new File(output);
       if (!inputDir.exists() || input.indexOf("BDMV") == -1) {
           System.out.println("Input directory needs to be an existing BDMV directory " + input);
           usage();
       }
       if (outputDir.exists()) {
	   System.out.println("ERROR:  Output directory already exists - please remove it.");
	   usage();
       }

       try {
	   BumfGenerator budagen = new BumfGenerator();
	   Manifest manifest 
		= budagen.constructManifest(idFile, input, outputDir, progressives);
	   budagen.writeXml(manifest, output+"/manifest.xml");
       } catch (Exception ex) {
	   ex.printStackTrace();
	   System.exit(1);
       }
       System.exit(0);
   }
   
    public static void usage() {
	System.out.println();
	System.out.println("Usage:");
	System.out.println();
	System.out.println( BumfGenerator.class.getName() + " id.bdmv bdmv-directory [-progressive comma-separated-m2ts-numbers] output-directory");
	System.out.println();
	System.out.println("\t-progressive is optional; use it to specify m2ts files that should be listed as progressive assets.");
	System.out.println();
	System.out.println("\tExample: " + BumfGenerator.class.getName() + " ../../BDImage/CERTIFICATE/id.bdmv ../../BDImage/BDMV -progressive 00001,00002 vfs_upload");
	System.out.println();
	System.out.println("        The output directory will be created.  It is an error if it exists already.");
	System.exit(1);
    }

   private Manifest constructManifest(String idFile, String bdmvDir, File outputDir,
   				      String[] progressives) throws Exception 
   {   
       
        Manifest m = new Manifest();
        
        FileInputStream fin = new FileInputStream(idFile);
        DataInputStream din = new DataInputStream(new BufferedInputStream(fin)); 
        Id idObject = new IdReader().readId(din);  
        
        /**
         * Note: 
         * The discID and the orgID entries in the bumf.xml's manifest element need "0x" prefix followed by 
         * 32 chars and 8 chars in hex, whereas the discID and the orgID used in the buda directory path
         * are expected to have no leading zeros.  Ex. for disc ID "0", the manifest should use 
         * discID "0x00000000000000000000000000000000", and the buda directory path should use discID "0".
         */
        BigInteger bi = new BigInteger(idObject.getDiscId());
        String discId     = bi.toString(16); 
        String fullDiscId = String.format("%032x", bi);
        String orgId      = String.format("%x", idObject.getOrgId());  // same as Integer.toHexString(int)
        String fullOrgId  = String.format("%08x", idObject.getOrgId());

        m.setID(MANIFEST_ID);
        m.setDiscID("0x" + fullDiscId); 
        m.setOrgID("0x" + fullOrgId);
    
        int index = bdmvDir.indexOf("BDMV");      
        File[] fs = new File[]{ new File(bdmvDir) };
        ArrayList<Entry> list = new ArrayList<Entry>();
        findFiles(fs, list, outputDir);
        ObjectFactory factory = new ObjectFactory();
        AssetsType assetsType = factory.createAssetsType();
        ProgressiveType progressiveType = factory.createProgressiveType();    
        
        for(int i = 0; i < list.size(); i++) {
	    Entry ent = list.get(i);
            String filename = ent.src.getPath().substring(index).replace('\\', '/');
            String buFilename = orgId + '/' + discId + '/' + ent.dest.getName();
            
            if (isProgressive(filename, progressives)) {
                addToProgressiveAssets(factory, progressiveType, filename, buFilename);
            } else {
                addToAssets(factory, assetsType, filename, buFilename);
            }            
        } 

        assetsType.setProgressive(progressiveType);        
        m.setAssets(assetsType);
        
        return m;     
   }
   
   private boolean isProgressive(String filename, String[] progressiveItems) {
       if (progressiveItems == null) {
           return false;
       }
       
       for (int i = 0; i < progressiveItems.length; i++) {
           if (filename.endsWith(progressiveItems[i])) {
               return true;
           } 
       }
       return false;
   }
   
   private void findFiles(File[] fs, ArrayList<Entry> v, File outputDir)
		throws IOException 
    {
        for (File f: fs) {
           if (!f.isDirectory()) {
	      File out = copyFile(f, outputDir);
	      Entry e = new Entry();
	      e.src = f;
	      e.dest = out;
              v.add(e);
           } else {
              findFiles(f.listFiles(), v, outputDir);
           }
        }
   }

    //
    // Returns the output file name
    //
    private File copyFile(File in, File outputDir) throws IOException {
	FileInputStream is = new FileInputStream(in);
	if (!outputDir.exists()) {
	    boolean ok = outputDir.mkdirs();
	    if (ok) {
		System.out.println("Created output directory " + outputDir);
	    } else {
		throw new IOException("Couldn't create " + outputDir);
	    }
	}
	File out;
	String inName = in.getName();
	String outName;
	if (inName.endsWith(".m2ts")) {
	    outName = inName.substring(0, inName.length()-4) + "m2t";
	} else if (inName.endsWith(".clpi")) {
	    outName = inName.substring(0, inName.length()-4) + "clp";
	} else if (inName.endsWith(".mpls")) {
	    outName = inName.substring(0, inName.length()-4) + "mpl";
	} else if (inName.equals("index.bdmv")) {
	    outName = inName.substring(0, inName.length()-4) + "bdm";
	} else if (inName.endsWith(".jar") && inName.length() == 9) {
	    outName = inName;
	} else if (inName.endsWith(".bdjo") && inName.length() == 10) {
	    outName = inName.substring(0, inName.length()-4) + "bdj";
	} else {
	    outName = "" + nextFile + ".vfs";
	    nextFile++;
	}
	out = new File(outputDir, outName);
	FileOutputStream os = new FileOutputStream(out);
	for (;;) {
	    int len = is.read(buffer);
	    if (len == -1) {
		break;
	    }
	    os.write(buffer, 0, len);
	}
	os.close();
	is.close();
	return out;
    }
   
   private void addToProgressiveAssets(ObjectFactory factory, ProgressiveType progressiveType, 
        String filename, String buFilename) {
        ProgressiveAssetType progressiveAssetType = factory.createProgressiveAssetType();  
        FileType fileType = factory.createFileType();
        
        fileType.setName(buFilename);

        progressiveAssetType.setBUDAFile(fileType);
        progressiveAssetType.setVPFilename(filename);
        progressiveType.getProgressiveAsset().add(progressiveAssetType);  
   }
   
   private void addToAssets(ObjectFactory factory, AssetsType assetsType, 
        String filename, String buFilename) {
        AssetType assetType = factory.createAssetType();
        FileType fileType = factory.createFileType();

        assetType.setVPFilename(filename);
        fileType.setName(buFilename);
        assetType.setBUDAFile(fileType);
        assetsType.getAsset().add(assetType);
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

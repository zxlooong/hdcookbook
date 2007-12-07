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

package net.java.bd.tools.bdsigner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;
import org.bouncycastle.asn1.x509.BasicConstraints;
import sun.security.tools.KeyTool;
import sun.security.tools.JarSigner;
import sun.security.tools.Base64;


/*
 * BLU-RAY SPECIFIC:
 * 
 * Changes have been made to jarsigner tool to add BDJ specific
 * stuff. Whenever a spec. section is referred it is from
 * the "System Description Blu-Ray Disc Read-Only Format  
 * - Part 3 Audio Visual Basic Specifications" - DRAFT Version 2.02.
 */
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.x509.X509V3CertificateGenerator;


/**
 * A class that wraps around jarsigner and keytool to perform bd-j required signing in one shot.
 * 
 * 7 steps for signing are:
 * 1) Generate app certificate.
 * 2) Generate root certificate.
 * 3) Generate a certificate signing request (CSR) based on (1).
 * 4) Issue a certificate based on the CSR on (3), using the alias for root certificate from (2).
 * 5) Import back the certificate issued at (4) to the app certificate for (1).
 * 6) Sign the jar using the app certificate after (5).
 * 7) Export the root certificate as "app.discroot.crt". 
 * 
 * BJSigner syntax is : BDSigner [-debug] 8-digit-hex-organization-ID jar-files 
 * 
 * Example: java -cp $BDSIGNER_HOME/build/bdsigner.jar:$JDK_HOME/lib/tools.jar:$BDSIGNER_HOME/resource/bcprov-jdk15-137.jar net.java.bd.tools.bdsigner.BDSigner 56789abc 00000.jar 
 * 
 * Make sure to put bdsigner.jar before tools.jar in the jdk distribution for the jre 
 * classpath so that the modified version of the sun.security.* classes in this BDSigner respository
 * are used during the run.  bdprov-jdk15-137.jar is a bouncycastle distribution; a copy can be bound at "resources" dir.
 * 
 */
public class BDSigner {
 
    static final String APPCERTALIAS = "appcert";
    static final String ROOTCERTALIAS = "rootcert";
    
    // Intermediate files to create, will be deleted at the tool exit time
    static final String APPCSRFILE = "appcert.csr";
    static final String APPCERTFILE = "appcert.cer";
    static final String KEYSTOREFILE = "keystore.store";
    
    // Certificate data.
    static final String KEYSTOREPASSWORD = "keystorepassword";
    static final String APPKEYPASSWORD = "appcertpassword";
    static final String ROOTKEYPASSWORD = "rootcertpassword";
    static final String APP_SUBJECT_ALT_NAME = "def@producer.com";
    static final String ROOT_ISSUER_ALT_NAME = "abc@studio.com";
    static final String ROOT_SUBJECT_ALT_NAME = "def@studio.com";
    static final int APP_SERIAL_NUMBER = 1;
    static final int ROOT_SERIAL_NUMBER = 2;

    static String[] jarfiles;
    static String orgId;
    String appCertDN =  "CN=Producer, OU=Codesigning Department, O=BDJCompany."+orgId+", C=US";
    String rootCertDN = "CN=Studio, OU=Codesigning Department, O=BDJCompany."+orgId+", C=US";

    
    private KeyStore store;

    static boolean debug = false;
    
    
    public static void main(String[] args) {
	    
	    // Parse the argments
	    if (args == null || args.length < 2) {
		   printUsage("Wrong set of arguments.");
		   return;
	    } 

	    int index = 0;

	    if (args[index].startsWith("-")) {
		debug = ("-debug".equalsIgnoreCase(args[index++]));
		System.out.println ("Debugging : " + debug);
            }
	    
	    orgId = args[index++];
	    if (orgId.length()!= 8) {
		    printUsage("Bad OrgID " + orgId + ", please provide an 8 digit hex.");
		    return;
	    }
	    
	    jarfiles = new String[args.length-index];
	    for (int i = 0; index < args.length; i++) {
	       jarfiles[i] = args[index++];
	       if (!new File(jarfiles[i]).exists()) {
		    printUsage("File " + jarfiles[i] + " not found.");
		    return;
	       }
            }  
            
	    new BDSigner();
    }
    
    private BDSigner(){
        
            boolean failed = false;
            
	    cleanup();  // Get rid of any previous key aliases first.
	     
	    try {               
                initKeyStore();
		generateCertificates(); 
		generateCSR();
		generateCSRResponse();
		importCSRResponse();
		signJarFile();
		exportRootCertificate();
                
                if (debug) {
                    verifyCertificates();
                }
                
	    } catch (Exception e) {
	       e.printStackTrace();
               failed = true;
	    } finally {
	       if (!debug ) { 
	 	   cleanup();
               }    
	    }
            
            if (failed) {
                System.exit(1); // VM exit with an error code
            }
    }
  
    private void initKeyStore() throws Exception {
        
        Security.addProvider(new BouncyCastleProvider());	  
        
        char[] password = KEYSTOREPASSWORD.toCharArray();
        
        store = KeyStore.getInstance(KeyStore.getDefaultType());
        File kfile = new File(KEYSTOREFILE);
        if (!kfile.exists()) {
            store.load(null, password);
            FileOutputStream fout = new FileOutputStream(kfile);
            store.store(fout, password);
            fout.close();
        }
        
        URL url = new URL("file:" + kfile.getCanonicalPath());
        InputStream is = url.openStream();
        store.load(is, password);
        is.close();
    }
    
    private void generateCertificates() throws Exception {
	
        generateSelfSignedCertificate(rootCertDN, ROOTCERTALIAS, ROOTKEYPASSWORD, true);
        generateSelfSignedCertificate(appCertDN, APPCERTALIAS, APPKEYPASSWORD, false);
    }

    private void generateCSR() throws Exception {
       String[] appCSRRequestArgs = {"-certreq", "-alias", APPCERTALIAS, "-keypass", APPKEYPASSWORD, 
                                   "-keystore", KEYSTOREFILE, "-storepass", KEYSTOREPASSWORD, 
				   "-debug", "-file", APPCSRFILE};
       
       KeyTool.main(appCSRRequestArgs);	    
    }
    
    private void generateCSRResponse() throws Exception {
       
       String[] appCSRRequestArgs = {"-keystore", KEYSTOREFILE, "-storepass", KEYSTOREPASSWORD,
                                     "-keypass", ROOTKEYPASSWORD, "-debug", 
				     "-issuecert", APPCSRFILE, APPCERTFILE, ROOTCERTALIAS};
       
        
       JarSigner.main(appCSRRequestArgs);
       
    }
    
    private void importCSRResponse() throws Exception {
       String[] responseImportArgs = {"-import", "-alias", APPCERTALIAS, "-keypass", APPKEYPASSWORD, 
                                      "-keystore", KEYSTOREFILE, "-storepass", KEYSTOREPASSWORD,
                                      "-debug", "-file", APPCERTFILE}; 
       
       KeyTool.main(responseImportArgs);
    }
    
    private void signJarFile() throws Exception {
       for (int i = 0; i < jarfiles.length; i++) {
          String[] jarSigningArgs = {"-sigFile", "SIG-BD00", "-keypass", APPKEYPASSWORD, 
                                  "-keystore", KEYSTOREFILE, "-storepass", KEYSTOREPASSWORD,
                                  "-debug", jarfiles[i], APPCERTALIAS};
       
          JarSigner.main(jarSigningArgs);
       }
    }
    
    private void exportRootCertificate() throws Exception {
	String[] exportRootCertificateArgs = {
		"-export", "-alias", ROOTCERTALIAS, "-keypass", ROOTKEYPASSWORD, 
		"-keystore", KEYSTOREFILE, "-storepass", KEYSTOREPASSWORD,
		"-debug", "-file", "app.discroot.crt" };
	
	KeyTool.main(exportRootCertificateArgs);
    }
    
    private void verifyCertificates() {
        File appCert = new File(APPCERTFILE);
        File rootCert = new File("app.discroot.crt");
        
        boolean check = new CertificateVerifier().runTest(appCert, rootCert);
        if (!check) {
            throw new RuntimeException("Problem with the certification generation");
        }
    }
    private void cleanup() {
        
	 File keystore = new File(KEYSTOREFILE);
	
         if (keystore.exists()) {
    	    try {
		 KeyTool.main(new String[]{"-delete", "-alias", APPCERTALIAS, "-keystore", KEYSTOREFILE, "-storepass", KEYSTOREPASSWORD});
	    } catch (Exception e) { e.printStackTrace(); }
	    try {
		 KeyTool.main(new String[]{"-delete", "-alias", ROOTCERTALIAS, "-keystore", KEYSTOREFILE, "-storepass", KEYSTOREPASSWORD});
	    } catch (Exception e) { e.printStackTrace(); }
	    
	    keystore.delete();
	 }   
	 
	 new File(APPCSRFILE).delete();
	 new File(APPCERTFILE).delete();
    }
    
    
    private static void printUsage(String reason) {
	 System.out.println("\n==============================\n");
	 System.out.println("Failed: " + reason);
	 System.out.println("\n==============================\n");
	 System.out.println("This is a tool to sign jar files according to the bd-j specification.\n");
	 System.out.println("BDSigner Syntax:");
	 System.out.println("net.java.bd.tools.bdsigner.BDSigner [-debug] 8-digit-hex-organization-ID jar-files");
	 System.out.println("Example: java -cp bdsigner.jar:tools.jar:bcprov-jdk15-137.jar net.java.bd.tools.bdsigner.BDSigner 56789abc 00000.jar\n");
	 System.out.println("\n==============================\n");
    }
    
    private void generateSelfSignedCertificate(String issuer, String alias, String keyPassword, boolean isRootCert) throws Exception {
        
        Date validFrom, validTo;
        
        // For forcing GeneralizedTime DER encoding, 
        // make the range before 1950 and after 2050.
        Calendar calendar = Calendar.getInstance();
        calendar.set(1949, 1, 1);
        validFrom = calendar.getTime();
        calendar.clear();
        calendar.set(2055, 1, 1);
        validTo = calendar.getTime();

        // Generate a new keypair for this certificate
        KeyPair keyPair = generateKeyPair();
        
        X509V3CertificateGenerator cg = new X509V3CertificateGenerator();
        cg.reset();      

        X509Name name = new X509Name(issuer, new X509BDJEntryConverter());
        
        if (isRootCert) {
           cg.setSerialNumber(BigInteger.valueOf(ROOT_SERIAL_NUMBER));
        } else {
           cg.setSerialNumber(BigInteger.valueOf(APP_SERIAL_NUMBER));           
        }
        cg.setIssuerDN(name);
        cg.setNotBefore(validFrom);
        cg.setNotAfter(validTo);
        cg.setSubjectDN(name);
        cg.setPublicKey(keyPair.getPublic());
 
        cg.setSignatureAlgorithm("SHA1WITHRSA");        
        
        if (isRootCert) {
            // Need to add root cert extensions.
            cg.addExtension(X509Extensions.KeyUsage.getId(), true,
        		new X509KeyUsage(X509KeyUsage.keyCertSign));

            cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
            	getRfc822Name(ROOT_SUBJECT_ALT_NAME));

            cg.addExtension(X509Extensions.IssuerAlternativeName.getId(), false,
            	getRfc822Name(ROOT_ISSUER_ALT_NAME));    
            
            cg.addExtension(X509Extensions.BasicConstraints.getId(), true, 
                new BasicConstraints(true));
               
        } else {
            // For an app cert, most of the extensions will be added when generating
            // a certificate in response to the certificate request file.
             cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
            	getRfc822Name(APP_SUBJECT_ALT_NAME));
           
        }
        
        Certificate cert = cg.generate(keyPair.getPrivate());
        
	store.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[] {cert});
        store.store(new FileOutputStream(KEYSTOREFILE), KEYSTOREPASSWORD.toCharArray());
    }
    
    private KeyPair generateKeyPair() throws Exception {
        
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random =
           SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);      
        
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
        
    }
    
    GeneralNames getRfc822Name(String name) {
        GeneralName gn = new GeneralName(GeneralName.rfc822Name,
		 new DERIA5String(name));
        DERConstructedSequence seq = new DERConstructedSequence();
        seq.addObject(gn);
	return new GeneralNames(seq);
    }
    
}

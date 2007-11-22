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
import sun.security.tools.KeyTool;
import sun.security.tools.JarSigner;


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
 * Example: java -Xbootclasspath/p:bdsigner-bootclasspath.jar -cp $BDSIGNER_HOME/build/bdsigner.jar:$JDK_HOME/lib/tools.jar:$BDSIGNER_HOME/resource/bcprov-jdk15-137.jar net.java.bd.tools.bdsigner.BDSigner 56789abc 00000.jar 
 * 
 * Make sure to put bdsigner.jar before tools.jar in the jdk distribution for the jre 
 * classpath, and to prepend bdsigner-bootclasspath.jar to the jre bootclasspath, so that 
 * the modified version of the sun.security.* classes in this BDSigner respository
 * are used during the run.  bdprov-jdk15-137.jar is a bouncycastle distribution; a copy can be bound at "resources" dir.
 * 
 */
public class BDSigner {
 
    String appcertalias = "appcert";
    String rootcertalias = "rootcert";
    static String[] jarfiles;
    static String orgId;
    
    // Intermediate files to create, will be deleted at the tool exit time
    String appcsrfile = "appcert.csr";
    String appcertfile = "appcert.cer";
    String keystorefile = "keystore.store";

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
	    
	    
	    // Then start the test
	    new BDSigner();
    }
    
    private BDSigner() {
	    cleanup();  // Get rid of any previous key aliases first.
	     
	    try {
		    
		generateCertificates(); 
		generateCSR();
		generateCSRResponse();
		importCSRResponse();
		signJarFile();
		exportRootCertificate();
		
	    } catch (Exception e) {
	       e.printStackTrace();
	    } finally {
	       if (!debug) 
			cleanup();
	    }
    }
  
    
    private void generateCertificates() throws Exception {
	    
       String[] appCertCreateArgs = {"-genkey", "-keyalg", "RSA", "-sigAlg", "SHA1WithRSA", "-alias", appcertalias, "-keypass", "appcertpassword", "-dname", 
                                     "CN=Producer, OU=Codesigning Department, O=BDJCompany."+orgId+", L=Santa Clara, S=California, C=US", 
                                     "-debug", 
				     "-keystore", keystorefile, "-storepass", "keystorepassword"};
    
       KeyTool.main(appCertCreateArgs);
       
       String[] rootCertCreateArgs = {"-genkey", "-keyalg", "RSA", "-sigAlg", "SHA1WithRSA", "-alias", rootcertalias, "-keypass", "rootcertpassword", "-dname", 
                                     "CN=Studio, OU=Codesigning Department, O=BDJCompany."+orgId+", L=Santa Clara, S=California, C=US", 
                                     "-debug", 
				     "-keystore", keystorefile, "-storepass", "keystorepassword"};
       
       KeyTool.main(rootCertCreateArgs);
    }

    private void generateCSR() throws Exception {
       String[] appCSRRequestArgs = {"-certreq", "-alias", appcertalias, "-keypass", "appcertpassword", 
                                   "-keystore", keystorefile, "-storepass", "keystorepassword", 
				   "-debug", "-file", appcsrfile};
       
       KeyTool.main(appCSRRequestArgs);	    
    }
    
    private void generateCSRResponse() throws Exception {

       String[] appCSRRequestArgs = {"-keystore", keystorefile, "-storepass", "keystorepassword",
                                     "-keypass", "rootcertpassword", "-debug", 
				     "-issuecert", appcsrfile, appcertfile, rootcertalias};
       
       JarSigner.main(appCSRRequestArgs);
    }
    
    private void importCSRResponse() throws Exception {
       String[] responseImportArgs = {"-import", "-alias", appcertalias, "-keypass", "appcertpassword", 
                                      "-keystore", keystorefile, "-storepass", "keystorepassword",
                                      "-debug", "-file", appcertfile}; 
       KeyTool.main(responseImportArgs);
    }
    
    private void signJarFile() throws Exception {
       for (int i = 0; i < jarfiles.length; i++) {
          String[] jarSigningArgs = {"-sigFile", "SIG-BD00", "-keypass", "appcertpassword", 
                                  "-keystore", keystorefile, "-storepass", "keystorepassword",
                                  "-debug", jarfiles[i], appcertalias};
       
          JarSigner.main(jarSigningArgs);
       }
    }
    
    private void exportRootCertificate() throws Exception {
	String[] exportRootCertificateArgs = {
		"-export", "-alias", rootcertalias, "-keypass", "rootcertpassword", 
		"-keystore", keystorefile, "-storepass", "keystorepassword",
		"-debug", "-file", "app.discroot.crt" };
	
	KeyTool.main(exportRootCertificateArgs);
    }
    
    private void cleanup() {
	 File keystore = new File(keystorefile);
	 if (keystore.exists()) {
    	    try {
		 KeyTool.main(new String[]{"-delete", "-alias", appcertalias, "-keystore", keystorefile, "-storepass", "keystorepassword"});
	    } catch (Exception e) { e.printStackTrace(); }
	    try {
		 KeyTool.main(new String[]{"-delete", "-alias", rootcertalias, "-keystore", keystorefile, "-storepass", "keystorepassword"});
	    } catch (Exception e) { e.printStackTrace(); }
	    
	    keystore.delete();
	 }   
	 
	 new File(appcsrfile).delete();
	 new File(appcertfile).delete();
    }
    
    
    private static void printUsage(String reason) {
	 System.out.println("\n==============================\n");
	 System.out.println("Failed: " + reason);
	 System.out.println("\n==============================\n");
	 System.out.println("This is a tool to sign jar files according to the bd-j specification.\n");
	 System.out.println("BDSigner Syntax:");
	 System.out.println("net.java.bd.tools.bdsigner.BDSigner [-debug] 8-digit-hex-organization-ID jar-files");
	 System.out.println("Example: java -Xbootclasspath/p:bdsigner-bootclasspath.jar -cp bdsigner.jar:tools.jar:bcprov-jdk15-137.jar net.java.bd.tools.bdsigner.BDSigner 56789abc 00000.jar\n");
	 System.out.println("\n==============================\n");
    }
}

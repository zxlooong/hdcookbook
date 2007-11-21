package net.java.bd.tools.bdsigner;

import java.io.File;
import sun.security.tools.KeyTool;
import sun.security.tools.JarSigner;

/**
 * A class that wraps around jarsigner and keytool to perform bd-j required
 * certificate generation.
 * 
 * The steps involved in generating a new certificate are:
 * 1) Generate an application certificate.
 * 2) Generate a root certificate.
 * 3) Generate a certificate signing request (CSR) based on (1).
 * 4) Issue a certificate based on the CSR from (3), using the alias
 *    for root certificate from (2).
 * 5) Import the certificate back into the store issued at (4) as
 *    the root cert signed application certificate generated at (1).
 * 
 * BDCertGenerator syntax is :
 * 		BDCertGenerator [-debug] 8-digit-hex-organization-ID
 * 
 * Example: java -cp $BDSIGNER_HOME/build/bdsigner.jar:$JDK_HOME/lib/tools.jar:$BDSIGNER_HOME/resource/bcprov-jdk15-137.jar net.java.bd.tools.bdsigner.BDCertGenerator 56789abc
 * 
 * Make sure to put bdsigner.jar before tools.jar in the jdk distribution
 * for the jre classpath so that the modified version of the sun.security.*
 * classes in BDCertGenerator this respository are used at the runtime.
 * bdprov-jdk15-137.jar is a bouncycastle distribution; a copy can be bound
 * at "resources" dir.
 * 
 */
public class BDCertGenerator {
 
    String appcertalias = "appcert";
    String rootcertalias = "rootcert";
    static String[] jarfiles;
    static String orgId;
    
    String appcsrfile = "appcert.csr";
    String appcertfile = "appcert.cer";
    String keystorefile = "keystore.store";

    static boolean debug = false;
    
    public static void main(String[] args) {
	    
	    // Parse the argments
	    if (args == null || args.length < 1) {
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
	    
	    // Then start the test
	    new BDCertGenerator();
    }
    
    private BDCertGenerator() {
	    cleanup();  // Get rid of any previous key aliases first.
	    try {
		generateCertificates(); 
		generateCSR();
		generateCSRResponse();
		importCSRResponse();
		exportRootCertificate();
	    } catch (Exception e) {
	       e.printStackTrace();
	    } finally {
	       if (!debug) 
		cleanup();
	    }
    }
  
    
    private void generateCertificates() throws Exception {
	    
       String[] appCertCreateArgs = {"-genkey", "-keyalg", "RSA", "-sigAlg",
		"SHA1WithRSA", "-alias", appcertalias, "-keypass",
		"appcertpassword", "-dname", 
                "EMAILADDRESS=email@email.com, CN=Producer, OU=Codesigning Department, O=BDJCompany."+orgId+", L=Santa Clara, S=California, C=US",
		"-validity", "100000", "-debug", "-keystore", keystorefile,
		"-storepass", "keystorepassword"};
    
       KeyTool.main(appCertCreateArgs);
       
       String[] rootCertCreateArgs = {"-genkey", "-keyalg", "RSA",
			"-sigAlg", "SHA1WithRSA", "-alias", rootcertalias,
			"-keypass", "rootcertpassword", "-dname",
		"EMAILADDRESS=email@email.com, CN=Studio, OU=Codesigning Department, O=BDJCompany."+orgId+", L=Santa Clara, S=California, C=US",
			"-validity", "100000", "-debug", 
			"-keystore", keystorefile, "-storepass",
			"keystorepassword"};
       KeyTool.main(rootCertCreateArgs);
    }

    private void generateCSR() throws Exception {
       String[] appCSRRequestArgs = {"-certreq", "-alias", appcertalias,
		"-keypass", "appcertpassword", "-keystore", keystorefile,
		"-storepass", "keystorepassword", "-debug", "-file",
		appcsrfile};
       
       KeyTool.main(appCSRRequestArgs);	    
    }
    
    private void generateCSRResponse() throws Exception {

       String[] appCSRRequestArgs = {"-keystore", keystorefile, "-storepass",
		"keystorepassword", "-keypass", "rootcertpassword", "-debug",
		"-issuecert", appcsrfile, appcertfile, rootcertalias};
       
       JarSigner.main(appCSRRequestArgs);
    }
    
    private void importCSRResponse() throws Exception {
       String[] responseImportArgs = {"-import", "-alias", appcertalias,
		"-keypass", "appcertpassword", "-keystore", keystorefile,
		"-storepass", "keystorepassword", "-debug", "-file",
		appcertfile}; 
       KeyTool.main(responseImportArgs);
    }
    
    
    private void exportRootCertificate() throws Exception {
	String[] exportRootCertificateArgs = {
		"-export", "-alias", rootcertalias, "-keypass",
		"rootcertpassword", "-keystore", keystorefile, "-storepass",
		"keystorepassword", "-debug", "-file", "app.discroot.crt" };
	
	KeyTool.main(exportRootCertificateArgs);
    }
    
    private void cleanup() {
	 new File(appcsrfile).delete();
	 new File(appcertfile).delete();
    }
    
    
    private static void printUsage(String reason) {
	 System.out.println("\n==============================\n");
	 System.out.println("Failed: " + reason);
	 System.out.println("\n==============================\n");
	 System.out.println("This is a tool to generate root and application" +
	    " certificates according to the bd-j specification.\n" +
	    "The generated certificates and the keys are stored in: \n" +
	    "\"keystore.store\" file, with password:\"keystorepassword\"");
	 System.out.println("BDCertGenerator Syntax:");
	 System.out.println("net.java.bd.tools.bdsigner.BDCertGenerator" +
		 " [-debug] 8-digit-hex-organization-ID");
	 System.out.println("Example: java -cp bdsigner.jar:tools.jar:bcprov-jdk15-137.jar net.java.bd.tools.bdsigner.BDCertGenerator 56789abc\n");
	 System.out.println("\n==============================\n");
    }
}

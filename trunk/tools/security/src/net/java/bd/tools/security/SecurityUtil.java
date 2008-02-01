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

package net.java.bd.tools.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.InvalidNameException;

import sun.security.tools.KeyTool;
import sun.security.tools.JarSigner;
import sun.security.tools.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.jce.provider.X509CertificateObject;

/**
 * A generic security util class that generates the certificates for the BD-J
 * applications and signs the jars.
 * This methods of this class wrap around jarsigner and keytool to perform bd-j
 * required signing.
 * 
 * 7 steps for signing are:
 * 1) Generate app certificate.
 * 2) Generate root certificate.
 * 3) Generate a certificate signing request (CSR) based on (1).
 * 4) Issue a certificate based on the CSR on (3), using the alias for root certificate from (2).
 * 5) Import back the certificate issued at (4) to the app certificate for (1).
 * 6) Sign the jar using the app certificate after (5).
 * 7) Export the root certificate as "app.discroot.crt". 
 */
public class SecurityUtil {
    
   // default values
   static final String DEF_KEYSTORE_FILE = "keystore.store";
   static final String DEF_KEYSTORE_PASSWORD = "keystorepassword";
   static final String DEF_APPKEY_PASSWORD = "appcertpassword";
   static final String DEF_APPCERT_ALIAS = "appcert";
   static final String DEF_ROOTKEY_PASSWORD = "rootcertpassword"; 
   static final String DEF_ROOTCERT_ALIAS = "rootcert";
   static final String DEF_APP_ALT_NAME = "app@producer.com";
   static final String DEF_ROOT_ALT_NAME = "root@studio.com";
   static final String DEF_APP_CERT_DN = "CN=Producer, OU=Codesigning Department, O=BDJCompany, C=US";
   static final String DEF_ROOT_CERT_DN = "CN=Studio, OU=Codesigning Department, O=BDJCompany, C=US";
    
   // Intermediate files to create, will be deleted at the tool exit time;
   // XXX Make sure they are always deleted.
   static final String APPCSRFILE = "appcert.csr";
   static final String APPCERTFILE = "appcert.cer";
    
    // XXX We need to change the hardcoded serial numbers and generate random values for them
    static final int APP_SERIAL_NUMBER = 1;
    static final int ROOT_SERIAL_NUMBER = 2;

    // Optional fields initialized by the nested Builder class.
    String keystoreFile;
    String keystorePassword;
    String appKeyPassword;
    String appCertAlias;
    List<String> jarfiles;
    String orgId;
    String appCertDN;
    String rootCertDN; 
    String appAltName;
    String rootAltName;
    boolean debug = false;
    
    // non-optional fields
    String rootKeyPassword = DEF_ROOTKEY_PASSWORD; 
    String rootCertAlias = DEF_ROOTCERT_ALIAS;
   
    private KeyStore store;    
   
    /*
     * Using the Builder pattern from Effective Java Reloaded. The arguments
     * for this constructor are way too many. The Builder Pattern makes
     * it easy to create an instance of this class.
     * reference:http://developers.sun.com/learning/javaoneonline/2007/pdf/TS-2689.pdf
     */
    private SecurityUtil(Builder b) throws Exception {
        // Optional parameters specified from the command line tools
        this.orgId = b.orgId;
        this.keystoreFile = b.keystoreFile;
        this.keystorePassword = b.keystorePassword;
        this.appKeyPassword = b.appKeyPassword;
        this.appCertAlias = b.appCertAlias;
        this.rootCertDN = b.rootCertDN;
        this.appCertDN = b.appCertDN;
        this.rootAltName = b.rootAltName;
        this.appAltName = b.appAltName;
        this.debug = b.debug;
        this.jarfiles = b.jarfiles;
        
        // Minor processing;append the orgid to the names
        rootCertDN = appendOrgId(rootCertDN);
        appCertDN = appendOrgId(appCertDN);
    }
    
    public static class Builder {
         // Initialize with default values.
         String keystoreFile = DEF_KEYSTORE_FILE;
         String keystorePassword = DEF_KEYSTORE_PASSWORD;
         String appKeyPassword = DEF_APPKEY_PASSWORD;
         String appCertAlias = DEF_APPCERT_ALIAS;
        
         // Certificate data.
         String appAltName = DEF_APP_ALT_NAME;
         String rootAltName = DEF_ROOT_ALT_NAME;
         String appCertDN = DEF_APP_CERT_DN;
         String rootCertDN = DEF_ROOT_CERT_DN;
         
         List<String> jarfiles;
         String orgId;
         boolean debug = false;
         
        public Builder() { }
        
        public Builder orgId(String id) {
           this.orgId = id;
           return this;
        }
        public Builder keystoreFile(String file) {
            this.keystoreFile = file;
            return this;
        }
        public Builder storepass(String password) {
            this.keystorePassword = password;
            return this;
        }
        public Builder appAlias(String alias) {
            this.appCertAlias = alias;
            return this;
        }
        public Builder appPassword(String password) {
             this.appKeyPassword = password;
             return this;
        }
        public Builder rootDN(String dn) {
            this.rootCertDN = dn;
            return this;
        }
        public Builder appDN(String dn) {
            this.appCertDN = dn;
            return this;
        }
        public Builder rootAltName(String name) {
            this.rootAltName = name;
            return this;
        }
        public Builder appAltName(String name) {
            this.appAltName = name;
            return this;
        }
        public Builder debug() {
            this.debug = true;
            return this;
        }
        public Builder jarfiles(List<String> files) {
            this.jarfiles = files;
            return this;
        }
        public SecurityUtil build() throws Exception {
            return new SecurityUtil(this);
        }
    }
    
     // append the orgId to the OrganizationName of the DN
    private String appendOrgId(String dn)
            throws InvalidNameException {
        LdapName name = new LdapName(dn);
        List<Rdn> rdns = name.getRdns();
        List<Rdn> newRdns = new ArrayList<Rdn>();
        for(Rdn rdn : rdns) {
           String type = rdn.getType();
           if (type.equalsIgnoreCase("O")) {
               String value = (String) rdn.getValue();
               String newValue = value + "." + orgId;
               newRdns.add(new Rdn(type, newValue));
           } else {
               newRdns.add(rdn);
           }
        }
        return (new LdapName(newRdns)).toString();
    }
    
    public void signJars() {
        try {               
            initKeyStore();
            if (debug) {
                verifyCertificates();
            }
            signJarFile();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // VM exit with an error code
        }
    }
    
    public void createCertificates() throws Exception {
        boolean failed = false;
        cleanup();  // Get rid of any previous key aliases first.
        try {
            initKeyStore();
            generateKeys();
            exportRootCertificate();
            if (debug) {
                verifyCertificates();
            }
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }
        if (failed) {
            System.exit(1); // VM exit with an error code
        }
    }
    
    private void generateKeys() throws Exception {
        generateCertificates(); 
        generateCSR();
        generateCSRResponse();
        importCSRResponse();
    }
  
    private void initKeyStore() throws Exception {
        Security.addProvider(new BouncyCastleProvider());	  
        char[] password = keystorePassword.toCharArray();
        store = KeyStore.getInstance(KeyStore.getDefaultType());
        File kfile = new File(keystoreFile);
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
        generateSelfSignedCertificate(rootCertDN, rootCertAlias, rootKeyPassword, true);
        generateSelfSignedCertificate(appCertDN, appCertAlias, appKeyPassword, false);
    }

    private void generateCSR() throws Exception {
        String[] appCSRRequestArgs = {"-certreq", "-alias", appCertAlias, "-keypass", appKeyPassword, 
                                   "-keystore", keystoreFile, "-storepass", keystorePassword, 
				   "-debug", "-file", APPCSRFILE};
       
       KeyTool.main(appCSRRequestArgs);	    
    }
    
    private void generateCSRResponse() throws Exception {
       issueCert(APPCSRFILE, APPCERTFILE, rootCertAlias, rootKeyPassword);
    }
    
    private void importCSRResponse() throws Exception {
       String[] responseImportArgs = {"-import", "-alias", appCertAlias, "-keypass", appKeyPassword, 
                                      "-keystore", keystoreFile, "-storepass", keystorePassword,
                                      "-debug", "-file", APPCERTFILE}; 
       
       KeyTool.main(responseImportArgs);
    }
    
    private void signJarFile() throws Exception {
       for (String jfile:jarfiles) {
          String[] jarSigningArgs = {"-sigFile", "SIG-BD00",
                                   "-keypass", appKeyPassword, 
                                  "-keystore", keystoreFile, "-storepass", keystorePassword,
                                  "-debug", jfile, appCertAlias};
          JarSigner.main(jarSigningArgs);
       }
    }
    
    private void exportRootCertificate() throws Exception {
	String[] exportRootCertificateArgs = {
		"-export", "-alias", rootCertAlias, "-keypass", rootKeyPassword, 
		"-keystore", keystoreFile, "-storepass", keystorePassword,
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
    
    private void cleanup() throws IOException {    
	 File keystore = new File(keystoreFile);
         if (keystore.exists()) {
             FileInputStream fis = null;
             try {
                 KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                 fis = new FileInputStream(keystore);
                 ks.load(fis, keystorePassword.toCharArray());
                 if (ks.containsAlias(appCertAlias)) {
                     ks.deleteEntry(appCertAlias);
                 }
                 if (ks.containsAlias(rootCertAlias)) {
                     ks.deleteEntry(rootCertAlias);
                 }
                 keystore.delete();      
	    } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null)
                    fis.close();
            }
	 }   
	 new File(APPCSRFILE).delete();
	 new File(APPCERTFILE).delete();
    }
    
    private void generateSelfSignedCertificate(String issuer, String alias,
             String keyPassword, boolean isRootCert) throws Exception { 
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
            	getRfc822Name(rootAltName));
            cg.addExtension(X509Extensions.IssuerAlternativeName.getId(), false,
            	getRfc822Name(rootAltName));    
            cg.addExtension(X509Extensions.BasicConstraints.getId(), true, 
                new BasicConstraints(true));
        } else {
            // For an app cert, most of the extensions will be added when generating
            // a certificate in response to the certificate request file.
             cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
                            getRfc822Name(appAltName));
        }   
        Certificate cert = cg.generate(keyPair.getPrivate());
	store.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[] {cert});
        store.store(new FileOutputStream(keystoreFile), keystorePassword.toCharArray());
    }
    
    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        SecureRandom random =
           SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);      
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
    }
    
    void issueCert(String csrfile, String certfile, String alias, String keypass)
            throws Exception {
        PKCS10CertificationRequest csr = new PKCS10CertificationRequest(
                                             convertFromBASE64(csrfile));
        String subject = csr.getCertificationRequestInfo().getSubject().toString();

        // Generate the app certificate
        X509V3CertificateGenerator cg = new X509V3CertificateGenerator();
        cg.reset();
        X509Certificate rootCert = (X509Certificate)store.getCertificate(alias);
        cg.setIssuerDN(new X509Name(false, rootCert.getSubjectDN().getName(), 
                                    new X509BDJEntryConverter()));
        cg.setSubjectDN(new X509Name(subject, new X509BDJEntryConverter()));
        cg.setNotBefore(rootCert.getNotBefore());
        cg.setNotAfter(rootCert.getNotAfter());
        cg.setPublicKey(csr.getPublicKey());
        cg.setSerialNumber(BigInteger.valueOf(1));

	// BD-J mandates using SHA1WithRSA as a signature Algorithm
        cg.setSignatureAlgorithm("SHA1WITHRSA"); 
        cg.addExtension(X509Extensions.KeyUsage.getId(), true,
        		new X509KeyUsage(X509KeyUsage.digitalSignature));

        // FIXME: how to pull this out from the original app cert's extension?
        // Email on X500Name is not encoded with UTF8String.
        cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
            	getRfc822Name(appAltName));
        
        // Assuming that the root certificate was generated using our tool,
        // the certificate should have IssuerAlternativeNames as an extension.
        List issuerName = (List) rootCert.getIssuerAlternativeNames().iterator().next();
        cg.addExtension(X509Extensions.IssuerAlternativeName.getId(), false,
            	getRfc822Name((String)issuerName.get(1)));
        PrivateKey privateKey = (PrivateKey) store.getKey(alias, keypass.toCharArray());
        X509Certificate cert = cg.generate(privateKey);

        // Now, write leaf certificate
        System.out.println("Writing cert to " + certfile + ".");
        FileOutputStream str = new FileOutputStream(certfile);
        str.write(cert.getEncoded());
	str.close(); 
    }
    
    GeneralNames getRfc822Name(String name) {
        GeneralName gn = new GeneralName(GeneralName.rfc822Name,
		 new DERIA5String(name));
        DERConstructedSequence seq = new DERConstructedSequence();
        seq.addObject(gn);
	return new GeneralNames(seq);
    }
    
    //
    // Bouncy castle API requires CSR file in DER (binary format i.e not BASE64)
    // format. CSR PKCS#10 files are normally BASE64 encoded. We remove
    // header, footer lines and decode BASE64 to binary.
    //
    static byte[] convertFromBASE64(String file) throws IOException {
        StringBuffer buf = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        line = reader.readLine();
        if (! line.equals("-----BEGIN NEW CERTIFICATE REQUEST-----")) {
           throw new IOException("not a valid CSR file");
        }
        boolean seenLastLine = false;
        while ((line = reader.readLine()) != null) {
          if (line.equals("-----END NEW CERTIFICATE REQUEST-----")) {
             seenLastLine = true;
             break;
          }
          buf.append(line);          
          buf.append('\n');
        }
        if (! seenLastLine) {
           throw new IOException("not a valid CSR file");
        }
        return Base64.decode(buf.toString());
    }
}

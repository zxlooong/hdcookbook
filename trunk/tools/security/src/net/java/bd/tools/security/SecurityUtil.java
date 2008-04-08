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
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
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
import java.security.Signature;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.naming.InvalidNameException;

import sun.security.tools.KeyTool;
import sun.security.tools.JarSigner;
import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.pkcs.ContentInfo;
import sun.security.pkcs.PKCS7;
import sun.security.pkcs.SignerInfo;
import sun.misc.BASE64Decoder;
import sun.tools.jar.Main;

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
 * applications and signs the jars or the BUMF.
 * This methods of this class wrap around jarsigner and keytool to perform bd-j
 * required signing.
 * 
 * Here are the 3 steps for signing jar{s}:
 * 1) a) Generate root certificate.
 *    b) Export the root certificate as "app.discroot.crt". 
 * 2) a) Generate app certificate.
 *    b) Generate a certificate signing request (CSR) from (a).
 *    c) Issue a certificate for the CSR from (a), using the alias for root certificate from (1a).
 *    d) Import back the certificate issued at (c) to the app certificate for (a).
 * 3) Sign the jar using the app certificate generated for (2).
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
   static final String SIG_ALG = "SHA1WithRSA";
    
   // Intermediate files to create, will be deleted at the tool exit time;
   // XXX Make sure they are always deleted.
   static final String APPCSRFILE = "appcert.csr";
   static final String APPCERTFILE = "appcert.cer";

    // Optional fields initialized by the nested Builder class.
    String keystoreFile;
    String keystorePassword;
    String appKeyPassword;
    String jarSignerAlias;
    String certSignerAlias;
    String newCertAlias;
    List<String> jarfiles;
    String orgId;
    String dn; 
    String altName;
    String BUMFile;   // Binding Unit Manifest File
    
    boolean isRootCert = false;
    boolean isAppCert = false;
    boolean isBindingUnitCert = false;
    boolean debug = false;
   
    // non-optional fields
    String rootKeyPassword = DEF_ROOTKEY_PASSWORD;
   
    private KeyStore store; 
    private BigInteger appCertSerNo;
    private boolean ksInitialized = false;
   
    /*
     * Using Builder pattern from Effective Java Reloaded. The arguments
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
        this.newCertAlias = b.newCertAlias;
        this.jarSignerAlias = b.jarSignerAlias;
        this.certSignerAlias = b.certSignerAlias;
        this.dn = b.dn;
        this.altName = b.altName;
        this.debug = b.debug;
        this.isAppCert = b.isAppCert;
        this.isRootCert = b.isRootCert;
        this.isBindingUnitCert = b.isBindingUnitCert;
        this.BUMFile = b.BUMFile;
        this.jarfiles = b.jarfiles;
        
        // Minor processing;append the orgid to the names
        dn = appendOrgId(dn);
    }
    
    public static class Builder {
         // Initialize with default values.
         String keystoreFile = DEF_KEYSTORE_FILE;
         String keystorePassword = DEF_KEYSTORE_PASSWORD;
         String appKeyPassword = DEF_APPKEY_PASSWORD;
         String jarSignerAlias; // initialized based on jar or bumf file
         String certSignerAlias = DEF_ROOTCERT_ALIAS;;
         String newCertAlias;  // initialized based on root/app/binding cert
        
         // Certificate data.
         String dn;
         String altName;
         
         List<String> jarfiles;
         String orgId;
         boolean debug = false;
         boolean isRootCert = false;
         boolean isAppCert = false;
         boolean isBindingUnitCert = false;
         String BUMFile;
         
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
        public Builder setRootCert() {
            this.isRootCert = true;
            setRootDefaults();
            return this;
        }
        
        private void setRootDefaults() {
            if (dn == null) {
                dn = DEF_ROOT_CERT_DN;
            }
            if (altName == null) {
                altName = DEF_ROOT_ALT_NAME;
            }
            if (newCertAlias == null) {
                newCertAlias = DEF_ROOTCERT_ALIAS;
            }
        }
        public Builder setAppCert() {
            this.isAppCert = true;
            if (dn == null) {
                dn = DEF_APP_CERT_DN;
            }
            if (altName == null) {
                altName = DEF_APP_ALT_NAME;
            }
            if (newCertAlias == null) {
                newCertAlias = DEF_APPCERT_ALIAS;
            }
            return this;
        }
        public Builder setBindingUnitCert() {
            this.isBindingUnitCert = true;
            setRootDefaults();
            return this;
        }
        public Builder newCertAlias(String alias) {
            this.newCertAlias = alias;
            return this;
        }
        public Builder certSignerAlias(String alias) {
            this.certSignerAlias = alias;
            return this;
        }
        public Builder jarSignerAlias(String alias) {
            this.jarSignerAlias = alias;
            return this;
        }
        public Builder appPassword(String password) {
             this.appKeyPassword = password;
             return this;
        }
        public Builder dn(String name) {
            this.dn = name;
            return this;
        }
        public Builder altName(String name) {
            this.altName = name;
            return this;
        }
        public Builder debug() {
            this.debug = true;
            return this;
        }
        public Builder bumf(String file) {
            this.BUMFile = file;
            if (jarSignerAlias == null) {
                jarSignerAlias = DEF_ROOTCERT_ALIAS;
            }
            return this;
        } 
        public Builder jarfiles(List<String> files) {
            this.jarfiles = files;
            if (jarSignerAlias == null) {
                jarSignerAlias = DEF_APPCERT_ALIAS;
            }
            return this;
        }
        public SecurityUtil build() throws Exception {
            return new SecurityUtil(this);
        }
    }
    
     // append the orgId to the OrganizationName of the DN
    private String appendOrgId(String dn)
            throws InvalidNameException {
        if (dn == null) {
            return null;
        }
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
            signJarFile();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // VM exit with an error code
        }
    }
    
    public void createCerts() throws Exception {
        if (isAppCert) {
            createAppCert();
        } else { 
            createRootCert();
        }
    }
    
    public void createRootCert() throws Exception {
         boolean failed = false;
         cleanup();  // Get rid of any previous key aliases first.
         try {
             initKeyStore();
             generateSelfSignedCertificate(dn, newCertAlias,
                                           rootKeyPassword, true);
             exportRootCertificate();
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }
        if (failed) {
            System.exit(1); // VM exit with an error code+
        }
    }
    
     public void createAppCert() throws Exception {
        boolean failed = false;
        try {
            initKeyStore();
            generateSelfSignedCertificate(dn, newCertAlias,
                                          appKeyPassword, false);
            generateCSR();
            generateCSRResponse();
            importCSRResponse();
            if (debug) {
                verifyCertificates();
            }
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }
        if (!debug) {
            new File(APPCSRFILE).delete();
            new File(APPCERTFILE).delete();
        }
        if (failed) {
             System.exit(1); // VM exit with an error code
        }
     }
    
    private void initKeyStore() throws Exception {
        if (ksInitialized)
            return;
        ksInitialized = true;
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

    private void generateCSR() throws Exception {
        String[] appCSRRequestArgs = {"-certreq", "-alias", newCertAlias,
            "-keypass", appKeyPassword, "-keystore", keystoreFile, "-storepass",
             keystorePassword, "-v", "-file", APPCSRFILE};
       
       KeyTool.main(appCSRRequestArgs);	    
    }
    
    private void generateCSRResponse() throws Exception {
       issueCert(APPCSRFILE, APPCERTFILE, certSignerAlias, rootKeyPassword);
    }
    
    private void importCSRResponse() throws Exception {
       String[] responseImportArgs = {"-import", "-v", "-alias", newCertAlias,
                "-keypass", appKeyPassword, "-keystore", keystoreFile,
                "-storepass", keystorePassword, "-v", "-file", APPCERTFILE}; 
       KeyTool.main(responseImportArgs);
    }
    
    private void signJarFile() throws Exception {
       for (String jfile:jarfiles) {
          String[] jarSigningArgs = {"-sigFile", "SIG-BD00",
                                     "-keypass", appKeyPassword, 
                                     "-keystore", keystoreFile,
                                     "-storepass", keystorePassword,
                                     "-verbose", jfile, jarSignerAlias};
          JarSigner.main(jarSigningArgs);
          signWithBDJHeader(jfile);
       }
    }
    
    /**
     * This method signs the Binding Unit Manifest File according to the
     * BD-J specification, Part: 3-2, section: 12.2.8.1 and 
     * 2.2.8.1.4 (verification).
     * @throws java.lang.Exception
     */
    public void signBUMF() throws Exception {
        try {            
            initKeyStore();
            Signature signer = Signature.getInstance(SIG_ALG);
            if (debug) {
                System.out.println("Signer of bumf.xml file is:" + jarSignerAlias);
            }
            PrivateKey key = (PrivateKey) store.getKey(jarSignerAlias,
                              rootKeyPassword.toCharArray());
            signer.initSign(key);
            byte[] data = readIntoBuffer(BUMFile);
            signer.update(data);
            byte[] signedData = signer.sign();
            DerOutputStream dos = new DerOutputStream();
            dos.putBitString(signedData);  
            int extIndex;
            String prefix = "tmp";
            if ((extIndex = BUMFile.lastIndexOf(".")) != -1) {
                prefix = BUMFile.substring(0, extIndex);
            }
            String sigFile = prefix + ".sf";
            BufferedOutputStream bos = new BufferedOutputStream(
                                       new FileOutputStream(sigFile));
            dos.derEncode(bos);
            bos.close();
            dos.close();
            if (debug) {
                verifySignatureFile(sigFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1); // VM exit with an error code
        }
    } 
    
     private void verifySignatureFile(String sigFile) throws Exception {
        Signature verifier = Signature.getInstance(SIG_ALG);
        if (debug) {
                System.out.println("Verifier of bumf.xml file is:" + jarSignerAlias);
        }
        Certificate cert =  store.getCertificate(jarSignerAlias);
        verifier.initVerify(cert);
        
        byte[] derData = readIntoBuffer(sigFile);
        DerInputStream din = new DerInputStream(derData);
        byte[] signature = din.getBitString();
        
        byte[] data = readIntoBuffer(BUMFile);
        verifier.update(data);
        if (verifier.verify(signature)) {
            System.out.println("BUSF Verification PASSED..");
            System.out.println("The signed file is written into:" + sigFile);
        } else {
             System.out.println("BUSF Verification FAILED..");
        }
    }
    
    private byte[] readIntoBuffer(String filename) throws Exception {
        FileInputStream fis = new FileInputStream(filename);
        int INITIAL_BUF_SIZE = 3000; // some value for the buffer
        byte[] buf = new byte[INITIAL_BUF_SIZE];
        int read;
        int off = 0;
        int len = buf.length;
        int size = buf.length;
        while ((read = fis.read(buf, off, len)) != -1) {
            off = off + read;
            if (off >= size) {
               size = size * 2;
               buf = Arrays.copyOf(buf, size);
            }
            len = size - off;
            //System.out.println("off:" + off + ", len:" + len + ", size:" + size);
        }
        fis.close();
        return Arrays.copyOfRange(buf, 0, off);
    }
    
    /**
     * This method adds the BD-J specific attribute to the signature file (.SF file)
     * of a signed jar (already signed using JDK's jarsigner). This leads to
     * resigning of the signature file and building a new signature block file
     * (.RSA file) with the updated signature and update the jar with new files.
     * @param jarFile
     * @throws java.lang.Exception
     */
    private void signWithBDJHeader(String jarFile) throws Exception {
        String SIG_FILE = "META-INF/SIG-BD00.SF";
        String SIG_BLOCK_FILE = "META-INF/SIG-BD00.RSA";
        if (debug) {
            System.out.println("Adding BD header to:" + jarFile);
        }
        JarFile jf = new JarFile(jarFile);
	JarEntry sigFile = (JarEntry) jf.getJarEntry(SIG_FILE);
        if (sigFile == null) {
            System.out.println("No entry found:" + SIG_FILE);
        }
        JarEntry sigBFile = (JarEntry) jf.getJarEntry(SIG_BLOCK_FILE);
        if (sigBFile == null) {
            System.out.println("No entry found:" + SIG_BLOCK_FILE);
        }
	InputStream pkcs7Is = jf.getInputStream(sigBFile);
	PKCS7 signBlock = new PKCS7(pkcs7Is);
	pkcs7Is.close();
        
        // Add the desired line to the Signature file.
	BufferedReader br = new BufferedReader(new InputStreamReader(
                                               jf.getInputStream(sigFile)));
        StringWriter sw = new StringWriter();
        String line;
        boolean addBDLine = false;
        while((line = br.readLine()) != null) {
            
            if (addBDLine) {
                // the BD-J header already exists;stop here.
                if (line.startsWith("BDJ-Signature_Version")) {
                    br.close();
                    jf.close();
                    return;
                }
                sw.write("BDJ-Signature-Version: 1.0\n");
                addBDLine = false;
            } 
            
            // BD-J doesn't mandate this attribute; keeping it should be fine
            // Lets just be on the safer side and take it out since it's not required by
            // BD-J
            if (!line.startsWith("SHA1-Digest-Manifest-Main-Attributes:")) {
                sw.write(line);
                sw.write("\n");
            }
            if (line.startsWith("Created-By:")) {
                addBDLine = true;
            }
        }
        br.close();
        jf.close();
        
        // Re-sign the signature file after adding the BD-J header
        Signature signer = Signature.getInstance(SIG_ALG);
        PrivateKey key = (PrivateKey) store.getKey(jarSignerAlias,
                              appKeyPassword.toCharArray());
        signer.initSign(key);
        byte[] newContent = sw.toString().getBytes();
        signer.update(newContent);
        byte[] signedContent = signer.sign();
        
        ContentInfo newContentInfo = new ContentInfo(ContentInfo.DATA_OID, null);
        SignerInfo[]  signerInfos =  signBlock.getSignerInfos();
        Certificate signerCert = store.getCertificate(jarSignerAlias);
        SignerInfo newSignerInfo = null;
        
        // Note, BD-J allows only one signerInfo, but let's have this loop since
        // it does not add more code and it's easy to read.
        for(int i = 0; i < signerInfos.length; i++) {
            SignerInfo si = signerInfos[i];
            if (signerCert.equals(si.getCertificate(signBlock))) {
                
                // update the encrypted digest.
                newSignerInfo = new SignerInfo(si.getIssuerName(),
                                si.getCertificateSerialNumber(),
                                si.getDigestAlgorithmId(),
                                si.getDigestEncryptionAlgorithmId(),
                                signedContent);
                signerInfos[i] = newSignerInfo;
            }
        }
        
        // generate the updated PKCS7 Block
        PKCS7 newSignBlock = new PKCS7(
		signBlock.getDigestAlgorithmIds(),
		newContentInfo,
		signBlock.getCertificates(),
		signerInfos); 
        if (debug) {
            System.out.println("Signer Info Verified:" + (newSignBlock.verify(
                                newSignerInfo, newContent)).toString());
        }
        
        // Write down the files and re-bundle the jar with the updated signature
        // and signature block files.
        File mif = new File("META-INF");
        File sf, sbf;
        if (!mif.isDirectory()) {
            if (!mif.mkdir()) {
                System.err.println("Could not create a META-INF directory");
                return;
            }
        }
        sf = new File(SIG_FILE);
        FileOutputStream fout = new FileOutputStream(sf);
        fout.write(newContent);
        fout.close();
        sbf = new File(SIG_BLOCK_FILE);
        FileOutputStream fos = new FileOutputStream(sbf);
        newSignBlock.encodeSignedData(fos);
        fos.close();
        String[] jarArgs = {"-uvf", jarFile, SIG_FILE, SIG_BLOCK_FILE};
        Main jar = new Main(System.out, System.err, "jar");
        jar.run(jarArgs);
    }
    
    private void exportRootCertificate() throws Exception {
	String exportFileName = "app.discroot.crt";
        if (isBindingUnitCert)
            exportFileName = "bu.discroot.crt";
        String[] exportRootCertificateArgs = {
		"-export", "-alias", newCertAlias, "-keypass", rootKeyPassword, 
		"-keystore", keystoreFile, "-storepass", keystorePassword,
		"-v", "-file", exportFileName};
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
             FileOutputStream fos = null;
             try {
                 KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                 fis = new FileInputStream(keystore);
                 ks.load(fis, keystorePassword.toCharArray());
                 fos = new FileOutputStream(keystore);
                 if (ks.containsAlias(newCertAlias)) {
                     ks.deleteEntry(newCertAlias);
                 }
                 
                 // Store back the updated keystore to the keystore file.
                 ks.store(fos, keystorePassword.toCharArray());    
	    } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fis != null)
                    fis.close();
                if (fos != null)
                    fos.close();
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
        
        // Generate Serial Number
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        BigInteger serNo = new BigInteger(32, prng);
        cg.setSerialNumber(serNo);
        if (!isRootCert) {
            appCertSerNo = serNo;
        }
        cg.setIssuerDN(name);
        cg.setNotBefore(validFrom);
        cg.setNotAfter(validTo);
        cg.setSubjectDN(name);
        cg.setPublicKey(keyPair.getPublic());
        cg.setSignatureAlgorithm("SHA1WITHRSA");        
        if (isRootCert) {
            // Need to add root cert extensions.
            if (isBindingUnitCert) { 
                // This certificate is used for signing
                cg.addExtension(X509Extensions.KeyUsage.getId(), true,
        		new X509KeyUsage(X509KeyUsage.digitalSignature));
            } else {
                cg.addExtension(X509Extensions.KeyUsage.getId(), true,
        		new X509KeyUsage(X509KeyUsage.keyCertSign));
            }
            
            cg.addExtension(X509Extensions.IssuerAlternativeName.getId(), false,
            	getRfc822Name(altName));    
            cg.addExtension(X509Extensions.BasicConstraints.getId(), true, 
                new BasicConstraints(true));
        }
        // For an app cert, most of the extensions will be added when generating
        // a certificate in response to the certificate request file.
        cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
                       getRfc822Name(altName));
          
        Certificate cert = cg.generate(keyPair.getPrivate());
	store.setKeyEntry(alias, keyPair.getPrivate(), keyPassword.toCharArray(), new Certificate[] {cert});
        FileOutputStream fos = new FileOutputStream(keystoreFile);
        store.store(fos, keystorePassword.toCharArray());
        fos.close();
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
        cg.setIssuerDN(new X509Name(true, rootCert.getSubjectDN().getName(), 
                                    new X509BDJEntryConverter()));
        cg.setSubjectDN(new X509Name(subject, new X509BDJEntryConverter()));
        cg.setNotBefore(rootCert.getNotBefore());
        cg.setNotAfter(rootCert.getNotAfter());
        cg.setPublicKey(csr.getPublicKey());
        cg.setSerialNumber(appCertSerNo);

	// BD-J mandates using SHA1WithRSA as a signature Algorithm
        cg.setSignatureAlgorithm("SHA1WITHRSA"); 
        cg.addExtension(X509Extensions.KeyUsage.getId(), true,
        		new X509KeyUsage(X509KeyUsage.digitalSignature));

        // FIXME: Ideally this should be pulled out from the original app cert's
        // extension. Email on X500Name is not encoded with UTF8String.
        cg.addExtension(X509Extensions.SubjectAlternativeName.getId(), false,
            	getRfc822Name(altName));
        
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
        BASE64Decoder decoder = new BASE64Decoder();
        return decoder.decodeBuffer(buf.toString());
    }
}

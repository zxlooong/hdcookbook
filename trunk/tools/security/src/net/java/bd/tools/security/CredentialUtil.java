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

/**
 * This tool is a credential generator tool.
 * Step 1)
 * Input: Permission Request File, Output: Permission Request File with credentials
 * This tool takes in a premission request file that has all other fields of persistent
 * credentials but the <signature> and the <certchainfileid> fields/elements of the XML.
 * This tool generates the credentials using the keystores of the grantor and 
 * the grantee. And adds these fields to the permission request file.
 * For example consider the following input permission request file:
 * <?xml version="1.0" encoding="UTF-8" standalone="no"?>
 * <n:permissionrequestfile xmlns:n="urn:BDA:bdmv;PRF" appid="0x4001" orgid="0x02">
 *   <file value="true"/>
 *   <applifecyclecontrol value="true"/>
 *   <servicesel value="true"/>
 *   <userpreferences read="true" write="false"/>
 *  <persistentfilecredential>
 *      <grantoridentifier id="0x01"/>
 *      <expirationdate date="10/12/2010"/>
 *      <filename read="true" write="true">01/4000/tmp.txt</filename>
 *  </persistentfilecredential>
 * </n:permissionrequestfile>
 * 
 * The output permission request file looks like below:
 *      ......
 *     <persistentfilecredential>
 *       <grantoridentifier id="0x01"/>
 *       <expirationdate date="10/12/2010"/>
 *       <filename read="true" write="true">01/4000/tmp.txt</filename>
 *       <signature>KSrmmBCGY9RkOCug6HRWjBLC29VkCOKBoPAVbbxv+q7Ed4iVv6tzerrkXudjs1rez
 * CYtrGysX0VK&#13;
 * qKE/GlqQy2ICTWl8RVdWHFR/1KobWcsghIqtXeyR89pKrUWw8Z52o00pQsV351MrYAb7wZUzRozO&#13
 * ;
 * 1VWAViCRoKkjHbxw/pI=</signature><certchainfileid>MGIwXTEPMA0GA1UEAwwGU3R1ZGlvMR8
 * wHQYDVQQLDBZDb2Rlc2lnbmluZyBEZXBhcnRtZW50MRww&#13;
 * GgYDVQQKDBNCREpDb21wYW55LjAwMDAwMDAxMQswCQYDVQQGDAJVUwIBAQ==</certchainfileid>
 * </persistentfilecredential>
 * </n:permissionrequestfile>
 * ...
 * 
 * Step 2):
 * Input: signed jarfile, Output: signed jar file with updated certificates that
 * includes the certchain to establish the trust cert chain of the grantor.
 */

package net.java.bd.tools.security;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.math.BigInteger;

import java.security.MessageDigest;
import java.security.Signature;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.cert.CertPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;

import sun.security.pkcs.PKCS7;
import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;
import sun.tools.jar.Main;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import sun.security.util.DerInputStream;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;

class CredentialUtil {
    
    // Default values used.
    static final String DEFAULT_GRANTOR_STORE = "grantor.store";
    static final String DEFAULT_GRANTOR_STOREPASS = "keystorepassword"; 
    static final String DEFAULT_GRANTOR_ALIAS = "appcert";
    static final String DEFAULT_GRANTOR_KEYPASS = "appcertpassword";
    static final String DEFAULT_GRANTEE_STORE = "keystore.store";
    static final String DEFAULT_GRANTEE_STOREPASS = "keystorepassword"; 
    static final String DEFAULT_GRANTEE_ROOT_ALIAS = "rootcert";
    
    // Options initialized by the Builder
    String grantorKeyStore;
    String grantorStorePass; 
    String grantorAlias;
    String grantorPassword;
    String granteeKeyStore;
    String granteeStorePass; 
    String granteeRootAlias;
    String jarFileName;
    boolean debug;
    String permReqFile;
    
    List<? extends Certificate> grantorCerts;
    
    // Constants used by this class
    static final String SIG_BLOCK_FILE = "META-INF/SIG-BD00.RSA";
    static ObjectIdentifier digestAlgorithmID;
    static final String SHA1OID = "1.3.14.3.2.26";
    static final String SIG_ALGO = "SHA1withRSA";
    static final String ENCR_ALGO ="RSA";   
    static final String PERM_REQ_FILE_TAG = "permissionrequestfile";
    static final String FILE_CRED_TAG =  "persistentfilecredential";
    static final String GRANTOR_ID_TAG = "grantoridentifier";
    static final String EXP_DATE_TAG =   "expirationdate";
    static final String FILE_NAME_TAG =  "filename";  
    static final String SIGNATURE_TAG =  "signature";
    static final String FILE_ID_TAG =    "certchainfileid";
    
    private CredentialUtil(Builder b) {
        this.grantorKeyStore = b.grantorKeyStore;
        this.grantorStorePass = b.grantorStorePass; 
        this.grantorAlias = b.grantorAlias;
        this.grantorPassword = b.grantorPassword;
        this.granteeKeyStore = b.granteeKeyStore;
        this.granteeStorePass = b.granteeStorePass; 
        this.granteeRootAlias = b.granteeRootAlias;
        this.jarFileName = b.jarFileName;
        this.debug = b.debug;
        this.permReqFile = b.permReqFile;
        if (debug) {
            printDebugMsg("grantor keystore:" + grantorKeyStore +
                                ", grantor alias: " + grantorAlias +
                                ", grantee keystore:" + granteeKeyStore +
                                ", grantee root alias:" + granteeRootAlias +
                                ", permReqFile:" + permReqFile +
                                ", jarFile    :" + jarFileName);
        }
    }
    
    public static class Builder {
         // Initialize with default values
        String grantorKeyStore = DEFAULT_GRANTOR_STORE;
        String grantorStorePass = DEFAULT_GRANTOR_STOREPASS; 
        String grantorAlias = DEFAULT_GRANTOR_ALIAS;
        String grantorPassword = DEFAULT_GRANTOR_KEYPASS;
        String granteeKeyStore = DEFAULT_GRANTEE_STORE;
        String granteeStorePass = DEFAULT_GRANTEE_STOREPASS; 
        String granteeRootAlias = DEFAULT_GRANTEE_ROOT_ALIAS;
        String jarFileName;
        String permReqFile;
        boolean debug = false;
        
        public Builder() {}
        public Builder grantorKeyStore(String storefile) {
            this.grantorKeyStore = storefile;
            return this;
        }
        public Builder grantorStorePass(String storepass) {
            this.grantorStorePass = storepass;
            return this;
        }
        public Builder grantorAlias(String alias) {
            this.grantorAlias = alias;
            return this;
        }
        public Builder granteeKeyStore(String storefile) {
            this.granteeKeyStore = storefile;
            return this;
        }
        public Builder granteeStorePass(String storepass) {
            this.granteeStorePass = storepass;
            return this;
        }
        public Builder granteeRootAlias(String alias) {
            this.granteeRootAlias = alias;
            return this;
        }
        public Builder permReqFile(String file) {
            this.permReqFile = file;
            return this;
        }
        public Builder jarFile(String file) {
            this.jarFileName = file;
            return this;
        }
        public Builder debug() {
            this.debug = true;
            return this;
        }
        public CredentialUtil build() {
            return new CredentialUtil(this);
        }
    }
    
    private void printDebugMsg(String msg) {
        System.out.println("[debug]:" + msg);
    }
    
    public void genCredentials() throws Exception {
        // Read the permission request file
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();    
	Document doc = factory.newDocumentBuilder().parse(new File(permReqFile));
        Element e = doc.getDocumentElement();
        String orgId = e.getAttribute("orgid");
        String appId = e.getAttribute("appid");
        Node credNode = getNodeWithTag(e, FILE_CRED_TAG);
        ArrayList<Files> fileList = new ArrayList<Files>();
        NodeList cns = credNode.getChildNodes();
        String gaOrgId = null;
        String expDate = null;
        for (int i = 0; i < cns.getLength(); i++) {
            Node cNode = cns.item(i);
            if (cNode.getNodeName().equals(GRANTOR_ID_TAG)) {
                gaOrgId = ((Element) cNode).getAttribute("id");
            } else if (cNode.getNodeName().equals(EXP_DATE_TAG)) {
                expDate = ((Element) cNode).getAttribute("date");
            } else if (cNode.getNodeName().equals(FILE_NAME_TAG)) {
                String filePath = cNode.getTextContent();
                NamedNodeMap fileAttrs = cNode.getAttributes();
                String read = fileAttrs.getNamedItem("read").getNodeValue();
                String write = fileAttrs.getNamedItem("write").getNodeValue();
                Files f = new Files(read, write, filePath);
                fileList.add(f);
            }
        }       
        String fileId = genCertChainFileId();
        byte credentialUsage = 0;
        String signature = genCredSignature(credentialUsage, orgId, appId,
                                            gaOrgId, expDate, fileList);
        Element se;
        if ((se = (Element) getNodeWithTag(credNode, SIGNATURE_TAG)) == null) {
            se = doc.createElement(SIGNATURE_TAG);
        }
        se.setTextContent(signature);
        Element fe;
        if ((fe = (Element) getNodeWithTag(credNode, FILE_ID_TAG)) == null) {
            fe = doc.createElement(FILE_ID_TAG);
        }
        fe.setTextContent(fileId);
        credNode.appendChild(se);
        credNode.appendChild(fe);
        Source domSource = new DOMSource(doc);
        Result fileResult = new StreamResult(new File(permReqFile));
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer transformer = tfactory.newTransformer();
        transformer.transform(domSource, fileResult);
        removeEntityReference(permReqFile);
    }
    
     private Node getNodeWithTag(Node node, String tag) throws Exception {
	NodeList nl = node.getChildNodes();

        for (int i = 0; i < nl.getLength(); i++) {
	    Node n = nl.item(i);
	    if (n.getNodeType() == Node.ELEMENT_NODE)
		if (n.getNodeName().equals(tag)) 
		    return n;
	}
        if (debug)
	    printDebugMsg("No elements with tag:" + tag);
        return null;
    }
    
    private String genCertChainFileId() throws Exception {
        Certificate[] certs = 
                getCerts(grantorKeyStore, grantorStorePass, grantorAlias);
        grantorCerts = getCertPath(certs);
	X509Certificate leafCert = (X509Certificate) grantorCerts.get(0);
        if (debug) 
	    printDebugMsg("Using the grantor cert for generating <certchainfileid> :" + leafCert);

	// read the issuer name
	byte[] issuerName = leafCert.getIssuerX500Principal().getEncoded();

	// read the serial number
	BigInteger certificateSerialNumber = leafCert.getSerialNumber();

	// lets do the DER encoding of above fields
 	DerOutputStream seq = new DerOutputStream();	
	DerOutputStream issuerAndSerialNumber = new DerOutputStream();
	issuerAndSerialNumber.write(issuerName, 0, issuerName.length);
        issuerAndSerialNumber.putInteger(certificateSerialNumber);
        seq.write(DerValue.tag_Sequence, issuerAndSerialNumber);

	// encode with base64
	ByteArrayOutputStream out = new ByteArrayOutputStream();
   	BASE64Encoder base64 = new BASE64Encoder();
	base64.encode(seq.toByteArray(), out);
        return out.toString("US-ASCII");
    }
    
    // Lets build a certpath to ensure that the certificate chain 
    // forms a trusted certificate chain to the root.
    private List<? extends Certificate> getCertPath(Certificate[] certs) 
            throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        List certList = Arrays.asList(certs);
    	CertPath cp = cf.generateCertPath(certList);
        return cp.getCertificates();
    }

    private Certificate[] getCerts(String keystore, String storepass, String alias) 
		throws Exception {
	KeyStore ks = KeyStore.getInstance("JKS");

    	// load the contents of the KeyStore
    	ks.load(new FileInputStream(keystore), storepass.toCharArray());

	// fetch certificate chain stored with the given alias
    	 return ks.getCertificateChain(alias);
    }
    
     private PrivateKey getGrantorKey () throws Exception {
	KeyStore ks = KeyStore.getInstance("JKS");
    	ks.load(new FileInputStream(grantorKeyStore), grantorStorePass.toCharArray());
        PrivateKey grantorKey = (PrivateKey) ks.getKey(grantorAlias,
                                grantorPassword.toCharArray());
        return grantorKey;
    }

    private String genCredSignature(byte credentialUsage,
                                    String granteeOrgIdStr,
                                    String granteeAppIdStr,
                                    String grantorOrgIdStr,
                                    String expDateStr,
                                    List<Files> fileList)
                throws Exception {
        if (debug) {
            printDebugMsg("Generating the signature using- granteeId:" + granteeOrgIdStr +
                                ", granteeAppId:" + granteeAppIdStr +
                                ", grantorOrgId:" + grantorOrgIdStr +
                                ", expDate:" + expDateStr +
                                ", file permissions:" + fileList);
        }
        // get the grantee org_id, 32 bits, assuming the grantonIdStr begins with "0x"
	int granteeOrgId = Integer.parseInt(granteeOrgIdStr.substring(2), 16);

	// get the grantee app_id 16 bits
	short granteeAppId = (short) Integer.parseInt(granteeOrgIdStr.substring(2), 16);

	// get grantee certificate digest.
        KeyStore gs = KeyStore.getInstance("JKS");
        gs.load(new FileInputStream(granteeKeyStore),
                	granteeStorePass.toCharArray());
        Certificate[] granteeCerts = gs.getCertificateChain(granteeRootAlias);
	
	byte[] granteeCertDigest = getCertDigest(granteeCerts[0]);	
	int grantorOrgId = Integer.parseInt(grantorOrgIdStr.substring(2), 16);
	byte[] grantorCertDigest = getCertDigest(grantorCerts.get(
                                        grantorCerts.size() - 1));
	byte[] expiryDate = getAscii(expDateStr);

	// binary concatenation of the fields to be signed
	ByteArrayOutputStream baos = new ByteArrayOutputStream(450);
	DataOutputStream dos = new DataOutputStream(baos);
	dos.writeByte(credentialUsage);
	dos.writeInt(granteeOrgId);
	dos.writeShort(granteeAppId);
	dos.write(granteeCertDigest, 0, granteeCertDigest.length);
	dos.writeInt(grantorOrgId);
	dos.write(grantorCertDigest, 0, grantorCertDigest.length);
	dos.write(expiryDate, 0, expiryDate.length);
        
        // file related attributes
        for (Files f : fileList) {
            byte[] readPerm = getAscii(f.read);
            byte[] writePerm = getAscii(f.write);
            byte[] filepath = getAscii(f.filepath);
            short fileLength = (short) filepath.length;
            dos.write(readPerm, 0, readPerm.length);
            dos.write(writePerm, 0, writePerm.length);
            dos.writeShort(fileLength);
            dos.write(filepath, 0, filepath.length);
        }
	dos.close();
        byte[] data = baos.toByteArray();
        if (debug)
	    printDebugMsg("To be signed data length:" + data.length);
        
        PrivateKey grantorKey = getGrantorKey();
        Signature sig = Signature.getInstance(SIG_ALGO);
        sig.initSign(grantorKey);
        sig.update(data);
        byte[] signature = sig.sign();
        
	// encode with base64
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	BASE64Encoder base64 = new BASE64Encoder();
        base64.encode(signature, out);
	return out.toString("US-ASCII");
    }
    
     static class Files {
        String read;
        String write;
        String filepath;
        
        Files(String read, String write, String filepath) {
            this.read = read;
            this.write = write;
            this.filepath = filepath;
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("read:" + read);
            sb.append(", write:" + write);
            sb.append(", filepath:" + filepath);
            return sb.toString();
        }
    };
    
    
    public void updateCerts() throws Exception {
	JarFile jf = new JarFile(jarFileName);
	JarEntry je = (JarEntry) jf.getJarEntry(SIG_BLOCK_FILE);
        if (je == null) {
            System.out.println("No entry found:" + SIG_BLOCK_FILE);
        }
	InputStream pkcs7Is = jf.getInputStream(je);
	PKCS7 signBlockFile = new PKCS7(pkcs7Is);
	pkcs7Is.close();
	jf.close();
	
	X509Certificate[] certs = (X509Certificate[]) signBlockFile.getCertificates();
	Certificate[] grantorCerts = getCerts(grantorKeyStore,
					grantorStorePass, grantorAlias);
	X509Certificate[] addedCerts =
			new X509Certificate[certs.length + grantorCerts.length];	
	int len = 0;
        for (;len < certs.length; len++) {
	    addedCerts[len] = (X509Certificate) certs[len];
	}
        for (int i = 0; i < grantorCerts.length; len++, i++) {
	    addedCerts[len] = (X509Certificate) grantorCerts[i];
	}
        if (debug) {
            printDebugMsg("Updated Certs:");
            for (Certificate cert : addedCerts) {
                  System.out.println("CERT:" + ((X509Certificate)cert).getSubjectX500Principal());
            }
        }
        
        // generate the updated PKCS7 Block including grantor certs
	PKCS7 newSignBlockFile = new PKCS7(
		signBlockFile.getDigestAlgorithmIds(),
		signBlockFile.getContentInfo(),
		addedCerts,
		signBlockFile.getSignerInfos()); 
        File mif = new File("META-INF");
        File sbf;
        if (!mif.isDirectory()) {
            if (!mif.mkdir())
            System.err.println("Could not create a META-INF directory");
            return;
        }
        sbf = new File(mif, "SIG-BD00.RSA");
	FileOutputStream fos = new FileOutputStream(sbf);	
	newSignBlockFile.encodeSignedData(fos);
 	fos.close();
        String[] jarArgs = {"-uvf", jarFileName, SIG_BLOCK_FILE};
        Main jar = new Main(System.out, System.err, "jar");
        jar.run(jarArgs);
        if (debug) {
            verify();
        }
    }
    
    private byte[] getAscii(String str) throws Exception {
	//return str.getBytes(Charset.forName("US-ASCII"));
        return str.getBytes("US-ASCII");
    }

    private byte[] getCertDigest(Certificate cert) throws Exception {
	byte[] encCertInfo = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance("SHA1");
        return md.digest(encCertInfo);
    }
    
    public void verify() throws Exception {
        X509Certificate grantorRootCert = verifyCertChainFileId(permReqFile, jarFileName);
        if (grantorRootCert == null) {
            verifyError("Unable to find the grantor cert");
        }
        System.out.println("<certchainfileid> Verification PASSED.");
        
        // TODO: A better mechanism independent of the way the signture is generated
        // is required here
        // verifySignature(permReqFile, grantorRootCert, "app.discroot.crt");
    }
    
    private void verifyError(String errMsg) {
        System.out.println("===========================");
        System.out.println("VERFICATION FAILED:" + errMsg);
        System.out.println("===========================");
        System.exit(1);
    }
    
    public X509Certificate verifyCertChainFileId(String permReqFile, String jarFileName) throws Exception {
        JarFile jf = new JarFile(jarFileName);
        ZipEntry je = jf.getEntry(permReqFile);
        if (je == null) {
            verifyError("Jar Entry:" + permReqFile + " not found.");
        }     
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();    
	Document doc = factory.newDocumentBuilder().parse(jf.getInputStream(je));
        Element e = doc.getDocumentElement();;
        Node credNode = getNodeWithTag(e, FILE_CRED_TAG);
        Node grantorNode = getNodeWithTag(credNode, GRANTOR_ID_TAG);
        String gaOrgId = ((Element) grantorNode).getAttribute("id");
        if (debug) {
            printDebugMsg("Grantor's orgId:" + gaOrgId);
        }
        // remove 0x suffix from the orgId field of the permission request file
        gaOrgId = gaOrgId.substring(2);
        int gaOid = Integer.parseInt(gaOrgId, 16);
        Node fileIdNode = getNodeWithTag(credNode, FILE_ID_TAG);  
        if(fileIdNode == null) {
            verifyError("No elements in the permission request file with tag: " + FILE_ID_TAG);
        }
        String base64Data = fileIdNode.getTextContent();
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] derData = decoder.decodeBuffer(base64Data);

        // issuerAndSerialNumber
        DerInputStream derin = new DerInputStream(derData);
        DerValue[] issuerAndSerialNumber = derin.getSequence(2);
        byte[] issuerBytes = issuerAndSerialNumber[0].toByteArray();
        //X500Name issuerName = new X500Name(new DerValue(DerValue.tag_Sequence,
        //                                        issuerBytes));
        X500Principal issuerName = new X500Principal(issuerBytes);
        BigInteger certificateSerialNumber = issuerAndSerialNumber[1].getBigInteger();
     
        System.out.println("Looking for cert with issuerName:" + issuerName);
        System.out.println(" and cert serial no:" + Integer.toHexString(certificateSerialNumber.intValue()));
        
        // retrieve the cert from the jarfile
        Collection certs = retrieveCerts(jarFileName);
        Iterator i = certs.iterator();
        X509Certificate cert = null;
        while (i.hasNext()) {
            cert = (X509Certificate) i.next();
            if (issuerName.equals(cert.getIssuerX500Principal())) {
                
                // check for the org id in the issuer name
                LdapName dn = new LdapName(issuerName.toString());
               
                // assumes the RDN second from left is the organization
                String org = (String) dn.getRdn(dn.size() - 2).getValue();
                int indexOrgId = org.lastIndexOf(".");
                String orgId = null;
                if (indexOrgId != -1) {
                    orgId = org.substring(indexOrgId + 1);
                    //System.out.println("orgId:" + orgId);
                } else {
                    System.out.println("Could not retrieve the orgId");
                    continue;
                }
                int certOid = Integer.parseInt(orgId, 16);
                if (gaOid != certOid) {
                    System.out.println("grantor org Id with that in the certificate did not match");
                    continue;
                }
                if (!certificateSerialNumber.equals(cert.getSerialNumber())) {
                    System.out.println("Certificate Serial Number did not match");
                    continue;
                }
                System.out.println("Found the grantor's certificate:" + cert);
                return cert;
            }
       }
       return cert;
    }
    
    /**
     * This method explicitly removes the character references from the PRF file
     * that were generated after updating the PRF file with credentials. The XML
     * APIs automatically generate character references when the XML document
     * is written to a file. According to MHP Specifcation section 14.3
     * character or entity references are not allowed in XML structure.
     * @param fileName
     * @throws java.lang.Exception
     */
    void removeEntityReference(String fileName) throws Exception {    
	BufferedReader br = new BufferedReader(
                            new FileReader(fileName));
        int ch;
        CharArrayWriter caw = new CharArrayWriter(2000);
        while ((ch = br.read()) != -1) {
            if (ch == '&') {
                if (br.skip(4) != 4) {
                    System.out.println("ERROR removing character references" +
                          " from PRF file; Could not skip 4 chars");
                    br.close();
                    System.exit(1);
                }
            } else {
                caw.write(ch);
            }
        }
        br.close();
        FileWriter fw = new FileWriter(fileName);
        fw.write(caw.toCharArray());
        fw.close();
    }
    
    // XXX This method's implementation is incomplete.
    public void verifySignature(String permReqFileName, X509Certificate grantorRootCert,
            String granteeRootCertName) throws Exception {
        byte credentialUsage = 0x00;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();    
	Document doc = factory.newDocumentBuilder().parse(new File(permReqFile));
        Element e = doc.getDocumentElement();
        String granteeOrgId = e.getAttribute("orgid");
        granteeOrgId = granteeOrgId.substring(2);
        int geOId = Integer.parseInt(granteeOrgId, 16);
        String granteeAppId = e.getAttribute("appid");
        granteeAppId = granteeAppId.substring(2);
        int geAppId = Short.parseShort(granteeAppId, 16);
        
        // compute grantee root cert digest
        FileInputStream fis = new FileInputStream(granteeRootCertName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate granteeRootCert = (X509Certificate) cf.generateCertificate(bis);
        byte[] granteeCertDigest = getCertDigest(granteeRootCert);
        
        // get grantor org  id;
        Node credNode = getNodeWithTag(e, FILE_CRED_TAG);
        Node grantorNode = getNodeWithTag(credNode, GRANTOR_ID_TAG);
        String gaOrgId = ((Element) grantorNode).getAttribute("id");
        
        // remove 0x suffix from the orgId field of the permission request file
        gaOrgId = gaOrgId.substring(2);
        int gaOId = Integer.parseInt(gaOrgId, 16);
        
        // compute grantor root cert digest
        byte[] grantorCertDigest = getCertDigest(grantorRootCert);
        ArrayList fileList = new ArrayList();
        Node expDateNode = getNodeWithTag(credNode, EXP_DATE_TAG);
        String expDate = ((Element) expDateNode).getAttribute("date");
        Node fileNode = getNodeWithTag(credNode, FILE_NAME_TAG);
        String filePath = fileNode.getTextContent();
        NamedNodeMap fileAttrs = fileNode.getAttributes();
        String read = fileAttrs.getNamedItem("read").getNodeValue();
        String write = fileAttrs.getNamedItem("write").getNodeValue();
        Files f = new Files(read, write, filePath);
        fileList.add(f);
        
        // This code is yet to be completed. The verification process here is
        // getting very close to the way signture was generated in the first place
        // we need a better verifation mechanism
    }
        
    private Collection retrieveCerts(String jarFileName) throws Exception {
        JarFile jf = new JarFile(jarFileName);
        //System.out.println("sig block file" + SIG_BLOCK_FILE);
        JarEntry jarEntry = (JarEntry) jf.getEntry("META-INF/SIG-BD00.RSA");
        InputStream in = jf.getInputStream(jarEntry);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Collection certs = cf.generateCertificates(in);
        System.out.println("# of certs in the signed Jar File:" + certs.size());
        jf.close();      
        return certs;
    }
}

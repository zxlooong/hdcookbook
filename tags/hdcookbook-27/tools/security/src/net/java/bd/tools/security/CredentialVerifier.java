/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.bd.tools.security;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.naming.ldap.LdapName;
import javax.security.auth.x500.X500Principal;
import javax.xml.parsers.DocumentBuilderFactory;
import net.java.bd.tools.security.CredentialUtil.Files;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Decoder;
import sun.misc.HexDumpEncoder;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;
import static net.java.bd.tools.security.CredentialUtil.*;

/**
 * This class contails static methods to verify the credentials; it's useful for
 * catching obvious errors in the encoding of the credentials.
 * The verify() method takes: the path to the signed jarfile, the location
 * of the permission request file within the jar file, and the path to the
 * grantee root certificate. 
 * 
 * @author Jaya Hangal  
 */
class CredentialVerifier {
    
   public static void verify(String jarfile,
                             String permReqFileName,
                             String rootCert)
                throws Exception {
        JarFile jf = new JarFile(jarfile);
        ZipEntry je = jf.getEntry(permReqFileName);
        if (je == null) {
            verifyError("Jar Entry:" + permReqFileName + " not found.");
        }     
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();    
	Document doc = factory.newDocumentBuilder().parse(jf.getInputStream(je));
        Element e = doc.getDocumentElement();;
        Node credNode = getNodeWithTag(e, FILE_CRED_TAG);
        Node grantorNode = getNodeWithTag(credNode, GRANTOR_ID_TAG);
        String grantorOrg = ((Element) grantorNode).getAttribute("id");
        System.out.println("*************** Verifying Credentials ***********");
        System.out.println("Grantor's organization Id:" + grantorOrg);
        
        // remove 0x suffix from the orgId field of the permission request file
        grantorOrg = grantorOrg.substring(2);
        int grantorId = Integer.parseInt(grantorOrg, 16);
        X509Certificate[] grantorCerts = verifyCertChainFileId(credNode,
                                            grantorId, jarfile);
        if (grantorCerts.length < 2) {
            verifyError("Unable to find grantor certs");
        }
        System.out.println("####### <certchainfileid> Verification PASSED #######");  
        verifySignature(e, grantorId, jarfile, rootCert, grantorCerts);
        System.out.println("*************** Verification Done ***********");
    }
    
    static private void verifyError(String errMsg) {
        System.out.println("===========================");
        System.out.println("VERFICATION FAILED:" + errMsg);
        System.out.println("===========================");
        System.exit(1);
    }
    
    static private X509Certificate[] verifyCertChainFileId(Node credNode,
                    int grantorId, String jarFileName) throws Exception {
        Node fileIdNode = getNodeWithTag(credNode, FILE_ID_TAG);  
        if(fileIdNode == null) {
            verifyError("No elements in the permission request file with tag: " +
                        FILE_ID_TAG);
        }
        String base64Data = fileIdNode.getTextContent();
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] derData = decoder.decodeBuffer(base64Data);

        // issuerAndSerialNumber
        DerInputStream derin = new DerInputStream(derData);
        DerValue[] issuerAndSerialNumber = derin.getSequence(2);
        byte[] issuerBytes = issuerAndSerialNumber[0].toByteArray();
        
        X500Principal issuerName = new X500Principal(issuerBytes);
        BigInteger certificateSerialNumber = issuerAndSerialNumber[1].getBigInteger();
     
        System.out.println("Looking for cert with issuerName:" + issuerName +
                " and cert serial no:" + Integer.toHexString(
                                            certificateSerialNumber.intValue()));
        
        // The return array below is for grantor's root cert and the
        // grantor cert.
        X509Certificate[] returnCerts = new X509Certificate[2];
        
        // retrieve the cert from the jarfile
        Collection certs = retrieveCerts(jarFileName);
        Iterator i = certs.iterator();
        int found = 0;
        while ((i.hasNext()) && (found < 2)) {
            X509Certificate cert = (X509Certificate) i.next();
            if (issuerName.equals(cert.getIssuerX500Principal())) {
                
                // check for the org id in the issuer name
                LdapName dn = new LdapName(issuerName.toString());
               
                // assumes the RDN second from left is the organization
                String org = (String) dn.getRdn(dn.size() - 2).getValue();
                int indexOrgId = org.lastIndexOf(".");
                String orgId = null;
                if (indexOrgId != -1) {
                    orgId = org.substring(indexOrgId + 1);
                } else {
                    System.out.println("Could not retrieve the orgId");
                }
                int certOrgId = Integer.parseInt(orgId, 16);
                if (grantorId != certOrgId) {
                    System.out.println(
                    "grantor org Id with that in the certificate did not match");
                }
                if (certificateSerialNumber.equals(cert.getSerialNumber())) {
                    System.out.println("Found the grantor's certificate:" +
                                            cert.getSubjectX500Principal());
                    returnCerts[0] = cert;
                    found++;
                }
            } if ((issuerName.equals(cert.getSubjectX500Principal())) &&
                      (issuerName.equals(cert.getIssuerX500Principal()))) {
                // Self signed certificate must be the root.
                returnCerts[1] = cert;
                found++;
            }
       }
       return returnCerts;
    }
    
    private static void verifySignature(Element e, int grantorId, String jarfile,
                                String granteeRootCertName,
                                X509Certificate[] grantorCerts) throws Exception {   
        byte credentialUsage = 0x00;
        String geOrgId = e.getAttribute("orgid");
        geOrgId = geOrgId.substring(2);
        int granteeOrgId = Integer.parseInt(geOrgId, 16);
        String geAppId = e.getAttribute("appid");
        geAppId = geAppId.substring(2);
        int granteeAppId = Short.parseShort(geAppId, 16);
        
        // compute grantee root cert digest
        FileInputStream fis = new FileInputStream(granteeRootCertName);
        BufferedInputStream bis = new BufferedInputStream(fis);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate granteeRootCert = (X509Certificate) cf.generateCertificate(bis);
        byte[] granteeRootCertDigest = getCertDigest(granteeRootCert);
        byte[] grantorRootCertDigest = getCertDigest(grantorCerts[1]);
        
        Node credNode = getNodeWithTag(e, FILE_CRED_TAG);
        NodeList credAttrs = credNode.getChildNodes();
        String expDate = null;
        ArrayList<Files> fileList = new ArrayList<Files>();
        for (int i = 0; i < credAttrs.getLength(); i++) {
            Node cNode = credAttrs.item(i);
            if (cNode.getNodeName().equals(EXP_DATE_TAG)) {
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
        byte[] expiryDate = getAscii(expDate);
       
        // binary concatenation of the fields to be signed
	ByteArrayOutputStream baos = new ByteArrayOutputStream(450);
	DataOutputStream dos = new DataOutputStream(baos);
	dos.writeByte(credentialUsage);
	dos.writeInt(granteeOrgId);
	dos.writeShort(granteeAppId);
	dos.write(granteeRootCertDigest, 0, granteeRootCertDigest.length);
	dos.writeInt(grantorId);
	dos.write(grantorRootCertDigest, 0, grantorRootCertDigest.length);
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
        System.out.println("Data gathered for signature verification:");
        HexDumpEncoder hexDump = new HexDumpEncoder();
        System.out.println(hexDump.encodeBuffer(data));
        
        Node signNode = getNodeWithTag(credNode, SIGNATURE_TAG);  
        if(signNode == null) {
            verifyError("No elements in the permission request file with tag: " +
                         SIGNATURE_TAG);
        }
        String base64Data = signNode.getTextContent();
        BASE64Decoder decoder = new BASE64Decoder();
        byte[] signature = decoder.decodeBuffer(base64Data);
        Signature verifier = Signature.getInstance(SIG_ALGO);
        verifier.initVerify(grantorCerts[0]);
        verifier.update(data);
        boolean verified = verifier.verify(signature);
        if (verified)
            System.out.println("####### Credentials signature verification PASSED ######");
        else 
           verifyError("Credentials signature verification FAILED"); 
    }
    
    private static Collection retrieveCerts(String jarFileName) throws Exception {
        JarFile jf = new JarFile(jarFileName);
        JarEntry jarEntry = (JarEntry) jf.getEntry("META-INF/SIG-BD00.RSA");
        InputStream in = jf.getInputStream(jarEntry);
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Collection certs = cf.generateCertificates(in);
        System.out.println("# of certs in the signed Jar File:" + certs.size());
        jf.close();      
        return certs;
    }
}
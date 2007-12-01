/*
 * @(#)CertAndKeyGen.java	1.59 05/11/17
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sun.security.x509;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.*;
import java.util.Calendar;
import java.util.Date;

import sun.security.pkcs.PKCS10;


/**
 * Generate a pair of keys, and provide access to them.  This class is
 * provided primarily for ease of use.
 *
 * <P>This provides some simple certificate management functionality.
 * Specifically, it allows you to create self-signed X.509 certificates
 * as well as PKCS 10 based certificate signing requests.
 *
 * <P>Keys for some public key signature algorithms have algorithm
 * parameters, such as DSS/DSA.  Some sites' Certificate Authorities
 * adopt fixed algorithm parameters, which speeds up some operations
 * including key generation and signing.  <em>At this time, this interface
 * does not provide a way to provide such algorithm parameters, e.g.
 * by providing the CA certificate which includes those parameters.</em>
 *
 * <P>Also, note that at this time only signature-capable keys may be
 * acquired through this interface.  Diffie-Hellman keys, used for secure
 * key exchange, may be supported later.
 *
 * @author David Brownell
 * @author Hemma Prafullchandra
 * @version 1.59
 * @see PKCS10
 * @see X509CertImpl
 */

// Blu-ray specific change:
// See method public X509Certificate getSelfCertificate (X500Name myname, long validity)
// in this class.
// This code is taken from jdk 6.0 latest workspace.

public final class CertAndKeyGen {
    /**
     * Creates a CertAndKeyGen object for a particular key type
     * and signature algorithm.
     *
     * @param keyType type of key, e.g. "RSA", "DSA"
     * @param sigAlg name of the signature algorithm, e.g. "MD5WithRSA",
     *		"MD2WithRSA", "SHAwithDSA".
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     */
    public CertAndKeyGen (String keyType, String sigAlg)
    throws NoSuchAlgorithmException
    {
	keyGen = KeyPairGenerator.getInstance(keyType);
	this.sigAlg = sigAlg;
    }

    /**
     * Creates a CertAndKeyGen object for a particular key type,
     * signature algorithm, and provider.
     *
     * @param keyType type of key, e.g. "RSA", "DSA"
     * @param sigAlg name of the signature algorithm, e.g. "MD5WithRSA",
     *		"MD2WithRSA", "SHAwithDSA".
     * @param providerName name of the provider
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     * @exception NoSuchProviderException on unrecognized providers.
     */
    public CertAndKeyGen (String keyType, String sigAlg, String providerName)
    throws NoSuchAlgorithmException, NoSuchProviderException
    {
	if (providerName == null) {
	    keyGen = KeyPairGenerator.getInstance(keyType);
	} else {
	    try {
		keyGen = KeyPairGenerator.getInstance(keyType, providerName);
	    } catch (Exception e) {
		// try first available provider instead
		keyGen = KeyPairGenerator.getInstance(keyType);
	    }
	}
	this.sigAlg = sigAlg;
    }

    /**
     * Sets the source of random numbers used when generating keys.
     * If you do not provide one, a system default facility is used.
     * You may wish to provide your own source of random numbers
     * to get a reproducible sequence of keys and signatures, or
     * because you may be able to take advantage of strong sources
     * of randomness/entropy in your environment.
     */
    public void		setRandom (SecureRandom generator)
    {
	prng = generator;
    }

    // want "public void generate (X509Certificate)" ... inherit DSA/D-H param

    /**
     * Generates a random public/private key pair, with a given key
     * size.  Different algorithms provide different degrees of security
     * for the same key size, because of the "work factor" involved in
     * brute force attacks.  As computers become faster, it becomes
     * easier to perform such attacks.  Small keys are to be avoided.
     *
     * <P>Note that not all values of "keyBits" are valid for all
     * algorithms, and not all public key algorithms are currently
     * supported for use in X.509 certificates.  If the algorithm
     * you specified does not produce X.509 compatible keys, an
     * invalid key exception is thrown.
     *
     * @param keyBits the number of bits in the keys.
     * @exception InvalidKeyException if the environment does not
     *	provide X.509 public keys for this signature algorithm.
     */
    public void generate (int keyBits)
    throws InvalidKeyException
    {
	KeyPair	pair;

	try {
	    if (prng == null) {
		prng = new SecureRandom();
	    }
	    keyGen.initialize(keyBits, prng);
	    pair = keyGen.generateKeyPair();

	} catch (Exception e) {
	    throw new IllegalArgumentException(e.getMessage());
	}

	publicKey = pair.getPublic();
	privateKey = pair.getPrivate();
    }


    /**
     * Returns the public key of the generated key pair if it is of type
     * <code>X509Key</code>, or null if the public key is of a different type.
     *
     * XXX Note: This behaviour is needed for backwards compatibility.
     * What this method really should return is the public key of the
     * generated key pair, regardless of whether or not it is an instance of
     * <code>X509Key</code>. Accordingly, the return type of this method
     * should be <code>PublicKey</code>.
     */
    public X509Key getPublicKey()
    {
	if (!(publicKey instanceof X509Key)) {
	    return null;
	}
	return (X509Key)publicKey;
    }


    /**
     * Returns the private key of the generated key pair.
     *
     * <P><STRONG><em>Be extremely careful when handling private keys.
     * When private keys are not kept secret, they lose their ability
     * to securely authenticate specific entities ... that is a huge
     * security risk!</em></STRONG>
     */
    public PrivateKey getPrivateKey ()
    {
	return privateKey;
    }


    /**
     * Returns a self-signed X.509v1 certificate for the public key.
     * The certificate is immediately valid.
     *
     * <P>Such certificates normally are used to identify a "Certificate
     * Authority" (CA).  Accordingly, they will not always be accepted by
     * other parties.  However, such certificates are also useful when
     * you are bootstrapping your security infrastructure, or deploying
     * system prototypes.
     *
     * @deprecated Use the new <a href =
     * "#getSelfCertificate(sun.security.x509.X500Name, long)">
     *
     * @param myname X.500 name of the subject (who is also the issuer)
     * @param validity how long the certificate should be valid, in seconds
     */
    @Deprecated
    public X509Cert             getSelfCert (X500Name myname, long validity)
    throws InvalidKeyException, SignatureException, NoSuchAlgorithmException
    {
	X509Certificate cert;

	try {
	    cert = getSelfCertificate(myname, validity);
	    return new X509Cert(cert.getEncoded());
	} catch (CertificateException e) {
	    throw new SignatureException(e.getMessage());
	} catch (NoSuchProviderException e) {
	    throw new NoSuchAlgorithmException(e.getMessage());
	} catch (IOException e) {
	    throw new SignatureException(e.getMessage());
	}
    }


    /**
     * Returns a self-signed X.509v3 certificate for the public key.
     * The certificate is immediately valid. No extensions.
     *
     * <P>Such certificates normally are used to identify a "Certificate
     * Authority" (CA).  Accordingly, they will not always be accepted by
     * other parties.  However, such certificates are also useful when
     * you are bootstrapping your security infrastructure, or deploying
     * system prototypes.
     *
     * @param myname X.500 name of the subject (who is also the issuer)
     * @param validity how long the certificate should be valid, in seconds
     * @exception CertificateException on certificate handling errors.
     * @exception InvalidKeyException on key handling errors.
     * @exception SignatureException on signature handling errors.
     * @exception NoSuchAlgorithmException on unrecognized algorithms.
     * @exception NoSuchProviderException on unrecognized providers.
     */
    public X509Certificate getSelfCertificate (X500Name myname, long validity)
    throws CertificateException, InvalidKeyException, SignatureException,
        NoSuchAlgorithmException, NoSuchProviderException
    {
	X500Signer	issuer;
	X509CertImpl	cert;
	Date		firstDate, lastDate;

        // Blu-ray specific:
        // MHP 12.5.5. Need GeneralizedTime format for the validity.
        //      Use time before 1950 and after 2050, since bouncycastle's 
        //      org.bouncycastle.asn1.x509.Time is formatted as GeneralizedTime
        //      only if the range is outside of 1950-2050.
        // MHP 12.5.9  Need KeyUsage, SubjectAlternativeName and IssuerAlternativeName 
        //      extensions for both leaf and root certificate.
	try {
	    issuer = getSigner (myname);

            // Start BD-J specific 
            Calendar calendar = Calendar.getInstance();
            calendar.clear();
            calendar.set(1949, 1, 1);
            firstDate = calendar.getTime();
            calendar.clear();
            calendar.set(2055, 1, 1);
            lastDate = calendar.getTime();
            // End BD-J specific
	    // Original code: firstDate = new Date ();
	    // Original code: lastDate = new Date ();
	    // Original code: lastDate.setTime (lastDate.getTime () + validity * 1000);

            CertificateValidity interval =
                                   new CertificateValidity(firstDate,lastDate);
                
            X509CertInfo info = new X509CertInfo();
            // Add all mandatory attributes
            info.set(X509CertInfo.VERSION,
                     new CertificateVersion(CertificateVersion.V3));
            info.set(X509CertInfo.SERIAL_NUMBER,
                 new CertificateSerialNumber((int)(firstDate.getTime()/1000)));
            AlgorithmId algID = issuer.getAlgorithmId();
            info.set(X509CertInfo.ALGORITHM_ID,
                     new CertificateAlgorithmId(algID));
            info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(myname));
            info.set(X509CertInfo.KEY, new CertificateX509Key(publicKey));
            info.set(X509CertInfo.VALIDITY, interval);
            info.set(X509CertInfo.ISSUER,
                     new CertificateIssuerName(issuer.getSigner()));
            
            // Start BD-J specific.  Adding Extensions
            CertificateExtensions extensions = new CertificateExtensions();
            KeyUsageExtension keyUsage = new KeyUsageExtension();

            // It's OK to set CERTSIGN blindly for all generated self certs,
            // since the leaf cert will be replaced with a Certificate response importing 
            // later on, with KeyUsageExtension.DIGITAL_SIGNATURE turned on.
            keyUsage.set(KeyUsageExtension.KEY_CERTSIGN, Boolean.TRUE);
            
            extensions.set(KeyUsageExtension.NAME, keyUsage);
            GeneralName name1 = new GeneralName(new RFC822Name("email" + (count++) + "@abc.com"));
            GeneralNames names1 = new GeneralNames();
            names1.add(name1);
            IssuerAlternativeNameExtension altName = new IssuerAlternativeNameExtension(names1);          
            extensions.set(IssuerAlternativeNameExtension.NAME, altName);           
            
            GeneralName name2 = new GeneralName(new RFC822Name("email" + (count++) + "@def.com"));  
            GeneralNames names2 = new GeneralNames();
            names2.add(name2);
            SubjectAlternativeNameExtension subName = new SubjectAlternativeNameExtension(names2);
            extensions.set(SubjectAlternativeNameExtension.NAME, subName);
            
            info.set(X509CertInfo.EXTENSIONS, extensions);
            // End BD-J specific

	    cert = new X509CertImpl(info);
	    cert.sign(privateKey, this.sigAlg);

	    return (X509Certificate)cert;

	} catch (IOException e) {
             e.printStackTrace();
             throw new CertificateEncodingException("getSelfCert: " +
                                                    e.getMessage());
	}
    }
    // BD-J specific:
    // A silly way of making each GeneralName "unique" for each certificate.
    // It would be much nicer if there is a way to specify extension values from the 
    // keytool command line.
    private static int count = 0;

    /**
     * Returns a PKCS #10 certificate request.  The caller uses either
     * <code>PKCS10.print</code> or <code>PKCS10.toByteArray</code>
     * operations on the result, to get the request in an appropriate
     * transmission format.
     *
     * <P>PKCS #10 certificate requests are sent, along with some proof
     * of identity, to Certificate Authorities (CAs) which then issue
     * X.509 public key certificates.
     *
     * @param myname X.500 name of the subject
     * @exception InvalidKeyException on key handling errors.
     * @exception SignatureException on signature handling errors.
     */
    public PKCS10 getCertRequest (X500Name myname)
    throws InvalidKeyException, SignatureException
    {
	PKCS10	req = new PKCS10 (publicKey);

	try {
	    req.encodeAndSign (getSigner (myname));

	} catch (CertificateException e) {
	    throw new SignatureException (sigAlg + " CertificateException");

	} catch (IOException e) {
	    throw new SignatureException (sigAlg + " IOException");

	} catch (NoSuchAlgorithmException e) {
	    // "can't happen"
	    throw new SignatureException (sigAlg + " unavailable?");
	}
	return req;
    }

    private X500Signer getSigner (X500Name me)
    throws InvalidKeyException, NoSuchAlgorithmException
    {
	Signature signature = Signature.getInstance(sigAlg);

	// XXX should have a way to pass prng to the signature
	// algorithm ... appropriate for DSS/DSA, not RSA

	signature.initSign (privateKey);
	return new X500Signer (signature, me);
    }
    
    private SecureRandom	prng;
    private String		sigAlg;
    private KeyPairGenerator	keyGen;
    private PublicKey		publicKey;
    private PrivateKey		privateKey;
}

This project contains the modification to the jdk JarSigner and some jdk 1.6 sun.security files.   
It is meant for generating certificates and signing xlets.  

========================

How to use this tool:

 * BJSigner syntax is : BDSigner [-debug] 8-digit-hex-organization-ID jarfile1 jarfile2 ...
 * 
 * Example: java -Xbootclasspath/p:bdsigner-bootclasspath.jar -cp $BDSIGNER_HOME/build/bdsigner.jar:$JDK_HOME/lib/tools.jar:$BDSIGNER_HOME/resource/bcprov-jdk15-137.jar net.java.bd.tools.bdsigner.BDSigner 56789abc 00000.jar 
 * 
 * Make sure to put bdsigner.jar before tools.jar in the jdk distribution for the jre 
 * classpath, and to prepend bdsigner-bootclasspath.jar to the jre bootclasspath, so that 
 * the modified version of the sun.security.* classes in this BDSigner respository
 * are used during the run.  bdprov-jdk15-137.jar is a bouncycastle distribution; a copy can be bound at "resources" dir.

This tool also generates a corresponding root certificate file after signing the xlets.
Place the generated file, app.discroot.crt, in the BDMV's CERTIFICATE directory.

========================

This is "bdsigner" tool to sign BD-J jar files. Why can't we
just use the standard "jarsigner" tool in Java SE?

Blu-Ray specification requires an extra header in the signature 
manifest file in signed jar files.  The signature manifest file 
must have an attribute called "BDJ-Signature-Version" with the 
value of "1.0". Please refer to the section "12.1.4.2" of the
"System Description Blu-Ray Disc Read-Only Format - Part 3
Audio Visual Basic Specifications" - DRAFT Version 2.02".

The "jarsigner" tool in Java SE generates signature manifest 
file automatically. There is no way to specify extensions to 
the signature manifest file. So, it can not be used to sign
jars for BD-J. 

Unfortunately, the jarsigner code is structured in such a way 
that it can not be extended by subclassing :-( In fact, it is 
expected that users will *not* extend this tool - and hence the
classes are kept in internal package named "sun.security.tools". 

Temporary "hack":

* The JarSigner is is taken from JDK workspace to create 
this small project.

* This JarSigner includes BD-J mandated header in 
signature manifest file. Please refer to comments with the 
header "BLU-RAY SPECIFIC:"

* Not all sources needed for the jarsigner tool are here. 
So, you need to put "tools.jar" from JDK in CLASSPATH. And 
the classpath needs to be prefixed with "bdsigner.jar" built
from this project - so that the modified JarSigner class will
be loaded instead of the one in tools.jar. See also the scripts
under the "bin" directory.

* Because we depend on (internal and implementation) classes
from the "tools.jar" of JDK, chances are that this tool may
*not* work with any other JDK version other than Sun's JDK implementation.

========================

The JarSigner is now also capable of signing certificates 
with -issuecert" option.
This tool uses the following open source libraries:

* Bouncy Castle Provider - http://www.bouncycastle.org/
- We use release 137 built for JDK 5.0.
* BASE-64 decoder - http://iharder.sourceforge.net/base64/
- This may be probably be replaced with "sun.misc.BASE64Decoder".

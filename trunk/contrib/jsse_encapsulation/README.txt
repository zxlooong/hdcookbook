JSSE API Wrapper Library
========================

This library defines the wrapper classes to access JSSE API without
static linking.


I. Purpose of the library
-------------------------

The JSSE API library is optional part on a BD-ROM player.

The Java VM specification permits different dependency resolution
strategies:

 "The Java programming language allows an implementation flexibility
 as to when linking activities (and, because of recursion, loading)
 take place, provided that the semantics of the language are
 respected, that a class or interface is completely verified and
 prepared before it is initialized"

Whereas some virtual machine implementations resolve these symbolic
references only when they are used for the first time, others resolve
all symbolic references during application startup. In the latter case
the dependency resolution occurs before any application code is
executed and since an error is thrown, the application is never
started. Thus the application does not get control and has no way to
react and handle NoClassDefFoundError or other LinkageErrors
appropriately.


This library is designed to resolve the issue above. The library
provide wrapper classes for JSSE functionality.  The wrapper classes
use reflection instead of the direct access to invoke the methods in
the JSSE API.

If the JSSE API is not presented on card, then
UnsupportedOperationException is thrown during the method or
constructor invocation.

The library guarantee that NoClassDefFoundError or other LinkageErrors
are never thrown of the JSSE API is absent.

II. Library Overview
--------------------

The librray defines the classes with the same names and with the same
methods name as JSSE API, but it is defined in the separate
package. In most cases you only need replace the imports in your
application from javax.net and javax.net.ssl to the
com.sun.blurayx.jsse and com.hdcookbook.contrib.jsse accordingly.

Every time when you are trying to access functionality which is absent
underneath, the UnsupportedOperationException is thrown.

III. Library limitations
------------------------

The library defined almost complete set of the JSSE functionality with
the following exceptions:

1. Accessing of the fields from JSSE API is not supported unles it is
supported by th methods in JSSE API.

2. Protected API and API extending is not supported.

The encapsulation has own limitation. and you can not create a
subclass of the class or implement the interface without static links
to the class or interface. The changes of the access from the
protected to the public (in subclass) may breaks access control
principles, which were put in the JSSE API design and it is too risky
without case by case basis evaluation.

Also miximg of the inheritance ans encapsulation in one place without
case by case basis evaluation is too risky too.
Therefore we do not support protected JSSE API support and creation of
the subclasses and subinterfaces, except, the places where it was
designed in JSSE API.

You can create only instances of the following interfaces:

 - com.hdcookbook.contrib.jsse.HandshakeCompletedListener
 - com.hdcookbook.contrib.jsse.HostnameVerifier
 - com.hdcookbook.contrib.jsse.SSLSessionBindingListener
 - com.hdcookbook.contrib.jsse.X509KeyManager
 - com.hdcookbook.contrib.jsse.X509TrustManager

3. SPI classes are not supported.

The SPI classes are used for creation of the own JSSE components and
they are rarely used by the application developers.
The SPI classes are:
  - javax.net.ssl.KeyManagerFactorySpi
  - javax.net.ssl.SSLContextSpi
  - javax.net.ssl.TrustManagerFactorySpi

4. Custom javax.net.ssl.TrustManager, javax.net.ssl.KeyManager are not supported.

These interfaces are tag interfaces and they have no methods.
The library uses encapsulation to prevent linking problem, therefore
the inheritance structure other than defined in JSSE API is
lost. Therefore when you are using this library you can not create
custom sub-interface of the TrustManager and KeyManager, which use some
algorithms specific management.

But you still can create and use own implementations of the
X509TrustManager and X509KeyManager and instances of the TrustManager
and KeyManager created by TrustManagerFactories and
KeyManagerFactories provided by the JSSE implementation underneath.

Note that custom Managers can be used together with SSLContext
implementations, and they are rarely used by the application
developers. The creation of the X509KeyManager and X509TrustManager
instances should be enough.

5. javax.net.ssl.ManagerFactoryParameters is not supported.

The reason for unsupporting of the ManagerFactoryParameters is the
same as for unsupporting of the custom TrustManagers. The only
difference is that the JSSE API does not provide mechanisms for
ManagerFactoryParameters instantiation.  Therefore the wrapper for
ManagerFactoryParameters is not included to the current library.

6. javax.net.ssl.SSLPermission is not supported.

7. Subsetting of the following classes is not supported:

 - javax.net.ssl.SSLSocket
 - javax.net.ssl.SSLServerSocket
 - javax.net.ssl.SSLSession
 - javax.net.ssl.SSLContext

8. Limited support of the exception defined in the javax.net.ssl package.

The library defines wrappers for the exceptions, but most of these
exception are subclasses of the IOException and they are thrown during
I/O operations.  Therefore do not differentiate these exception and
IOException if the exception is not declared in the throws
clause. Only the following methods can throw wrapper exceptions:

  - javax.net.ssl.HttpsURLConnection.getServerCertificates()
  - javax.net.ssl.SSLSession.getPeerCertificates()
  - javax.net.ssl.SSLSession.getPeerCertificateChain()
  - javax.net.ssl.HandshakeCompletedEvent.getPeerCertificates()
  - javax.net.ssl.HandshakeCompletedEvent.getPeerCertificateChain()

9. javax.net.ssl.HttpsURLConnection is supported, but you need create
a wrapper class by yourself.

javax.net.ssl.HttpsURLConnection is created via classes, which are bot
part of the javax.net.ssl package.  The HttpsURLConnection is create
either via java.net.URL.openConnection() or via
javax.microedition.io.Connector.

In both cases the instance of the javax.net.ssl.HttpsURLConnection is
created. It does cause any linking problem unless you cat it to the
javax.net.ssl.HttpsURLConnection to access HttpsURLConnection
functionality.

This behavior is not controlled by the library.

To avoid the casting to the javax.net.ssl.HttpsURLConnection, you can
use create a instance com.hdcookbook.contrib.jsse.HttpsURLConnection
passing javax.net.ssl.HttpsURLConnection as constructor parameter and
access javax.net.ssl.HttpsURLConnection using
com.hdcookbook.contrib.jsse.HttpsURLConnection methods.

JSSE 1.0.3 Support
------------------

The JSSE 1.0.3 is subset of the Security OP JSSE 1.0 byt because the
library implements full set of the Security OP JSSE 1.0 your
application compilation can pass even if your are using API, which is
not defined in the JSSE 1.0.3. As result these methods will throw
UnsupportedOperationException at run-time.

In futurewe are going to create JSSE 1.0.3 wrapper library for the
application, whihc want to use JSSE 1.0.3 API only.

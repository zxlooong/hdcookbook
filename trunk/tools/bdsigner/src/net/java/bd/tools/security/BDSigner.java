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

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import static net.java.bd.tools.security.SecurityUtil.*;

/**
 * 
 * BJSigner syntax is : BDSigner [-debug] -orgid 8-digit-hex-organization-ID jar-files 
 * 
 * Example: java -cp $SECURITY_HOME/build/security.jar:$JDK_HOME/lib/tools.jar:$SECURITY_HOME/resource/bcprov-jdk15-137.jar net.java.bd.tools.security.BDSigner -orgid 56789abc 00000.jar 
 * 
 * Make sure to put security.jar before tools.jar in the jdk distribution for the jre 
 * classpath so that the modified version of the sun.security.* classes in this BDSigner respository
 * are used during the run.  bdprov-jdk15-137.jar is a bouncycastle distribution; a copy can be bound at "resources" dir.
 * 
 */
public class BDSigner {

    static String keystoreFile = DEFAULT_KEYSTORE_FILE;
    static String keystorePassword = DEFAULT_KEYSTORE_PASSWORD;
    static String appCertAlias = DEFAULT_APPCERT_ALIAS;
    static String appKeyPassword = DEFAULT_APPKEY_PASSWORD;
    static boolean debug = false;
    static boolean newKeys = true;
    static String orgId;
    static List<String> jarfiles;
    
    public static void main(String[] args) {
	    
	    // Parse the argments
	    parseArgs(args);
            SecurityUtil util;
	    if (newKeys) {
               if (orgId == null) {
                   printUsageAndExit("Bad OrgID " + orgId + ", please provide an 8 digit hex.");
               }
               util = new SecurityUtil(orgId, debug, jarfiles);
            } else {
               util = new SecurityUtil(keystoreFile,
                                      keystorePassword,
                                      appCertAlias,
                                      appKeyPassword,
                                      orgId,
                                      debug,
                                      jarfiles);
            }
            util.signJars();
    }
    
     public static void parseArgs(String args[]) {
        if (args.length < 2) {
            printUsageAndExit("No arguments specified");
        }
        jarfiles = new ArrayList<String>();
        for(int i = 0; i < args.length; i++) {
            String opt = args[i];
            if (opt.equals("-keystore")) {
                if (++i == args.length) errorNeedArgument(opt);
                keystoreFile = args[i];
                newKeys = false;
            } else if (opt.equals("-storepass")) {
                 if (++i == args.length) errorNeedArgument(opt);
                keystorePassword = args[i];
            } else if (opt.equals("-appkeyalias")) {
               if (++i == args.length) errorNeedArgument(opt);
                appCertAlias = args[i];
            } else if (opt.equals("-appkeypass")) {
               if (++i == args.length) errorNeedArgument(opt);
               appKeyPassword = args[i];
            } else if (opt.equals("-orgid")) {
                 if (++i == args.length) errorNeedArgument(opt);
                 orgId = args[i];
            	 if (orgId.length()!= 8) {
		    printUsageAndExit("Bad OrgID " + orgId + ", please provide an 8 digit hex.");
                 }
            } else if (opt.equals("-help")) {
                printUsageAndExit("");
            } else if (opt.equals("-debug")) {
                debug = true;
                System.out.println ("Debugging : " + debug);   
            } else {
	          jarfiles.add(args[i]);
	          if (!new File(args[i]).exists()) {
		    printUsageAndExit("File " + args[i] + " not found.");
                  } 
             } 
        }
    }
     
     static private void tinyHelp() {
        System.err.println("Try BDSigner -help");
                
        // do not drown user with the help lines.
        System.exit(1);
    }
    
    static private void errorNeedArgument(String flag) {
        System.err.println("Command option <flag> needs an argument.");
        tinyHelp();
    }
    
    private static void printUsageAndExit(String reason) {
	 System.err.println("\n==============================\n");
         if (!reason.isEmpty())
	     System.err.println("Failed: " + reason);
	 System.err.println("\n==============================\n");
	 System.err.println("This is a tool to sign jar files according to the bd-j specification.\n");
         System.err.println("To sign the jar using the existing keystore use -keystore option;");
         System.err.println("otherwise this tool generates new keys and certificates that are");
         System.err.println("complaint with the bd-j spec");
	 System.err.println("BDSigner Syntax:");
	 System.err.println(
		 "\t     [-keystore <keystorefile>] [-storepass <password>]");
         System.err.println(
		 "\t     [-appkeyalias <alias>] [-appkeypass <password>]");
         System.err.println(
                 "\t     [-debug]");
         System.err.println(
                 "\t     -orgid <8-digit-hex-organization-ID> jarfiles\n");
         
         System.err.println("Example: java -cp security.jar:tools.jar:bcprov-jdk15-137.jar net.java.bd.tools.security.BDSigner -orgid 56789abc 00000.jar\n");
	 System.err.println("\n==============================\n");
         System.exit(1);
    }
}

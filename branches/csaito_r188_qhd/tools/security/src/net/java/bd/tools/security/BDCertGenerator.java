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

/** 
 * BDCertGenerator syntax is : BDCertGenerator [-debug] -orgid 8-digit-hex-organization-ID
 * 
 * Example: java -cp $SECURITY_HOME/build/security.jar:$JDK_HOME/lib/tools.jar:$SECURITY_HOME/resource/bcprov-jdk15-137.jar net.java.bd.tools.signer.BDCertGenerator -orgid 56789abc 00000.jar 
 * 
 * Make sure to put security.jar before tools.jar in the jdk distribution for
 * the jre classpath so that the modified version of the sun.security.* classes
 * in this BDCertGenerator respository are used during the run.
 * bdprov-jdk15-137.jar is a bouncycastle distribution; a copy can be found in
 * the "resources" dir.
 */
public class BDCertGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            printUsageAndExit("Please enter an orgid");
        }
        SecurityUtil.Builder builder = new SecurityUtil.Builder();
        for (int i = 0; i < args.length; i++) {
            String opt = args[i];
            if (opt.equals("-keystore")) {
                if (++i == args.length) {
                    errorNeedArgument(opt);
                }
                builder = builder.keystoreFile(args[i]);
            } else if (opt.equals("-rootdn")) {
                 if (++i == args.length) {
                    errorNeedArgument(opt);
                 }
                 builder = builder.rootDN(args[i]);
            } else if (opt.equals("-appdn")) {
                 if (++i == args.length) {
                    errorNeedArgument(opt);
                 }
                 builder = builder.appDN(args[i]);
            } else if (opt.equals("-rootaltname")) {
                 if (++i == args.length) {
                    errorNeedArgument(opt);
                 }
                 builder = builder.rootAltName(args[i]);
            } else if (opt.equals("-appaltname")) {
                 if (++i == args.length) {
                    errorNeedArgument(opt);
                 }
                 builder = builder.appAltName(args[i]);
            } else if (opt.equals("-help")) {
                printUsageAndExit("");
            } else if (opt.equals("-debug")) {
                builder = builder.debug();
            } else {
                if (args[i].length() != 8) {
                    printUsageAndExit("Bad OrgID " + args[i] + ", please provide an 8 digit hex.");
                }
                builder = builder.orgId(args[i]);
            }          
        }
        SecurityUtil util = builder.build();
        util.createCertificates();
    }

    static private void tinyHelp() {
        System.err.println("Try BDCertGenerator -help");

        // do not drown user with the help lines.
        System.exit(1);
    }

    static private void errorNeedArgument(String flag) {
        System.err.println("Command option <flag> needs an argument.");
        tinyHelp();
    }

    private static void printUsageAndExit(String reason) {
        if (!reason.isEmpty()) {
            System.err.println("\nFailed: " + reason + "\n");
        }
        System.err.println("***This tool generates keystore/certificates for securing BD-J applications***\n");
        System.err.println("usage: BDCertGenerator [options] organization_id\n");
        System.err.println("Valid Options:");
        System.err.println(" -keystore filename \t:Create a keystore file with the given filename");
        System.err.println(" -rootdn name       \t:Distinguished name of the root certificate");
        System.err.println(" -appdn  name       \t:Distinguished name of the application certificate");
        System.err.println("                    \t Note: The organization_id is appended to the org name");
        System.err.println("                    \t component of both names");
        System.err.println(" -rootaltname name  \t:Subject alternate name for the root certificate");
        System.err.println(" -appaltname name   \t:Subject alternate name for the application certificate");
        System.err.println(" -debug             \t:Prints debug messages");
        System.err.println(" -help              \t:Prints this message");
        System.err.println("\nExample: java -cp security.jar:tools.jar:bcprov-jdk15-137.jar net.java.bd.tools.security.BDCertGenerator 56789abc \n");
        System.exit(1);
    }
}

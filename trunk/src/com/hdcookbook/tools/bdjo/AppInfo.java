
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

 package com.hdcookbook.tools.bdjo;


/**
 * ApplicationManagementTable syntax - section 10.2.5.2
 * @author A. Sundararajan
 */
public class AppInfo {
    // Table 10.5 - Application Types
    public static final byte MAX_APPLICATION_TYPE = 0xF;
    // Table 10.5 - BD-J application 
    public static final byte APPLICATION_TYPE_BD_J = 0x1;
    // Table 10.6 - BD-J application control codes
    public static final byte CONTROL_CODE_AUTOSTART = 0x1;
    public static final byte CONTROL_CODE_PRESENT = 0x2;
    
    private byte controlCode;
    private byte type;
    private int organizationId;
    private short applicationId;
    private ApplicationDescriptor applicationDescriptor;

    @HexFormat
    public byte getControlCode() {
        return controlCode;
    }

    public void setControlCode(byte controlCode) {
        this.controlCode = controlCode;
    }

    @HexFormat
    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        if (type < 0 || type > MAX_APPLICATION_TYPE) {
            throw new IllegalArgumentException("invalid type : " + type);
        }
        this.type = type;
    }

    @HexFormat
    public int getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(int organizationId) {
        this.organizationId = organizationId;
    }

    @HexFormat
    public short getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(short applicationId) {
        this.applicationId = applicationId;
    }

    public ApplicationDescriptor getApplicationDescriptor() {
        return applicationDescriptor;
    }

    public void setApplicationDescriptor(ApplicationDescriptor applicationDescriptor) {
        this.applicationDescriptor = applicationDescriptor;
    }
}

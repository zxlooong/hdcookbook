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

package com.hdcookbook.grin.io.binary;

/**
 * Defines constants used for the binary format of the Show file.
 */

class Constants {
 
	static final int GRINSCRIPT_IDENTIFIER = 0xc00cb00c;
	static final int GRINSCRIPT_VERSION = 12;
	
	static final int FEATURE_IDENTIFIER= 0x00000fea;
	static final int SEGMENT_IDENTIFIER= 0x00000ce6;
        static final int RCHANDLER_IDENTIFIER=0x000008ee;
        static final int PUBLIC_ELEMENTS_IDENTIFIER=0x0000040b;
	
	static final byte ASSEMBLY_IDENTIFIER= 0x01;
	static final byte BOX_IDENTIFIER = 0x02;
	static final byte FIXEDIMAGE_IDENTIFIER = 0x03; 
	static final byte GROUP_IDENTIFIER = 0x04;
	static final byte IMAGESEQUENCE_IDENTIFIER = 0x05;
	static final byte TEXT_IDENTIFIER = 0x06;
	static final byte INTERPOLATED_MODEL_IDENTIFIER = 0x08;
	static final byte TRANSLATOR_IDENTIFIER = 0x09;
	static final byte CLIPPED_IDENTIFIER = 0x0a;
	static final byte FADE_IDENTIFIER = 0x0b;
	static final byte SRCOVER_IDENTIFIER = 0x0c;
        static final byte USER_MODIFIER_IDENTIFIER = 0x0d;        
        static final byte USER_FEATURE_IDENTIFIER = 0x0e;
        
        static final byte ACTIVATEPART_CMD_IDENTIFIER = 0x10;
        static final byte ACTIVATESEGMENT_CMD_IDENTIFIER = 0x11;
        static final byte SEGMENTDONE_CMD_IDENTIFIER = 0x12;
        static final byte SETVISUALRCSTATE_CMD_IDENTIFIER = 0x13;
        static final byte USER_CMD_IDENTIFIER = 0x14;
        static final byte SHOW_COMMANDS_CMD_IDENTIFIER = 0x15;
        
        static final byte COMMAND_RCHANDLER_IDENTIFIER = 0x20;
        static final byte VISUAL_RCHANDLER_IDENTIFIER = 0x21;
        static final byte USER_RCHANDLER_IDENTIFIER = 0x22;
        static final byte GUARANTEE_FILL_IDENTIFIER = 0x23;
	static final byte SET_TARGET_IDENTIFIER = 0x24;
        
        static final byte NULL = (byte) 0xff;
        static final byte NON_NULL = (byte) 0xee;
   
}	

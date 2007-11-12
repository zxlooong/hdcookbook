/*
 * Constants.java
 */

package com.hdcookbook.grin.binaryconverter;

public class Constants {
 
	public static final int GRINSCRIPT_IDENTIFIER = 0xc00cb00c;
	public static final int GRINSCRIPT_VERSION = 1;
	
	public static final int FEATURE_IDENTIFIER= 0x00000fea;
	public static final int SEGMENT_IDENTIFIER= 0x00000ce6;
	public static final int COMMAND_IDENTIFIER= 0x00000c3d;
	
	public static final byte ASSEMBLY_IDENTIFIER= 0x01;
	public static final byte BOX_IDENTIFIER = 0x02;
	public static final byte FIXEDIMAGE_IDENTIFIER = 0x03; 
	public static final byte GROUP_IDENTIFIER = 0x04;
	public static final byte IMAGESEQUENCE_IDENTIFIER = 0x05;
	public static final byte TEXT_IDENTIFIER = 0x06;
	public static final byte TIMER_IDENTIFIER = 0x07;
	public static final byte TRANSLATION_IDENTIFIER = 0x08;
	public static final byte TRANSLATOR_IDENTIFIER = 0x09;
	public static final byte CLIPPED_IDENTIFIER = 0x0a;
	public static final byte FADE_IDENTIFIER = 0x0b;
	public static final byte SRCOVER_IDENTIFIER = 0x0c;
        public static final byte USER_MODIFIER_IDENTIFIER = 0x0d;
        
        public static final byte ACTIVATEPART_CMD_IDENTIFIER = 0x10;
        public static final byte ACTIVATESEGMENT_CMD_IDENTIFIER = 0x11;
        public static final byte SEGMENTDONE_CMD_IDENTIFIER = 0x12;
        public static final byte SETVISUALRCSTATE_CMD_IDENTIFIER = 0x13;
        public static final byte USER_CMD_IDENTIFIER = 0x14; 

        
        public static final byte NULL = (byte) 0xff;
        public static final byte NON_NULL = (byte) 0xee;
   
}	

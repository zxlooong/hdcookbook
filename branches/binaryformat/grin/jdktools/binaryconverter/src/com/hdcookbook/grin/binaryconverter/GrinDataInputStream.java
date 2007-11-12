/*
 * GrinObjectOutputStream.java
 */

package com.hdcookbook.grin.binaryconverter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Just a convenience DataInputStream subclass that knows how to handle certain classes
 */
public class GrinDataInputStream extends DataInputStream {
   
   public GrinDataInputStream(InputStream in) {
       super(in);
   }
   
   public Color readColor() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       int red = readInt();
       int green = readInt();
       int blue = readInt();
       int alpha = readInt();
       return new Color(red, green, blue, alpha);
   }
   
   public Rectangle readRectangle() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       double x = readDouble();
       double y = readDouble();
       double w = readDouble();
       double h = readDouble();
       return new Rectangle((int)x,(int)y,(int)w,(int)h);
   }
   
   public Font readFont() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       String name = readUTF();
       int style = readInt();
       int size = readInt();
       return new Font(name, style, size);
   }  
   
}

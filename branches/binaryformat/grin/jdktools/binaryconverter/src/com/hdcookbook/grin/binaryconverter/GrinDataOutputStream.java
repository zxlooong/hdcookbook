/*
 * GrinObjectOutputStream.java
 */

package com.hdcookbook.grin.binaryconverter;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Just a convenience DataOutputStream subclass that knows how to handle certain classes
 */
public class GrinDataOutputStream extends DataOutputStream {
   
   /** Creates a new instance of GrinObjectOutputStream */
   public GrinDataOutputStream(OutputStream out) {
      super(out);
   }
   
   public void writeColor(Color color) throws IOException {
       if (color == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(color.getRed());
           writeInt(color.getGreen());
           writeInt(color.getBlue());
           writeInt(color.getAlpha()); 
       }    
   }
   
   public void writeRectangle(Rectangle rect) throws IOException {
       if (rect == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);   
           writeDouble(rect.getX());
           writeDouble(rect.getY());
           writeDouble(rect.getWidth());
           writeDouble(rect.getHeight());
       }
   }
   
   public void writeFont(Font font) throws IOException {
       if (font == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeUTF(font.getName());
           writeInt(font.getStyle());
           writeInt(font.getSize());   
       }    
   }  
   
}

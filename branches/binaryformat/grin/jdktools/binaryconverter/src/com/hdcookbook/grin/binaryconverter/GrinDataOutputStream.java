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
 * Just a convenience DataOutputStream subclass that can handle certain Objects.
 */
public class GrinDataOutputStream extends DataOutputStream {
   
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
   
   public void writeRectangleArray(Rectangle[] array) throws IOException {
       if (array == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(array.length);
           for (int i = 0; i < array.length; i++) {
               writeRectangle(array[i]);
           }    
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
   
   public void writeIntArray(int[] array) throws IOException {
       if (array == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(array.length);
           for (int i = 0; i < array.length; i++) {
               writeInt(array[i]);
           }    
       }
   }   
   
   public void writeStringArray(String[] array) throws IOException {
       if (array == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           writeInt(array.length);
           for (int i = 0; i < array.length; i++) {
               writeUTF(array[i]);
           }
       }
   }
   
   public void writeInt2Array(int[][] array) throws IOException {
       if (array == null) {
           writeByte(Constants.NULL);
       } else {
           writeByte(Constants.NON_NULL);
           int length = array.length;
           writeInt(length);
           for (int i = 0; i < length; i++) {
               writeIntArray(array[i]);
           }
       }
   }   
}

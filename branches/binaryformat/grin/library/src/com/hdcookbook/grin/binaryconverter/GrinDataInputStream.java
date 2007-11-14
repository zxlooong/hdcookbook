/*
 * GrinObjectOutputStream.java
 */

package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.binaryconverter.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Just a convenience DataInputStream subclass to handle certain Objects.
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
   
   public Rectangle[] readRectangleArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       Rectangle[] array = new Rectangle[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readRectangle();
       }
       return array;       
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
   
   
   public int[] readIntArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       int[] array = new int[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readInt();
       }
       return array;
   }   

      
   public String[] readStringArray() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
       
       String[] array = new String[readInt()];
       for (int i = 0; i < array.length; i++) {
           array[i] = readUTF();
       }
       return array;
   }
   
   
   public int[][] readInt2Array() throws IOException {
       byte b = readByte();
       if (b == Constants.NULL) {
           return null;
       }
 
       int[][] array = new int[readInt()][];
       
       for (int i = 0; i < array.length; i++) {
           array[i] = readIntArray();
       }
       return array;
   }   
}

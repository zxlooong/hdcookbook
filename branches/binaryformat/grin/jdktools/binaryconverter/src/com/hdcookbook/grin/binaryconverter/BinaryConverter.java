
package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Timer;
import com.hdcookbook.grin.features.Translation;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.parser.ExtensionsParser;
import com.hdcookbook.grin.parser.ShowParser;
import com.hdcookbook.grin.util.AssetFinder;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

/* Just a test driver */

public class BinaryConverter {
   
   public static void main(String[] args) {
       
        if (args == null || args.length == 0) {
            System.out.println("Missing argument - need grin script to parse");
            return;
        }
        
        String filename = args[0];
        
        GenericDirector director = new GenericDirector(filename);
	AssetFinder.setSearchPath(new String[]{""}, new File[]{new File("."), new File("")});
  
        Show show = director.createShow(null);
        if (filename.indexOf('.') != -1) {
           filename = filename.substring(0, filename.indexOf('.'));
        }   
        new BinaryConverter(show, filename + ".grin");
   }

   public BinaryConverter(Show show, String filename) {
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
            GrinBinaryWriter out = new GrinBinaryWriter(show);
	    out.writeShow(dos);
            dos.close();
            
            return;
            
            //GrinBinaryReader reader = new GrinBinaryReader(new GenericDirector(""), filename);
            //Show recreatedShow = reader.readShow();            
            
        } catch (IOException e) { 
            e.printStackTrace();

        } 
        // failed on writing, delete file
        try {
            new File(filename).delete();
        } catch (Exception e) {/* Oh well */}
   }
}     

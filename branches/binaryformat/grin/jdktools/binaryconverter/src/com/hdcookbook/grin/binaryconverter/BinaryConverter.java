
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

public class BinaryConverter {
	
   public static final String showfile = "menu";
   
   public static void main(String[] args) {
        GenericDirector director = new GenericDirector(showfile + ".txt");
	AssetFinder.setSearchPath(new String[]{""}, null);
        Show show = director.createShow(null);
        new BinaryConverter(show, showfile);
   }

   public BinaryConverter(Show show, String name) {
        String filename = name + ".grin";
        try {
            DataOutputStream dos = new DataOutputStream(new FileOutputStream(filename));
            GrinBinaryWriter out = new GrinBinaryWriter(show);
            
            Feature[] original = show.getFeaturesAsArray();
            
	    out.writeScriptIdentifier(dos);
	    out.writeShow(dos);
            dos.close();
            
            Show recreatedShow = new Show(new GenericDirector(""));
            DataInputStream dis = new DataInputStream(new FileInputStream(filename));
            GrinBinaryReader reader = new GrinBinaryReader(show);
            reader.readScriptIdentifier(dis);
            Feature[] retrieved = reader.readFeatures(dis);
            
            for (int i = 0; i < retrieved.length; i++) {
                System.out.println(i + " : " + retrieved[i]);
            }
            
            dis.close();
            
        } catch (IOException e) { 
            e.printStackTrace();

        } 
        // failed on writing, delete file
        try {
            new File(filename).delete();
        } catch (Exception e) {/* Oh well */}
   }

}     

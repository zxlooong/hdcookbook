
package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.commands.SegmentDoneCommand;
import com.hdcookbook.grin.commands.SetVisualRCStateCommand;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.Clipped;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Timer;
import com.hdcookbook.grin.features.Translation;
import com.hdcookbook.grin.features.Translator;
import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;

import java.io.OutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class GrinBinaryWriter {

    private Show show;
    private ArrayList featuresList;
    
    public GrinBinaryWriter(Show show) {
        this.show = show;
       
        Feature[] features = show.getFeaturesAsArray();
        featuresList = new ArrayList(features.length);
        for (int i = 0; i < features.length; i++) {
            featuresList.add(features[i]);
        }  
    }

    protected static void writeScriptIdentifier(DataOutputStream out) throws IOException {
        out.writeInt(Constants.GRINSCRIPT_IDENTIFIER);
        out.writeInt(Constants.GRINSCRIPT_VERSION);
    }
 
    protected void writeShow(DataOutputStream out) throws IOException {
        writeFeatures(out, (Feature[])featuresList.toArray(new Feature[]{}));        
    }
    
    protected void writeFeatures(DataOutputStream out, Feature[] features) 
       throws IOException {
	
        if (features == null || features.length == 0) 
            return;
        
	// First, write out two integers - identifier and number of elements.
        out.writeInt(Constants.FEATURE_IDENTIFIER);
        out.writeInt(features.length);
		
        for (int i = 0; i < features.length; i++) {
            Feature feature = features[i];
            if (feature instanceof Assembly) {
                writeAssembly(out, (Assembly)feature);
            } else if (feature instanceof Box) {
                writeBox(out, (Box)feature);
	    } else if (feature instanceof Clipped) {
                writeClipped(out, (Clipped)feature);
	    } else if (feature instanceof Fade) {
                writeFade(out, (Fade)feature);		
            } else if (feature instanceof FixedImage) {
                writeFixedImage(out, (FixedImage)feature);
            } else if (feature instanceof Group) {
                writeGroup(out, (Group)feature);
            } else if (feature instanceof ImageSequence) {
                writeImageSequence(out, (ImageSequence)feature);
            } else if (feature instanceof Text) {
                writeText(out, (Text)feature);
            } else if (feature instanceof Timer) {
                writeTimer(out, (Timer)feature);
            } else if (feature instanceof Translation) {
                writeTranslation(out, (Translation)feature);
            } else if (feature instanceof Translator) {
                writeTranslator(out, (Translator)feature);
            } else if (feature instanceof SrcOver) {
                writeSrcOver(out, (SrcOver)feature);
            } else if (feature instanceof Modifier) {
                writeUserModifier(out, (Modifier)feature);
            } else {
                throw new IOException("Unknown feature " + feature);
            }
	    
        }
   }
       
    public void writeAssembly(DataOutputStream out, Assembly assembly) throws IOException {
        
        out.writeByte((int)Constants.ASSEMBLY_IDENTIFIER);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);
        
        dos.writeUTF(assembly.getName());
        String[] partNames = assembly.getPartNames();
        Feature[] parts = assembly.getParts();
        
        dos.writeInt(partNames.length);
        for (int i = 0; i < partNames.length; i++) {
            dos.writeUTF(partNames[i]);
        }
        dos.writeInt(parts.length);
        for (int i = 0; i < parts.length; i++) {
            dos.writeInt(featuresList.indexOf(parts[i]));
        }
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();
    }

    public void writeBox(DataOutputStream out, Box box) throws IOException {
 
       out.writeByte((int)Constants.BOX_IDENTIFIER);
             
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       dos.writeUTF(box.getName());
       dos.writeRectangle(box.getPlacement());
       dos.writeInt(box.getOutlineWidth());
       dos.writeColor(box.getOutlineColor());
       dos.writeColor(box.getFillColor());
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
    }
    
    public void writeClipped(DataOutputStream out, Clipped clipped) throws IOException {
       
       out.writeByte((int)Constants.CLIPPED_IDENTIFIER);
       
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       Rectangle rect = clipped.getClipRegion();      
       dos.writeUTF(clipped.getName());
       dos.writeRectangle(rect);
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
    }
    
    public void writeFade(DataOutputStream out, Fade fade) throws IOException {
       
       out.writeByte((int)Constants.FADE_IDENTIFIER);
      
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       DataOutputStream dos = new DataOutputStream(baos);

       dos.writeUTF(fade.getName());
       dos.writeBoolean(fade.getSrcOver());
       int[] keyframes = fade.getKeyframes();
       dos.writeInt(keyframes.length);
       for (int i = 0; i < keyframes.length; i++) {
          dos.writeInt(keyframes[i]);
       }
       int[] keyAlphas = fade.getKeyAlphas();
       dos.writeInt(keyAlphas.length);
       for (int i = 0; i < keyAlphas.length; i++) {
          dos.writeInt(keyAlphas[i]);
       }      
       Command[] endCommands = fade.getEndCommands();
       writeCommands(dos, endCommands);
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
       
    }

    public void writeFixedImage(DataOutputStream out, FixedImage image) throws IOException {
       
       out.writeByte((int)Constants.FIXEDIMAGE_IDENTIFIER);
       
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       DataOutputStream dos = new DataOutputStream(baos);
       
       dos.writeUTF(image.getName());
       dos.writeInt(image.getStartX());
       dos.writeInt(image.getStartY());
       dos.writeUTF(image.getFileName());
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
    }

    public void writeGroup(DataOutputStream out, Group group) throws IOException {

       out.writeByte((int)Constants.GROUP_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       DataOutputStream dos = new DataOutputStream(baos);
       
       dos.writeUTF(group.getName());
       Feature[] parts = group.getParts();
       
       dos.writeInt(parts.length);
       for (int i = 0; i < parts.length; i++) {
          dos.writeInt(featuresList.indexOf(parts[i]));  // Write the index only
       }
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
 
    }

    public void writeImageSequence(DataOutputStream out, ImageSequence imageSequence) throws IOException {
       
       out.writeByte((int)Constants.IMAGESEQUENCE_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       DataOutputStream dos = new DataOutputStream(baos);
                         
       dos.writeUTF(imageSequence.getName());
       dos.writeInt(imageSequence.getStartX());
       dos.writeInt(imageSequence.getStartY());
       dos.writeUTF(imageSequence.getFileName());
       String[] middle = imageSequence.getMiddle();
       dos.writeInt(middle.length);
       for (int i = 0; i < middle.length; i++) {
          dos.writeUTF(middle[i]);
       }
       dos.writeUTF(imageSequence.getExtension());
       dos.writeBoolean(imageSequence.getRepeat());
       Command[] endCommands = imageSequence.getEndCommands();
       writeCommands(dos, endCommands);
       
       out.writeInt(baos.size());
       baos.writeTo(out);      
       dos.close();
    }

    
    public void writeSrcOver(DataOutputStream out, SrcOver srcOver) throws IOException {
       
       out.writeByte((int)Constants.SRCOVER_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       DataOutputStream dos = new DataOutputStream(baos);
       
       dos.writeUTF(srcOver.getName());
       
       out.writeInt(baos.size());
       baos.writeTo(out);      
       dos.close();
       
    }

    public void writeText(DataOutputStream out, Text text) throws IOException {
       
       out.writeByte(Constants.TEXT_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       dos.writeUTF(text.getName());
       dos.writeInt(text.getStartX());
       dos.writeInt(text.getStartY());
       String[] strings = text.getStrings();
       dos.writeInt(strings.length);
       for (int i = 0; i < strings.length; i++) {
          dos.writeUTF(strings[i]);
       }
       dos.writeInt(text.getVspace());
       dos.writeFont(text.getFont());
       
       Color[] colors = text.getColors();
       dos.writeInt(colors.length);
       for (int i = 0; i < colors.length; i++) {
          dos.writeColor(colors[i]);  
       }
       
       dos.writeColor(text.getBackground());             
      
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();    
  
    }

    public void writeTimer(DataOutputStream out, Timer timer) throws IOException {
        
       out.writeByte(Constants.TIMER_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       dos.writeUTF(timer.getName());
       dos.writeInt(timer.getNumFrames());
       dos.writeBoolean(timer.getRepeat());
       
       writeCommands(dos, timer.getEndCommands());

       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();      
       
    }

    public void writeTranslation(DataOutputStream out, Translation translation) throws IOException {
        out.writeByte((int)Constants.TRANSLATION_IDENTIFIER);
      
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);  
        
        dos.writeUTF(translation.getName());
        int[] frames = translation.getFrames();
        dos.writeInt(frames.length);
        for (int i = 0; i < frames.length; i++) {
            dos.writeInt(frames[i]);
        }
        int[] xs = translation.getXs();
        dos.writeInt(xs.length);
        for (int i = 0; i < xs.length; i++) {
            dos.writeInt(xs[i]);
        }
        int[] ys = translation.getYs();
        dos.writeInt(ys.length);
        for (int i = 0; i < ys.length; i++) {
            dos.writeInt(ys[i]);
        }        
        dos.writeInt(translation.getRepeatFrame());
        writeCommands(dos, translation.getEndCommands());        

        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();           
  
    }

    public void writeTranslator(DataOutputStream out, Translator translator) throws IOException {
        out.writeByte((int)Constants.TRANSLATOR_IDENTIFIER);
 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);  
        
        dos.writeUTF(translator.getName());
        Translation translation = translator.getTranslation();
        int index = featuresList.indexOf(translation);
        dos.writeInt(index); // write the index only
        
        Feature[] features = translator.getFeatures();
        dos.writeInt(features.length);
        for (int i = 0; i < features.length; i++) {
           dos.writeInt(featuresList.indexOf(features[i]));  // Write the index only
        }
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();             
    }
    
    public void writeUserModifier(DataOutputStream out, Modifier modifier) throws IOException {
        out.writeByte((int)Constants.USER_MODIFIER_IDENTIFIER);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(modifier.getName());
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();           
    }
    
    public void writeCommands(DataOutputStream out, Command[] commands) 
        throws IOException {
 
       if (commands == null || commands.length < 0)
           return;
       
	// First, write out two integers - identifier and number of elements. 
       out.writeInt(Constants.COMMAND_IDENTIFIER);  
       out.writeInt(commands.length);
       
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       DataOutputStream dos = new DataOutputStream(baos);
       
       for (int i = 0; i < commands.length; i++) {
          Command command = commands[i];
              
          if (command instanceof ActivatePartCommand) {
             writeActivatePartCmd(dos, (ActivatePartCommand) command);
          } else if (command instanceof ActivateSegmentCommand) {
             writeActivateSegmentCmd(dos, (ActivateSegmentCommand) command);
          } else if (command instanceof SegmentDoneCommand) {
             writeSegmentDoneCmd(dos, (SegmentDoneCommand) command);
          } else if (command instanceof SetVisualRCStateCommand) {
             writeSetVisualRCStateCmd(dos, (SetVisualRCStateCommand) command);
          } else { /* user-defined */
             writeUserCmd(dos, command);
          }
       }
       
       baos.writeTo(out);
       dos.close();
    }

   private void writeSetVisualRCStateCmd(DataOutputStream out, SetVisualRCStateCommand setVisualRCStateCommand) 
       throws IOException {
       
        out.writeByte((int)Constants.SETVISUALRCSTATE_CMD_IDENTIFIER);
       
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeBoolean(setVisualRCStateCommand.getActivated());
        dos.writeInt(setVisualRCStateCommand.getState());
        /* TODO: write RCHandler here */
        dos.writeBoolean(setVisualRCStateCommand.getRunCommands());
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();     
   }

   private void writeActivatePartCmd(DataOutputStream out, ActivatePartCommand activatePartCommand) 
       throws IOException {
       
        out.writeByte((int)Constants.ACTIVATEPART_CMD_IDENTIFIER);
       
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        Assembly assembly = activatePartCommand.getAssembly();
        Feature part = activatePartCommand.getPart();
        
        dos.writeInt(featuresList.indexOf(assembly));
        dos.writeInt(featuresList.indexOf(part));
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                
   }

   private void writeActivateSegmentCmd(DataOutputStream out, ActivateSegmentCommand activateSegmentCommand) 
       throws IOException {
       
        out.writeByte((int)Constants.ACTIVATESEGMENT_CMD_IDENTIFIER);
   
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        
        dos.writeBoolean(activateSegmentCommand.getPush());
        dos.writeBoolean(activateSegmentCommand.getPop());
        
        // TODO: write out segment index!!
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();         
        
   }    

   private void writeSegmentDoneCmd(DataOutputStream out, SegmentDoneCommand segmentDoneCommand) 
       throws IOException {
       
       out.writeByte((int)Constants.SEGMENTDONE_CMD_IDENTIFIER);
       
       // nothing to record for this command.  Return.
   }

   private void writeUserCmd(DataOutputStream out, Command command) 
       throws IOException {
       
        out.writeByte((int)Constants.USER_CMD_IDENTIFIER);
       
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(command.toString());
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                 
       
       
   }
   
}
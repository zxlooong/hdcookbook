
package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
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
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;
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
    private ArrayList rcHandlersList;
    private ArrayList segmentsList;
    
    public GrinBinaryWriter(Show show) {
        
        this.show = show;
       
        Feature[] features = show.getFeaturesAsArray();
        featuresList = createFeaturesArrayList(features);
        
        RCHandler[] rcHandlers = show.getRCHandlersAsArray();
        rcHandlersList = new ArrayList(rcHandlers.length);
        for (int i = 0; i < rcHandlers.length; i++) {
            rcHandlersList.add(rcHandlers[i]);
        }     
        
        Segment[] segments = show.getSegmentsAsArray();
        segmentsList = new ArrayList(segments.length);
        for (int i = 0; i < segments.length; i++) {
            segmentsList.add(segments[i]);
        }         
    }

    private static void writeScriptIdentifier(DataOutputStream out) throws IOException {
        out.writeInt(Constants.GRINSCRIPT_IDENTIFIER);
        out.writeInt(Constants.GRINSCRIPT_VERSION);
    }
 
    public void writeShow(DataOutputStream out) throws IOException {
        
        writeScriptIdentifier(out);
        
        out.writeInt(featuresList.size());
        out.writeInt(rcHandlersList.size());
        out.writeInt(segmentsList.size());
        
        writeFeatures(out, (Feature[])featuresList.toArray(new Feature[]{}));   
        writeSegments(out, (Segment[])segmentsList.toArray(new Segment[]{}));
        writeRCHandlers(out, (RCHandler[])rcHandlersList.toArray(new RCHandler[]{}));
    }
    
    protected void writeFeatures(DataOutputStream out, Feature[] features) 
       throws IOException {
	
        if (features == null) 
            return;
 
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

    private void writeRCHandlers(DataOutputStream out, RCHandler[] rcHandlers) throws IOException {
        if (rcHandlers == null) 
            return;
        
        out.writeInt(Constants.RCHANDLER_IDENTIFIER);
        out.writeInt(rcHandlers.length);
        
        for (int i = 0; i < rcHandlers.length; i++) {
            RCHandler handler = rcHandlers[i];
            if (handler instanceof CommandRCHandler) {
                writeCommandRCHandler(out, (CommandRCHandler)handler);
            } else if (handler instanceof VisualRCHandler) {
                writeVisualRCHandler(out, (VisualRCHandler) handler);
            } else {
                throw new IOException("Unknown RCHandler " + handler);
            }
        }
    }

    private void writeSegments(DataOutputStream out, Segment[] segments) throws IOException {        
        if (segments == null) 
            return;
        
        out.writeInt(Constants.SEGMENT_IDENTIFIER);
        out.writeInt(segments.length);
        
        for (int i = 0; i < segments.length; i++) {
            writeSegment(out, segments[i]);
        }
    }
    
    public void writeAssembly(DataOutputStream out, Assembly assembly) throws IOException {
        
        out.writeByte((int)Constants.ASSEMBLY_IDENTIFIER);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);
        
        dos.writeUTF(assembly.getName());
        String[] partNames = assembly.getPartNames();
        Feature[] parts = assembly.getParts();
        
        dos.writeStringArray(partNames);
        writeFeaturesIndex(dos, parts);
        
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
       dos.writeInt(featuresList.indexOf(clipped.getPart()));
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
    }
    
    public void writeFade(DataOutputStream out, Fade fade) throws IOException {
       
       out.writeByte((int)Constants.FADE_IDENTIFIER);
      
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);

       dos.writeUTF(fade.getName());
       dos.writeBoolean(fade.getSrcOver());
       int[] keyframes = fade.getKeyframes();
       dos.writeIntArray(keyframes);
       int[] keyAlphas = fade.getKeyAlphas();
       dos.writeIntArray(keyAlphas);
       Command[] endCommands = fade.getEndCommands();
       writeCommands(dos, endCommands);
       dos.writeInt(featuresList.indexOf(fade.getPart()));
       
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
       writeFeaturesIndex(dos, group.getParts());
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
 
    }

    public void writeImageSequence(DataOutputStream out, ImageSequence imageSequence) throws IOException {
       
       out.writeByte((int)Constants.IMAGESEQUENCE_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
                         
       dos.writeUTF(imageSequence.getName());
       dos.writeInt(imageSequence.getStartX());
       dos.writeInt(imageSequence.getStartY());
       dos.writeUTF(imageSequence.getFileName());
       dos.writeStringArray(imageSequence.getMiddle());
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
       dos.writeInt(featuresList.indexOf(srcOver.getPart()));
      
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
       dos.writeStringArray(text.getStrings());
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
        dos.writeIntArray(translation.getFrames());
        dos.writeIntArray(translation.getXs());
        dos.writeIntArray(translation.getYs());
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
        
        writeFeaturesIndex(dos, translator.getFeatures());
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();             
    }
    
    public void writeUserModifier(DataOutputStream out, Modifier modifier) throws IOException {
        out.writeByte((int)Constants.USER_MODIFIER_IDENTIFIER);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(modifier.getName());
        dos.writeInt(featuresList.indexOf(modifier.getPart()));
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();           
    }
    
    public void writeCommands(DataOutputStream out, Command[] commands) 
        throws IOException {
 
       if (commands == null) {
           out.writeByte(Constants.NULL);
           return;
       } 
       
       out.writeByte(Constants.NON_NULL);
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
          } else {    /* user-defined or null */
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
        VisualRCHandler handler = setVisualRCStateCommand.getVisualRCHandler();
        dos.writeInt(rcHandlersList.indexOf(handler));
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
        Segment segment = activateSegmentCommand.getSegment();
        dos.writeInt(segmentsList.indexOf(segment));
        
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
        
        if (command == null) {
            out.writeByte(Constants.NULL);
        } else {
            out.writeByte(Constants.NON_NULL);
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        dos.writeUTF(command.toString());
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                 
       
    }

    private void writeCommandRCHandler(DataOutputStream out, CommandRCHandler commandRCHandler) throws IOException {
        out.writeByte((int)Constants.COMMAND_RCHANDLER_IDENTIFIER);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);    

        dos.writeInt(commandRCHandler.getMask());
        writeCommands(dos, commandRCHandler.getCommands());   
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                 
    }

    private void writeVisualRCHandler(DataOutputStream out, VisualRCHandler visualRCHandler) throws IOException {
        out.writeByte((int)Constants.VISUAL_RCHANDLER_IDENTIFIER);
 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);    
        
        dos.writeUTF(visualRCHandler.getName());
        dos.writeInt2Array(visualRCHandler.getGrid());
        dos.writeStringArray(visualRCHandler.getStateNames());
        Command[][] selectCommands = visualRCHandler.getSelectCommands();
        if (selectCommands == null) {
            dos.writeByte(Constants.NULL);
        } else {
            dos.writeByte(Constants.NON_NULL);
            dos.writeInt(selectCommands.length);
            for (int i = 0; i < selectCommands.length; i++) {
                writeCommands(dos, selectCommands[i]);
            }
        }
        Command[][] activateCommands = visualRCHandler.getActivateCommands();
        if (activateCommands == null) {
            dos.writeByte(Constants.NULL);
        } else {
            dos.writeByte(Constants.NON_NULL);
            dos.writeInt(activateCommands.length);
            for (int i = 0; i < activateCommands.length; i++) {
                writeCommands(dos, activateCommands[i]);
            }
        }
        
        dos.writeRectangleArray(visualRCHandler.getMouseRects());
        dos.writeIntArray(visualRCHandler.getMouseRectStates());
        dos.writeInt(visualRCHandler.getTimeout());
        writeCommands(dos, visualRCHandler.getTimeoutCommands());
        
        dos.writeInt(featuresList.indexOf(visualRCHandler.getAssembly()));
        
        Feature[] selectFeatures = visualRCHandler.getSelectFeatures();
        writeFeaturesIndex(dos, selectFeatures);
        
        Feature[] activateFeatures = visualRCHandler.getActivateFeatures();
        writeFeaturesIndex(dos, activateFeatures);
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();         
    }

    private void writeSegment(DataOutputStream out, Segment segment) throws IOException {
        
        out.writeByte((int)Constants.SEGMENT_IDENTIFIER);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);    
        
        dos.writeUTF(segment.getName());
        
        Feature[] active = segment.getActiveFeatures();
        writeFeaturesIndex(dos, active);
      
        Feature[] setup = segment.getSetupFeatures();
        writeFeaturesIndex(dos, setup);    

        RCHandler[] rcHandlers = segment.getRCHandlers();
        dos.writeInt(rcHandlers.length);
        for (int i = 0; i < rcHandlers.length; i++) {
            dos.writeInt(rcHandlersList.indexOf(rcHandlers[i]));
        }
        
        dos.writeBoolean(segment.getNextOnSetupDone());
        
        writeCommands(dos, segment.getNextCommands());
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                 
    }
    
    private void writeFeaturesIndex(DataOutputStream out, Feature [] features) throws IOException {
        if (features == null) {
            out.writeByte(Constants.NULL);
            return;
        } 
        out.writeByte(Constants.NON_NULL);
        out.writeInt(features.length);
        
        for (int i = 0; i < features.length; i++) {
            if (features[i] == null) {
                out.writeInt(-1);
            } else {
                out.writeInt(featuresList.indexOf(features[i]));
            }
        }   
    }

    public ArrayList getFeaturesList() {
        return featuresList;
    }

    /**
     * Move Features that could possibly have forward references
     * to the end of the list - Assembly, Group, Modifier and Translator.  
     */
    private ArrayList createFeaturesArrayList(Feature[] features) {
        ArrayList common = new ArrayList();
        ArrayList deferred = new ArrayList();
        
        for (int i = 0; i < features.length; i++) {
            if (features[i] instanceof Assembly || 
                features[i] instanceof Group ||
                features[i] instanceof Modifier ||
                features[i] instanceof Translator ) {
                   deferred.add(features[i]);
            } else {    
                common.add(features[i]);
            }          
        }
        
        // Sort the deferred list to ensure that items contain no forward references in them.
        ArrayList sorted = new ArrayList();
        Feature feature;
        while (!deferred.isEmpty()) {
            
            for (int i = 0; i < deferred.size(); i++ ) {
                feature = (Feature) deferred.get(i);
                if (!containsReference(deferred, feature)) {
                    sorted.add(feature);
                }    
            }
            deferred.removeAll(sorted);
        }
        
        common.addAll(sorted);
        
        return common;
    }
    
    private boolean containsReference(ArrayList list, Feature feature) {
        if (feature instanceof Assembly) {
            Feature[] parts = ((Assembly)feature).getParts();
            for (int i = 0; i < parts.length; i++) {
                if (list.contains(parts[i])) {
                    return true;
                }   
            }    
            return false;
        } else if (feature instanceof Group) {
            Feature[] parts = ((Group)feature).getParts();
            for (int i = 0; i < parts.length; i++) {
                if (list.contains(parts[i])) {
                    return true;
                }
            }
            return false;
        } else if (feature instanceof Modifier) {
            Feature part = ((Modifier)feature).getPart();
            if (list.contains(part)) {
                return true;
            }
            return false;
        } else if (feature instanceof Translator) {
            Feature[] parts = ((Translator)feature).getFeatures();
            for (int i = 0; i < parts.length; i++) {
                if (list.contains(parts[i])) {
                    return true;
                }
            }
            return false;
        } else {
            throw new RuntimeException("Unexpected instance " + feature);
        }
    }
    
}
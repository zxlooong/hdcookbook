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

import com.hdcookbook.grin.features.SEUserCommand;
import com.hdcookbook.grin.io.binary.*;
import java.awt.Color;
import java.awt.Rectangle;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.SEShow;
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
import com.hdcookbook.grin.features.GuaranteeFill;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.features.SEUserFeature;
import com.hdcookbook.grin.features.SEUserModifier;
import com.hdcookbook.grin.features.SetTarget;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Timer;
import com.hdcookbook.grin.features.TranslatorModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;


/**
 * The main class to write out the Show object to a binary file format.
 */

/*
 * The array of features are sorted to eliminate forward references
 * before writing out to the file.
 *
 * This class works with a binary file reader, GrinBinaryReader, which contains
 * read(...) versions of the methods defined in this class.
 * If you change one file, make sure to update the other file as well.
 *
 * The syntax of a Show's binary file is as follows:
 * 
 *  --------------------------------
 *  xxxxx.grin {
 *      script_identifier                  integer
 *      version_number                     integer
 *      FeatureArray_info()
 *      RCHandlerArray_info()
 *      SegmentArray_info()
 *      Show_Setup_info()
 *  }
 *
 *  FeatureArray_info() {
 *      feature_identifier                 integer
 *      features_length                    integer
 *      for (i = 0; i < features_length; i++) {
 *          Feature_info();                 
 *      }
 *  }
 * 
 *  RCHandlerArray_info() {
 *      rcHandler_identifier               integer
 *      rcHandlers_length                    integer
 *      for (i = 0; i < rcHandlers_length; i++) {
 *          RCHandler_info();                 
 *      }
 *  }
 *
 *  SegmentArray_info() {
 *      segment_identifier                 integer
 *      segments_length                    integer
 *      for (int i = 0; i < segments_length; i++ ) {
 *          Segment_info();
 *      }
 *  }
 *  
 *  Show_setup_info() {
 *      show_segment_stack_depth           integer
 *      // could be more data in the future
 *  }
 *
 *  Feature_info() {
 *      feature_type                    byte
 *      feature_length                  integer
 *      ... List of data in this Feature subclass indicated by "feature_type" ... 
 *  }
 *  
 *  RCHandler_info() {
 *      rcHandler_type                    byte
 *      rcHandler_length                  integer
 *      ... List of data in this RCHandler subclass indicated by "rcHandler_type" ... 
 *  }
 *
 *  Segment_info() {
 *      segment_identifier                 byte
 *      segment_length                     integer
 *      ... List of data in this Segment class ... 
 *  }   
 *
 *  --------------------------------      
 */
public class GrinBinaryWriter {

    /**
     * Show file to write out 
     */
    private SEShow show;
    /**
     * List of Feature in the show.
     */
    private ArrayList featuresList;
    
    /**
     * List of RCHandler in the show.
     */
    private ArrayList rcHandlersList;
    
    /** 
     * List of Segment in the show.
     */
    private ArrayList segmentsList;
    
    /**
     * ExtensionsWriter for writing out user-defined extensions.
     */
    private ExtensionsWriter extensionsWriter;
    
    /**
     * Constructs GrinBinaryWriter.
     * 
     * @param show The show object which this GrinBinaryWriter makes into a binary format.
     **/
    public GrinBinaryWriter(SEShow show, ExtensionsWriter writer) {
       
        this.show = show;
        this.extensionsWriter = writer;
       
        Feature[] features = show.getFeatures();
        featuresList = createFeaturesArrayList(features);
        
        RCHandler[] rcHandlers = show.getRCHandlers();
        rcHandlersList = new ArrayList(rcHandlers.length);
        for (int i = 0; i < rcHandlers.length; i++) {
            rcHandlersList.add(rcHandlers[i]);
        }     
        
        Segment[] segments = show.getSegments();
        segmentsList = new ArrayList(segments.length);
        for (int i = 0; i < segments.length; i++) {
            segmentsList.add(segments[i]);
        }         
    }

    /**
     * Writes out the script identifier and the script version to the DataOutputStream.
     *
     */
    private static void writeScriptIdentifier(DataOutputStream out) throws IOException {
        out.writeInt(Constants.GRINSCRIPT_IDENTIFIER);
        out.writeInt(Constants.GRINSCRIPT_VERSION);
    }
 
    /**
     * Writes out the show object for this GrinBinaryWriter.
     *
     * @param out The DataOutputStream to write out show's binary data to.
     * @throws IOException if writing to the DataOutputStream fails.
     */
    public void writeShow(DataOutputStream out) throws IOException {
        
        writeScriptIdentifier(out);
        
        out.writeInt(featuresList.size());
        out.writeInt(rcHandlersList.size());
        out.writeInt(segmentsList.size());

	{
	    GrinDataOutputStream dos = new GrinDataOutputStream(out);
	    dos.writeInt(show.getSegmentStackDepth());
	    dos.writeStringArray(show.getDrawTargets());
	    dos.flush();
	    // We intentionally don't close it, because the underlying output
	    // stream needs to stay open.
	}
        
        writeFeatures(out, (Feature[])featuresList.toArray(new Feature[featuresList.size()]));   
        writeRCHandlers(out, (RCHandler[])rcHandlersList.toArray(new RCHandler[rcHandlersList.size()]));
        writeSegments(out, (Segment[])segmentsList.toArray(new Segment[segmentsList.size()]));
	writePublicElements(out);
    }
    
    private void writeFeatures(DataOutputStream out, Feature[] features) 
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
            } else if (feature instanceof TranslatorModel) {
                writeTranslatorModel(out, (TranslatorModel)feature);
            } else if (feature instanceof Translator) {
                writeTranslator(out, (Translator)feature);
            } else if (feature instanceof SrcOver) {
                writeSrcOver(out, (SrcOver)feature);
            } else if (feature instanceof GuaranteeFill) {
                writeGuaranteeFill(out, (GuaranteeFill)feature);
            } else if (feature instanceof SetTarget) {
                writeSetTarget(out, (SetTarget)feature);
            } else if (feature instanceof Modifier) {
                writeUserModifier(out, (SEUserModifier)feature);
            } else {
                writeUserFeature(out, (SEUserFeature)feature);
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
    
    private void writeAssembly(DataOutputStream out, Assembly assembly) throws IOException {
        
        out.writeByte((int)Constants.ASSEMBLY_IDENTIFIER);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
	if (show.isPublic(assembly)) {
	    dos.writeString(assembly.getName());
	} else {
	    dos.writeString(null);
	}
        String[] partNames = assembly.getPartNames();
        Feature[] parts = assembly.getParts();
        
        dos.writeStringArray(partNames);
        writeFeaturesIndex(dos, parts);
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();
    }

    private void writeBox(DataOutputStream out, Box box) throws IOException {
 
       out.writeByte((int)Constants.BOX_IDENTIFIER);
             
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       if (show.isPublic(box)) {
	   dos.writeString(box.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeInt(box.getX());
       dos.writeInt(box.getY());
       dos.writeInt(box.implGetWidth());
       dos.writeInt(box.implGetHeight());
       dos.writeInt(box.getOutlineWidth());
       dos.writeColor(box.getOutlineColor());
       dos.writeColor(box.getFillColor());
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
    }
    
    private void writeClipped(DataOutputStream out, Clipped clipped) throws IOException {
       
       out.writeByte((int)Constants.CLIPPED_IDENTIFIER);
       
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       Rectangle rect = clipped.getClipRegion();      
       if (show.isPublic(clipped)) {
	   dos.writeString(clipped.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeRectangle(rect);
       dos.writeFeature(featuresList.indexOf(clipped.getPart()));
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
    }
    
    private void writeFade(DataOutputStream out, Fade fade) throws IOException {
       
       out.writeByte((int)Constants.FADE_IDENTIFIER);
      
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);

       if (show.isPublic(fade)) {
	   dos.writeString(fade.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeBoolean(fade.getSrcOver());
       int[] keyframes = fade.getKeyframes();
       dos.writeIntArray(keyframes);
       int[] keyAlphas = fade.getKeyAlphas();
       dos.writeIntArray(keyAlphas);
       dos.writeInt(fade.getRepeatFrame());
       Command[] endCommands = fade.getEndCommands();
       writeCommands(dos, endCommands);
       dos.writeFeature(featuresList.indexOf(fade.getPart()));
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
       
    }

    private void writeFixedImage(DataOutputStream out, FixedImage image) throws IOException {
       
       out.writeByte((int)Constants.FIXEDIMAGE_IDENTIFIER);
       
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       if (show.isPublic(image)) {
	   dos.writeString(image.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeInt(image.getX());
       dos.writeInt(image.getY());
       dos.writeUTF(image.getFileName());
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
    }

    private void writeGroup(DataOutputStream out, Group group) throws IOException {

       out.writeByte((int)Constants.GROUP_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       if (show.isPublic(group)) {
	   dos.writeString(group.getName());
       } else {
	   dos.writeString(null);
       }
       writeFeaturesIndex(dos, group.getParts());
       
       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();
 
    }

    private void writeImageSequence(DataOutputStream out, ImageSequence imageSequence) throws IOException {
       
       out.writeByte((int)Constants.IMAGESEQUENCE_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
                         
       if (show.isPublic(imageSequence)) {
	   dos.writeString(imageSequence.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeInt(imageSequence.getX());
       dos.writeInt(imageSequence.getY());
       dos.writeUTF(imageSequence.getFileName());
       dos.writeStringArray(imageSequence.getMiddle());
       dos.writeUTF(imageSequence.getExtension());
       dos.writeBoolean(imageSequence.getRepeat());
       ImageSequence model = imageSequence.getModel();
       dos.writeBoolean(model != null);
       if (model != null) {
           dos.writeFeature(featuresList.indexOf(model));
       }
       Command[] endCommands = imageSequence.getEndCommands();
       writeCommands(dos, endCommands);
       
       out.writeInt(baos.size());
       baos.writeTo(out);      
       dos.close();
    }

    
    private void writeSrcOver(DataOutputStream out, SrcOver srcOver) throws IOException {
       
       out.writeByte((int)Constants.SRCOVER_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       if (show.isPublic(srcOver)) {
	   dos.writeString(srcOver.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeFeature(featuresList.indexOf(srcOver.getPart()));
      
       out.writeInt(baos.size());
       baos.writeTo(out);      
       dos.close();
       
    }

    private void writeText(DataOutputStream out, Text text) throws IOException {
       
       out.writeByte(Constants.TEXT_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       if (show.isPublic(text)) {
	   dos.writeString(text.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeInt(text.getX());
       dos.writeInt(text.getY());
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

    private void writeTimer(DataOutputStream out, Timer timer) throws IOException {
        
       out.writeByte(Constants.TIMER_IDENTIFIER);

       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
       if (show.isPublic(timer)) {
	   dos.writeString(timer.getName());
       } else {
	   dos.writeString(null);
       }
       dos.writeInt(timer.getNumFrames());
       dos.writeBoolean(timer.getRepeat());
       
       writeCommands(dos, timer.getEndCommands());

       out.writeInt(baos.size());
       baos.writeTo(out);
       dos.close();      
       
    }

    private void writeTranslatorModel(DataOutputStream out, TranslatorModel translation) throws IOException {
        out.writeByte((int)Constants.TRANSLATOR_MODEL_IDENTIFIER);
      
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);  
        
       if (show.isPublic(translation)) {
	   dos.writeString(translation.getName());
       } else {
	   dos.writeString(null);
       }
        dos.writeIntArray(translation.getFrames());
        dos.writeIntArray(translation.getXs());
        dos.writeIntArray(translation.getYs());
        dos.writeInt(translation.getRepeatFrame());
        dos.writeBoolean(translation.getIsRelative());
        writeCommands(dos, translation.getEndCommands());        

        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();           
  
    }

    private void writeTranslator(DataOutputStream out, Translator translator) throws IOException {
        out.writeByte((int)Constants.TRANSLATOR_IDENTIFIER);
 
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);  
        
        if (show.isPublic(translator)) {
	   dos.writeString(translator.getName());
        } else {
	   dos.writeString(null);
        }
        dos.writeInt(translator.getAbsoluteXOffset());
        dos.writeInt(translator.getAbsoluteYOffset());
        TranslatorModel model = translator.getModel();
        int index = featuresList.indexOf(model);
        dos.writeFeature(index); // write the index only
       	dos.writeFeature(featuresList.indexOf(translator.getPart()));
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();             
    }
    
    private void writeGuaranteeFill(DataOutputStream out, GuaranteeFill feature) throws IOException {
	out.writeByte((int)Constants.GUARANTEE_FILL_IDENTIFIER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);  
        
	if (show.isPublic(feature)) {
	    dos.writeString(feature.getName());
	} else {
	    dos.writeString(null);
	}
	dos.writeFeature(featuresList.indexOf(feature.getPart()));
	dos.writeRectangle(feature.getGuaranteed());
	dos.writeRectangleArray(feature.getFills());
      
        out.writeInt(baos.size());
        baos.writeTo(out);      
        dos.close();
    }

    private void writeSetTarget(DataOutputStream out, SetTarget feature) throws IOException {
        out.writeByte(Constants.SET_TARGET_IDENTIFIER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);  
        
	if (show.isPublic(feature)) {
	    dos.writeString(feature.getName());
	} else {
	    dos.writeString(null);
	}
        dos.writeFeature(featuresList.indexOf(feature.getPart()));
	dos.writeInt(feature.getTarget());
      
        out.writeInt(baos.size());
        baos.writeTo(out);      
        dos.close();
    }

    private void writeUserFeature(DataOutputStream out, SEUserFeature feature) throws IOException {
        out.writeByte((int)Constants.USER_FEATURE_IDENTIFIER);
         
        if (feature == null) {
            out.writeByte(Constants.NULL);
            return;
        } 
        
        out.writeByte(Constants.NON_NULL);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);

	extensionsWriter.writeExtensionFeature(dos, feature);
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();   	    
    }

    private void writeUserModifier(DataOutputStream out, SEUserModifier modifier) throws IOException {
        out.writeByte((int)Constants.USER_MODIFIER_IDENTIFIER);
         
        if (modifier == null) {
            out.writeByte(Constants.NULL);
            return;
        } 
        
        out.writeByte(Constants.NON_NULL);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);
	
	extensionsWriter.writeExtensionModifier(dos, modifier);
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();           
    }
    
    private void writeCommands(DataOutputStream out, Command[] commands) 
        throws IOException {
 
       if (commands == null) {
           out.writeByte(Constants.NULL);
           return;
       } 
       
       out.writeByte(Constants.NON_NULL);
       out.writeInt(commands.length);
       
       ByteArrayOutputStream baos = new ByteArrayOutputStream();
       GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
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
             writeUserCmd(dos, (SEUserCommand)command);
          }
       }
       
       baos.writeTo(out);
       dos.close();
    }

   private void writeSetVisualRCStateCmd(DataOutputStream out, SetVisualRCStateCommand setVisualRCStateCommand) 
       throws IOException {
       
        out.writeByte((int)Constants.SETVISUALRCSTATE_CMD_IDENTIFIER);
       
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);
        
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
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);
       
        Assembly assembly = activatePartCommand.getAssembly();
        Feature part = activatePartCommand.getPart();
        
        dos.writeFeature(featuresList.indexOf(assembly));
        dos.writeFeature(featuresList.indexOf(part));
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                
   }

   private void writeActivateSegmentCmd(DataOutputStream out, ActivateSegmentCommand activateSegmentCommand) 
       throws IOException {
       
        out.writeByte((int)Constants.ACTIVATESEGMENT_CMD_IDENTIFIER);
   
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);
        
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

    private void writeUserCmd(DataOutputStream out, SEUserCommand command) 
       throws IOException {
       
        out.writeByte((int)Constants.USER_CMD_IDENTIFIER);
        
        if (command == null) {
            out.writeByte(Constants.NULL);
            return;
        } 
        
        out.writeByte(Constants.NON_NULL);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);

	extensionsWriter.writeExtensionCommand(dos, command);
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                 
       
    }

    private void writeCommandRCHandler(DataOutputStream out, CommandRCHandler commandRCHandler) throws IOException {
        out.writeByte((int)Constants.COMMAND_RCHANDLER_IDENTIFIER);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);    

	if (show.isPublic(commandRCHandler)) {
	    dos.writeString(commandRCHandler.getName());
	} else {
	    dos.writeString(null);
	}
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
        
	if (show.isPublic(visualRCHandler)) {
	    dos.writeString(visualRCHandler.getName());
	} else {
	    dos.writeString(null);
	}
        dos.writeIntArray(visualRCHandler.getUpDown());
        dos.writeIntArray(visualRCHandler.getRightLeft());
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
       
	Feature assembly = visualRCHandler.getAssembly();
        dos.writeBoolean(assembly != null);
	if (assembly != null) {
	    dos.writeFeature(featuresList.indexOf(assembly));
	}
        
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
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);    
        
        dos.writeString(segment.getName());
	//
	// We could chech show.isPublic(segment), and write out null
	// if it isn't.  That would save a bit of space, but the segment
	// name is really key for debugging.  If we ever do this, there
	// should be an option for including the names of private segments
	// for debugging purposes.
	//
        
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
		int index = featuresList.indexOf(features[i]);
		if (index < 0) {
		    throw new IOException("Invalid feature index");
		}
                out.writeInt(index);
            }
        }   
    }

    private void writePublicElements(DataOutputStream out) throws IOException {
        
        out.writeInt(Constants.PUBLIC_ELEMENTS_IDENTIFIER);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos);    

	for (int i = 0; i < segmentsList.size(); i++) {
	    Segment seg = (Segment) segmentsList.get(i);
	    if (show.isPublic(seg)) {
		dos.writeInt(i);
	    }
	}
	dos.writeInt(-1);

	for (int i = 0; i < featuresList.size(); i++) {
	    Feature feature = (Feature) featuresList.get(i);
	    if (show.isPublic(feature)) {
		dos.writeInt(i);
	    }
	}
	dos.writeInt(-1);

	for (int i = 0; i < rcHandlersList.size(); i++) {
	    RCHandler hand = (RCHandler) rcHandlersList.get(i);
	    if (show.isPublic(hand)) {
		dos.writeInt(i);
	    }
	}
	dos.writeInt(-1);
        
        out.writeInt(baos.size());
        baos.writeTo(out);
        dos.close();                 
    }
    

    /**
     * Sort the array to eliminates forward references within this array of Features.
     * Note that the references from the Command objects within a Feature is ignored.
     */
    private ArrayList createFeaturesArrayList(Feature[] features) {
        ArrayList common = new ArrayList();
        ArrayList deferred = new ArrayList();
        
        for (int i = 0; i < features.length; i++) {
            /* 3 types of Features that could have forward references.  */
            if (features[i] instanceof Assembly || 
                features[i] instanceof Group ||
                features[i] instanceof Modifier ) {
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
        } else {
            throw new RuntimeException("Unexpected instance " + feature);
        }
    }
    
}

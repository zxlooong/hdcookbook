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

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import com.hdcookbook.grin.Director;
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
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.util.Debug;

/**
 * The main class to read in a Show object from a binary file format.
 **/

/*
 * This class works with a binary file writer, GrinBinaryWriter, which contains
 * write(...) version of the methods defined in this class.
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

public class GrinBinaryReader {

    private Show show;
    private Director director;
    private Feature[] features;
    private RCHandler[] rcHandlers;
    private Segment[] segments;
    private String filename;
    private InputStream stream;
    
    private ArrayList deferred = new ArrayList();
    
    /**
     * Constructs a GrinBinaryReader instance.
     *
     * @param director A Director for the show instance this Reader will create.
     * @param stream    An InputStream to the grin binary format data.  It is recommended to be
     *                  an instance of BufferedInputStream for a performance improvement.
     */
    public GrinBinaryReader(Director director, InputStream stream) {
        
       this.director = director;
       
       if (Debug.ASSERT) {
           this.stream = new DebugInputStream(stream);
       } else {
          this.stream = stream;
       }
       
    }

    private void checkValue(int x, int y, String message) throws IOException {
        if (x != y) {
            throw new IOException("Mismatch: " + message);
        }
    }
    
    private void checkScriptHeader(DataInputStream in) throws IOException {
       checkValue(in.readInt(), Constants.GRINSCRIPT_IDENTIFIER, "Script identifier");
       int version = in.readInt();
       checkValue(version, Constants.GRINSCRIPT_VERSION, 
           "Script version mismatch, expects " + Constants.GRINSCRIPT_VERSION + ", found " + version);       
    }
    
    /**
     * Reconstructes the Show object that this GrinBinaryReader is associated with.
     *
     * @return Show a show object that has been reconstructed.
     * @throws IOException if binary data parsing fails.
     */
    
    public Show readShow(ShowBuilder builder) throws IOException {

        this.show = new Show(director);
        
        GrinDataInputStream in = new GrinDataInputStream(stream);       
        checkScriptHeader(in);
        
        features = new Feature[in.readInt()];
        rcHandlers = new RCHandler[in.readInt()];
        segments = new Segment[in.readInt()];
            
        // Read in the show file
        readFeatures(in);
        readRCHandlers(in);
        readSegments(in);      
        int showSegmentStackDepth = in.readInt();
        
        // Resolve forward references 
        for (int i = 0; i < deferred.size(); i++) {
            CommandSetup setup = (CommandSetup) deferred.get(i);
            setup.setup();
        }
        deferred.clear(); 
        
        builder.init(show);
        
        show.setSegmentStackDepth(showSegmentStackDepth);
        
        // Recreate an show object based on what's been read
        for (int i = 0; i < features.length; i++) {
            builder.addFeature(features[i].getName(), 0, features[i]);
        }
        
        for (int i = 0; i < segments.length; i++) {
            builder.addSegment(segments[i].getName(),  0, segments[i]);
        }
        
        for (int i = 0; i < rcHandlers.length; i++) {
            builder.addRCHandler(rcHandlers[i].getName(), 0, rcHandlers[i]); 
        }
        
        builder.finishBuilding();
        
        return show;
    }
    
    private void readFeatures(GrinDataInputStream in) 
       throws IOException {
        
        checkValue(in.readInt(), Constants.FEATURE_IDENTIFIER, "Feature array identifier");
        
        int count = in.readInt();
        Feature feature = null;  
        
        for (int i = 0; i < count; i++) {
            int identifier = in.readByte();           
            
            switch (identifier) {
                case Constants.ASSEMBLY_IDENTIFIER :
                    feature = readAssembly(in);
                    break;
                case Constants.BOX_IDENTIFIER :
                    feature = readBox(in);
                    break;
                case Constants.CLIPPED_IDENTIFIER :
                    feature = readClipped(in);
                    break;
                case Constants.FADE_IDENTIFIER :
                    feature = readFade(in);
                    break;
                case Constants.FIXEDIMAGE_IDENTIFIER :
                    feature = readFixedImage(in);
                    break;
                case Constants.GROUP_IDENTIFIER :
                    feature = readGroup(in);
                    break;
                case Constants.IMAGESEQUENCE_IDENTIFIER :
                    feature = readImageSequence(in);
                    break;
                case Constants.TEXT_IDENTIFIER :
                    feature = readText(in);
                    break;
                case Constants.TIMER_IDENTIFIER :
                    feature = readTimer(in);
                    break;
                case Constants.TRANSLATION_IDENTIFIER :
                    feature = readTranslation(in);
                    break;
                case Constants.TRANSLATOR_IDENTIFIER :
                    feature = readTranslator(in);
                    break;
                case Constants.SRCOVER_IDENTIFIER :
                    feature = readSrcOver(in);
                    break;
                case Constants.USER_MODIFIER_IDENTIFIER :
                    feature = readUserModifier(in);
                    break;
                default:
                    throw new IOException("Unknown feature identifier " + identifier);
            }
            
            features[i] = feature;
        }
    }

    private void readRCHandlers(GrinDataInputStream in) throws IOException {
        
        checkValue(in.readInt(), Constants.RCHANDLER_IDENTIFIER, "RCHandler array identifier");
                    
        int count = in.readInt();
        RCHandler rcHandler = null;
            
        for (int i = 0; i < count; i++) {
            int identifier = in.readByte();

            switch (identifier) {     
                case Constants.COMMAND_RCHANDLER_IDENTIFIER :
                    rcHandler = readCommandRCHandler(in);
                    break;
                case Constants.VISUAL_RCHANDLER_IDENTIFIER :
                    rcHandler = readVisualRCHandler(in);
                    break;
                default :
                    throw new IOException("Unknown RCHandler type " + identifier);
            }          
            rcHandlers[i] = rcHandler;
        }     
    }    

    private void readSegments(GrinDataInputStream in) throws IOException {

        checkValue(in.readInt(), Constants.SEGMENT_IDENTIFIER, "Segment array identifier");
        
        int count = in.readInt();       
        Segment segment = null;
        
        for (int i = 0; i < count; i++) {
            in.readByte(); // SEGMENT_IDENTIFIER;
            segment = readSegment(in);
            segments[i] = segment;
        }
    } 
    
    private Assembly readAssembly(GrinDataInputStream dis) throws IOException {
        
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
               
        String name = dis.readUTF();
        String[] partNames = dis.readStringArray();
        Feature[] parts = readFeaturesIndex(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }
        
        Assembly assembly = new Assembly(show, name);        
        assembly.setParts(partNames, parts);

        return assembly;
    }

    private Box readBox(GrinDataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }       
        String name = dis.readUTF();
        Rectangle placement = dis.readRectangle();
        int outlineWidth = dis.readInt();
        Color outline = dis.readColor();
        Color fill = dis.readColor();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }       
        return new Box(show, name, placement, outlineWidth, outline, fill);
    }
    
    private Clipped readClipped(GrinDataInputStream dis) throws IOException {

        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
        String name = dis.readUTF();
        Rectangle clipRegion = dis.readRectangle();
        Feature part = features[dis.readInt()];
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }       
        Clipped clipped = new Clipped(show, name, clipRegion);
        clipped.setup(part);
       
        return clipped;
    }
    
    private Fade readFade(GrinDataInputStream dis) throws IOException {
 
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
        
        String name = dis.readUTF();
        boolean srcOver = dis.readBoolean();
        int[] keyframes = dis.readIntArray();
        int[] keyAlphas = dis.readIntArray();
        Command[] endCommands = readCommands(dis);
        Feature part = features[dis.readInt()];
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }
       
        Fade fade = new Fade(show, name, srcOver, keyframes, keyAlphas, endCommands);
        fade.setup(part);
       
        return fade;
    }

    private FixedImage readFixedImage(GrinDataInputStream dis) throws IOException {

        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
       
        String name = dis.readUTF();
        int startX = dis.readInt();
        int startY = dis.readInt();
        String filename = dis.readUTF();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }
        
        return new FixedImage(show, name, startX, startY, filename);       
    } 

    private Group readGroup(GrinDataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
        String name = dis.readUTF();
        Feature[] parts = readFeaturesIndex(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }
        
        Group group = new Group(show, name);      
        group.setup(parts);
       
        return group;
    }

    private ImageSequence readImageSequence(GrinDataInputStream dis) throws IOException {
      
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
        String name = dis.readUTF();
        int startX = dis.readInt();
        int startY = dis.readInt();
        String filename = dis.readUTF();
        String[] middle = dis.readStringArray();       
        String extension = dis.readUTF();
        boolean repeat = dis.readBoolean();
        Command[] endCommands = readCommands(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }       
        return new ImageSequence(show, name, startX, startY, filename, middle, extension, repeat, endCommands);
    }

    
    private SrcOver readSrcOver(GrinDataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }   
        String name = dis.readUTF();
        Feature part = features[dis.readInt()];
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }       
        SrcOver srcOver = new SrcOver(show, name);
        srcOver.setup(part);
       
        return srcOver;
    }
    

    private Text readText(GrinDataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
        String name = dis.readUTF();
        int x = dis.readInt();
        int y = dis.readInt();
        String[] strings = dis.readStringArray();
        int vspace = dis.readInt();
        Font font = dis.readFont();
        length = dis.readInt();
        Color[] colors = new Color[length];
        for (int i = 0; i < colors.length; i++) {
            colors[i] = dis.readColor();
        }

        Color background = dis.readColor();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }     
        return new Text(show, name, x, y, strings, vspace, font, colors, background);
 
    }

    private Timer readTimer(GrinDataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }
        String name = dis.readUTF();
        int numFrames = dis.readInt();
        boolean repeat = dis.readBoolean();
       
        Command[] endCommands = readCommands(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }      
        return new Timer(show, name, numFrames, repeat, endCommands);
    }

    private Translation readTranslation(GrinDataInputStream dis) throws IOException {  
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }        
        String name = dis.readUTF();  
        int[] frames = dis.readIntArray();
        int[] xs = dis.readIntArray();
        int[] ys = dis.readIntArray();
        int repeatFrame = dis.readInt();
        Command[] endCommands = readCommands(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }       
        return new Translation(show, name, frames, xs, ys, repeatFrame, endCommands);
  
    }

    private Translator readTranslator(GrinDataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }        
        String name = dis.readUTF();
       
        int index = dis.readInt();
        Translation translation = (Translation) features[index];
        Feature[] parts = readFeaturesIndex(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }              
        
        Translator translator = new Translator(show, name);
        translator.setup(translation, parts);
       
        return translator;
    }
    
    private Modifier readUserModifier(GrinDataInputStream dis) throws IOException {
        if (dis.readByte() == Constants.NULL) {
            return null;
        }
        
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }  
        String name = dis.readUTF();
        String typeName = dis.readUTF();
        String arg = dis.readUTF();
        Feature parts = features[dis.readInt()];
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }       
        Modifier modifier = director.getExtensionsBuilder().getModifier(show, typeName, name, arg);
        modifier.setup(parts);
       
        return modifier;
    }
    
    private Command[] readCommands(GrinDataInputStream in) 
        throws IOException {
 
        if (in.readByte() == Constants.NULL) {
           return null;
        }

        int count = in.readInt(); 
   
        Command[] commands = new Command[count];
       
        for (int i = 0; i < count; i++) {
           byte identifier = in.readByte();           
           switch (identifier) {
               case Constants.ACTIVATEPART_CMD_IDENTIFIER:
                   commands[i] = readActivatePartCmd(in);
                   break;
               case Constants.ACTIVATESEGMENT_CMD_IDENTIFIER:
                   commands[i] = readActivateSegmentCmd(in);
                   break;
               case Constants.SEGMENTDONE_CMD_IDENTIFIER :
                   commands[i] = readSegmentDoneCmd(in);
                   break;
               case Constants.SETVISUALRCSTATE_CMD_IDENTIFIER :
                   commands[i] = readSetVisualRCStateCmd(in);
                   break;
               default:
                   commands[i] = readUserCmd(in);
                   break;    
           }
       }
     
       return commands;
    }

    private SetVisualRCStateCommand readSetVisualRCStateCmd(GrinDataInputStream dis) throws IOException {

        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }  
        
        final boolean activated = dis.readBoolean();
        final int state = dis.readInt();
        final int handlerIndex = dis.readInt();
        boolean runCommands = dis.readBoolean();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }        
        final SetVisualRCStateCommand command = new SetVisualRCStateCommand();
        
        if (rcHandlers[handlerIndex] != null) {
            command.setup(activated, state, (VisualRCHandler)rcHandlers[handlerIndex]);
        } else {
            deferred.add(new CommandSetup() {
               public void setup() {
                  command.setup(activated, state, (VisualRCHandler)rcHandlers[handlerIndex]);
               } 
            });
        }    
        return command;
    }
    
    private ActivatePartCommand readActivatePartCmd(GrinDataInputStream dis) 
        throws IOException {
        
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }    
        final int assemblyIndex = dis.readInt();
        final int partIndex = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }        
        
        final ActivatePartCommand command = new ActivatePartCommand();
        
        if (features[assemblyIndex] != null && features[partIndex] != null) {
            command.setup((Assembly)features[assemblyIndex], (Feature) features[partIndex]);
        } else {
            deferred.add(new CommandSetup() {
                public void setup() {
                    command.setup((Assembly)features[assemblyIndex], (Feature)features[partIndex]);
                } 
            });
        }    
        return command;
    }

    private ActivateSegmentCommand readActivateSegmentCmd(GrinDataInputStream dis) 
        throws IOException {

        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }  
        
        boolean push = dis.readBoolean();
        boolean pop = dis.readBoolean();
        Segment segment = null;
        final int segmentIndex = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }        
        final ActivateSegmentCommand command = new ActivateSegmentCommand(show, push, pop);
        if (segmentIndex != -1) {
            if (segments[segmentIndex] != null) {
                command.setup((Segment)segments[segmentIndex]);
            } else {
                deferred.add(new CommandSetup() {
                    public void setup() {
                        command.setup((Segment)segments[segmentIndex]);
                    } 
                });
            }    
        }    
        
        return command;
    } 

    private SegmentDoneCommand readSegmentDoneCmd(GrinDataInputStream dis) 
        throws IOException {
       
        return new SegmentDoneCommand(show);
    }

    private Command readUserCmd(GrinDataInputStream dis) 
        throws IOException {
        
        if (dis.readByte() == Constants.NULL) {
            return null;
        }
        
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }   
        String name = dis.readUTF();
        String[] args = dis.readStringArray();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }       
        return show.getDirector().getExtensionsBuilder().getCommand(show, name, args);
       
    }

    private RCHandler readCommandRCHandler(GrinDataInputStream dis) throws IOException {
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }  
        
        String name = dis.readUTF();
        int mask = dis.readInt();     
        Command[] commands = readCommands(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }        
        CommandRCHandler command = new CommandRCHandler(name, mask, commands);
        
        return command;        
    }

    private VisualRCHandler readVisualRCHandler(GrinDataInputStream dis) throws IOException {
        
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }  
        
        String name = dis.readUTF();
        int[][] grid = dis.readInt2Array();
        String[] stateNames = dis.readStringArray();
        Command[][] selectCommands;
        if (dis.readByte() == Constants.NULL) {
            selectCommands = null;
        } else {
            selectCommands = new Command[dis.readInt()][];
            for (int i = 0; i < selectCommands.length; i++) {
                selectCommands[i] = readCommands(dis);
            }
        }
        Command[][] activateCommands;
        if (dis.readByte() == Constants.NULL) {
            activateCommands = null;
        } else {
            activateCommands = new Command[dis.readInt()][];
            for (int i = 0; i < activateCommands.length; i++) {
                activateCommands[i] = readCommands(dis);
            }
        }
        
        Rectangle[] mouseRects = dis.readRectangleArray();
        int[] mouseRectStates = dis.readIntArray();
        int timeout = dis.readInt();
        Command[] timeoutCommands = readCommands(dis);
        
        Assembly assembly;
        int assemblyIndex = dis.readInt();
        if (assemblyIndex == -1) {
            assembly = null;
        } else {
            assembly = (Assembly) features[assemblyIndex];
        }    
        Feature[] selectFeatures = readFeaturesIndex(dis);
        Feature[] activateFeatures = readFeaturesIndex(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }        
                           
        VisualRCHandler visualRCHandler = new VisualRCHandler(name, grid,
            stateNames, selectCommands, activateCommands, mouseRects, mouseRectStates,
            timeout, timeoutCommands);
        
        visualRCHandler.setup(assembly, selectFeatures, activateFeatures);
        
        return visualRCHandler;
    }

    private Segment readSegment(GrinDataInputStream dis) throws IOException {
        
        int length = dis.readInt();
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).pushExpectedLength(length);
        }  
        
        String name = dis.readUTF();
        Feature[] active = readFeaturesIndex(dis);
        Feature[] setup = readFeaturesIndex(dis);
        
        length = dis.readInt();
        RCHandler[] handlers = new RCHandler[length];
        for (int i = 0; i < handlers.length; i++) {
            handlers[i] = (RCHandler)rcHandlers[dis.readInt()];
        }
        
        boolean nextOnSetupDone = dis.readBoolean();
        Command[] commands = readCommands(dis);
        if (Debug.ASSERT) {
            ((DebugInputStream)stream).popExpectedLength();
        }        

        // TODO: what about ChapterManager?
        return new Segment(name, active, setup, null, handlers, nextOnSetupDone, commands);
        
    }
    
    private Feature[] readFeaturesIndex(DataInputStream in) throws IOException {
        
        if (in.readByte() == Constants.NULL)
            return null;
        
        Feature[] f = new Feature[in.readInt()];
        
        for (int i = 0; i < f.length; i++) {
            int index = in.readInt();
            if (index == -1) {
                f[i] = null;
            } else {
                f[i] = (Feature) features[index];
            }
        }   
        
        return f;
    }
    
    /**
     * A class to resolve forward references from the Command objects.
     * GrinBinaryReader.read(...) methods for Command uses this class to
     * deferr resolving the references whenever necessary.
     */
    abstract class CommandSetup {
        abstract void setup(); 
    }    
}
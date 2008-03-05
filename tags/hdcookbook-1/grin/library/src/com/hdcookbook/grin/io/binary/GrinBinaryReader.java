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

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.commands.SegmentDoneCommand;
import com.hdcookbook.grin.commands.SetVisualRCStateCommand;
import com.hdcookbook.grin.GrinXHelper;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.Clipped;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.GuaranteeFill;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.SetTarget;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;
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
 *      StringArray_info()
 *      IntArrays_info()
 *      Show_Setup_info()
 *      Nodes_declarations()
 *      Nodes_contents()
 *  }
 *
 *  StringArray_info {  // Saves all String values needed in this binary file.
 *                      // The array index integer is used in the file to
 *                       // refer to String values.
 *      array_length        integer
 *      for (i = 0; i < array_length; i++) {
 *          value                           String
 *      }
 *  }
 * 
 *  IntArrays_info {  // Saves all immutable int[] instances needed in this binary file.
 *                    // The array index integer is used in the file to
 *                    // refer to 2 dimensional int arrays that can be shared.
 *      array_length        integer
 *      for (i = 0; i < array_length; i++) {
 *          value                           int[]
 *      }
 *  }
 *  
 *  Show_setup_info() {
 *      show_segment_stack_depth           integer
 *      draw_targets                       String[]
 *      isdebuggable                       boolean
 *      ShowCommand_classname              String
 *  }
 *  
 *  Nodes_declaration() {
 *      foreach (Feature_array, RCHandler_array, Segment_array) 
 *         array_length                    integer
 *         for (i = 0; i < array_length; i++) {
 *             class_indicator     byte                 
 *         }
 *      }
 *  }
 *  
 *  Nodes_contents() {
 *      foreach (Feature_array, RCHandler_array, Segment_array) 
 *         array_length                    integer
 *         for (i = 0; i < array_length; i++) {
 *             node specific info              
 *         }
 *      }
 *  }
 *
 *  --------------------------------      
 */

public class GrinBinaryReader {

    private Show show;
    
    private ArrayList featureList;
    private ArrayList rcHandlerList;
    private ArrayList segmentList;
    Hashtable publicSegments = new Hashtable();
    Hashtable publicFeatures= new Hashtable();
    Hashtable publicRCHandlers = new Hashtable();
    
    private InputStream stream;
    private Class showCommandsClass = null;
    private String[] stringConstants = null;
    private int[][]  intArrayConstants = null;
    private GrinXHelper showCommands = null;
    
    /*
     * If true, the binary file contains some debugging information.
     */
    boolean debuggable = false; 
    
    /**
     * Constructs a GrinBinaryReader instance.
     *
     * @param stream    An InputStream to the grin binary format data.  It is recommended to be
     *                  an instance of BufferedInputStream for a performance improvement.
     */
    public GrinBinaryReader(InputStream stream) {
       
       if (Debug.ASSERT) {
           this.stream = new DebugInputStream(stream);
       } else {
          this.stream = stream;
       }
       
    }

    /**
     * Returns an instace of feature that corresponds to the index number
     * that this GrinBinaryReader keeps track of.
     * This method is expected to be used by the user defined ExtensionsReader class.
     * 
     * @param index     The index number for the feature.
     * @return          The feature corresponding to the index number.
     * 
     * @see GrinBinaryWriter#getFeatureIndex(Feature)
     */
     Feature getFeatureFromIndex(int index) throws IOException {
        if (index == -1 || index > featureList.size()) {
            throw new IOException("non-existing feature reference");
        }  else {
            return (Feature) featureList.get(index);
        }
        
    }
 
    /**
     * Returns an instace of a segment that corresponds to the index number
     * that this GrinBinaryReader keeps track of.
     * This method is expected to be used by the user defined ExtensionsReader class.
     * 
     * @param index     The index number for the feature.
     * @return          The segment corresponding to the index number
     * 
     * @see GrinBinaryWriter#getSegmentIndex(Segment)
     */
     Segment getSegmentFromIndex(int index) throws IOException {
        if (index == -1 || index > segmentList.size()) {
            throw new IOException("non-existing segment reference");
        }  else {
            return (Segment) segmentList.get(index);
        } 
        
    }
    
    /**
     * Returns an instace of a RCHandler that corresponds to the index number
     * that this GrinBinaryReader keeps track of.
     * This method is expected to be used by the user defined ExtensionsReader class.
     * 
     * @param index     The index number for the feature.
     * @return          The RCHandler corresponding to the index number
     * 
     * @see GrinBinaryWriter#getRCHandlerIndex(RCHandler)
     */
    RCHandler getRCHandlerFromIndex(int index) throws IOException {
        if (index == -1 || index > rcHandlerList.size()) {
            throw new IOException("non-existing rchandler reference");
        }  else {
            return (RCHandler) rcHandlerList.get(index);
        }
        
    }

    int[] readIntArrayFromReference(int index) {
        if (index == -1 || index > intArrayConstants.length) {
            //return null;
        }  else {
            return intArrayConstants[index];
        }
        
            throw new RuntimeException("wrong int array reference");
    }

    String readStringFromReference(int index) {
        if (index == -1 || index > stringConstants.length) {
            //return null;
        }  else {
            return stringConstants[index];
        }
        
            throw new RuntimeException("wrong string reference");
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
     * Reconstructs the Show object passed in as argument.
     *
     * @param show	An empty Show object to reconstruct.
     * @throws IOException if binary data parsing fails.
     */
    
    public void readShow(Show show) throws IOException {

        this.show = show;
        
        GrinDataInputStream in = new GrinDataInputStream(stream, this);       
        checkScriptHeader(in);

        stringConstants = readStringConstants(in);
        intArrayConstants = readIntArrayConstants(in);
        
        int showSegmentStackDepth = in.readInt();
	String[] showDrawTargets = in.readStringArray();
        show.setSegmentStackDepth(showSegmentStackDepth);
	show.setDrawTargets(showDrawTargets);
        debuggable = in.readBoolean();
            
        // Read in the show file
	readShowCommandsClass(in);
        showCommands = instantiateShowCommandsCmd();
        
        featureList = new ArrayList();
        rcHandlerList = new ArrayList();
        segmentList = new ArrayList();
        
        readDeclarations(in, featureList);
        readDeclarations(in, rcHandlerList);
        readDeclarations(in, segmentList);  
        readContents(in, featureList);
        readContents(in, rcHandlerList);
        readContents(in, segmentList);
        
        Feature[] features = 
                (Feature[]) featureList.toArray(new Feature[featureList.size()]);
        RCHandler[] rcHandlers = 
                (RCHandler[]) rcHandlerList.toArray(new RCHandler[rcHandlerList.size()]);
        Segment[] segments = 
                (Segment[]) segmentList.toArray(new Segment[segmentList.size()]);        

	show.buildShow(segments, features, rcHandlers, 
		       publicSegments, publicFeatures, publicRCHandlers);
    }

    private void readDeclarations(GrinDataInputStream in, ArrayList list)
            throws IOException {

        int length = in.readInt();
        Node node;

        for (int i = 0; i < length; i++) {
            int identifier = in.readInt();
            switch (identifier) {
                case Constants.ASSEMBLY_IDENTIFIER:
                    node = new Assembly(show);
                    break;
                case Constants.BOX_IDENTIFIER:
                    node = new Box(show);
                    break;
                case Constants.CLIPPED_IDENTIFIER:
                    node = new Clipped(show);
                    break;
                case Constants.FADE_IDENTIFIER:
                    node = new Fade(show);
                    break;
                case Constants.FIXEDIMAGE_IDENTIFIER:
                    node = new FixedImage(show);
                    break;
                case Constants.GROUP_IDENTIFIER:
                    node = new Group(show);
                    break;
                case Constants.IMAGESEQUENCE_IDENTIFIER:
                    node = new ImageSequence(show);
                    break;
                case Constants.TEXT_IDENTIFIER:
                    node = new Text(show);
                    break;
                case Constants.INTERPOLATED_MODEL_IDENTIFIER:
                    node = new InterpolatedModel(show);
                    break;
                case Constants.TRANSLATOR_IDENTIFIER:
                    node = new Translator(show);
                    break;
                case Constants.SRCOVER_IDENTIFIER:
                    node = new SrcOver(show);
                    break;
                case Constants.GUARANTEE_FILL_IDENTIFIER:
                    node = new GuaranteeFill(show);
                    break;
                case Constants.SET_TARGET_IDENTIFIER:
                    node = new SetTarget(show);
                    break;
                case Constants.COMMAND_RCHANDLER_IDENTIFIER:
                    node = new CommandRCHandler();
                    break;
                case Constants.VISUAL_RCHANDLER_IDENTIFIER:
                    node = new VisualRCHandler();
                    break;
                case Constants.SEGMENT_IDENTIFIER:
                    node = new Segment();
                    break;
                case Constants.ACTIVATEPART_CMD_IDENTIFIER:
                    node = new ActivatePartCommand(show);
                    break;
                case Constants.ACTIVATESEGMENT_CMD_IDENTIFIER:
                    node = new ActivateSegmentCommand(show);
                    break;
                case Constants.SEGMENTDONE_CMD_IDENTIFIER:
                    node = new SegmentDoneCommand(show);
                    break;
                case Constants.SETVISUALRCSTATE_CMD_IDENTIFIER:
                    node = new SetVisualRCStateCommand(show);
                    break;
                case Constants.NULL: // happens for commands
                    node = null;
                default:  // extensions  
                    if (showCommands == null) {
                        throw new IOException("Missing GrinXHelper for instantiating extensions");
                    }
                    node = showCommands.getInstanceOf(show, identifier);
                    break;
                }

            list.add(node);
        }

    }
    
    private int[][] readIntArrayConstants(GrinDataInputStream in) 
        throws IOException {
        
        checkValue(in.readByte(),
                Constants.INT_ARRAY_CONSTANTS_IDENTIFIER,
                "Integer array constants identifier");        
        int length = in.readInt();
        int[][] array = new int[length][];
        for (int i = 0; i < length; i++) {
            array[i] = in.readIntArray();
        }
        return array;
    }

    
    private void readShowCommandsClass(GrinDataInputStream in)
	   throws IOException 
    {
	String className = in.readString();
	if (className == null) {
	    return;
	}
	try {
	    showCommandsClass = Class.forName(className);
	} catch (Exception ex) {
	    throw new IOException(ex.toString());
	}
    }

    private void readContents(GrinDataInputStream in, ArrayList list) 
       throws IOException {
        
        for (int i = 0; i < list.size(); i++) {  
            Node node = (Node) list.get(i);
            if (node != null) {
                int length = in.readInt();
                if (Debug.ASSERT) {
                    ((DebugInputStream) stream).pushExpectedLength(length);
                }
                node.readInstanceData(in, length);
                if (Debug.ASSERT) {
                    ((DebugInputStream) stream).popExpectedLength();
                }
            }
        }
    }

    private String[] readStringConstants(GrinDataInputStream in) 
        throws IOException {
        checkValue(in.readByte(), 
                Constants.STRING_CONSTANTS_IDENTIFIER,
                "String array identifier");
        
        String[] strings = new String[in.readInt()];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = in.readUTF();
        }
        return strings;
    }

    Command[] readCommands(GrinDataInputStream in) 
	    throws IOException 
    {
        if (in.isNull()) {
           return null;
        }

        ArrayList commands = new ArrayList();
        readDeclarations(in, commands);
        readContents(in, commands);
       
        return (Command[]) commands.toArray(new Command[commands.size()]);
    }

    private GrinXHelper instantiateShowCommandsCmd() 
        throws IOException {	
        if (showCommandsClass == null) {
            return null;
	}
	GrinXHelper result;
        Class[] paramType = { Show.class };
        Object[] param = { show };
	try {
	    result = (GrinXHelper) 
                showCommandsClass.getConstructor(paramType).newInstance(param);
	} catch (Throwable ex) {
	    throw new IOException(ex.toString());
	}
        return result;
    }  
}

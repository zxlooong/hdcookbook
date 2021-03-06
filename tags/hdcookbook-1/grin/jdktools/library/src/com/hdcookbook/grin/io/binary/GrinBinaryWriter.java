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

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.GrinXHelper;
import com.hdcookbook.grin.Node;
import com.hdcookbook.grin.SENode;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.SEShow;
import com.hdcookbook.grin.SEShowCommands;
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
import com.hdcookbook.grin.features.SetTarget;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.InterpolatedModel;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.input.VisualRCHandler;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import static com.hdcookbook.grin.io.binary.Constants.*;


/**
 * The main class to write out the Show object to a binary file format.
 */

/*
 *
 * This class works with a binary file reader, GrinBinaryReader, which contains
 * read(...) versions of the methods defined in this class.
 * If you change one file, make sure to update the other file as well.
 *
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
public class GrinBinaryWriter {

    /**
     * Show file to write out 
     */
    SEShow show;
    
    /**
     * SEShowCommands file to write out
     */
    private SEShowCommands seShowCommands;
    
    /**
     * List of Feature, RCHandler and Segments in the show.
     */
    private IndexedSet<SENode> featureList;
    private IndexedSet<SENode> rcHandlerList;
    private IndexedSet<SENode> segmentList;
    
    /**
     * List of shared String instances.
     */
    private IndexedSet<String> stringsList;
    
    /* 
     * List of shared integer array instances.
     */
    private IndexedSet<IntArray> intArrayList;
    
    /**
     * List of class names, both built-ins and extensions.
     */
    private IndexedSet<String> runtimeClassNames;
    
    /**
     * An index indicating the beginning of the extension
     * classses in the runtimeClassNames IndexedSet above.
     * We want to auto-generate the code for instantiating
     * extensions but not for the built-time classes.
     */
    private int extensionIndex = 0;
    
    /* 
     * An indication of whether to include debug information
     * to the binary file.  If true, write out the name field of
     * non-public segements and features.
     */
    boolean isDebugging = false;
    
    /**
     * Constructs GrinBinaryWriter.
     * 
     * @param show The show object which this GrinBinaryWriter makes into a binary format.
     **/
    public GrinBinaryWriter(SEShow show, boolean isDebugging) {
       
        this.show = show;
        this.seShowCommands = show.getShowCommands();
        this.isDebugging = isDebugging;
        
        featureList = new IndexedSet();
        rcHandlerList = new IndexedSet();
        segmentList = new IndexedSet();
        
        Feature[] features = show.getFeatures();
        for (Feature feature: features) {
            featureList.getIndex((SENode) feature);
        }    
        
        RCHandler[] rcHandlers = show.getRCHandlers();
        for (RCHandler rcHandler: rcHandlers) {
            rcHandlerList.getIndex((SENode) rcHandler);
        }     
        
        Segment[] segments = show.getSegments();
        for (Segment segment: segments) {
            segmentList.getIndex((SENode) segment);
        }         
        
        stringsList = new IndexedSet();
        intArrayList = new IndexedSet();
        runtimeClassNames = new IndexedSet();
    }
    
    /**
     * Returns an index number of the feature that this GrinBinaryWriter class 
     * is internally using in the Show.  
     * 
     * @see GrinBinaryReader#getFeatureFromIndex(int)
     * @param feature the feature to get the index number of.
     * @return the index number for the feature, or -1 if no such feature exists.
     */
    int getFeatureIndex(Feature feature) {
        if (feature == null) {
           return -1;
        } else {
           int index = featureList.getIndex((SENode) feature);
           return index;
        }   
    }
    
    /**
     * Returns an index number of the segment that this GrinBinaryWriter class 
     * is internally using in the Show.  
     * 
     * @see GrinBinaryReader#getSegmentFromIndex(int)
     * @param segment the segment to get the index number of.
     * @return the index number for the segment, or -1 if no such segment exists.
     */
    int getSegmentIndex(Segment segment) {
        if (segment == null) {
           return -1;
        } else {
           int index = segmentList.getIndex((SENode) segment);
           return index;
        }   
    }
    
    /**
     * Returns an index number of the RCHandler that this GrinBinaryWriter class 
     * is internally using in the Show.  
     * 
     * @see GrinBinaryReader#getRCHandlerFromIndex(int)
     * @param rcHandler the RCHandler to get the index number of.
     * @return the index number for the RCHandler, or -1 if no such RCHandler exists.
     */
    int getRCHandlerIndex(RCHandler rcHandler) {
        if (rcHandler == null) {
           return -1;
        } else {
           int index = rcHandlerList.getIndex((SENode) rcHandler);
           return index;
        }   
    }    
    
    int getStringIndex(String s) {
        return stringsList.getIndex(s);
    }

    int getIntArrayIndex(int[] array) {
        return intArrayList.getIndex(new IntArray(array));
    }
    /**
     * Writes out the script identifier and the script version to the DataOutputStream.
     *
     */
    private static void writeScriptIdentifier(DataOutputStream out) 
            throws IOException {
        out.writeInt(GRINSCRIPT_IDENTIFIER);
        out.writeInt(GRINSCRIPT_VERSION);
    }
 
    /**
     * Writes out the show object for this GrinBinaryWriter.
     *
     * @param out The DataOutputStream to write out show's binary data to.
     * @throws IOException if writing to the DataOutputStream fails.
     */
    public void writeShow(DataOutputStream out) 
            throws IOException {
        
        // The first item on the script should be a header.
        writeScriptIdentifier(out);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GrinDataOutputStream dos = new GrinDataOutputStream(baos, this);

        // First, include all of the known pre-defined Grin runtime node
        // class names into the runtimeClasses data structure.
        registerBuiltInClasses();
        extensionIndex = runtimeClassNames.size();
        
        // Write out information about the show itself.
        dos.writeInt(show.getSegmentStackDepth());
        dos.writeStringArray(show.getDrawTargets());
        dos.writeBoolean(isDebugging);
        dos.writeString(seShowCommands.getClassName());
        
        // We can create one large SENode array including all elements and write
        // it out, but if we do that, the reader needs to sort through it and
        // separate it out to three arrays for reconstructing Show.
        // Hence, treating them as three arrays for now.
        SENode[] features =
                (SENode[])featureList.toArray(SENode.class);
        SENode[] handlers = 
                (SENode[])rcHandlerList.toArray(SENode.class);
        SENode[] segments = 
                (SENode[])segmentList.toArray(SENode.class);
      
        writeDeclarations(dos, features);
        writeDeclarations(dos, handlers);
        writeDeclarations(dos, segments);
        writeContents(dos, features);  
        writeContents(dos, handlers); 
        writeContents(dos, segments);   
        
        // Note, this is writing to the base DataOutputStream instance.
        // We want to write out all the String and integer array instances 
        // at the beginning of the binary file.
        writeStringConstants(out, (String[]) stringsList.toArray(String.class));
        writeIntArrayConstants(out, (IntArray[]) intArrayList.toArray(IntArray.class));
        
        baos.writeTo(out);
        dos.close();  
    }

    /**
     * Records buildin node classes.
     * Note that order of the registration needs to match exactly the value of the
     * Identifier integer defined in Constants.java, since the identifier values
     * are later used as an index to the runtimeClassName list (see 
     * getRuntimeClassNames.getIndex() call in writeDeclarations(OutputStream, SENode[])).
     */
    private void registerBuiltInClasses() throws IOException {
        registerBuiltInClass(ASSEMBLY_IDENTIFIER, Assembly.class.getName());
        registerBuiltInClass(BOX_IDENTIFIER, Box.class.getName());
        registerBuiltInClass(FIXEDIMAGE_IDENTIFIER, FixedImage.class.getName());
        registerBuiltInClass(GROUP_IDENTIFIER, Group.class.getName());
        registerBuiltInClass(IMAGESEQUENCE_IDENTIFIER, ImageSequence.class.getName());
	registerBuiltInClass(TEXT_IDENTIFIER, Text.class.getName());
	registerBuiltInClass(INTERPOLATED_MODEL_IDENTIFIER, InterpolatedModel.class.getName());
	registerBuiltInClass(TRANSLATOR_IDENTIFIER, Translator.class.getName());
	registerBuiltInClass(CLIPPED_IDENTIFIER, Clipped.class.getName());
	registerBuiltInClass(FADE_IDENTIFIER, Fade.class.getName());
	registerBuiltInClass(SRCOVER_IDENTIFIER, SrcOver.class.getName());        
        registerBuiltInClass(ACTIVATEPART_CMD_IDENTIFIER, ActivatePartCommand.class.getName());
        registerBuiltInClass(ACTIVATESEGMENT_CMD_IDENTIFIER, ActivateSegmentCommand.class.getName());
        registerBuiltInClass(SEGMENTDONE_CMD_IDENTIFIER, SegmentDoneCommand.class.getName());
        registerBuiltInClass(SETVISUALRCSTATE_CMD_IDENTIFIER, SetVisualRCStateCommand.class.getName());
        registerBuiltInClass(COMMAND_RCHANDLER_IDENTIFIER, CommandRCHandler.class.getName());
        registerBuiltInClass(VISUAL_RCHANDLER_IDENTIFIER, VisualRCHandler.class.getName());
        registerBuiltInClass(GUARANTEE_FILL_IDENTIFIER, GuaranteeFill.class.getName());	
        registerBuiltInClass(SET_TARGET_IDENTIFIER, SetTarget.class.getName());
	registerBuiltInClass(SEGMENT_IDENTIFIER, Segment.class.getName());
    }

    private void registerBuiltInClass(int identifier, String className) 
        throws IOException {
        if (runtimeClassNames.getIndex(className) != identifier)
            throw new IOException("Built-in class ID mismatch for " + className);
    }

    private void writeDeclarations(GrinDataOutputStream out, SENode[] nodes) 
            throws IOException {
        
        if (nodes == null) {
            return;
        }
        
        out.writeInt(nodes.length);    
        for (SENode node : nodes) {
            if (node == null) {
                out.writeInt(NULL);
            }
            String runtimeName = node.getRuntimeClassName();
            out.writeInt(runtimeClassNames.getIndex(runtimeName));
        }
    }   
    
    private void writeContents(GrinDataOutputStream out, SENode[] nodes) 
       throws IOException {
	
        if (nodes == null) 
            return;
		
        for (SENode node: nodes) {          
            if (node != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                GrinDataOutputStream dos = new GrinDataOutputStream(baos, this);
                node.writeInstanceData(dos);
                out.writeInt(baos.size());
                baos.writeTo(out);      
                dos.close();
            }
        }
    }

    private void writeStringConstants(DataOutputStream out, String[] list) 
        throws IOException {
        out.writeByte(STRING_CONSTANTS_IDENTIFIER);     
        out.writeInt(list.length);
        for (String str : list) {
            out.writeUTF(str);
        }
    }
    
    private void writeIntArrayConstants(DataOutputStream out, IntArray[] list) 
        throws IOException {
        out.writeByte(INT_ARRAY_CONSTANTS_IDENTIFIER);     
        out.writeInt(list.length);
        for (IntArray intArray : list) {
            int[] array = intArray.array;
            if (array == null) {
                out.writeByte(Constants.NULL);
            } else {
                out.writeByte(Constants.NON_NULL);
                out.writeInt(array.length);
                for (int j = 0; j < array.length; j++) {
                    out.writeInt(array[j]);
                }
            }
        }       
    }
   
    void writeCommands(GrinDataOutputStream out, Command[] commands) 
        throws IOException {
 
       if (commands == null) {
           out.writeByte(NULL);
           return;
       }     
       out.writeByte(NON_NULL);
       
       SENode[] nodes = new SENode[commands.length];
       for (int i = 0; i < nodes.length; i++) {
           nodes[i] = (SENode) commands[i];
       }
       
       writeDeclarations(out, nodes);
       writeContents(out, nodes);
       
       for (int i = 0; i < nodes.length; i++) {
           commands[i] = (Command) nodes[i];
       }
    }   
  
   /**
    * Writes out an auto-generated java class that includes information about
    * Extension classes and ShowCommand subclasses.
    */
   public void writeCommandClass(SEShow show, boolean forXlet, File file)
           throws IOException 
   {
       SEShowCommands cmds = show.getShowCommands();
       if (cmds.getClassName() == null) {
           file.delete();  // Just in case an old version was there
           return;      // No commands class
       }
       FileWriter w = new FileWriter(file);
       String extensionCode = generateExtensionCode();
       w.write(cmds.getJavaSource(forXlet, extensionCode));
       w.close();
   }
   
   private String generateExtensionCode() {     
       
       String[] list = (String[]) runtimeClassNames.toArray(String.class);
       assert (extensionIndex <= list.length);
       
       String nodeClassName = Node.class.getName();
       StringBuffer generated = new StringBuffer();
       generated.append("    public " + nodeClassName + " getInstanceOf(Show show, int id) ");
       generated.append("throws java.io.IOException {\n");
       generated.append("        switch (id) {\n");
       for (int i = extensionIndex; i < list.length; i++ ) {
            String className = (String) list[i];
            generated.append("         case " + i + ": ");
            if (className == null || "".equals(className)) {
                generated.append(" return null; \n");
            } else {
                generated.append(" return new " + className + "(show); \n");
            }    
        }
        generated.append("        }\n");
        generated.append("    throw new java.io.IOException(\"Error instantiating extension, id=\" + id); \n \n    }\n");
        
        return generated.toString();             
   }
    
   private class IntArray {
       int[] array;
       int hashcode = 0;
       boolean isHashComputed = false;
       public IntArray(int[] array) {
           this.array = array;
       }
       public boolean equals(Object other) {
           if (!(other instanceof IntArray)) {
               return false;
           }
           return Arrays.equals(array, ((IntArray)other).array);
       }
       public synchronized int hashCode() {
           if (!isHashComputed) {
              hashcode = Arrays.hashCode(array);
              isHashComputed = true;
           }
           return hashcode;
       }
   }
   
   private class IndexedSet<T> {

        HashMap<T, Integer> mapTToInt = new HashMap();
        
        public synchronized int getIndex(T element) {
            Integer i = mapTToInt.get(element);
            if (i == null) {
                i = new Integer(mapTToInt.size());
                mapTToInt.put(element, i);
            }
            return i;    // autoboxing converts to int
        }

        public synchronized T[] toArray(Class type) {
            
            T[] result = (T[]) Array.newInstance(type, size());
            
            for (Map.Entry<T, Integer> entry : mapTToInt.entrySet()) {
               assert result[entry.getValue()] == null;
               result[entry.getValue()] = entry.getKey();
            }
            
            return result;
        }

        public synchronized int size() {
            return mapTToInt.size();
        }
    }
    
}

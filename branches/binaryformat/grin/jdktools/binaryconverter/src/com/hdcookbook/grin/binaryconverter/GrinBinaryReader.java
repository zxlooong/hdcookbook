
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
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;

import java.io.InputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.print.attribute.Size2DSyntax;

class GrinBinaryReader {

    private Show show;
    private Feature[] features;
    private RCHandler[] rcHandlers;
    private Segment[] segments;
    
    public GrinBinaryReader(Show show) {
       this.show = show;
    }

    private void checkValue(int x, int y, String message) throws IOException {
        if (x != y)
            throw new IOException("Mismatch: " + message);
    }
    
    void readScriptIdentifier(DataInputStream in) throws IOException {
       checkValue(in.readInt(), Constants.GRINSCRIPT_IDENTIFIER, "Script identifier");
       checkValue(in.readInt(), Constants.GRINSCRIPT_VERSION, "Script version");
    }
    
    protected Show readShow(DataInputStream in) throws IOException {
        
        features = new Feature[in.readInt()];
        rcHandlers = new RCHandler[in.readInt()];
        segments = new Segment[in.readInt()];
            
        readFeatures(in);
        readRCHandlers(in);
        readSegments(in);
        
        System.out.println("Features");
        for (int i = 0; i < features.length; i++) {
            System.out.println(i + " " + features[i]);
        }
        System.out.println("RCHandlers");
        for (int i = 0; i < rcHandlers.length; i++) {
            System.out.println(i + " " + rcHandlers[i]);
        }
        System.out.println("Segments");
        for (int i = 0; i < segments.length; i++) {
            System.out.println(i + " " + segments[i]);
        }
        
        
        // TODO
        return null;
    }
    
    protected void readFeatures(DataInputStream in) 
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

    protected void readRCHandlers(DataInputStream in) throws IOException {
        
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

    protected void readSegments(DataInputStream in) throws IOException {

        checkValue(in.readInt(), Constants.SEGMENT_IDENTIFIER, "Segment array identifier");
        
        int count = in.readInt();       
        Segment segment = null;
        
        for (int i = 0; i < count; i++) {
            in.readByte(); // SEGMENT_IDENTIFIER;
            segment = readSegment(in);
            segments[i] = segment;
        }
    } 
    
    protected Assembly readAssembly(DataInputStream in) throws IOException {
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);
       
        String name = dis.readUTF();
        String[] partNames = dis.readStringArray();
        Feature[] parts = readFeaturesIndex(dis);
        
        dis.close();
        
        Assembly assembly = new Assembly(show, name);
        //assembly.setParts(partNames, parts);
        
        return assembly;
    }

    protected Box readBox(DataInputStream in) throws IOException {
        
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);
       
       String name = dis.readUTF();
       Rectangle placement = dis.readRectangle();
       int outlineWidth = dis.readInt();
       Color outline = dis.readColor();
       Color fill = dis.readColor();
       
       dis.close();
       
       return new Box(show, name, placement, outlineWidth, outline, fill);
    }
    
    protected Clipped readClipped(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);
       
       String name = dis.readUTF();
       Rectangle clipRegion = dis.readRectangle();
       
       dis.close();
       
       return new Clipped(show, name, clipRegion);
       
    }
    
    protected Fade readFade(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);
       
       String name = dis.readUTF();
       boolean srcOver = dis.readBoolean();
       int[] keyframes = dis.readIntArray();
       int[] keyAlphas = dis.readIntArray();
       Command[] endCommands = readCommands(dis);
       
       dis.close();
   
       return new Fade(show, name, srcOver, keyframes, keyAlphas, endCommands);
      
    }

    protected FixedImage readFixedImage(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);      
       
       String name = dis.readUTF();
       int startX = dis.readInt();
       int startY = dis.readInt();
       String filename = dis.readUTF();
       
       dis.close();
       
       return new FixedImage(show, name, startX, startY, filename);
       
    }

    protected Group readGroup(DataInputStream in) throws IOException {

       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);   
       
       String name = dis.readUTF();
       Feature[] parts = readFeaturesIndex(dis);
       
       dis.close();
       
       Group group = new Group(show, name);
       //group.setup(parts);
       
       return group;
    }

    protected ImageSequence readImageSequence(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);  
       
       String name = dis.readUTF();
       int startX = dis.readInt();
       int startY = dis.readInt();
       String filename = dis.readUTF();
       String[] middle = dis.readStringArray();       
       String extension = dis.readUTF();
       boolean repeat = dis.readBoolean();
       Command[] endCommands = readCommands(dis);
       
       dis.close();
       
       return new ImageSequence(show, name, startX, startY, filename, middle, extension, repeat, endCommands);
    }

    
    protected SrcOver readSrcOver(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);  
       
       String name = dis.readUTF();
       
       dis.close();
       
       return new SrcOver(show, name);
    }
    

    protected Text readText(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais); 
       
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

       dis.close();
     
       return new Text(show, name, x, y, strings, vspace, font, colors, background);
 
    }

    protected Timer readTimer(DataInputStream in) throws IOException {
  
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais); 
             
       String name = dis.readUTF();
       int numFrames = dis.readInt();
       boolean repeat = dis.readBoolean();
       
       Command[] endCommands = readCommands(dis);
       
       dis.close();
      
       return new Timer(show, name, numFrames, repeat, endCommands);
    }

    protected Translation readTranslation(DataInputStream in) throws IOException {
        
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);   
        
       String name = dis.readUTF();  
       int[] frames = dis.readIntArray();
       int[] xs = dis.readIntArray();
       int[] ys = dis.readIntArray();
       int repeatFrame = dis.readInt();
       Command[] endCommands = readCommands(dis);
       
       dis.close();
       
       return new Translation(show, name, frames, xs, ys, repeatFrame, endCommands);
  
    }

    protected Translator readTranslator(DataInputStream in) throws IOException {

       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);   
        
       String name = dis.readUTF();
       
       int index = dis.readInt();
       Translation translation = (Translation) features[index];
       Feature[] parts = readFeaturesIndex(dis);
       
       dis.close();
       
       Translator translator = new Translator(show, name);
       //translator.setup(translation, parts);
       
       return translator;
    }
    
    protected Modifier readUserModifier(DataInputStream in) throws IOException {
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);   
        
       String name = dis.readUTF();

       dis.close();
       
       return show.getDirector().getExtensionsParser().getModifier(show, name, null, null);
    }
    
    public Command[] readCommands(DataInputStream in) 
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

    protected SetVisualRCStateCommand readSetVisualRCStateCmd(DataInputStream in) throws IOException {

        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);  
        
        boolean activated = dis.readBoolean();
        int state = dis.readInt();
        VisualRCHandler handler = (VisualRCHandler) rcHandlers[dis.readInt()];
        boolean runCommands = dis.readBoolean();
        
        dis.close();
        
        SetVisualRCStateCommand command = new SetVisualRCStateCommand();
        
        // command.setup(activated, state, handler); 
        
        return command;
    }

    
    protected ActivatePartCommand readActivatePartCmd(DataInputStream in) 
        throws IOException {
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);  
        
        Assembly assembly = (Assembly) features[dis.readInt()];
        Feature part = (Feature) features[dis.readInt()];
        
        dis.close();
        
        ActivatePartCommand command = new ActivatePartCommand();
        //command.setup(assembly, part);
     
        return command;
    }
    

    protected ActivateSegmentCommand readActivateSegmentCmd(DataInputStream in) 
        throws IOException {

        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);  
        
        boolean push = dis.readBoolean();
        boolean pop = dis.readBoolean();
        Segment segment = null;
        int segmentIndex = dis.readInt();
        if (segmentIndex != -1) {
            segment = segments[segmentIndex];
        } 
        
        dis.close();
        
        ActivateSegmentCommand command = new ActivateSegmentCommand(show, push, pop);
        //command.setup(segment);
        
        return command;
    }    



    protected SegmentDoneCommand readSegmentDoneCmd(DataInputStream in) 
        throws IOException {
       
        return new SegmentDoneCommand(show);
    }

    protected Command readUserCmd(DataInputStream in) 
        throws IOException {
        
        if (in.readByte() == Constants.NULL) {
            return null;
        }
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);   
        
        String name = dis.readUTF();

        dis.close();
       
       // TODO
       // return show.getDirector().getExtensionsParser().parseCommand();
       
        return null;
    }

    private RCHandler readCommandRCHandler(DataInputStream in) throws IOException {
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);  
        
        int mask = dis.readInt();
        
        Command[] commands = readCommands(dis);
        
        dis.close();
        
        CommandRCHandler command = new CommandRCHandler(mask, commands);
        
        return command;        
    }

    private VisualRCHandler readVisualRCHandler(DataInputStream in) throws IOException {
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);          
   
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
        
        Rectangle[] mouseRects = new Rectangle[dis.readInt()];
        for (int i = 0; i < mouseRects.length; i++) {
            mouseRects[i] = dis.readRectangle();
        }
        
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
        
        dis.close();
                           
        VisualRCHandler visualRCHandler = new VisualRCHandler(name, grid,
            stateNames, selectCommands, activateCommands, mouseRects, mouseRectStates,
            timeout, timeoutCommands);
        
        visualRCHandler.setup(assembly, selectFeatures, activateFeatures);
        
        return visualRCHandler;
    }

    private Segment readSegment(DataInputStream in) throws IOException {
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);          

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
        
        dis.close();

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
   
}
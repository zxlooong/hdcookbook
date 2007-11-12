
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
        
        // TODO
        return null;
    }
    
    protected void readFeatures(DataInputStream in) 
       throws IOException {
        
        if (features.length == 0)
            return;
        
        checkValue(in.readInt(), Constants.FEATURE_IDENTIFIER, "Feature array identifier");
        
        int count = in.readInt();
        
        for (int i = 0; i < features.length; i++) {
            int identifier = in.readByte();
            switch (identifier) {
                case Constants.ASSEMBLY_IDENTIFIER :
                    features[i] = readAssembly(in);
                    break;
                case Constants.BOX_IDENTIFIER :
                    features[i] = readBox(in);
                    break;
                case Constants.CLIPPED_IDENTIFIER :
                    features[i] = readClipped(in);
                    break;
                case Constants.FADE_IDENTIFIER :
                    features[i] = readFade(in);
                    break;
                case Constants.FIXEDIMAGE_IDENTIFIER :
                    features[i] = readFixedImage(in);
                    break;
                case Constants.GROUP_IDENTIFIER :
                    features[i] = readGroup(in);
                    break;
                case Constants.IMAGESEQUENCE_IDENTIFIER :
                    features[i] = readImageSequence(in);
                    break;
                case Constants.TEXT_IDENTIFIER :
                    features[i] = readText(in);
                    break;
                case Constants.TIMER_IDENTIFIER :
                    features[i] = readTimer(in);
                    break;
                case Constants.TRANSLATION_IDENTIFIER :
                    features[i] = readTranslation(in);
                    break;
                case Constants.TRANSLATOR_IDENTIFIER :
                    features[i] = readTranslator(in);
                    break;
                case Constants.SRCOVER_IDENTIFIER :
                    features[i] = readSrcOver(in);
                    break;
                case Constants.USER_MODIFIER_IDENTIFIER :
                    features[i] = readUserModifier(in);
                    break;
                default:
                    throw new IOException("Unknown feature identifier " + identifier);
            }       
        }
    }

    protected void readRCHandlers(DataInputStream in) throws IOException {
        
        if (rcHandlers.length == 0)
            return;
        
        checkValue(in.readInt(), Constants.RCHANDLER_IDENTIFIER, "RCHandler array identifier");
        
        int count = in.readInt();

        for (int i = 0; i < rcHandlers.length; i++) {
            int identifier = in.readByte();
            switch (identifier) {     
                case Constants.COMMAND_RCHANDLER_IDENTIFIER :
                    rcHandlers[i] = readCommandRCHandler(in);
                    break;
                case Constants.VISUAL_RCHANDLER_IDENTIFIER :
                    rcHandlers[i] = readVisualRCHandler(in);
                    break;
                default :
                    throw new IOException("Unknown RCHandler type " + identifier);
            }              
        }     
    }    

    protected void readSegments(DataInputStream in) throws IOException {
        if (segments.length == 0)
            return;
        
        checkValue(in.readInt(), Constants.SEGMENT_IDENTIFIER, "Segment array identifier");
        
        int count = in.readInt();
        
        for (int i = 0; i < segments.length; i++) {
            in.readByte(); // SEGMENT_IDENTIFIER;
            segments[i] = readSegment(in);
        }
    } 
    
    protected Assembly readAssembly(DataInputStream in) throws IOException {
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);
       
        String name = dis.readUTF();
        
        length = dis.readInt();
        String[] partNames = new String[length];
        for (int i = 0; i < length; i++) {
            partNames[i] = dis.readUTF();
        }
        
        length = dis.readInt();
        Feature[] parts = new Feature[length];
        for (int i = 0; i < length; i++) {
            int index = dis.readInt();
            parts[i] = features[index];
        }
        
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
       int[] keyframes = new int[dis.readInt()];
       for (int i = 0; i < keyframes.length; i++) {
           keyframes[i] = dis.readInt();
       }
       int[] keyAlphas = new int[dis.readInt()];
       for (int i = 0; i < keyAlphas.length; i++) {
           keyAlphas[i] = dis.readInt();
       }
       
       Command[] endCommands = null;
       
       if (bais.available() > 0) {
          endCommands = readCommands(dis);
       }
       
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
       length = dis.readInt();
       
       Feature[] parts = new Feature[length];
       for (int i = 0; i < length; i++) {
          int index = dis.readInt();
          parts[i] = features[index];
       }
       
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
       int middleLength = dis.readInt();
       String[] middle = new String[middleLength];
       
       for (int i = 0; i < middle.length; i++) {
           middle[i] = dis.readUTF();
       }
       
       String extension = dis.readUTF();
       boolean repeat = dis.readBoolean();
       
 
       Command[] endCommands = null;
       
       if (bais.available() > 0) {
          endCommands = readCommands(dis);
       }
       
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
       length = dis.readInt();
       String[] strings = new String[length];
       for (int i = 0; i < strings.length; i++) {
           strings[i] = dis.readUTF();
       }
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
       
       Command[] endCommands = null;
       if (bais.available() > 0) {
          endCommands = readCommands(dis);
       }
       
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
       
       length = dis.readInt();       
       int[] frames = new int[length];
       for (int i = 0; i < frames.length; i++) {
           frames[i] = dis.readInt();
       }
       
       length = dis.readInt();
       int[] xs = new int[length];
       for (int i = 0; i < xs.length; i++) {
           xs[i] = dis.readInt();
       }
       
       length = dis.readInt();
       int[] ys = new int[length];
       for (int i = 0; i < ys.length; i++) {
           ys[i] = dis.readInt();
       }
       
       int repeatFrame = dis.readInt();
       
       Command[] endCommands = null;
       if (bais.available() > 0) {
          endCommands = readCommands(dis);
       }
       
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
       
       length = dis.readInt();
       Feature[] parts = new Feature[length];
       for (int i = 0; i < length; i++) {
           index = dis.readInt();
           parts[i] = features[index];
       }
       
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
 
       checkValue(in.readInt(), Constants.COMMAND_IDENTIFIER, "Command identifier");

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
        Feature part = features[dis.readInt()];
        
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
        Segment segment = segments[dis.readInt()];
        
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

    private RCHandler readVisualRCHandler(DataInputStream in) {
        return null;
    }

    private Segment readSegment(DataInputStream in) throws IOException {
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);          

        String name = dis.readUTF();
        length = dis.readInt();
        Feature[] active = new Feature[length];
        for (int i = 0; i < active.length; i++) {
            active[i] = features[dis.readInt()];
        } 
        length = dis.readInt();
        Feature[] setup = new Feature[length];
        for (int i = 0; i < setup.length; i++) {
            setup[i] = features[dis.readInt()];
        }
        length = dis.readInt();
        RCHandler[] handlers = new RCHandler[length];
        for (int i = 0; i < handlers.length; i++) {
            handlers[i] = rcHandlers[dis.readInt()];
        }
        boolean nextOnSetupDone = dis.readBoolean();
        Command[] commands = readCommands(dis);
        
        dis.close();

        // TODO: what about ChapterManager?
        return new Segment(name, active, setup, null, rcHandlers, nextOnSetupDone, commands);
        
    }
}
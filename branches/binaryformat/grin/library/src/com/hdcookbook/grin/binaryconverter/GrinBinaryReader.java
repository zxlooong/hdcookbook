
package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.binaryconverter.*;
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

public class GrinBinaryReader {

    private Show show;
    private Director director;
    private Feature[] features;
    private RCHandler[] rcHandlers;
    private Segment[] segments;
    private String filename;
    private DataInputStream filereader;
    
    private ArrayList deferred = new ArrayList();
        
    public GrinBinaryReader(Director director, String filename) {
        
       this.director = director;
       this.filename = filename;
       
       show = new Show(director);
    }

    private void checkValue(int x, int y, String message) throws IOException {
        if (x != y)
            throw new IOException("Mismatch: " + message);
    }
    
    private void checkScriptIdentifier(DataInputStream in) throws IOException {
       checkValue(in.readInt(), Constants.GRINSCRIPT_IDENTIFIER, "Script identifier");
       checkValue(in.readInt(), Constants.GRINSCRIPT_VERSION, "Script version");
    }
    
    public Show readShow() throws IOException {
 
        DataInputStream in = new DataInputStream(new FileInputStream(filename));       
        checkScriptIdentifier(in);
        
        features = new Feature[in.readInt()];
        rcHandlers = new RCHandler[in.readInt()];
        segments = new Segment[in.readInt()];
            
        readFeatures(in);
        readSegments(in);
        readRCHandlers(in);
        
        for (int i = 0; i < deferred.size(); i++) {
            CommandSetup setup = (CommandSetup) deferred.get(i);
            setup.setup();
        }
        deferred.clear();
        
        for (int i = 0; i < features.length; i++) {
            show.addFeature(features[i].getName(), features[i]);
        }
        for (int i = 0; i < segments.length; i++) {
            show.addSegment(segments[i].getName(), segments[i]);
        }
        for (int i = 0; i < rcHandlers.length; i++) {
            if (rcHandlers[i] instanceof VisualRCHandler) {
                show.addRCHandler(((VisualRCHandler)rcHandlers[i]).getName(), rcHandlers[i]);
            } else {
                show.addRCHandler(""+i, rcHandlers[i]);
            }   
        }
        
        return show;
    }
    
    private void readFeatures(DataInputStream in) 
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

    private void readRCHandlers(DataInputStream in) throws IOException {
        
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

    private void readSegments(DataInputStream in) throws IOException {

        checkValue(in.readInt(), Constants.SEGMENT_IDENTIFIER, "Segment array identifier");
        
        int count = in.readInt();       
        Segment segment = null;
        
        for (int i = 0; i < count; i++) {
            in.readByte(); // SEGMENT_IDENTIFIER;
            segment = readSegment(in);
            segments[i] = segment;
        }
    } 
    
    private Assembly readAssembly(DataInputStream in) throws IOException {
        
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
        
        assembly.setParts(partNames, parts);
        
        return assembly;
    }

    private Box readBox(DataInputStream in) throws IOException {
        
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
    
    private Clipped readClipped(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);
       
       String name = dis.readUTF();
       Rectangle clipRegion = dis.readRectangle();
       Feature part = features[dis.readInt()];
       
       dis.close();
       
       Clipped clipped = new Clipped(show, name, clipRegion);
       clipped.setup(part);
       
       return clipped;
    }
    
    private Fade readFade(DataInputStream in) throws IOException {
       
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
       Feature part = features[dis.readInt()];
       
       dis.close();
   
       Fade fade = new Fade(show, name, srcOver, keyframes, keyAlphas, endCommands);
       fade.setup(part);
       
       return fade;
    }

    private FixedImage readFixedImage(DataInputStream in) throws IOException {
       
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

    private Group readGroup(DataInputStream in) throws IOException {

       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);   
       
       String name = dis.readUTF();
       Feature[] parts = readFeaturesIndex(dis);
       
       dis.close();
       
       Group group = new Group(show, name);
      
       group.setup(parts);
       
       return group;
    }

    private ImageSequence readImageSequence(DataInputStream in) throws IOException {
       
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

    
    private SrcOver readSrcOver(DataInputStream in) throws IOException {
       
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);  
       
       String name = dis.readUTF();
       Feature part = features[dis.readInt()];
       
       dis.close();
       
       SrcOver srcOver = new SrcOver(show, name);
       srcOver.setup(part);
       
       return srcOver;
    }
    

    private Text readText(DataInputStream in) throws IOException {
       
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

    private Timer readTimer(DataInputStream in) throws IOException {
  
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

    private Translation readTranslation(DataInputStream in) throws IOException {
        
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

    private Translator readTranslator(DataInputStream in) throws IOException {

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
       
       translator.setup(translation, parts);
       
       return translator;
    }
    
    protected Modifier readUserModifier(DataInputStream in) throws IOException {
       int length = in.readInt();
       byte[] buffer = new byte[length];
       in.read(buffer, 0, length);
       
       ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
       GrinDataInputStream dis = new GrinDataInputStream(bais);   
        
       String name = dis.readUTF();
       Feature parts = features[dis.readInt()];
       dis.close();
       
       Modifier modifier = director.getExtensionsParser().getModifier(show, name, null, null);
       modifier.setup(parts);
       
       return modifier;
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

    private SetVisualRCStateCommand readSetVisualRCStateCmd(DataInputStream in) throws IOException {

        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);  
        
        final boolean activated = dis.readBoolean();
        final int state = dis.readInt();
        final int handlerIndex = dis.readInt();
        boolean runCommands = dis.readBoolean();
        
        dis.close();
        
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
    
    private ActivatePartCommand readActivatePartCmd(DataInputStream in) 
        throws IOException {
        
        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);  
        
        final int assemblyIndex = dis.readInt();
        final int partIndex = dis.readInt();
        
        dis.close();
        
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

    private ActivateSegmentCommand readActivateSegmentCmd(DataInputStream in) 
        throws IOException {

        int length = in.readInt();
        byte[] buffer = new byte[length];
        in.read(buffer, 0, length);
       
        ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
        GrinDataInputStream dis = new GrinDataInputStream(bais);  
        
        boolean push = dis.readBoolean();
        boolean pop = dis.readBoolean();
        Segment segment = null;
        final int segmentIndex = dis.readInt();
        
        dis.close();
        
        final ActivateSegmentCommand command = new ActivateSegmentCommand(show, push, pop);
        if (segments[segmentIndex] != null) {
            command.setup((Segment)segments[segmentIndex]);
        } else {
            deferred.add(new CommandSetup() {
               public void setup() {
                   command.setup((Segment)segments[segmentIndex]);
               } 
            });
        }    
        
        return command;
    } 

    private SegmentDoneCommand readSegmentDoneCmd(DataInputStream in) 
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
       
        return show.getDirector().getExtensionsParser().getCommand(show, name, new String[0]);
       
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
    
    abstract class CommandSetup {
        abstract void setup(); 
    }    
}
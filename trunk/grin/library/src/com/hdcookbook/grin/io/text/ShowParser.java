
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

package com.hdcookbook.grin.io.text;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.commands.SetVisualRCStateCommand;
import com.hdcookbook.grin.commands.SegmentDoneCommand;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.Box;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Clipped;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.features.SrcOver;
import com.hdcookbook.grin.features.Fade;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Timer;
import com.hdcookbook.grin.features.Translation;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.input.RCKeyEvent;
import com.hdcookbook.grin.input.VisualRCHandler;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.io.ExtensionsBuilder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.AssetFinder;

import java.io.Reader;
import java.io.IOException;
import java.awt.Font;
import java.awt.Color;
import java.awt.Rectangle;
import java.util.ArrayList;

import java.util.Vector;

/**
 * The parser of a show file.  This is a really simple-minded
 * parser.  For example, all tokens are just strings, so, for example,
 * you have to write "( 0 3 )" and not "(0 3)", since the first has four
 * tokens and the second only two.
 *
 *   @author     Bill Foote (http://jovial.com)
 **/
public class ShowParser {

    private Show show;
    private Lexer lexer;
    private ExtensionsBuilder extBuilder;
    private Vector[] deferred = { new Vector(), new Vector() };  
    	// Array of Vector<ForwardReference>

    private final static String[] emptyStringArray = new String[0];
    private final static Command[] emptyCommandArray = new Command[0];

    private ShowBuilder builder;

    /**
     * Create a parser to parse a show at the given location.
     *
     * @param reader    Where to read the show from.  We read it up to the
     *		    	end_show token.  It is recommended to be a BufferedReader
     *                  instance for a performance improvement.
     *
     * @param showName	The name of the show, for error messages.
     *
     * @param show	The show to populate.  This should be a new, empty
     *			show.
     **/
    public ShowParser(Reader reader, String showName, Show show) {
	this(reader, showName, show, null);
    }

    /**
     * Create a parser to parse a show at the given location.
     *
     * @param reader    Where to read the show from.  We read it up to the
     *		    	end_show token. It is recommended to be a BufferedReader
     *                  instance for a performance improvement.
     *
     * @param showName	The name of the show, for error messages.
     *
     * @param show	The show to populate.  This should be a new, empty
     *			show.
     *
     * @param builder   A helper to build the tree.  You can use something
     *			other than the default to add decorations to the
     *			tree, e.g. for debugging.
     **/
    public ShowParser(Reader reader, String showName, Show show, 
    		      ShowBuilder builder) 
    {
        this.show = show;
	Director d = show.getDirector();
	this.lexer = new Lexer(reader, showName);
	if (d == null) {
	    this.extBuilder = null;
	} else {
	    this.extBuilder = d.getExtensionsBuilder();
	}
	if (builder == null) {
	    builder = new ShowBuilder();
	}
	this.builder = builder;
	builder.init(this, show);
    }

    /**
     * Parse the current show file.
     **/
    public void parse() throws IOException {
	String tok = lexer.getString();
	if (!"show".equals(tok)) {
	    lexer.reportError("\"show\" expected");
	}
	for (;;) {
	    tok = lexer.getString();
	    int lineStart = lexer.getLineNumber();
	    if (tok == null) {
		lexer.reportError("EOF unexpected");
	    } else if ("end_show".equals(tok)) {
		finishBuilding();
		return;
	    } else if ("segment".equals(tok)) {
		parseSegment(lineStart);
	    } else if ("setting".equals(tok)) {
		tok = lexer.getString();
		if ("segment_stack_depth".equals(tok)) {
		    parseSegmentStackDepth();
		} else {
		    lexer.reportError("Unrecognized setting \"" + tok + "\".");
		}
	    } else if ("feature".equals(tok)) {
		tok = lexer.getString();
		if ("fixed_image".equals(tok)) {
		    parseFixedImage(lineStart);
		} else if ("image_sequence".equals(tok)) {
		    parseImageSequence(lineStart);
		} else if ("box".equals(tok)) {
		    parseBox(lineStart);
		} else if ("assembly".equals(tok)) {
		    parseAssembly(lineStart);
		} else if ("group".equals(tok)) {
		    parseGroup(lineStart);
		} else if ("clipped".equals(tok)) {
		    parseClipped(lineStart);
		} else if ("src_over".equals(tok)) {
		    parseSrcOver(lineStart);
		} else if ("fade".equals(tok)) {
		    parseFade(lineStart);
		} else if ("timer".equals(tok)) {
		    parseTimer(lineStart);
		} else if ("translation".equals(tok)) {
		    parseTranslation(lineStart);
		} else if ("translator".equals(tok)) {
		    parseTranslator(lineStart);
		} else if ("text".equals(tok)) {
		    parseText(lineStart);
		} else if (extBuilder == null || tok == null) {
		    lexer.reportError("Unrecognized feature \"" + tok + "\"");
		} else if ("extension".equals(tok) || "modifier".equals(tok)) {
		    String typeName = lexer.getString();
		    String name = lexer.getString();
		    String subName = null;
		    if ("modifier".equals(tok)) {
			subName = lexer.getString();
		    }
		    String arg = lexer.getString();
		    parseExpected(";");
		    Feature f;
		    if (typeName.indexOf(':') < 0) {
			lexer.reportError(typeName + " doesn't contain \":\"");
		    }
		    if (subName == null) {
			f = extBuilder.getFeature(show, typeName, name, arg);
		    } else {
			Modifier m = extBuilder.getModifier(show, typeName,
							   name, arg);
			f = m;
			if (m != null) {
			    resolveModifier(m, subName);
			}
		    }
		    if (f == null) {
			lexer.reportError("Unrecognized feature " + typeName);
		    }
		    builder.addFeature(name, lineStart, f);
		}
	    } else if ("rc_handler".equals(tok)) {
		tok = lexer.getString();
		if ("assembly_grid".equals(tok)) {
		    parseAssemblyGridRCHandler();	// deprecated
		} else if ("visual".equals(tok)) {
		    parseVisualRCHandler();
		} else if ("key_pressed".equals(tok)) {
		    parseCommandRCHandler();
		} else {
		    lexer.reportError("Unrecognized token \"" + tok + "\"");
		}
	    } else if ("mosaic_hint".equals(tok)) {
		String name = lexer.getString();
		int width = lexer.getInt();
		int height = lexer.getInt();
		String[] files = parseStrings();
		parseExpected(";");
		if (extBuilder != null) {
		    extBuilder.takeMosaicHint(name, width, height, files);
		}
	    } else {
		lexer.reportError("Unrecognized token \"" + tok + "\"");
	    }
	}
    }

    private void parseSegmentStackDepth() throws IOException {
	int depth = lexer.getInt();
	if (depth < 0) {
	    lexer.reportError("Illegal depth:  " + depth);
	}
	parseExpected(";");
	show.setSegmentStackDepth(depth);
    }

    private void parseSegment(final int line) throws IOException {
	final String name = lexer.getString();
	String[] sa;
	String s;
	String tok = lexer.getString();
	if ("active".equals(tok)) {
	    sa = parseStrings();
	    tok = lexer.getString();
	} else {
	    sa = emptyStringArray;
	}
	final String[] active = sa;
	if ("setup".equals(tok)) {
	    sa = parseStrings();
	    tok = lexer.getString();
	} else {
	    sa = emptyStringArray;
	}
	final String[] setup = sa;
	if ("chapter".equals(tok)) {
	    s = lexer.getString();
	    tok = lexer.getString();
	} else {
	    s = null;
	}
	final String chapter = s;
	if ("rc_handlers".equals(tok)) {
	    sa = parseStrings();
	    tok = lexer.getString();
	} else {
	    sa = emptyStringArray;
	}
	final String[] rcHandlers = sa;
	Command[] ca;
	final boolean nextOnSetupDone = "setup_done".equals(tok);
	if (nextOnSetupDone || "next".equals(tok)) {
	    ca = parseCommands();
	    tok = lexer.getString();
	} else {
	    ca = emptyCommandArray;
	}
	final Command[] next = ca;
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		Feature[] a = makeFeatureList(active);
		Feature[] s = makeFeatureList(setup);
		ChapterManager c;
		RCHandler[] h = makeRCHandlerList(rcHandlers);
		if (chapter == null) {
		    c = null;
		} else {
		    if (show.getDirector() == null) {
			c = null;
		    } else {
		        c = show.getDirector().getChapterManager(chapter);
		    }
		    if (c == null) {
			reportError("Chapter \"" + name + "\" not found");
		    }
		}
		builder.addSegment(name,line,new Segment(name, a, s, c, h,
				   nextOnSetupDone, next));
	    }
	};
	deferred[0].addElement(fw);
    }

    private void parseFixedImage(int line) throws IOException {
	String name = lexer.getString();
	int x = lexer.getInt();
	int y = lexer.getInt();
	int bobble = 0;
	String fileName = lexer.getString();
	parseExpected(";");
	builder.addFeature(name,line, new FixedImage(show, name, x,y,fileName));
    }

    private void parseImageSequence(int line) throws IOException {
	final String name = lexer.getString();
	int x = lexer.getInt();
	int y = lexer.getInt();
	String fileName = lexer.getString();
	String[] middle = parseStrings();
	if (middle.length == 0) {
	    lexer.reportError("Must have at least one file in a sequence");
	}
	for (int i = 0; i < middle.length; i++) {
	    if ("+".equals(middle[i])) {
		if (i == 0) {
		    middle[i] = null;
		} else { 
		    middle[i] = middle[i-1];
		}
	    } else if ("-".equals(middle[i])) {
		middle[i] = null;
	    }
	}
	String extension = lexer.getString();
	String tok = lexer.getString();
	boolean repeat = false;
	if ("repeat".equals(tok)) {
	    repeat = true;
	    tok = lexer.getString();
	}
	String linkedTo = null;
	Command[] endCommands = emptyCommandArray;
	if ("linked_to".equals(tok)) {
	    linkedTo = lexer.getString();
	    tok = lexer.getString();
	}
	if ("end_commands".equals(tok)) {
	    endCommands = parseCommands();
	    tok = lexer.getString();
	}
	if (";".equals(tok)) {
	} else {
	    lexer.reportError("';' expected, " + tok + " seen");
	}
	final ImageSequence f 
                = new ImageSequence(show, name, x, y, fileName, 
                                    middle,  extension, repeat, endCommands);
	builder.addFeature(name, line, f);
	if (linkedTo != null) {
	    final String lt = linkedTo;
	    ForwardReference fw = new ForwardReference(lexer) {
		void resolve() throws IOException {
		    Feature ltf = show.getFeature(lt);
		    if (ltf == null || !(ltf instanceof ImageSequence)) {
			    lexer.reportError("In image_sequence " + name + 
				      " can't find image_sequence linked_to " 
				      + lt + ".");
		    }
		    f.setLinkedTo((ImageSequence) ltf);
		}
	    };
	    deferred[0].addElement(fw);
	}
    }

    private void parseBox(int line) throws IOException {
	String name = lexer.getString();
	Rectangle placement = parseRectangle();
	String tok = lexer.getString();
	int outlineWidth = 0;
	Color outlineColor = null;
	if ("outline".equals(tok)) {
	    outlineWidth = lexer.getInt();
	    if (outlineWidth < 1) {
		lexer.reportError("" + outlineWidth + " is an illegal width");
	    }
	    if (outlineWidth*2 > placement.width || outlineWidth*2 > placement.height) {
		lexer.reportError("Outline too wide for box size");
	    }
	    outlineColor = parseColor();
	    tok = lexer.getString();
	}
	Color fillColor = null;
	if ("fill".equals(tok)) {
	    fillColor = parseColor();
	    tok = lexer.getString();
	}
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	builder.addFeature(name, line, 
	    new Box(show, name, placement, outlineWidth, outlineColor, 
	    	    fillColor));
    }


    private void parseAssembly(int line) throws IOException {
	String name = lexer.getString();
	String[] strings = parseStrings();
	parseExpected(";");
	if ((strings.length % 2) == 1) {
	    lexer.reportError("Assembly part \"" +
	    		      strings[strings.length-1] +
			      "\" has no feature");
	}
	final Assembly a = new Assembly(show, name);
	builder.addFeature(name, line, a);
	final String[] names = new String[strings.length / 2];
	final String[] featureNames = new String[strings.length / 2];
	for (int i = 0; i < names.length; i++) {
	    names[i] = strings[i * 2];
	    featureNames[i] = strings[i*2 + 1];
	}
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		a.setParts(names, makeFeatureList(featureNames));
	    }
	};
	deferred[0].addElement(fw);
    }

    private void parseGroup(int line) throws IOException {
	String name = lexer.getString();
	final String[] featureNames = parseStrings();
	parseExpected(";");
	final Group group = new Group(show, name);
	builder.addFeature(name, line, group);
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		group.setup(makeFeatureList(featureNames));
	    }
	};
	deferred[0].addElement(fw);
    }

    private void parseClipped(int line) throws IOException {
	String name = lexer.getString();
	String partName =  lexer.getString();
	Rectangle clipRegion = parseRectangle();
	parseExpected(";");
	Clipped clipped = new Clipped(show, name, clipRegion);
	builder.addFeature(name, line, clipped);
	resolveModifier(clipped, partName);
    }

    private void parseSrcOver(int line) throws IOException {
	String name = lexer.getString();
	String partName =  lexer.getString();
	parseExpected(";");
	SrcOver so = new SrcOver(show, name);
	builder.addFeature(name, line, so);
	resolveModifier(so, partName);
    }

    private void resolveModifier(final Modifier m, final String partName) {
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		m.setup(makeFeature(partName));
	    }
	};
	deferred[0].addElement(fw);
    }

    private void parseFade(int line) throws IOException {
	String name = lexer.getString();
	String partName =  lexer.getString();
	String tok = lexer.getString();
	boolean srcOver = false;
	if ("src_over".equals(tok)) {
	    srcOver = true;
	    tok = lexer.getString();
	}
	if (!("{".equals(tok))) {
	    lexer.reportError("'{' expected, \"" + tok + "\" seen.");
	}
	Vector keyframes = new Vector();
	for (;;) {
	    tok = lexer.getString();
	    if ("}".equals(tok)) {
		break;
	    }
	    int frameNum = 0;
	    try {
		frameNum = Integer.decode(tok).intValue();
	    } catch (NumberFormatException ex) {
		lexer.reportError(ex.toString());
	    }
	    int alpha = lexer.getInt();
	    keyframes.addElement(new int[] { frameNum, alpha } );
	    parseExpected("linear");
	}
	tok = lexer.getString();
	Command[] endCommands = emptyCommandArray;
	if ("end_commands".equals(tok)) {
	    endCommands = parseCommands();
	    tok = lexer.getString();
	}
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	int[] fs = new int[keyframes.size()];
	int[] alphas = new int[keyframes.size()];
	for (int i = 0; i < keyframes.size(); i++) {
	    int[] el = (int[]) keyframes.elementAt(i);
	    fs[i] = el[0];
	    alphas[i] = el[1];
	    if (i > 0 && fs[i] <= fs[i-1]) {
		lexer.reportError("Frame number must be increasing");
	    }
	    if (alphas[i] < 0 || alphas[i] > 255) {
		lexer.reportError("Illegal alpha value:  " + alphas[i]);
	    }
	}
	if (fs.length < 1) {
	    lexer.reportError("Need at least one keyframe");
	}
	if (fs[0] != 0) { 
	    lexer.reportError("Keyframes must start at frame 0");
	}
	Fade fade = new Fade(show, name, srcOver, fs, alphas, endCommands);
	builder.addFeature(name, line, fade);
	resolveModifier(fade, partName);
    }

    private void parseTimer(int line) throws IOException {
	String name = lexer.getString();
	int numFrames = lexer.getInt();
	String tok = lexer.getString();
	boolean repeat = false;
	if ("repeat".equals(tok)) {
	    repeat = true; 
	    parseExpected("{");
	} else if ("{".equals(tok)) {
	    // do nothing, i.e. consume token
	} else {
	    lexer.reportError("'{' or 'repeat' expected");
	}
	Command[] commands = parseCommandsNoOpenBrace();
	parseExpected(";");
	if (numFrames < 0 || (repeat && numFrames < 1)) {
	    lexer.reportError("More frames, please.");
	}
	Timer timer = new Timer(show, name, numFrames, repeat, commands);
	builder.addFeature(name, line, timer);
    }

    private void parseTranslation(int line) throws IOException {
	String name = lexer.getString();
	parseExpected("{");
	Vector keyframes = new Vector();
	for (;;) {
	    String tok = lexer.getString();
	    if ("}".equals(tok)) {
		break;
	    }
	    int frameNum = 0;
	    try {
		frameNum = Integer.decode(tok).intValue();
	    } catch (NumberFormatException ex) {
		lexer.reportError(ex.toString());
	    }
	    int x = lexer.getInt();
	    int y = lexer.getInt();
	    keyframes.addElement(new int[] { frameNum, x, y } );
	    parseExpected("linear");
	}
	String tok = lexer.getString();
	int repeatFrame = -1;
	if ("repeat".equals(tok)) {
	    repeatFrame = lexer.getInt();
	    tok = lexer.getString();
	}
	Command[] endCommands = emptyCommandArray;
	if ("end_commands".equals(tok)) {
	    endCommands = parseCommands();
	    tok = lexer.getString();
	}
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	int[] fs = new int[keyframes.size()];
	int[] xs = new int[keyframes.size()];
	int[] ys = new int[keyframes.size()];
	for (int i = 0; i < keyframes.size(); i++) {
	    int[] el = (int[]) keyframes.elementAt(i);
	    fs[i] = el[0];
	    xs[i] = el[1];
	    ys[i] = el[2];
	    if (i > 0 && fs[i] <= fs[i-1]) {
		lexer.reportError("Frame number must be increasing");
	    }
	}
	if (fs.length < 2) {
	    lexer.reportError("Need at least two keyframes");
	}
	if (fs[0] != 0) { 
	    lexer.reportError("Keyframes must start at frame 0");
	}
	if (repeatFrame == -1) {
	    repeatFrame = fs[fs.length - 1];	// Make it stick at end
	} else if (repeatFrame > fs[fs.length - 1]) {
	    lexer.reportError("repeat > max frame");
	}
	final Translation trans 
	    = new Translation(show, name, fs, xs, ys, repeatFrame, endCommands);
	builder.addFeature(name, line, trans);
    }

    private void parseTranslator(int line) throws IOException {
	String name = lexer.getString();
	final String translationName = lexer.getString();
	final String[] featureNames = parseStrings();
	parseExpected(";");
	final Translator trans = new Translator(show, name);
	builder.addFeature(name, line, trans);
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		Feature t  = show.getFeature(translationName);
		if (t == null || !(t instanceof Translation)) {
		    lexer.reportError("Translation \"" + translationName 
		    			+ "\" not found");
		}
		Feature[] fa = makeFeatureList(featureNames);
		trans.setup((Translation) t, fa);
	    }
	};
	deferred[0].addElement(fw);
    }


    private void parseText(int line) throws IOException {
	String name = lexer.getString();
	int x = lexer.getInt();
	int y = lexer.getInt();
	String tok = lexer.getString();
	String[] textStrings;
	int vspace = 0;
	if ("{".equals(tok)) {
	    textStrings = parseStringsWithOpenBraceRead();
	} else {
	    textStrings = new String[] { tok };
	}
	tok = lexer.getString();
	if ("vspace".equals(tok)) {
	    vspace = lexer.getInt();
	    tok = lexer.getString();
	}
	Font font = parseFontSpec(tok);
	parseExpected("{");
	Vector colors = new Vector();
	Color lastColor = null;
	for (;;) {
	    tok = lexer.getString();
	    if ("}".equals(tok)) {
		break;
	    } else if ("+".equals(tok)) {
		if (lastColor == null) {
		    lexer.reportError("First color must be specified");
		}
	    } else if (!("{".equals(tok))) {
		lexer.reportError("'{' or '+' expected, \"" + tok + "\" seen.");
	    } else {
		lastColor = parseColorNoOpenBrace();
	    }
	    colors.addElement(lastColor);
	}
	tok = lexer.getString();
	Color background = null;
	if ("background".equals(tok)) {
	    background = parseColor();
	    tok = lexer.getString();
	}
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	Color[] cols = new Color[colors.size()];
	for (int i = 0; i < cols.length; i++) {
	    cols[i] = (Color) colors.elementAt(i);
	}
	if (cols.length < 1) {
	    lexer.reportError("At least one color needed");
	}
	Text text = new Text(show, name, x, y, textStrings, vspace, 
			     font, cols, background);
	builder.addFeature(name, line, text);
    }

    private Font parseFontSpec(String tok) throws IOException {
	String fontName = tok;
	tok = lexer.getString();
	int style = 0;
	if ("plain".equals(tok)) {
	    style = Font.PLAIN;
	} else if ("bold".equals(tok)) {
	    style = Font.BOLD;
	} else if ("italic".equals(tok)) {
	    style = Font.ITALIC;
	} else if ("bold-italic".equals(tok)) {
	    style = Font.BOLD | Font.ITALIC;
	} else {
	    lexer.reportError("font style expected, \"" + tok + "\" seen");
	}
	int size = lexer.getInt();
	return AssetFinder.getFont(fontName, style, size);
    }

    private void parseAssemblyGridRCHandler() throws IOException {
	lexer.reportWarning("Deprecated rc_handler assembly_grid");
	int lineStart = lexer.getLineNumber();
	String handlerName = lexer.getString();
	parseExpected("assembly");
	final String assemblyName = lexer.getString();
	parseExpected("select");
	final String[][] selectParts = parseMatrix();
	parseExpected("invoke");
	final String[][] invokeParts = parseMatrix();
	final int height = selectParts.length;
	if (height <= 0) {
	    lexer.reportError("Empty grid");
	}
	final int width = selectParts[0].length;
	if (selectParts.length != invokeParts.length) {
	    lexer.reportError("matricies have different number of rows: " +
	    		selectParts.length + " vs. " + invokeParts.length
			+ ".");
	}
	for (int i = 0; i < selectParts.length; i++) {
	    if (selectParts[i].length != invokeParts[i].length) {
		lexer.reportError("row " + (i+1) 
			    + " has different number of entries: "
			    + selectParts[i].length + " vs. "
			    + invokeParts[i].length + ".");
	    }
	    if (selectParts[i].length != width) {
		lexer.reportError("Unequal widths of grid");
	    }
	}

	String tok = lexer.getString();
	int timeout = -1;
	Command[] timeoutCommands = emptyCommandArray;
	if ("timeout".equals(tok)) {
	    timeout = lexer.getInt();
	    parseExpected("frames");
	    timeoutCommands = parseCommands();
	    tok = lexer.getString();
	}
	Command[][] activateCommands = null;
	if ("when_invoked".equals(tok)) {
	    activateCommands = new Command[height * width][];
	    parseExpected("{");
	    for (;;) {
		tok = lexer.getString();
		if ("}".equals(tok)) {
		    break;
		}
		Command[] c = parseCommands();
		int i = findInMatrix(tok, invokeParts);
		if (i == -1) {
		    lexer.reportError("Can't find part " + tok);
		} else if (activateCommands[i] != null) {
		    lexer.reportError("Duplicate part " + tok);
		}
		activateCommands[i] = c;
	    }
	    tok = lexer.getString();
	}
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	int[][] grid = new int[selectParts.length][];
	String[] stateNames = new String[height * width];
	{
	    int i = 0;
	    for (int y = 0; y < height; y++) {
		grid[y] = new int[width];
		for (int x = 0; x < selectParts[y].length; x++) {
		    grid[y][x] = i;
		    stateNames[i] = "" + x + "," + y;
		    i++;
		}
	    }
	}
	final VisualRCHandler hand 
	    = new VisualRCHandler(handlerName, grid, stateNames, null,
	    			  activateCommands, null, null,
				  timeout, timeoutCommands);
	builder.addRCHandler(handlerName, lineStart, hand);
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		Assembly assembly = lookupAssemblyOrFail(assemblyName);
		Feature[] realSelectParts 
		    = lookupFeatureGrid(assembly, selectParts, width, height);
		Feature[] realInvokeParts 
		    = lookupFeatureGrid(assembly, invokeParts, width, height);
		hand.setup(assembly, realSelectParts, realInvokeParts);
	    }
	};
	deferred[0].addElement(fw);
    }

    private void parseVisualRCHandler() throws IOException {
	int lineStart = lexer.getLineNumber();
	String handlerName = lexer.getString();
	parseExpected("grid");
	Vector statesV = new Vector();  // <String>
	int[][] grid = parseVisualGrid(statesV);
	String[] states = new String[statesV.size()];
	for (int i = 0; i < states.length; i++) {
	    states[i] = (String) statesV.elementAt(i);
	}
	String tok = lexer.getString();
	if ("assembly".equals(tok)) {
	    tok = lexer.getString();	// Assembly name
	    parseExpected("select");
	} else if ("select".equals(tok)) {
	    tok = null;		// Assembly name
	} else {
	   lexer.reportError("\"select\" expected, \"" + tok + "\" seen");
	}
	final String assemblyName = tok;

	Object[] oa = parseVisualActions(statesV, assemblyName != null);
	final String[] selectParts = (String[]) oa[0];
	final Command[][] selectCommands = (Command[][]) oa[1];
	parseExpected("activate");
	oa = parseVisualActions(statesV, assemblyName != null);
	final String[] activateParts = (String[]) oa[0];
	final Command[][] activateCommands = (Command[][]) oa[1];

	tok = lexer.getString();
	Rectangle[] mouseRects = null;
	int[] mouseRectStates = null;
	if ("mouse".equals(tok)) {
	    Vector v = parseMouseLocations(statesV);
	    mouseRects = new Rectangle[v.size() / 2];
	    mouseRectStates = new int[v.size() / 2];
	    for (int i = 0; i < mouseRects.length; i++) {
		mouseRectStates[i] = ((Integer)v.elementAt(i*2)).intValue();
		mouseRects[i] = (Rectangle) v.elementAt(i*2 + 1);
	    }
	    tok = lexer.getString();
	}
	int timeout = -1;
	Command[] timeoutCommands = emptyCommandArray;
	if ("timeout".equals(tok)) {
	    timeout = lexer.getInt();
	    parseExpected("frames");
	    timeoutCommands = parseCommands();
	    tok = lexer.getString();
	}
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	final VisualRCHandler hand 
	    = new VisualRCHandler(handlerName, grid, states, 
	    			  selectCommands, activateCommands,
				  mouseRects, mouseRectStates,
				  timeout, timeoutCommands);
	builder.addRCHandler(handlerName, lineStart, hand);
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		Assembly assembly = null;
		Feature[] realSelParts = null;
		Feature[] realActParts = null;
		if (assemblyName != null) {
		    assembly = lookupAssemblyOrFail(assemblyName);
		    realSelParts = lookupAssemblyParts(assembly, selectParts);
		    realActParts =  lookupAssemblyParts(assembly,activateParts);
		}
		hand.setup(assembly, realSelParts, realActParts);
	    }
	};
	deferred[0].addElement(fw);
    }

    private int[][] parseVisualGrid(Vector states) throws IOException {
	Vector v = new Vector();
	parseExpected("{");
	for (;;) {
	    String tok = lexer.getString();
	    if ("}".equals(tok)) {
		break;
	    } else if ("{".equals(tok)) {
		int[] el = parseVisualGridRow(states);
		v.addElement(el);
	    } else {
		lexer.reportError("'{' or '}' expected, " + tok + " seen");
	    }
	}
	int num = v.size();
	int[][] result = new int[num][];
	for (int i = 0; i < num; i++) {
	    result[i] = (int[]) v.elementAt(i);
	}
	if (result.length < 1) {
	    lexer.reportError("Grid must have at least one row");
	}
	for (int i = 1; i < num; i++) {
	    if (result[0].length != result[i].length) {
		lexer.reportError("Grid row " + i 
			+ " (counting from 0) has a different length");
	    }
	}
	for (int y = 0; y < result.length; y++) {
	    for (int x = 0; x < result[y].length; x++) {
		int g = result[y][x];
		if ((g & 0x10000) != 0 && g != VisualRCHandler.GRID_ACTIVATE) {
		    int y2 = 0xff & (g >> 8);
		    int x2 = 0xff & g;
		    if ((result[y2][x2] & 0x10000) != 0) {
			lexer.reportError(
			    "Grid refers to cell that refers to cell at x,y " 
			    + x + ", " + y + " (counting from 0)");
		    }
		}
	    }
	}
	return result;
    }

    private int[] parseVisualGridRow(Vector states) throws IOException {
	Vector v = new Vector();
	for (;;) {
	    String tok = lexer.getString();
	    if (tok == null) {
		lexer.reportError("EOF unexpected in string list");
	    } else if ("}".equals(tok)) {
		break;
	    } else if ("(".equals(tok)) {
		int x = lexer.getInt();
		int y = lexer.getInt();
		if (x < 0 || x > 0xff || y < 0 || y > 0xff) {
		    lexer.reportError("Invalid cell address ( " + x + " "
		    		      + y + " )");
		}
		v.addElement(new Integer(0x10000 | (y << 8) | x));
		parseExpected(")");
	    } else if ("<activate>".equals(tok)) {
		v.addElement(new Integer(VisualRCHandler.GRID_ACTIVATE));
	    } else {
		if (states.contains(tok)) {
		    lexer.reportError("Duplicate state name:  " + tok);
		}
		    // Grid gets number of new state, then add name to states
		v.addElement(new Integer(states.size()));
		states.add(tok);
	    }
	}
	int num = v.size();
	int[] result = new int[num];
	for (int i = 0; i < num; i++) {
	    result[i] = ((Integer) v.elementAt(i)).intValue();
	}
	return result;
    }

    // Return value [0] is list of parts, [1] is list of commands lists.
    Object[] parseVisualActions(Vector states, boolean hasAssembly) 
    		throws IOException 
    {
	String[] parts = null;
	Command[][] commands = null;
	parseExpected("{");
	String tok = lexer.getString();
	for (;;) {
	    if ("}".equals(tok)) {
		break;
	    }
	    int state = states.indexOf(tok);
	    if (state == -1) {
		lexer.reportError("State " + tok + " not found");
	    }
	    tok = lexer.getString();
	    if (!("{".equals(tok)))	{	// If not command list
		if (!hasAssembly) {
		    lexer.reportError("No assembly specified");
		}
		if (parts == null) {
		    parts = new String[states.size()];
		}
		if (parts[state] != null) {
		    lexer.reportError("State " + states.elementAt(state)
				      + " has duplicate assembly parts");
		}
		parts[state] = tok;
		tok = lexer.getString();
	    }
	    if ("{".equals(tok)) {		// If command list
		if (commands == null) {
		    commands = new Command[states.size()][];
		}
		if (commands[state] != null) {
		    lexer.reportError("State " + states.elementAt(state)
				      + " has duplicate commands");
		}
		commands[state] = parseCommandsNoOpenBrace();
		tok = lexer.getString();
	    }
	}
	return new Object[] { parts, commands };
    }

    // Return value alternates Integer state # and Rectangle
    private Vector parseMouseLocations(Vector states) throws IOException {
	parseExpected("{");
        Vector result = new Vector();
        for (;;) {
            String tok = lexer.getString();
	    if ("}".equals(tok)) {
		break;
	    }
	    int state = states.indexOf(tok);
	    if (state == -1) {
		lexer.reportError("State " + tok + " not found");
	    }
	    result.add(new Integer(state));
	    result.add(parseRectangle());
	}
	return result;
    }

    private Rectangle parseRectangle() throws IOException {
	parseExpected("(");
	int x1 = lexer.getInt();
	int y1 = lexer.getInt();
	int x2 = lexer.getInt();
	int y2 = lexer.getInt();
	if (x2 < x1 ||  y2 < y1) {
	    lexer.reportError("Second coordinates must be lower-right corner");
	}
	parseExpected(")");
	return new Rectangle(x1, y1, 1+x2-x1, 1+y2-y1);
    }

    private int findInMatrix(String target, String[][] matrix) {
	for (int y = 0; y < matrix.length; y++) {
	    for (int x = 0; x < matrix[y].length; x++) {
		if (target.equals(matrix[y][x])) {
		    return y * matrix[0].length + x;
		}
	    }
	}
	return -1;
    }

    private void parseCommandRCHandler() throws IOException {
	int lineStart = lexer.getLineNumber();
	String handlerName = lexer.getString();
	int mask = parseRCKeyList();
	parseExpected("execute");
	Command[] commands = parseCommands();
	parseExpected(";");
	builder.addRCHandler(handlerName, lineStart,
			     new CommandRCHandler(handlerName, mask, commands));
    }

    //
    // Returns the key mask of the set of keys
    //
    private int parseRCKeyList() throws IOException {
	parseExpected("{");
	int mask = 0;
	for (;;) {
	    String tok = lexer.getString();
	    RCKeyEvent e = null;
	    if ("}".equals(tok)) {
		break;
	    }
	    if (tok != null) {
		e = RCKeyEvent.getKeyByName(tok);
	    }
	    if (e == null) {
		lexer.reportError("Unexpected token " + tok);
	    }
	    mask |= e.getBitMask();
	}
	return mask;
    }

    //
    // Parse a list of commands
    //
    private Command[] parseCommands() throws IOException {
	parseExpected("{");
	return parseCommandsNoOpenBrace();
    }

    private Command[] parseCommandsNoOpenBrace() throws IOException {
        Vector v = new Vector();
	for (;;) {
	    String tok = lexer.getString();
	    int lineStart = lexer.getLineNumber();
	    if ("}".equals(tok)) {
		break;
	    } else if ("activate_segment".equals(tok)) {
		Command c = parseActivateSegment();
		v.addElement(c);
		builder.addCommand(c, lineStart);
	    } else if ("activate_part".equals(tok)) {
		Command c = parseActivatePart();
		v.addElement(c);
		builder.addCommand(c, lineStart);
	    } else if ("segment_done".equals(tok)) {
		Command c = parseSegmentDone();
		v.addElement(c);
		builder.addCommand(c, lineStart);
	    } else if ("invoke_assembly".equals(tok)) { // deprecated
		Command c = parseInvokeAssembly();
		v.addElement(c);
		builder.addCommand(c, lineStart);
	    } else if ("set_visual_rc".equals(tok)) {
		Command c = parseVisualRC();
		v.addElement(c);
		builder.addCommand(c, lineStart);
	    } else if (extBuilder == null || tok == null || tok.indexOf(':') < 0) 
	    {
		lexer.reportError("command expected, " + tok + " seen");
	    } else {
		String typeName = tok;
 	        ArrayList args =new ArrayList();
	        for (;;) {
	           tok = lexer.getString();
	           if (tok == null) {
		      parseExpected(";");
	           } else if (";".equals(tok)) {
		      break;
	           } else {
		      args.add(tok);
	           }
	        }               
		Command c = extBuilder.getCommand(show, typeName, (String[])args.toArray(new String[]{}));
		v.addElement(c);
		builder.addCommand(c, lineStart);
	    }
	}
	int num = v.size();
	Command[] result = new Command[num];
	for (int i = 0; i < num; i++) {
	    result[i] = (Command) v.elementAt(i);
	}
	return result;
    }

    private Command parseActivateSegment() throws IOException {
	final String name = lexer.getString();
	final boolean pop = "<pop>".equals(name);
	String tok = lexer.getString();
	final boolean push = !pop && "<push>".equals(tok);
	if (push) {
	    tok = lexer.getString();
	}
	if (!(";".equals(tok))) {
	   lexer.reportError("\";\" expected, \"" + tok + "\" seen");
	}
	final ActivateSegmentCommand cmd 
		= new ActivateSegmentCommand(show, push, pop);
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		if (!pop) {
		    Segment s = show.getSegment(name);
		    if (s == null) {
			reportError("Segment \"" + name + " not found");
		    } else {
			cmd.setup(s);
		    }
		}
		if (push || pop) {
		    if (show.getSegmentStackDepth() <= 0) {
			reportError("Segment stack depth is 0");
		    }
		}
	    }
	};
	deferred[1].addElement(fw);
	return cmd;
    }

    private Command parseActivatePart() throws IOException {
	final String assemblyName = lexer.getString();
	final String partName = lexer.getString();
	parseExpected(";");
	final ActivatePartCommand cmd = new ActivatePartCommand();
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		Assembly a = lookupAssemblyOrFail(assemblyName);
		Feature f = a.findPart(partName);
		if (f == null) {
		    reportError("Assembly part \"" + partName + " not found");
		}
		cmd.setup(a, f);
	    }
	};
	deferred[1].addElement(fw);
	return cmd;
    }

    private Command parseSegmentDone() throws IOException {
	parseExpected(";");
	return new SegmentDoneCommand(show);
    }

    private Command parseInvokeAssembly() throws IOException {
	lexer.reportWarning("Deprecated invoke_assembly command");
	int r = 0;
	int c = 0;
	String tok = lexer.getString();
	if ("selected_cell".equals(tok)) {
	    r = -1;
	    c = -1;
	} else if ("cell".equals(tok)) {
	    r = lexer.getInt();
	    c = lexer.getInt();
	    if (r < 0 || c < 0) {
		lexer.reportError("Negative value for row or column");
	    }
	} else {
	    lexer.reportError("Unexpected token:  " + tok);
	}
	final String handlerName = lexer.getString();
	final int row = r;
	final int column = c;
	parseExpected(";");
	final SetVisualRCStateCommand cmd = new SetVisualRCStateCommand();
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		RCHandler h = show.getRCHandler(handlerName);
		if (h == null || !(h instanceof VisualRCHandler)) {
		    reportError("Handler not found or wrong type ");
		}
		VisualRCHandler handler = (VisualRCHandler) h;
		int state = -1;
		if (row > -1) {
		    state = handler.getStateChecked(column, row);
		}
		cmd.setup(true, state, handler);
	    }
	};
	deferred[0].addElement(fw);
	return cmd;
    }

    private Command parseVisualRC() throws IOException {
	final String handlerName = lexer.getString();
	String tok = lexer.getString();
	String state = null;
	if ("current".equals(tok)) {
	    state = null;
	} else if ("state".equals(tok)) {
	    state = lexer.getString();
	} else {
	    lexer.reportError("Unexpected token:  " + tok);
	}
	tok = lexer.getString();
	boolean activate = false;
	if ("selected".equals(tok)) {
	    activate = false;
	} else if ("activated".equals(tok)) {
	    activate = true;
	} else {
	    lexer.reportError("Unexpected token:  " + tok);
	}
	parseExpected(";");
	final SetVisualRCStateCommand cmd = new SetVisualRCStateCommand();
	final String stateF = state;
	final boolean activateF = activate;
	ForwardReference fw = new ForwardReference(lexer) {
	    void resolve() throws IOException {
		RCHandler h = show.getRCHandler(handlerName);
		if (h == null || !(h instanceof VisualRCHandler)) {
		    reportError("Handler not found or wrong type ");
		}
		VisualRCHandler handler = (VisualRCHandler) h;
		int state = -1;
		if (stateF != null)  {
		    state = handler.lookupState(stateF);
		    if (state == -1) {
			lexer.reportError("State \"" + stateF + "\" not found");
		    }
		}
		cmd.setup(activateF, state, handler);
	    }
	};
	deferred[0].addElement(fw);
	return cmd;
    }


    private void finishBuilding() throws IOException {
	for (int i = 0; i < deferred.length; i++) {
	    for (int j = 0; j < deferred[i].size(); j++) {
		ForwardReference fw 
			= (ForwardReference) deferred[i].elementAt(j);
		fw.resolveAtLine();
	    }
	}
	
	if (extBuilder != null) {
	   extBuilder.finishBuilding(show);
	}
	builder.finishBuilding();
    }

    //***************    Convenience Methods    ******************

    private Feature[] makeFeatureList(String[] names) throws IOException {
	Feature[] result = new Feature[names.length];
	for (int i = 0; i < names.length; i++) {
	    result[i] = makeFeature(names[i]);
	}
	return result;
    }

    private Feature makeFeature(String name) throws IOException {
	Feature result = show.getFeature(name);
	if (result == null) {
	    lexer.reportError("Feature \"" + name + "\" not found");
	}
	return result;
    }


    private RCHandler[] makeRCHandlerList(String[] names) throws IOException {
	RCHandler[] result = new RCHandler[names.length];
	for (int i = 0; i < names.length; i++) {
	    result[i] = show.getRCHandler(names[i]);
	    if (result[i] == null) {
		lexer.reportError("RC handler \"" + names[i] + "\" not found");
	    }
	}
	return result;
    }

    private Feature[] lookupFeatureGrid(Assembly assembly, String[][] grid,
    					int width, int height) 
            throws IOException 
    {
	Feature[] result = new Feature[width * height];
	int i = 0;
	for (int y = 0; y < height; y++) {
	    String[] names = grid[y];
	    for (int x = 0; x < width; x++) {
		result[i] = assembly.findPart(names[x]);
		if (result[i] == null) {
		    lexer.reportError("Assembly part \"" + names[x] 
		    		      + "\" not found");
		}
		i++;
	    }
	}
        return result;
    }


    private Feature[] lookupAssemblyParts(Assembly assembly, String[] parts)
            throws IOException 
    {
	if (parts == null) {
	    return null;
	}
	Feature[] result = new Feature[parts.length];
	for (int i = 0; i < parts.length; i++) {
	    if (parts[i] == null) {
		result[i] = null;
	    } else {
		result[i] = assembly.findPart(parts[i]);
		if (result[i] == null) {
		    lexer.reportError("Assembly part \"" + parts[i]
				      + "\" not found");
		}
	    }
	}
        return result;
    }

    private Feature lookupFeatureOrFail(String name) throws IOException {
	Feature f = show.getFeature(name);
	if (f == null) {
	    lexer.reportError("Feature " + name + " not found,");
	}
        return f;
    }


    private Assembly lookupAssemblyOrFail(String name) throws IOException {
	Feature f = lookupFeatureOrFail(name);
	if (f == null || !(f instanceof Assembly)) {
	    lexer.reportError("Feature " + name + " is not an assembly");
	}
        return (Assembly) f;
    }


    //***************    BASIC TYPES  ************************

    private String[][] parseMatrix() throws IOException {
	Vector v = new Vector();
	parseExpected("{");
	for (;;) {
	    String tok = lexer.getString();
	    if ("}".equals(tok)) {
		break;
	    } else if ("{".equals(tok)) {
		String[] el = parseStringsWithOpenBraceRead();
		v.addElement(el);
	    } else {
		lexer.reportError("'{' or '}' expected, " + 
                                  tok + " seen");
	    }
	}
	int num = v.size();
	String[][] result = new String[num][];
	for (int i = 0; i < num; i++) {
	    result[i] = (String[]) v.elementAt(i);
	}
	return result;
    }

    private String[] parseStrings() throws IOException {
	parseExpected("{");
	return parseStringsWithOpenBraceRead();
    }

    private String[] parseStringsWithOpenBraceRead() throws IOException {
	Vector v = new Vector();
	for (;;) {
	    String tok = lexer.getString();
	    if (tok == null) {
		lexer.reportError("EOF unexpected in string list");
	    } else if ("}".equals(tok)) {
		break;
	    } else {
		v.addElement(tok);
	    }
	}
	int num = v.size();
	String[] result = new String[num];
	for (int i = 0; i < num; i++) {
	    result[i] = (String) v.elementAt(i);
	}
	return result;
    }

    private boolean parseBoolean() throws IOException {
	String tok = lexer.getString();
	if ("true".equals(tok)) {
	    return true;
	} else if ("false".equals(tok)) {
	    return false;
	} else {
	    lexer.reportError("\"true\" or \"false\" expected, \"" + tok 
	    			+ "\" seen");
	    return false;
	}
    }

    /**
     * Parse a color representation ("{ r g b a }")
     **/
    public Color parseColor() throws IOException {
	parseExpected("{");
	return parseColorNoOpenBrace();
    }

    /**
     * Parse a color representation when the opening brace has already
     * been read.
     *
     * @see #parseColor()
     **/
    public Color parseColorNoOpenBrace() throws IOException {
	int r = lexer.getInt();
	int g = lexer.getInt();
	int b = lexer.getInt();
	int a = lexer.getInt();
	parseExpected("}");
	return AssetFinder.getColor(r, g, b, a);
    }

    /**
     * Parses a token that we expect to see.  A token is read, and
     * if it's not the expected token, an IOException is generated.
     * This can be useful for things like parsing the ";" at the
     * end of various constructs.
     **/
    public void parseExpected(String expected) throws IOException {
	String tok = lexer.getString();
	if (!(expected.equals(tok))) {
	   lexer.reportError("\"" + expected + "\" expected, \"" + tok 
	   		     + "\" seen");
	}
    }



}

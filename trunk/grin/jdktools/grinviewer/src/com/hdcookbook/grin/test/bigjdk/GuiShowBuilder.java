
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

package com.hdcookbook.grin.test.bigjdk;


import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.commands.ActivateSegmentCommand;
import com.hdcookbook.grin.commands.ActivatePartCommand;
import com.hdcookbook.grin.commands.SegmentDoneCommand;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.features.FixedImage;
import com.hdcookbook.grin.features.Group;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.features.ImageSequence;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.features.Timer;
import com.hdcookbook.grin.features.Translation;
import com.hdcookbook.grin.features.Translator;
import com.hdcookbook.grin.input.RCKeyEvent;
import com.hdcookbook.grin.input.CommandRCHandler;
import com.hdcookbook.grin.input.RCHandler;
import com.hdcookbook.grin.io.ShowBuilder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.AssetFinder;

import java.awt.Font;
import java.awt.Color;
import java.io.Reader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import javax.swing.tree.TreeNode;

/**
 * This is a ShowBuilder that decorates the Show with 
 * tree-browsing classes for GrinView
 *
 * @see GrinView
 *
 * @author Bill Foote (http://jovial.com)
 */
public class GuiShowBuilder extends ShowBuilder {
   
    private GrinView gui;
    private ArrayList segments = new ArrayList();	// <Node>

    public GuiShowBuilder(GrinView gui) {
	this.gui = gui;
    }

    public void addFeature(String name, int line, Feature f) throws IOException
    {
	super.addFeature(name, line, f);
	gui.addLineNumber(f, line);
    }

    public void addSegment(String name, int line, Segment s) throws IOException
    {
	super.addSegment(name, line, s);
	segments.add(new Node(s, null));
	gui.addLineNumber(s, line);
    }

    public void addCommand(Command command, int line) {
	super.addCommand(command, line);
	gui.addLineNumber(command, line);
    }

    public void addRCHandler(String name, int line, RCHandler hand)
    			throws IOException
    {
	super.addRCHandler(name, line, hand);
	gui.addLineNumber(hand, line);
    }

    public void finishBuilding() throws IOException {
	super.finishBuilding();
    }

    TreeNode getShowTree(String showName) {
	Node[] sa = expandNodes(segments);
	segments = null;
	Node tree = new Node(showName, sa);
	return tree;
    }

    private Node[] expandNodes(ArrayList list) {
	Node[] result  = (Node[]) list.toArray(new Node[list.size()]);
	for (int i = 0; i < result.length; i++) {
	    result[i].expand();
	}
	return result;
    }


    static class Node implements TreeNode {

	Object contents;
	Node[] children;
	boolean leaf;
	TreeNode parent;
	boolean expanded = false;
	String label = null;	// Extra label, usually null

	Node(Object contents, Node[] children) {
	    this.contents = contents;
	    this.leaf = children == null;
	    if (this.leaf) {
		if (contents instanceof Segment
		    || contents instanceof Assembly
		    || contents instanceof Timer
		    || contents instanceof Translator
		    || contents instanceof Translation
		    || contents instanceof Group
		    || contents instanceof Modifier) 
		{
		    leaf = false;
		    // expansion will create children
		} else {
		    children = new Node[0];
		}
	    }
	    if (children != null) {
		setChildren(children);
	    }
	}

	private void setChildren(Node[] children) {
	    this.children = children;
	    for (int i = 0; i < children.length; i++) {
		children[i].parent = this;
	    }
	}

	public Enumeration children() {
	    expand();
	    return new Enumeration() {
		private int i = 0;
		public boolean hasMoreElements() {
		    return i < children.length;
		}

		public Object nextElement() {
		    if (i < children.length) {
			return children[i];
		    } else {
			throw new NoSuchElementException();
		    }
		}
	    };
	}

	public boolean getAllowsChildren() {
	    return !leaf;
	}

	public int getChildCount() {
	    expand();
	    return children.length;
	}
        
        public TreeNode getChildAt(int i) {
	    expand();
            return children[i];
        }

	public int getIndex(TreeNode node) {
	    expand();
	    for (int i = 0; i < children.length; i++) {
		if (children[i] == node) {
		    return i;
		}
	    }
	    return -1;
	}

	public TreeNode getParent() {
	    return parent;
	}

	public boolean isLeaf() {
	    return leaf;
	}

	void expand() {
	    if (expanded) {
		return;
	    }
	    expanded = true;
	    if (contents instanceof Segment) {
		Segment seg = (Segment) contents;
		leaf = false;
		Node[] newChildren = new Node[4];
		newChildren[0] = makeNode("active", seg.getActiveFeatures());
		newChildren[1] = makeNode("setup", seg.getSetupFeatures());
		newChildren[2] = makeNode("rc_handlers", seg.getRCHandlers());
		newChildren[3] = makeNode("next", seg.getNextCommands());
		setChildren(newChildren);
	    } else if (contents instanceof Assembly) {
		Assembly a = (Assembly) contents;
		String[] partNames = a.getPartNames();
		setChildren(makeChildren(a.getParts()));
		for (int i = 0; i < children.length; i++) {
		    children[i].label = partNames[i];
		}
	    } else if (contents instanceof Timer) {
		Timer t = (Timer) contents;
		setChildren(makeChildren(t.getEndCommands()));
	    } else if (contents instanceof Translator) {
		Translator t = (Translator) contents;
		Node[] na = new Node[2];
		na[0] = new Node(t.getTranslation(), null);
		na[1] = makeNode("translated feature(s)", t.getFeatures());
		setChildren(na);
		children[0].label = "Translation";
	    } else if (contents instanceof Translation) {
		Translation t = (Translation) contents;
		setChildren(makeChildren(t.getEndCommands()));
	    } else if (contents instanceof Group) {
		Group g = (Group) contents;
		setChildren(makeChildren(g.getParts()));
	    } else if (contents instanceof Modifier) {
		Modifier c = (Modifier) contents;
		Feature[] f = { c.getPart() };
		setChildren(makeChildren(f));
	    } else if (children == null) {
		setChildren(new Node[0]);
	    }
	}

	private static Node makeNode(String name, Object[] kids) {
	    return new Node(name, makeChildren(kids));
	}

	private static Node[] makeChildren(Object[] kids) {
	    Node[] children = new Node[kids.length];
	    for (int i = 0; i < children.length; i++) {
		children[i] = new Node(kids[i], null);
	    }
	    return children;
	}

        
        public String toString() {
	    if (label == null) {
		return contents.toString();
	    } else {
		return label + ": " + contents.toString();
	    }
        }
    }
}

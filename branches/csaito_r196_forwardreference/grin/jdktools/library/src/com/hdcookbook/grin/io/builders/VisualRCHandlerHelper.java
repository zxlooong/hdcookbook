
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

package com.hdcookbook.grin.io.builders;

import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.input.SEVisualRCHandler;

import java.awt.Rectangle;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


/**
 * A helper class for creating a VisualRCHandler object.
 * <p>
 * To use this, create an instance, populate it, then call 
 * getFinishedHandler().
 *
 * @author Bill Foote (http://jovial.com)
 */
public class VisualRCHandlerHelper {

    private String handlerName;
    private List<List<VisualRCHandlerCell>> grid;
    private Map<String, Integer> states = new HashMap<String, Integer>();
    	// Maps state name to number, counting from 0
    private Map<String, VisualRCHandlerCell> stateToCell
	= new HashMap<String, VisualRCHandlerCell>();
	// Maps state name to the defining cell
    private Map<String, String> rcOverrides = new HashMap<String, String>();
	// See setRCOverrides
    private Command[][] selectCommands;
    private Command[][] activateCommands;
    private Rectangle[] mouseRects = null;
    private int[] mouseRectStates = null;
    private int timeout;
    private Command[] timeoutCommands;


    public VisualRCHandlerHelper() {
    }

    /**
     * Sets the handler's name
     **/
    public void setHandlerName(String name) {
	handlerName = name;
    }

    /**
     * Sets the RC grid.
     *
     * @return null 	if all goes well, or an error message if there's
     *			a problem.
     **/
    public String setGrid(List<List<VisualRCHandlerCell>> grid) {
	this.grid = grid;
	if (grid.size() == 0) {
	    return "Empty grid";
	}

	int columns = grid.get(0).size();
	if (columns == 0) {
	    return "Empty grid";
	}

	    // Set up the cells with us (the handler) and the x,y pos
	for (int y = 0; y < grid.size(); y++) {
	    List<VisualRCHandlerCell> row = grid.get(y);
	    for (int x = 0; x < row.size(); x++) {
		VisualRCHandlerCell cell = row.get(x);
		cell.setHelper(this);
		cell.setXY(x, y);
	    }
	}

	    // Set the state at (0,0) to be state 0 numerically.  This
	    // is the default state of the handler.
	String msg;
	assert states.size() == 0;
	msg = grid.get(0).get(0).addState(states, stateToCell);
	if (msg != null) {
	    return msg;
	}
	assert states.size() == 1;


	    // Do two consistency checks:  Make sure that all rows
	    // are the same width, and make sure that a cell doesn't
	    // refer to a cell that itself refers to a cell.
	    //
	    // For each cell, populate the states map

	for (int y = 0; y < grid.size(); y++) {
	    List<VisualRCHandlerCell> row = grid.get(y);
	    if (row.size() != columns) {
		return "Grid row " + y 
			+ " (counting from 0) has a different length.";
	    }
	    for (int x = 0; x < row.size(); x++) {
		VisualRCHandlerCell cell = row.get(x);
		VisualRCHandlerCell to = cell.getRefersTo();
		if (to != null && to.getRefersTo() != null) {
		    return "Grid refers to cell that refers to cell at x,y "
		    	   + x + ", " + y + " (counting from 0)";
		}
		msg = cell.addState(states, stateToCell);
		if (msg != null) {
		    return msg;
		}
	    }
	}

	    // Now check all the cells
	for (int y = 0; y < grid.size(); y++) {
	    List<VisualRCHandlerCell> row = grid.get(y);
	    for (int x = 0; x < row.size(); x++) {
		VisualRCHandlerCell cell = row.get(x);
		msg = cell.check();
		if (msg != null) {
		    return msg;
		}
	    }
	}

	return null;	// null means "no error to report"
    }

    List<List<VisualRCHandlerCell>> getGrid() {
	return grid;
    }

    /**
     * Sets the RC override list.  The maps Maps a key of the form 
     * "direction:from state name" to a state name.  direction is 
     * "up", "down", "right" or "left".  When in the state is
     * "from state name", the given direction will move to the 
     * second state.
     * <p>
     * The value of the map is the state to go to, or the special string
     * "&lt;activate>".
     **/
    public void setRCOverrides(Map<String, String> rcOverrides) {
	this.rcOverrides = rcOverrides;
    }

    Map<String, String> getRCOverrides() {
	return rcOverrides;
    }

    public void setSelectCommands(Command[][] commands) {
	selectCommands = commands;
    }

    public void setActivateCommands(Command[][] commands) {
	activateCommands = commands;
    }

    public void setMouseRects(Rectangle[] rects) {
	mouseRects = rects;
    }

    public void setMouseRectStates(int[] states) {
	mouseRectStates = states;
    }

    public void setTimeout(int timeout) {
	this.timeout = timeout;
    }

    public void setTimeoutCommands(Command[] commands) {
	timeoutCommands = commands;
    }

    /**
     * @return The map from state name to index in array of states
     **/
    public Map<String, Integer> getStates() {
	return states;
    }

    Map<String, VisualRCHandlerCell> getStateToCell() {
	return stateToCell;
    }

    /**
     * @return The state number referred to by the given cell, or -1 if 
     *	       that cell isn't a state or doesn't refer to one.
     **/
    public int getState(int column, int row) {
	String name = grid.get(row).get(column).getState();
	if (name == null) {
	    return -1;
	}
	return states.get(name).intValue();
    }

    public SEVisualRCHandler getFinishedHandler() {
	int[] upDown = new int[states.size()];
	int[] rightLeft = new int[states.size()];
	String[] stateNames = new String[states.size()];
	for (Map.Entry<String, Integer> entry : states.entrySet()) {
	    int stateNum = entry.getValue().intValue();
	    stateNames[stateNum] = entry.getKey();
	    VisualRCHandlerCell cell = stateToCell.get(entry.getKey());
	    upDown[stateNum] = cell.getUpDown();
	    rightLeft[stateNum] = cell.getRightLeft();
	}
	SEVisualRCHandler result
	    = new SEVisualRCHandler(handlerName,  stateNames, upDown, rightLeft,
	    			  selectCommands, activateCommands,
				  mouseRects, mouseRectStates,
				  timeout, timeoutCommands);
	return result;
    }
}

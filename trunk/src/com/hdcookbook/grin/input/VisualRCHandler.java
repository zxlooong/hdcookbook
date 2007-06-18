
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

package com.hdcookbook.grin.input;

import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.features.Assembly;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.util.Debug;
import java.awt.Rectangle;

/**
 * A VisualRCHandler models interaction with the remote control and the
 * mouse as a grid of cells.  Each cell can contain a state, but it
 * can instead contain an address of another cell.  When the user
 * navigates from one cell to another, he lands on the indicated
 * state, or if he lands on a cell that contains the address of another
 * cell, he goes to that cell.  A given state can only occupy one cell.
 * <p>
 * No wrapping behavior is specified:  If a user tries to navigate off
 * the edge of the grid, the state remains unchanged.  If that isn't
 * the desired UI behavior, then simply create a grid where the border
 * consists of cell addresses.
 * <p>
 * A visual handler may optionally be tied to an assembly.  When this
 * handler is activated, the handler is put into the state determined by
 * the assembly, by finding the state that corresponding to the 
 * assembly's currently active part.  Once a handler is active,
 * it assumes that it's the only one in control fo the assembly; it
 * doesn't check for the assembly changing state out from under it.
 * <p>
 * A handler can also optionally have commands associated with its
 * states.  These are invoked only when the handler changes state - a
 * command is <i>not</i> sent for the current state when the handler
 * is activated.  It's up to the application to ensure that the UI
 * is in a state that matches the handler's state before the handler
 * is activated.
 * <p>
 * The state of a handler is determined by the activated flag, and by
 * the named state.  It may be safely changed with a set_visual_rc command.
 * Once the handler is activated, it stays in that state until reset
 * by a set_visual_rc command, a call to setState(...), or the user
 * navigating with the arrow keys.  If the handler is activated and
 * the user presses the enter key again, the handler is "re-activated;"
 * the activation commands are executed again, and
 * the assembly's state is set to selected then immediately to activated,
 * in order to re-run any activation animation.
 * 
 *
 * @author Bill Foote (http://jovial.com)
 */
public class VisualRCHandler extends RCHandler {
 
    /**
     * A special value in a grid that means to activate the current
     * state
     **/
    public final static int GRID_ACTIVATE = -2;

    private static int MASK = RCKeyEvent.KEY_UP.getBitMask()
    				| RCKeyEvent.KEY_DOWN.getBitMask()
    				| RCKeyEvent.KEY_RIGHT.getBitMask()
    				| RCKeyEvent.KEY_LEFT.getBitMask()
    				| RCKeyEvent.KEY_ENTER.getBitMask();
    private String name;
    private int[][] grid;  // [y][x], contains state # if < 0xffff. If bit
    			   // 0x10000 is set, contains y in bits 0xff00, and
			   // x in bits 0x00ff.  Or, it can contain the special
			   // value GRID_ACTIVATE.
    private int[] stateToGrid; // state to x, y mapping; y in bits 0xff00
    private String[] stateNames;   // The names corresponding to state numbers.
    private Assembly assembly;     // can be null
    private Feature[] selectFeatures; // By state #, array can be null, and
    				      // any element can be null.
    private Command[][] selectCommands; // By state #, array can be null, and
           			      // any element can be null.
    private Feature[] activateFeatures;  // by state #, etc.
    private Command[][] activateCommands;  // by state #
    private Rectangle[] mouseRects;   // hit zones on screen for the mouse
    private int[] mouseRectStates;    // The state # corresponding to each rect
    int timeout;	// -1 means "no timeout"
    private Command[] timeoutCommands;

    private boolean activated = false;
    private int currState;
    private int startFrame;
    private boolean timedOut;


    public VisualRCHandler(String name, int[][] grid, String[] stateNames,
			   Command[][] selectCommands, 
			   Command[][] activateCommands, 
			   Rectangle[] mouseRects, int[] mouseRectStates,
			   int timeout, Command[] timeoutCommands) 
    {
	this.name = name;
	this.grid = grid;
	this.stateNames = stateNames;
	this.selectCommands = selectCommands;
	this.activateCommands = activateCommands;
	this.mouseRects = mouseRects;
	this.mouseRectStates = mouseRectStates;
	this.timeout = timeout;
	this.timeoutCommands = timeoutCommands;
	this.stateToGrid = new int[stateNames.length];
	for (int i = 0; i < this.stateToGrid.length; i++) {
	    this.stateToGrid[i] = -1;
	}
	for (int y = 0; y < this.grid.length; y++) {
	    for (int x = 0; x < this.grid[y].length; x++) {
		int g = this.grid[y][x];
		if (g < 0xffff && g != GRID_ACTIVATE) {
		    this.stateToGrid[g] = (y << 8) | x;
		}
	    }
	}
    }

    public void setup(Assembly assembly, Feature[] selectFeatures, 
    		      Feature[] activateFeatures)
    {
	this.assembly = assembly;
	this.selectFeatures = selectFeatures;
	this.activateFeatures = activateFeatures;
	currState = getNewState(0, 0);
	// activating this handler can change its state
    }

    public String toString() {
	return super.toString() + "(" + name + ")";
    }

    private boolean handlesActivation() {
	return activateFeatures != null || activateCommands != null;
    }

    /**
     * This is intended for applications that wish to query the UI
     * state.  The value of this will not be changed as long as a lock
     * is held on our show.
     **/
    public boolean getActivated() {
	return activated;
    }

    /**
     * This is intended for applications that wish to query the UI
     * state.  The value of this will not be changed as long as a lock
     * is held on our show.
     **/
    public int getState() {
	return currState;
    }

    /**
     * Get the name of a numbered state.
     **/
    public String getStateName(int stateNum) {
	return stateNames[stateNum];
    }

    /**
     * Lookup a state number by name.  Used for parsing, or by xlets.
     *
     * @return -1 if not found
     **/
    public int lookupState(String name) {
	for (int i = 0; i < stateNames.length; i++) {
	    if (stateNames[i].equals(name)) {
		return i;
	    }
	}
	return -1;
    }

    public boolean handleRCEvent(RCKeyEvent ke) {
	if ((ke.getBitMask() & MASK) == 0) {
	    return false;
	}
	synchronized(show) {
	    if (ke == ke.KEY_ENTER) {
		if (!handlesActivation()) {
		    return false;
		}
		setState(-1, true, true);
		return true;
	    } 
	    int y = stateToGrid[currState];
            int x = y & 0x00ff;
            y = y >> 8;
            if (Debug.ASSERT && y > 0xff) {
                Debug.assertFail();
            }
            if (ke == ke.KEY_UP) {
                if (y > 0) {
                    y--;
                }
            } else if (ke == ke.KEY_DOWN) {
                if (y < grid.length - 1) {
                    y++;
                }
            } else if (ke == ke.KEY_LEFT) {
                if (x > 0) {
                    x--;
                }
            } else if (ke == ke.KEY_RIGHT) {
                if (x < grid[y].length - 1) {
                    x++;
                }
            }
            setState(getNewState(x, y), false, true);
            return true;
	}
    }
    
    public boolean handleMouse(int x, int y, boolean activate) {
        if (mouseRects == null) {
            return false;
        }
        // Mouse events probably only occur on pretty high-end
        // players, and there probably aren't may rects, so a
        // simple linear search is fine.
        for (int i = 0; i < mouseRects.length; i++) {
            if (mouseRects[i].contains(x, y)) {
                setState(mouseRectStates[i], activate, true);
		return true;
            }
        }
	return false;
    }
    
    private int getNewState(int x, int y) {
        int i = 0;
        for (;;) {
            int newState = grid[y][x];
            if (newState < 0xffff || newState == GRID_ACTIVATE) {
                return newState;
            }
            x = newState & 0x00ff;
            y = 0xff & (newState  >> 8);
            if (Debug.ASSERT && i++ > 5) {
                Debug.assertFail();  // Really, one iteration should always work
            }
        }
    }

    /**
     * Get the state number of the grid position x, y.  Return
     * -1 if the coordinates are outside of the grid.
     **/
    public int getStateChecked(int x, int y) {
	if (x < 0 || y < 0 || grid.length >= y || grid[y].length >= x) {
	    return -1;
	}
	int result = getNewState(x, y);
	if (result < 0) {	// Includes  GRID_ACTIVATE
	    return -1;
	} else {
	    return result;
	}
    }

    /**
     * Called from InvokeVisualCellCommand, and from internal methods.
     * This is synchronized on our show, to only occur during model
     * updated.
     *
     * @param newState	     New state, -1 means "current"
     * @param newActivated   New value for activated
     * @param runCommands
     **/
    public void setState(int newState, boolean newActivated,
		         boolean runCommands) 
    {
	synchronized(show) {
	    if (newState == GRID_ACTIVATE) {
		newState = currState;
		newActivated = true;
	    } else if (newState == -1) {
		newState = currState;
	    }
	    if (newState == currState && newActivated == activated) {
		if (activated) {
		    // If activated, re-run any animations by
		    // briefly setting the assembly to the selected
		    // state.
		    setState(newState, false, false);
		} else {
		    return;
		}
	    }
	    if (Debug.LEVEL > 1) {
		Debug.println("RC handler state becomes " 
			      + stateNames[newState]);
	    }
	    Feature[] fs = newActivated ? activateFeatures : selectFeatures;
	    Command[][] cs = newActivated ? activateCommands : selectCommands;
	    if (fs != null && fs[newState] != null) {
		assembly.setCurrentFeature(fs[newState]);
		if (Debug.LEVEL > 1) {
		    Debug.println("    Setting assembly to " + fs[newState]);
		}
	    }
	    if (runCommands && cs != null) {
		Command[] arr = cs[newState];
		if (arr != null) {
		    for (int i = 0; i < arr.length; i++) {
			show.runCommand(arr[i]);
		    }
		}
	    }
	    currState = newState;
	    activated = newActivated;
	} // end synchronized
    }

    public void activate(Segment s) {
	timedOut = timeout <= -1;
	startFrame = s.getShow().getCurrentFrame();
	if (assembly != null) {
		// If we have an assembly, make our state mirror
		// that of the assembly.
	    Feature curr = assembly.getCurrentPart();
	    int i = lookForFeature(curr, selectFeatures);
	    if (i != -1) {
		currState = i;
		activated = false;
	    } else  {
		i = lookForFeature(curr, activateFeatures);
		if (i != -1) {
		    currState = i;
		    activated = true;
		} else if (Debug.LEVEL > 0) {
		    Debug.println("Handler " + name 
		                  + " can't find current assembly state");
		}
	    }
	}
    }

    private int lookForFeature(Feature f, Feature[] fs) {
	if (fs == null) {
	    return -1;
	}
	for (int i = 0; i < fs.length; i++) {
	    if (fs[i] == f) {
		return i;
	    }
	}
	return -1;
    }

    public void advanceToFrame(int frameNumber) {
    	if (!timedOut && frameNumber > startFrame + timeout) {
	    timedOut = true;
	    for (int i = 0; i < timeoutCommands.length; i++) {
		show.runCommand(timeoutCommands[i]);
	    }
	}
    }
}

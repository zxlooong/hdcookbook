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

package com.hdcookbook.grin;

import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.io.binary.GrinDataInputStream;
import com.hdcookbook.grin.util.Debug;
import java.io.IOException;

/**
 * This class has three distinct functions.  It's mostly a helper
 * class that acts as a superclass for code that gets generated
 * in a GRIN show file, for the java_command structures and for
 * instantiating GRIN extensions.  This class is also used directly
 * for some built-in GRIN commands.  Doing this kind of functional
 * overloading in one class definition is admittedly not very OO,
 * but experimental data shows that classloading is a moderately
 * expensive operation, so we do this overloading to optimize
 * xlet start-up time.
 * <p>
 * This class has three disctinct functions.  To illustrate, assume a
 * show that defines a class called MenuShowCommands as the helper
 * classname in the show file.  The functions of GrinXHelper are:
 * <ul>
 *     <li>There's one instance of MenuShowCommands that's not a 
 *         command at all - it's really a factory object that's used 
 *	   to call the instantiateXXX() method
 *
 *    <li>For each java_command in the show, there's an instance 
 *	  of MenuShowCommands that's set up with the correct 
 *	  commandNumber.  MenuShowCommands overrides execute(), 
 *	  so the switch statement in the override determines
 *	  the meaning of commandNumber, which is automatically generated
 *	  by the show compiler.
 *
 *    <li>For each built-in GRIN command that uses GrinXHelper (that is, 
 *	  each sync_display or segment_done command) becomes a direct 
 *	  instance of GrinXHelper.  In this case, execute() isn't 
 *	  overriden, so we get the built-in switch statement.
 * </ul>
 * <p>
 * For the built-in GRIN commands, the direct instances of GrinXHelper
 * get instantiated "automatically" by the GRIN binary reader.  The
 * class GrinXHelper is registered as a built-in class, so that part 
 * "just works" -- it's represented by the integer constant 
 * GRINXHELPER_CMD_IDENTIFIER.  For the MenuShowCommands instances, 
 * they're represented by the (interned) string that hold the 
 * fully-qualified classname of MenuShowCommands, which the binary 
 * reader feeds (once) into Class.forName() so that it can call
 * newInstance().
 * <p>
 * To see how the java_command commands get compiled into the show's
 * subclass of GrinXHelper, see
 * <code>com.hdcookbook.grin.SEShowCommands</code>
 *
 * <h2>Accessing Blu-ray Player Registers</h2>
 *
 * One thing you might want to do in a java_command is access a player register.
 * That's easy to do, and there's even support in GrinView to emulate the
 * values of player registers, so you can do some testing of the control
 * logic of your show on a PC.  In a show file, you can do this by including
 * a static data member of your command subclass, as defined in the
 * java_generated_class section of your show file:
 * 
 * <pre>
 *     XLET_ONLY [[
 *     private final static org.bluray.system.RegisterAccess
 *         registers = org.bluray.system.RegisterAccess.getInstance();
 *     ]]
 *     GRINVIEW_ONLY [[
 *     private final static com.hdcookbook.grin.test.bigjdk.BDRegisterEmulator
 *         registers = com.hdcookbook.grin.test.bigjdk.BDRegisterEmulator.getInstance();
 *     ]]
 * </pre>
 * 
 * With that, your commands can have statements like
 * "int angleNumber = registers.getPSR(3)" or
 * "registers.setGPR(10, 42)".
 *
 *  @author     Bill Foote (http://jovial.com)
 **/
public class GrinXHelper extends Command implements Node {
    
    protected int commandNumber;
    protected Command[] subCommands;

    /**
     * The commandNumber for a sync_display command
     **/
    protected final static int SYNC_DISPLAY = 0;

    /**
     * The commandNumber for a segment_done command
     **/
    protected final static int SEGMENT_DONE = 1;
    
    public GrinXHelper(Show show) {
        super(show);
    }
    
    public void setCommandNumber(int commandNumber) {
        this.commandNumber = commandNumber;
    }

    public void setSubCommands(Command[] subCommands) {
        this.subCommands = subCommands;
    }

    /**
     * Run a sub-command.  This supports the GRIN_COMMAND_[[ ]] commands
     * within a command body.  This may only be called within the thread
     * that is calling execute() on the parent command.
     * 
     * @param num  The numeric ID of the command to execute.  This is 
     *             automatically generated by the compiler.
     */
    protected void runSubCommand(int num) {
        subCommands[num].execute();
    }
    
    public void readInstanceData(GrinDataInputStream in, int length) 
            throws IOException 
    {
        in.readSuperClassData(this);
	this.commandNumber = in.readInt();
	this.subCommands = in.readCommands();
    }

    /**
     * Execute the command.  This method must be overridden in the show's
     * subclass of GrinXHelper, and that override must not call this
     * method.  The implementation of this method in GrinXHelper executes
     * some built-in GRIN commands.
     **/

    public void execute() {
	switch (commandNumber) {
	    case SYNC_DISPLAY: {
		show.deferNextCommands();
		break;
	    }
	    case SEGMENT_DONE: {
		// This command only makes sense inside a show, so
		// we are being called within Show.nextFrame(),
		// with the show lock held.  That means we don't have to
		// worry about a race condition with the show moving to
		// a different segment before this gets executed.
		show.doSegmentDone();
		break;
	    }
	    default: {
		if (Debug.ASSERT) {
		    Debug.assertFail();
		}
	    }
	}
    }
   
    /**
     * Instantiate an extension class.  This method must be overridden
     * by the show's sublcass of GrinXHelper.
     **/
    public Node getInstanceOf(Show show, int id) throws IOException {
	throw new IOException();
    }
}

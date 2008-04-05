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
import java.io.IOException;

/**
 * Superclass for an automatically generated class that contains
 * methods that are defined as inline java_command objects in a Show file.
 * A show that uses these commands will have a single subclass generated
 * for the show, and an instance of that class will be generated for each
 * occurance of a command in the show.  In the show text file, this is done
 * with a "java_command".  To see how this gets generated, see
 * <code>com.hdcookbook.grin.SEShowCommands</code>
 *
 * <h2>Accessing Blu-ray Player Registers</h2>
 *
 * One thing you might want to do in a command is access a player register.
 * That's easy to do, and there's even support in GrinView to emulate the
 * values of player registers, so you can do some testing of the control
 * logic of your show on a PC.  In a show file, you can do this by including
 * a static data member of your command subclass, as defined in the
 * java_command_class section of your show file:
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
public abstract class GrinXHelper extends Command implements Node {
    
    protected int commandNumber;
    protected Command[] subCommands;
    
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
            throws IOException {   
                
        in.readSuperClassData(this);
	this.commandNumber = in.readInt();
	this.subCommands = in.readCommands();
    }
    
    public abstract Node getInstanceOf(Show show, int id)
            throws IOException;
}
/*
 * UserCommand.java
 */

package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.commands.Command;

/**
 *
 * @author Chihiro Saito
 */
class UserCommand extends Command {
    
    private String typeName;
    private String[] args;
    
    /** Creates a new instance of UserCommand */
    public UserCommand(String typeName, String[] args) {
        super();
        this.typeName = typeName;
        this.args = args;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public String[] getArgs() {
        return args;
    }
    
    public void execute() {
	System.out.println("Executing " + typeName);
    }
    
}

/*
 * UserModifier.java
 */

package com.hdcookbook.grin.binaryconverter;

import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.features.Modifier;

class UserModifier extends Modifier {
    
    private String typeName;
    private String name;
    private String arg;
    
    /** Creates a new instance of UserModifier */
    public UserModifier(Show show, String typeName, String name, String arg)
    {
        super(show, typeName);
        this.typeName = typeName;
        this.arg = arg;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public String getArg() {
        return arg;
    }
    
    public String toString() {
        return typeName;
    }
}

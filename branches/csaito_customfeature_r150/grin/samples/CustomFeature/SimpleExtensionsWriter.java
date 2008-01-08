
import com.hdcookbook.grin.io.binary.ExtensionsWriter;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.commands.Command;

import java.io.DataOutputStream;
import java.io.IOException;

public class SimpleExtensionsWriter implements ExtensionsWriter {

    /**
     * Writes out a feature subclass to a given DataOutputStream.
     * 
     * @param out The OutputStream to write out the data to.
     * @param feature The user-defined Feature subclass to write out.
     * @throws java.io.IOException if IO error occurs.
     */
    public void writeExtensionFeature(DataOutputStream out, Feature feature) throws IOException {
        System.out.println("Writing " + feature.getName());
        out.writeUTF(feature.getName());
    }
    
    /**
     * Writes ou a modifier subclass to a given DataOutputStream.
     * @param out The OutputStream to write out the data to.
     * @param modifier The user-defined Modifier subclass to write out.
     * @throws java.io.IOException if IO error occurs.
     */
    public void writeExtensionModifier(DataOutputStream out, Modifier modifier) throws IOException {
        out.writeUTF(modifier.getName());
    }    
    
    
    /**
     * Writes ou a command subclass to a given DataOutputStream.
     * @param out The OutputStream to write out the data to.
     * @param command The user-defined Command subclass to write out.
     * @throws java.io.IOException if IO error occurs.
     */
    public void writeExtensionCommand(DataOutputStream out, Command command) throws IOException {
    }    

}

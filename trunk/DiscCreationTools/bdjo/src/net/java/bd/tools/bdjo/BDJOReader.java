
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

package net.java.bd.tools.bdjo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
        
/**
 * A class to read BDJO object specified as JavaFX object literal
 * or as a XML document or as a binary BDJO file.
 *
 * @author A. Sundararajan
 */
public final class BDJOReader {
    // don't create me!
    private BDJOReader() {}
    
    private static volatile ScriptEngine javaFxEngine;
    private static void initJavaFxEngine() {
        if (javaFxEngine == null) {
            synchronized (BDJOReader.class) {
                ScriptEngineManager m = new ScriptEngineManager();
                javaFxEngine = m.getEngineByName("FX");
                if (javaFxEngine == null) {
                    throw new RuntimeException("cannot load JavaFX engine, check your CLASSPATH");
                }
            }
        }
    }
    
    public static synchronized BDJO readFX(Reader reader) 
                throws ScriptException {
        initJavaFxEngine();
        return (BDJO) javaFxEngine.eval(reader);
    }
    
    public static synchronized BDJO readFX(String code) 
                throws ScriptException {
        initJavaFxEngine();
        return (BDJO) javaFxEngine.eval(code);
    }
    
    public static BDJO readXML(Reader reader) throws JAXBException {
        String className = BDJO.class.getName();
        String pkgName = className.substring(0, className.lastIndexOf('.'));
        JAXBContext jc = JAXBContext.newInstance(pkgName);
        Unmarshaller u = jc.createUnmarshaller();
        u.setEventHandler(new DefaultValidationEventHandler());
        return (BDJO) u.unmarshal(reader);
    }
    
    public static BDJO readXML(String str) throws JAXBException {
        return readXML(new StringReader(str));
    }
    
    private static String iso646String(byte[] buf) {
        try {
            return new String(buf, "ISO646-US");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }
    
    private static String readISO646String(DataInputStream dis, int len)
            throws IOException {
        byte[] buf = new byte[len];
        dis.read(buf);
        return iso646String(buf);
    }
    
    private static String toUTF8String(byte[] buf) {
        try {
            return new String(buf, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            throw new RuntimeException(uee);
        }
    }
    
    private static String readUTF8String(DataInputStream dis, int len)
            throws IOException {
        byte[] buf = new byte[len];
        dis.read(buf);
        return toUTF8String(buf);
    }
    // section 10.2.2.2 TerminalInfo - Syntax
    private static TerminalInfo readTerminalInfo(DataInputStream dis)
            throws IOException {
        TerminalInfo ti = new TerminalInfo();
        // skip "length" field which is 4 bytes
        dis.skipBytes(4);
        // followed by length, we have default_font_file_name
        ti.setDefaultFontFile(readISO646String(dis, 5));
        // read initial_HAVi_configuration_id, menu_call_mask and title_search_mask
        // - which is 4 bits + 1 bit + 1 bit
        byte b = dis.readByte();
        int id = ((0x0F0 & b) >> 4);
        Enum[] values = HaviDeviceConfig.values();
        for (int i = 0; i < values.length; i++) {
            HaviDeviceConfig hdc = (HaviDeviceConfig)values[i];
            if (hdc.getId() == id) {
                ti.setInitialHaviConfig(hdc);
                break;
            }
        }
        ti.setMenuCallMask((b & 0x08) != 0);
        ti.setTitleSearchMask((b & 0x04) != 0);
        
        // skip reserved_for_future_use 34 bits
        // 2 bits already read as part of above readByte
        dis.skipBytes(4);
        
        return ti;
    }
    
    // section 10.2.3.2 AppCacheInfo - Syntax
    private static AppCacheInfo readAppCacheInfo(DataInputStream dis)
            throws IOException {
        AppCacheInfo aci = new AppCacheInfo();
        // ignore "length" field which is 4 bytes
        dis.skipBytes(4);
        // get "number_of_entries" field
        final int numEntries = dis.readUnsignedByte();
        // followed by that we have "reserved_for_word_align" field
        // which is one byte
        dis.skipBytes(1);
        
        AppCacheEntry[] entries = new AppCacheEntry[numEntries];
        /* each app cache entry is of size is 12 bytes */
        int entrySize = 12;
        for (int e = 0; e < entries.length; e++) {
            AppCacheEntry ace = new AppCacheEntry();
            // entry_type field
            ace.setType(dis.readByte());
            // ref_to_name field
            ace.setName(readISO646String(dis, 5));
            // language_code field
            ace.setLanguage(readISO646String(dis, 3));
            entries[e] = ace;
            // skip "reserved_for_future_use" field
            dis.skipBytes(3);
        }
        aci.setEntries(entries);
        return aci;
    }
    
    // section 10.2.4.2 TableOfAccessiblePlayLists - Syntax
    private static TableOfAccessiblePlayLists readTableOfAccessiblePlayLists(
            DataInputStream dis) throws IOException {
        // ignore "length" field which is 4 bytes
        dis.skipBytes(4);
        // read "number_of_acc_Playlists", "access_to_all_flag" and
        // "autostart_first_PlayList_flag" fields.
        // -- which are 11 bits + 1 bit + 1 bit respectively
        // extract the first 11 bits of the combined short value
        int tmp = 0x0FFFF & (dis.readShort());
        // 0xFFE0 is the pattern 1111111111100000
        final int numPlayLists = (tmp & 0x0FFE0) >> 5;
        TableOfAccessiblePlayLists tapl = new TableOfAccessiblePlayLists();
        // extract 12'th bit of the short value
        tapl.setAccessToAllFlag((tmp & 0x010) != 0);
        // extract 13'th bit of the short value
        tapl.setAutostartFirstPlayListFlag((tmp & 0x08) != 0);
        String[] playLists = new String[numPlayLists];
        
        // read another short to skip the 19 reserved bits as well
        // Out of 19, 3 bits are already read as part of above
        // readShort call
        dis.skipBytes(2);
        
        for (int p = 0; p < playLists.length; p++) {
            // read PlayList_file_name field
            playLists[p] = readISO646String(dis, 5);
            // skip reserved_for_word_align field
            dis.skipBytes(1);
        }
        tapl.setPlayListFileNames(playLists);
        return tapl;
    }
    
    private static ApplicationManagementTable readApplicationManagementTable(
            DataInputStream dis) throws IOException {
        // ignore "length" field which is 4 bytes
        dis.skipBytes(4);
        // get "number_of_applications" field
        int numApps = dis.readUnsignedByte(); 
        // skip reserved_for_word_align field
        dis.skipBytes(1);
        
        AppInfo[] apps = new AppInfo[numApps];
        for (int a = 0; a < numApps; a++) {
            AppInfo ai = new AppInfo();
            // get application_control_code field
            ai.setControlCode(dis.readByte());
            
            // application_type is 4 bits, followed by
            // 4 bit alignment field
            byte b = dis.readByte();
            ai.setType((byte) ((b & 0x0F0) >> 4));
            
            ai.setOrganizationId(dis.readInt());
            ai.setApplicationId(dis.readShort());
            
            // read application descriptor
            // ignore the following fields
            // - descriptor_tag (1 byte)
            // - reserved_word_align (1 byte)
            // - descriptor_length (4 bytes)
            // - reserved_for_future_use (4 bytes)
            dis.skipBytes(10);
                    
            // Application_profiles_count is 4 bit field
            b = dis.readByte();
            final int appProfileCount = ((b & 0x0F0) >> 4);
            
            // ignore "Reserved_for_word_align" (12 bis)
            // out of the 12 bits, we have read 4 bits in
            // the last readByte.
            dis.skipBytes(1);
            
            AppProfile[] profiles = new AppProfile[appProfileCount];
            for (int p = 0; p < appProfileCount; p++) {
                AppProfile ap = new AppProfile();
                ap.setProfile(dis.readUnsignedShort());
                ap.setMajorVersion((short)dis.readUnsignedByte());
                ap.setMinorVersion((short)dis.readUnsignedByte());
                ap.setMicroVersion((short)dis.readUnsignedByte());
                // skip "reserved_for_word_align" field
                dis.skipBytes(1);
                profiles[p] = ap;
            }
            ApplicationDescriptor appDesc = new ApplicationDescriptor();
            appDesc.setProfiles(profiles);
            
            // get "application_priority" field
            appDesc.setPriority((short)dis.readUnsignedByte());
            
            // get "application_binding", "Visibility" fields
            // each 2 bits in size and also the "reserved_for_word_align"
            // field (4 bits)
            b = dis.readByte();
            int bind = ((b & 0x0C0) >> 6);
            Enum[] bindings = Binding.values();
            for (int i = 0; i < bindings.length; i++) {
                if (bindings[i].ordinal() == bind) {
                    appDesc.setBinding((Binding)bindings[i]);
                    break;
                }
            }
            
            int visibility = ((b & 0x030) >> 4);
            Enum[] visibilities = Visibility.values();
             for (int i = 0; i < visibilities.length; i++) {
                if (visibilities[i].ordinal() == visibility) {
                    appDesc.setVisibility((Visibility)visibilities[i]);
                    break;
                }
            }
            
            // read "number_of_application_name_bytes" field
            int totalNameBytes = dis.readUnsignedShort();
            if (totalNameBytes > 0) {
                int nameBytesRead = 0;
                List<AppName> appNames = new ArrayList<AppName>();
                while (nameBytesRead < totalNameBytes) {
                    AppName an = new AppName();
                    an.setLanguage(readISO646String(dis, 3));
                    nameBytesRead += 3;
                    int nameLen = dis.readUnsignedByte();
                    nameBytesRead++;
                    an.setName(readUTF8String(dis, nameLen));
                    nameBytesRead += nameLen;
                    appNames.add(an);
               }
                AppName[] appNamesArr = new AppName[0];
                appNames.toArray(appNamesArr);
                appDesc.setNames(appNamesArr);
            }
                
            // The field next to the alignment for-loop has to start at 16-bit 
	    // word bounday. The current field (in this case "names" is at 
	    // 16-bit boundary. So, if the "length" value is odd, then we have 
	    // 2 bytes length field + odd number of bytes for the name(s) => we 
	    // need to skip one more byte to make it even again.
            if ((totalNameBytes & 0x1) != 0) {
                dis.skipBytes(1);
            }
            
            int iconLength = dis.readUnsignedByte();
            appDesc.setIconLocator(readISO646String(dis, iconLength));           
            
            // length is 1 byte field. If length value is even, then we
            // have 1 byte for length + even bytes for string => we need
            // skip one more byte to make it even again. 
            if ((iconLength & 0x1) == 0) {
                dis.skipBytes(1);
            }
            
            // read "application_icon_flags" field
            appDesc.setIconFlags(dis.readShort());
           
            int baseDirLength = dis.readUnsignedByte();
            appDesc.setBaseDirectory(readISO646String(dis, baseDirLength));
            // there is another for-loop here for word align!!
            if ((baseDirLength & 0x1) == 0) {
                dis.skipBytes(1);
            }
            int classPathLength = dis.readUnsignedByte();
            appDesc.setClasspathExtension(readISO646String(dis, classPathLength));
            // there is another for-loop here for word align!!
            if ((classPathLength & 0x1) == 0) {
                dis.skipBytes(1);
            }
            int initClassLength = dis.readUnsignedByte();
            appDesc.setInitialClassName(readUTF8String(dis, initClassLength));
            // there is another for-loop here for word align!!
            if ((initClassLength & 0x1) == 0) {
                dis.skipBytes(1);
            }
            
            int totalParamBytes = dis.readUnsignedByte();
            if (totalParamBytes > 0) {
                int paramBytesRead = 0;
                List<String> params = new ArrayList<String>();
                while (paramBytesRead < totalParamBytes) {
                    int paramLen = dis.readUnsignedByte();
                    paramBytesRead++;
		    params.add(readUTF8String(dis, paramLen));
                    paramBytesRead += paramLen;
               }
                String[] paramsArr = new String[params.size()];
                params.toArray(paramsArr);
                appDesc.setParameters(paramsArr);
            }
            
            // there is another for-loop here for word align!! 
            if ((totalParamBytes & 0x1) == 0) {
               dis.skipBytes(1);
            }
            
            ai.setApplicationDescriptor(appDesc);
            apps[a] = ai;
        }
        
        ApplicationManagementTable amt = new ApplicationManagementTable();
        amt.setApplications(apps);
        return amt;
    }
    
    public static BDJO readBDJO(InputStream in) throws IOException {
        DataInputStream dis;
        if (in instanceof DataInputStream) {
            dis = (DataInputStream) in;
        } else {
            dis = new DataInputStream(in);
        }
        
        String magic = readISO646String(dis, 4);
        if (!magic.equals("BDJO")) {
            throw new IOException("BDJO magic is missing, not a bdjo file?");
        }
        String version = readISO646String(dis, 4);
        BDJO bdjo = new BDJO();
        bdjo.setVersion(Version.valueOf("V_" + version));

        // TerminalInfo_start_address
        dis.skipBytes(4);
        // AppCacheInfo_start_address
        dis.skipBytes(4);
        // TableOfAccessiblePlayLists_start_address
        dis.skipBytes(4);
        // ApplicationManagementTable_start_address
        dis.skipBytes(4);
        // KeyInterestTable_start_address
        dis.skipBytes(4);
        // FileAccessInfo_start_address
        dis.skipBytes(4);
        
        // reserved_for_future_use 128 bits
        dis.skipBytes(16);
        
        bdjo.setTerminalInfo(readTerminalInfo(dis));
        bdjo.setAppCacheInfo(readAppCacheInfo(dis));
        bdjo.setTableOfAccessiblePlayLists(readTableOfAccessiblePlayLists(dis));
        bdjo.setApplicationManagementTable(readApplicationManagementTable(dis));
        bdjo.setKeyInterestTable(dis.readInt());
        int dirPathsLength = dis.readUnsignedShort();
        bdjo.setFileAccessInfo(readISO646String(dis, dirPathsLength));
        
        return bdjo;
    }
    
    public static void main(String[] args) throws Exception {
        BDJO bdjo;
        if (args[0].endsWith(".fx")) {
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            bdjo = readFX(reader);
        } else if (args[0].endsWith(".bdjo")) {
            bdjo = readBDJO(new BufferedInputStream(new FileInputStream(args[0])));
        } else {
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            bdjo = readXML(reader);
        }
        BDJOWriter.writeFX(bdjo, new PrintWriter(System.out));
    }
}

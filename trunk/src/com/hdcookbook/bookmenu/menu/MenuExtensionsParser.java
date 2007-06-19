
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


package com.hdcookbook.bookmenu.menu;

import java.io.IOException;

import org.bluray.net.BDLocator;

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.Segment;
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.ChapterManager;
import com.hdcookbook.grin.commands.Command;
import com.hdcookbook.grin.features.Modifier;
import com.hdcookbook.grin.input.RCKeyEvent;
import com.hdcookbook.grin.parser.ExtensionsParser;
import com.hdcookbook.grin.parser.ShowBuilder;
import com.hdcookbook.grin.parser.ShowParser;
import com.hdcookbook.grin.parser.Lexer;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;

import com.hdcookbook.bookmenu.menu.commands.PlayVideoCommand;
import com.hdcookbook.bookmenu.menu.commands.PlaySoundCommand;
import com.hdcookbook.bookmenu.menu.commands.SetTextCommand;
import com.hdcookbook.bookmenu.menu.commands.PlayGameCommand;
import com.hdcookbook.bookmenu.menu.commands.NotifyLoadedCommand;
import com.hdcookbook.bookmenu.menu.commands.ActivateBioCommand;
import com.hdcookbook.bookmenu.menu.commands.DownloadBioCommand;
import com.hdcookbook.bookmenu.menu.commands.MakeBookmarkCommand;
import com.hdcookbook.bookmenu.menu.commands.DeleteBookmarkCommand;
import com.hdcookbook.bookmenu.menu.commands.BookmarkUICommand;
import com.hdcookbook.bookmenu.menu.commands.SelectAudioCommand;
import com.hdcookbook.bookmenu.menu.commands.SelectSubtitlesCommand;


public class MenuExtensionsParser implements ExtensionsParser {

    private MenuXlet xlet;

    public MenuExtensionsParser(MenuXlet xlet) {
	this.xlet = xlet;
    }

    public Feature getFeature(Show show, String typeName, 
    			      String name, String arg)
		   throws IOException
    {
	return null;
    }

    public Modifier getModifier(Show show, String typeName, 
    			        String name, String arg)
		   throws IOException
    {
	if ("BOOK:bio_image".equals(typeName)) {
	    return new BioImageFeature(show, name);
	} else {
	    return null;
	}
    }

    public Command parseCommand(Show show, String typeName, Lexer lexer,
				ShowParser parser) 
		    throws IOException
    {
	if ("BOOK:PlayVideo".equals(typeName)) {
	    String tok = lexer.getString();
	    BDLocator loc = null;
	    if ("menu".equals(tok)) {
		loc = xlet.navigator.menuVideoStartPL;
	    } else if ("movie".equals(tok)) {
		loc = xlet.navigator.movieVideoStartPL;
	    } else if ("scene_1".equals(tok)) {
		loc = xlet.navigator.sceneVideoStartPL[0];
	    } else if ("scene_2".equals(tok)) {
		loc = xlet.navigator.sceneVideoStartPL[1];
	    } else if ("scene_3".equals(tok)) {
		loc = xlet.navigator.sceneVideoStartPL[2];
	    } else if ("scene_4".equals(tok)) {
		loc = xlet.navigator.sceneVideoStartPL[3];
	    } else if ("scene_5".equals(tok)) {
		loc = xlet.navigator.sceneVideoStartPL[4];
	    } else if ("nothing".equals(tok)) {
		loc = null;
	    }
	    parser.parseExpected(";");
	    return new PlayVideoCommand(xlet, loc);
	} else if ("BOOK:SetText".equals(typeName)) {
	    String text = lexer.getString();
	    parser.parseExpected(";");
	    return new SetTextCommand(xlet, text);
	} else if ("BOOK:PlayGame".equals(typeName)) {
	    parser.parseExpected(";");
	    return new PlayGameCommand(xlet);
	} else if ("BOOK:ActivateBio".equals(typeName)) {
	    parser.parseExpected(";");
	    return new ActivateBioCommand(xlet);
	} else if ("BOOK:DownloadBio".equals(typeName)) {
	    parser.parseExpected(";");
	    return new DownloadBioCommand(xlet);
	} else if ("BOOK:PlaySound".equals(typeName)) {
	    String text = lexer.getString();
	    parser.parseExpected(";");
	    return new PlaySoundCommand(xlet, text);
	} else if ("BOOK:MakeBookmark".equals(typeName)) {
	    parser.parseExpected(";");
	    return new MakeBookmarkCommand(xlet);
	} else if ("BOOK:DeleteBookmark".equals(typeName)) {
	    parser.parseExpected(";");
	    return new DeleteBookmarkCommand(xlet);
	} else if ("BOOK:BookmarkUI".equals(typeName)) {
	    String tok = lexer.getString();
	    boolean activate = false;
	    if ("select".equals(tok)) {
		activate = false;
	    } else if ("activate".equals(tok)) {
		activate = true;
	    } else {
		lexer.reportError("\"select\" or \"activate\" expected, \""
				  + tok + "\" seen.");
	    }
	    int num = lexer.getInt();
	    if (num < -1 || num > 5) {
		lexer.reportError("" + num + " is an illegal scene number.");
	    }
	    parser.parseExpected(";");
	    return new BookmarkUICommand(xlet, activate, num);
	} else if ("BOOK:SelectAudio".equals(typeName)) {
	    int streamNumber = lexer.getInt();
	    parser.parseExpected(";");
	    return new SelectAudioCommand(xlet, streamNumber);
	} else if ("BOOK:SelectSubtitles".equals(typeName)) {
	    int streamNumber = lexer.getInt();
	    parser.parseExpected(";");
	    return new SelectSubtitlesCommand(xlet, streamNumber);
	} else if ("BOOK:NotifyLoaded".equals(typeName)) {
	    parser.parseExpected(";");
	    return new NotifyLoadedCommand(xlet);
	}
	lexer.reportError("Unrecognized command type  \"" + typeName + "\"");
	return null;
    }

    public void finishBuilding(Show s) throws IOException {
    }

    public void takeMosaicHint(String name, int width, int height, 
                               String[] images)
    {
    }


}

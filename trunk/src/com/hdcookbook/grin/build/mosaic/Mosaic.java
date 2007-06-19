
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

package com.hdcookbook.grin.build.mosaic;

import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grin.util.ManagedImage;
import com.hdcookbook.grin.util.AssetFinder;

import java.awt.AlphaComposite;
import java.awt.Frame;
import java.awt.Component;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Iterator;
import javax.imageio.ImageIO;

/**
 **/

public class Mosaic {

    private LinkedList parts = new LinkedList();

    private int width = 0;
    private int height = 0;
    private String outputName;
    private int position;

    private BufferedImage buffer;
    private Graphics2D graphics;	// into buffer

    public Mosaic() {
	this(2048, 1024);
    }

    public Mosaic(int width, int height) {
	buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
	graphics  = (Graphics2D)  buffer.getGraphics();
	graphics.setComposite(AlphaComposite.Src);
	graphics.setColor(Color.yellow);
	graphics.fillRect(0, 0, buffer.getWidth(), buffer.getHeight());
    }


    public BufferedImage getBuffer() {
	return buffer;
    }

    public int getHeight() {
	return buffer.getHeight();
    }

    public int getWidth() {
	return buffer.getWidth();
    }

    /**
     * Put the image represented by mi and im into the mosaic buffer,
     * if possible.  Otherwise return null.
     **/
    public MosaicPart putImage(ManagedImage mi, BufferedImage im) {
	// Use a fairly simple brute-force first-fit algorithm.

	Rectangle placement = new Rectangle(im.getWidth(), im.getHeight());
	int nextY = buffer.getHeight();
	while (placement.y + placement.height < buffer.getHeight()) {
	    boolean found = true;
	    for (Iterator it = parts.iterator(); found && it.hasNext(); ) {
		MosaicPart part = (MosaicPart) it.next();
		if (part.intersects(placement)) {
		    found = false;
		    placement.x = part.nextX();
		    int y = part.nextY();
		    if (y < nextY) {
			nextY = y;
		    }
		    if (placement.x + placement.width >= buffer.getWidth()) {
			placement.x = 0;
			if (Debug.ASSERT && placement.y >= nextY) {
			    Debug.assertFail();
			}
			placement.y = nextY;
                        nextY = buffer.getHeight();
                    }
		}
	    }
	    if (found) {
		MosaicPart part = new MosaicPart(mi.getName(), this, placement);
		parts.add(part);
		graphics.drawImage(im, placement.x, placement.y, null);
		if (part.nextX() > width) {
		    width = part.nextX();
		}
		if (part.nextY() > height) {
		    height = part.nextY();
		}
		return part;
	    }
	}
	return null;
    }

    public void setOutputName(String outputName) {
	this.outputName = outputName;
    }

    public String getOutputName() {
	return outputName;
    }

    public void setPosition(int position) {
	this.position = position;
    }

    public int getPosition() {
	return position;
    }

    public void writeBuffer(File out) throws IOException {
	BufferedImage used = buffer.getSubimage(0, 0, width, height);
	boolean ok = ImageIO.write(used, "PNG", out);
	if (!ok) {
	    throw new IOException("No writer found");
	}
    }
}

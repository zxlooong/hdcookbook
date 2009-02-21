/*
 * Copyright (c) 2008, Sun Microsystems, Inc.
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

/**
 *  This is a simple program that runs on a computer and collects the time
 *  profile information of the desired operations of an xlet running on a player.
 *  Note down the IP address and the port this program displays when it is
 *  launched, and provide them in a call to Debug.initProfiler() in an xlet
 *  of interest.
 */

import java.io.ByteArrayInputStream;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Date;

import com.hdcookbook.grin.util.Debug;

public class PCProfiler extends Thread {

    private int myPort;
    private DatagramSocket socket;
    private static Map<Integer, Long> timestamps = new HashMap<Integer, Long>();
    private static Map<Integer, String> profileMessages =
            new HashMap<Integer, String>();

    public PCProfiler(int myPort) {

        this.myPort = myPort;
        try {
            socket = new DatagramSocket(myPort);
            InetAddress myAddr = InetAddress.getLocalHost();
            System.out.println("Waiting for messages on port: " + myPort +
                    " on IP address:" + myAddr);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //Receives datagram packets
    public void run() {

        byte[] buffer = new byte[512];
        int read;
        while (true) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            try {
                byte type = 0;
                int id = 0;
                String description = null;
                socket.receive(packet);
                ByteArrayInputStream bis = new ByteArrayInputStream(
                        packet.getData(),
                        packet.getOffset(),
                        packet.getLength());
                DataInputStream din = new DataInputStream(bis);
                try {
                    type = din.readByte();
                    id = din.readInt();
                    byte[] buf = new byte[(packet.getLength() - 5)];
                    din.readFully(buf);
                    description = new String(buf, "UTF-8");
                } catch (IOException e) {
                    Debug.printStackTrace(e);
                }

                if (type == Debug.TIMER_START) {
                    long sTime = System.nanoTime();
                    timestamps.put(id, sTime);
                    profileMessages.put(id, description);

                } else if (type == Debug.TIMER_STOP) {
                    long eTime = System.nanoTime();
                    long sTime = timestamps.get(id);
                    long timeInSec = (eTime - sTime) / 1000000;
                    description = profileMessages.get(id);
                    System.out.println("[Time Profile," +
                            (new Date()).toString() +
                            "] " + description + " took " +
                            (eTime - sTime) + " nano-seconds. or " +
                            timeInSec + " milli-seconds");

                } else {
                    System.out.println("Received an unknown data type:" + type);
                }
            } catch (IOException e) {
                e.printStackTrace();
                socket.close();
                break;
            }
        }
    }


    // Closes the receiving socket
    public void closeReceiveSocket() {
        try {
            socket.close();
        } catch (Exception e) {
        }
    }

    public static void main(String args[]) {
        PCProfiler prof = new PCProfiler(2008);
        prof.start();
    }
}

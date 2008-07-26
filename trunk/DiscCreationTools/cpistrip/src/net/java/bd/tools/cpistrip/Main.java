

package net.java.bd.tools.cpistrip;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;



/**
 * This program strips the Copy Permission Indicator flag from
 * a BDAV MPEG-2 transport stream.  This takes longer to explain 
 * than it takes to do!  This is a utility program you can use to
 * convert an audio/video transport stream into the format expected
 * for a Blu-Ray read/write disc (a BD-RE).
 * <p>
 * Here's the deal:  In a BD-R disc (a pre-recorded disc), the
 * A/V media files are required to be protected with AACS.  The
 * transport streams themselves have a flag set to indicate that
 * they were created with the expectation of being protected. 
 * This flag is called the "Copy Permission Indicator," and it's
 * embedded in every packet of the transport stream.
 * <p>
 * When debugging, you often want to burn a disc image onto a BD-RE.
 * To be spec compliant, you need to remove the AACS directory (if
 * present), and make sure that the copy permission indicator flags
 * are set to 00.  If you got a BD transport stream from an authoring
 * tool, it might have had these flags set to 11, in the expectation of
 * creating a BD-R disc with AACS.
 * <p>
 * This little utility strips off those bits.
 * <p>
 * As of this writing (October 2007), the PS/3 was known to insist that
 * these bits be set to 00.  If they aren't, then a BD-J app will typically
 * start, and run for maybe half a second, until the player notices that
 * the bits are set wrong for a BD-RE disc.  It then kills the disc playback.
 * We expect that this behavior will be typical of most or all players in
 * the near future, since it is the correct behavior.
 * <p>
 * See the BD-ROM spec part 3-1 section 6.2.1 and the AACS spec part
 * 3.10.2 for details.
 * 
 **/
public class Main {


    public static int read(InputStream in, byte[] data) throws IOException {
	int len = 0;
	while (len < data.length) {
	    int i = in.read(data, len, data.length - len);
	    if (i == -1) {
		if (len > 0) {
		    return len;
		} else {
		    return -1;
		}
	    }
	    len += i;
	}
	return len;
    }

    public static void stripCPIBits(InputStream in, OutputStream out) 
    	    throws IOException 
    {
	byte[] packet = new byte[192];	// cf. BD-ROM part 3-1 sec. 6.2.1
	int[] flagCount = { 0, 0, 0, 0 };
	int num = 0;
	for (;;) {
	    int len = read(in, packet);
	    if (len == -1) {
		break;
	    } else if (len < 192) {
		System.err.println("Warning:  Final packet only " + len 
				   + " bytes long.");
		out.write(packet, 0, len);
		break;
	    }
	    num++;
	    if (packet[4] != 0x47) {
		System.err.println("Warning:  packet " + num 
			+ "'s transport packet doesn't start with 0x47.");
	    }
	    int flagVal = (((int) packet[0]) & 0xff) >> 6;
	    flagCount[flagVal]++;
	    packet[0] = (byte) (packet[0] & 0x3f);	
	    	// cf. AACS Blu-ray Disc section 3.10.2
	    out.write(packet);
	}
	System.err.println();
	System.err.println("Finished modifying transport stream.");
	System.err.println("    "+flagCount[0] + " CPI flags left as 00");
	System.err.println("    "+flagCount[1] + " CPI flags changed 01 to 00");
	System.err.println("    "+flagCount[2] + " CPI flags changed 10 to 00");
	System.err.println("    "+flagCount[3] + " CPI flags changed 11 to 00");
	System.err.println(num + " total packets in transport stream.");
	System.err.println();
    }

    public static void usage() {
	System.err.println();
	System.err.println("Usage:  java -jar cpistrip.jar < in.m2ts > out.m2ts");
	System.err.println();
	System.exit(1);
    }

    public static void main(String[] args) {
	if (args.length != 0) {
	    usage();
	}
	try {
	    stripCPIBits(System.in, System.out);
	} catch (IOException ex) {
	    ex.printStackTrace();
	    System.exit(1);
	}
	System.exit(0);
    }
}

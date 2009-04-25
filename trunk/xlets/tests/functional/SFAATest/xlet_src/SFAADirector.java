

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.SFAAEngine;
import com.hdcookbook.grin.animator.AnimationClient;
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.media.PlayerWrangler;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grinxlet.GrinXlet;

import org.bluray.ui.SyncFrameAccurateAnimation;
import org.bluray.ui.AnimationParameters;
import org.bluray.ui.FrameAccurateAnimationTimer;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import javax.media.Time;

public class SFAADirector extends Director implements AnimationContext {

    private static SFAAEngine engine;
    private static final Rectangle sfaaBounds = new Rectangle(0, 160, 960, 360);

    private int count = 0;
    private Text text;
    private Show subShow;

    public SFAADirector() {
    }

    public void initialize() {
	text = (Text) getFeature("F:Text");
    }

    public void heartbeat() {
	count++;
	String s1 = "Count:  " + count;
	String s2 = "SFAA time:  ";
	String s3 = "Player time:  " 
	  	    + PlayerWrangler.getInstance().getMediaTime();
	try {
	    s2 += engine.getAnimationFrameTime().getSeconds();
	} catch (Throwable t) {
	    s2 += t;
	}
	String s4 = "Number of SFAA buffers:  " + MyDirector.numBuffers;
	String s5 = "Skipped frames:  " + engine.getSkippedFrames();
	text.setText(new String[] { s1, s2, s3, s4, s5 });
    }

    public static void startSFAA() {
	Debug.println("Starting SFAA");
	engine = new SFAAEngine(0, true);
	
	AnimationParameters p = new AnimationParameters();
	p.threadPriority = Thread.NORM_PRIORITY - 1;
	p.scaleFactor = 1;
	p.repeatCount = null;
	p.lockedToVideo = false;
	Time start = new Time(2102088888L);
	Time stop = new Time(9776422222L);
	p.faaTimer = FrameAccurateAnimationTimer.getInstance(start, stop);
	Dimension d = new Dimension(sfaaBounds.width, sfaaBounds.height);
	int numBuffers = MyDirector.numBuffers;
	SyncFrameAccurateAnimation sfaa 
	    = SyncFrameAccurateAnimation.getInstance(d, numBuffers, p);
	Debug.println("Created sfaa with " + numBuffers + " buffers.");
	SFAAXlet xlet = (SFAAXlet) GrinXlet.getInstance();
	Container c = xlet.getSFAAContainer();
	c.add(sfaa);
	sfaa.setLocation(sfaaBounds.x, sfaaBounds.y);
	engine.setSFAA(sfaa);

	engine.initialize(new SFAADirector());
    }

    public static void stopSFAA() {
	Debug.println("Stopping the SFAA engine");
	engine.destroy();
    }

    public void animationInitialize() throws InterruptedException {
	Director director = this;
	try {
	    GrinBinaryReader reader = new GrinBinaryReader(AssetFinder.getURL
				    ("sfaa_show.grin").openStream());
	    subShow = new Show(director);
	    reader.readShow(subShow);
	} catch (IOException ex) {
	    if (Debug.LEVEL > 0) {
		Debug.printStackTrace(ex);
		Debug.println("Error reading sfaa_show.grin");
	    }
	    throw new InterruptedException();
	}

	engine.checkDestroy();
	engine.initClients(new AnimationClient[] { subShow });
	SFAAXlet xlet = (SFAAXlet) GrinXlet.getInstance();
	Container c = xlet.getSFAAContainer();
	engine.initContainer(c, sfaaBounds);
    }

    public void animationFinishInitialization() {
	subShow.activateSegment(subShow.getSegment("S:Initialize"));
    }
}

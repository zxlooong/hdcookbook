

import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DirectDrawEngine;
import com.hdcookbook.grin.animator.AnimationClient;
import com.hdcookbook.grin.animator.AnimationContext;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.io.binary.GrinBinaryReader;
import com.hdcookbook.grin.util.AssetFinder;
import com.hdcookbook.grin.util.Debug;
import com.hdcookbook.grinxlet.GrinXlet;

import java.awt.Container;
import java.awt.Rectangle;
import java.io.IOException;

public class SFAADirector extends Director implements AnimationContext {

    private static DirectDrawEngine engine;

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
	text.setText(new String[] { "Count:  " + count });
    }

    public static void startSFAA() {
	Debug.println("Pretending to start SFAA");
	engine = new DirectDrawEngine();
	engine.setFps(24000);
	engine.initialize(new SFAADirector());
	engine.start();
    }

    public static void stopSFAA() {
	Debug.println("Stopping the pretend SFAA");
	engine.destroy();
    }

    public void animationInitialize() throws InterruptedException {
	Director director = this;
	try {
	    GrinBinaryReader reader = new GrinBinaryReader(AssetFinder.getURL
				    ("../build/sfaa_show.grin").openStream());
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
	Container c = GrinXlet.getInstance().getAnimationEngine()
				.getComponent().getParent();
	engine.initContainer(c, new Rectangle(0, 0, 960, 540));
    }

    public void animationFinishInitialization() {
	subShow.activateSegment(subShow.getSegment("S:Initialize"));
    }
}



import com.hdcookbook.grin.Director;
import com.hdcookbook.grin.features.Text;
import com.hdcookbook.grin.media.Playlist;
import com.hdcookbook.grin.media.PlayerWrangler;
import com.hdcookbook.grinxlet.GrinXlet;


public class MyDirector extends Director {

    public Playlist playlist;
    private int count = 0;

    public MyDirector() {
    }

    public void initialize() {
	PlayerWrangler.getInstance().initialize(
		    GrinXlet.getInstance().getAnimationEngine());
	playlist = (Playlist) getFeature("F:Playlist");
    }

    /**
     * @inheritDoc
     **/
    public void notifyDestroyed() {
	PlayerWrangler.getInstance().destroy();
	SFAADirector.stopSFAA();
    }

}

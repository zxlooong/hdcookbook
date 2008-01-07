
import com.hdcookbook.grin.Feature;
import com.hdcookbook.grin.Show;
import com.hdcookbook.grin.animator.DrawRecord;
import com.hdcookbook.grin.animator.RenderContext;
import java.awt.Color;
import java.awt.Graphics2D;

public class Oval extends Feature {
    
    int x;
    int y;
    int w;
    int h;
    Color color;
    boolean isActivated;
    private DrawRecord drawRecord = new DrawRecord();
    
    /** Creates a new instance of Oval */
    public Oval(Show show, String name, int x, int y, int w, int h, Color color) {
        super(show, name);
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.color = color;
    }

        /**
     * @inheritDoc
     **/
    protected void setActivateMode(boolean mode) {
	//
	// This is synchronized to only occur within model updates.
	//
	isActivated = mode;
    }

    /**
     * @inheritDoc
     **/
    protected void setSetupMode(boolean mode) {
    }

    /**
     * @inheritDoc
     **/
    public void doSomeSetup() {
    }

    /**
     * @inheritDoc
     **/
    public boolean needsMoreSetup() {
	return false;
    }

    /**
     * @inheritDoc
     **/
    public void nextFrame() {
    }

    /**
     * @inheritDoc
     **/
    public void addDisplayAreas(RenderContext context) {
	drawRecord.setArea(x, y, w, h);
	drawRecord.setSemiTransparent();
	context.addArea(drawRecord);
    }

    /**
     * @inheritDoc
     **/
    public void paintFrame(Graphics2D gr) {
	if (!isActivated) {
	    return;
	}
        gr.setColor(color);
        gr.fillOval(x, y, w, h);
    }
    
    public void destroy() {}
    
    public int getX() { return x; }
    
    public int getY() { return y; }
    
    public void initialize() {}
    
}

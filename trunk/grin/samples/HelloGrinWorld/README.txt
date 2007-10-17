This is meant to be the simplest GRIN script example imaginable.

The helloworld.txt GRIN script has only one real element - a static image.  
You can run the script directly using grin_viewer.  The "run_grinview" target in build.xml will execute the viewer.

The HelloGrinWorld xlet is a simple BD-J application that uses the helloworld.txt script.  
It parses the script and displays the image directly to the root container (HScene).  
Note that the xlet does not have the animation loop but just displays the first frame in this example.

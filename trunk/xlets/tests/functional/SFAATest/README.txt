
This directory contains a test of sync frame accurate animation,
using the BD API and com.hdcookbook.grin.animator.SFAAEngine.
It starts an xlet with some video, and tracks a circle over a moving
part of the video.

This xlet goes way out of its way to have two non-overlapping
animation managers - one direct draw manager, and one SFAA
manager.  Depending on what you're doing with SFAA, this might
not be necessary, and a subclass of GrinXlet can use the SFAA
manager as its primary.  However, in this example we set a media
start/stop time, which means that the SFAA manager stops the
animation thread when we're not in that part of the video.  This
means the SFAA show can't do things like re-start the video from
the anmation thread.  That's why we do the playlist management
from the direct draw show.


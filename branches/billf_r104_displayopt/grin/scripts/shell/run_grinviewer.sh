#!/bin/sh

GRIN_BASE=`dirname $0`/../..
CLASSES=$GRIN_BASE/build/grin_viewer/classes

case $1 in
    menu)
	    HD_SRC=$GRIN_BASE/xlets/bookmenu/src/com/hdcookbook/bookmenu
	    ASSETS=$HD_SRC/assets
	    BG_IMG=$HD_SRC/menu/test_assets/MenuScreenBG_gray.png
	    java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS \
		    -background $BG_IMG \
		    -fps 24 menu.txt
	    ;;
    test)
	    ASSETS=$GRIN_BASE/samples/GrinViewerXlet
	    java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS show.txt
	    ;;
    *)
    	echo ""
    	echo "Usage:  $0 [ menu | test ]"
    	echo ""
	exit 1
	;;
esac


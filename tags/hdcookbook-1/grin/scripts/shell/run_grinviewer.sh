#!/bin/sh -x
#
#  This shell script runs grinview on various show file
#

GRIN_BASE=`dirname $0`/../..
CLASSES=$GRIN_BASE/build/jdktools/grin_viewer/grinviewer.jar
MENU_CLASSES=$GRIN_BASE/build/xlets_tools/menuxlet
MENU_GENERATED=$GRIN_BASE/build/xlets/menu_generated/grinview
EXTENSION_PARSER=com.hdcookbook.bookmenu.menu.MenuExtensionParser

case $1 in
    menu)
	    HD_SRC=$GRIN_BASE/xlets/bookmenu/src/com/hdcookbook/bookmenu
	    ASSETS=$HD_SRC/assets
	    BG_IMG=$HD_SRC/menu/test_assets/MenuScreenBG_gray.png
	    java -cp $CLASSES:$MENU_CLASSES:$MENU_GENERATED \
	    	    com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS \
		    -extension_parser $EXTENSION_PARSER \
		    -background $BG_IMG \
		    -fps 24 menu.txt
	    ;;

    menu-bin)
    	    cd $GRIN_BASE/scripts/ant
	    ant -f run_jdktools.xml run-grin-viewer-binary
	    ;;

    test)
	    ASSETS=$GRIN_BASE/samples/Scripts/DrawingOptimization
	    java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS show.txt
	    ;;

    ryan)
	    ASSETS=$GRIN_BASE/jdktools/grinviewer/src/com/hdcookbook/grin/test/assets
	    java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS ryan_show.txt
	    ;;
    *)
    	echo ""
    	echo "Usage:  $0 [ menu | menu-bin | test ]"
    	echo ""
	exit 1
	;;
esac


#!/bin/sh
#
#  This shell script runs grinview on various show file
#

GRIN_BASE=`dirname $0`/../..
CLASSES=$GRIN_BASE/build/grin_viewer/classes
MENU_CLASSES=$GRIN_BASE/build/menu_tools
MENU_GENERATED=$GRIN_BASE/build/menu_generated/grinview
EXTENSIONS_FACTORY=com.hdcookbook.bookmenu.menu.MenuExtensionsBuilderFactory

case $1 in
    menu)
	    HD_SRC=$GRIN_BASE/xlets/bookmenu/src/com/hdcookbook/bookmenu
	    ASSETS=$HD_SRC/assets
	    BG_IMG=$HD_SRC/menu/test_assets/MenuScreenBG_gray.png
	    java -cp $CLASSES:$MENU_CLASSES:$MENU_GENERATED \
	    	    com.hdcookbook.grin.test.bigjdk.GrinView \
		    -asset_dir $ASSETS \
		    -extensions_factory $EXTENSIONS_FACTORY \
		    -background $BG_IMG \
		    -fps 24 menu.txt
	    ;;

    menu-bin)
    	    cd $GRIN_BASE/scripts/ant
	    ant -f build_grinview.xml run-grin-viewer
	    ;;

    test)
	    ASSETS=$GRIN_BASE/samples/GrinViewerXlet
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


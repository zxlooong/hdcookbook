#!/bin/sh

GRIN_BASE=`dirname $0`/../..
CLASSES=$GRIN_BASE/build/grin_viewer
HD_SRC=$GRIN_BASE/xlets/bookmenu/src
ASSETS=$HD_SRC/com/hdcookbook/bookmenu/assets
BG_IMG=$HD_SRC/com/hdcookbook/bookmenu/menu/test_assets/MenuScreenBG_gray.png
java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GrinView \
	-asset_dir $ASSETS \
	-background $BG_IMG \
	-fps 5 menu.txt

#!/bin/sh -x
source vars.sh

COMPILE=no	# Set to "yes" to compile before running each time

if [[ $COMPILE == yes ]] ; then
    ./build_book_menu.sh
    if [[ $? != 0 ]] ; then
	exit 1;
    fi
fi

CLASSES=$HDC_BUILD_DIR/grin_viewer
HD_SRC=$HDC_REPOSITORY/grin/xlets/bookmenu/src
ASSETS=$HD_SRC/com/hdcookbook/bookmenu/assets
BG_IMG=$HD_SRC/com/hdcookbook/bookmenu/menu/test_assets/MenuScreenBG_gray.png
java -cp $CLASSES com.hdcookbook.grin.test.bigjdk.GuiGenericMain \
	-asset_dir $ASSETS \
	-background $BG_IMG \
	-fps 5 menu.txt

#!/bin/sh
HDC_NOCHECK_MOUNT="yes"
source vars.sh
DEST=$HDC_BUILD_DIR/grin_tester
COMPILE=yes	# Set to "yes" to compile before running each time

if [[ $COMPILE == yes ]] ; then
    ./build_book_menu.sh
    if [[ $? != 0 ]] ; then
	exit 1;
    fi
fi

HD_SRC=$HDC_REPOSITORY/src
BG_IMG=$HD_SRC/com/hdcookbook/bookmenu/menu/test_assets/MenuScreenBG_gray.png

cp $HD_SRC/com/hdcookbook/bookmenu/assets/menu.txt $DEST/com/hdcookbook/bookmenu/assets
java -cp $DEST com.hdcookbook.grin.test.bigjdk.GuiGenericMain \
	-assets /com/hdcookbook/bookmenu/assets  \
	-background $BG_IMG \
	-fps 5 menu.txt

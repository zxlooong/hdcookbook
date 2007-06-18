#!/bin/sh

HDC_NOCHECK_MOUNT="yes"
source vars.sh
DEST=$HDC_BUILD_DIR/grin_tester

cd $HDC_REPOSITORY/src
SRCS=`find com/hdcookbook/grin \
	-path 'com/hdcookbook/grin/build/*' -prune -o \
	-name '*.java' -print`
rm -rf $DEST
mkdir -p $DEST
echo "Running javac..."
javac -d $DEST $SRCS
if [[ $? != 0 ]] ; then
    exit 1;
fi

echo "Copying assets..."
ASSETS="com/hdcookbook/grin/test/assets com/hdcookbook/bookmenu/assets"
tar cf - $ASSETS --exclude .svn | (cd $DEST ; tar xf -)

echo ""
echo "Created book menu testing tool for big JDK"
echo "see $DEST"
echo ""

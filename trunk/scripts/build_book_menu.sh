#!/bin/sh

HDC_NOCHECK_MOUNT="yes"
source vars.sh
DEST=$HDC_BUILD_DIR/grin_tester
SCRATCH=$HDC_BUILD_DIR/tmp_file
rm -rf $DEST
mkdir -p $DEST

cd $HDC_REPOSITORY/src
echo com/hdcookbook/grin/*.java > $SCRATCH
echo com/hdcookbook/grin/commands/*.java >> $SCRATCH
echo com/hdcookbook/grin/features/*.java >> $SCRATCH
echo com/hdcookbook/grin/input/*.java >> $SCRATCH
echo com/hdcookbook/grin/parser/*.java >> $SCRATCH
echo com/hdcookbook/grin/test/*.java >> $SCRATCH
echo com/hdcookbook/grin/test/bigjdk/*.java >> $SCRATCH
echo com/hdcookbook/grin/util/*.java >> $SCRATCH
echo "Running javac..."
javac -d $DEST @$SCRATCH
if [[ $? != 0 ]] ; then
    exit 1;
fi
rm -f $SCRATCH

echo "Copying assets..."
tar cf - com/hdcookbook/grin/test/assets com/hdcookbook/bookmenu/assets \
	--exclude .svn | (cd $DEST ; tar xf -)

echo ""
echo "Created book menu testing tool for big JDK"
echo "see $DEST"
echo ""

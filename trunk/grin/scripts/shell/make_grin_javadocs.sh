#!/bin/sh

HDC_NOCHECK_MOUNT="yes"
source vars.sh

DEST=$HDC_BUILD_DIR/javadocs/grin
SCRATCH=$HDC_BUILD_DIR/tmp_file

rm -rf $DEST
mkdir -p $DEST

cd $HDC_REPOSITORY/src
find com/hdcookbook/grin -name '*.java' -print > $SCRATCH
javadoc -d $DEST -classpath $HDC_BDJ_PLATFORM_CLASSES @$SCRATCH
if [[ $? != 0 ]] ; then
    echo "Error creating javadocs."
    exit 1;
fi
rm -f $SCRATCH

echo ""
echo "Built javadocs for all source files in the repository."
echo "in $DEST"
echo ""
echo "By the way, before a putback to hdcookbook on dev.java.net, I do:"
echo "    cp -r $DEST/* ../../www/javadocs/grin"
echo ""


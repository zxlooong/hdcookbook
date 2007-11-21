#!/bin/sh

source vars.sh
DEST=$HDC_BUILD_DIR/grin_javadoc

cd $HDC_REPOSITORY
ant build-grin-javadoc
if [[ $? != 0 ]] ; then
    exit 1;
fi


echo ""
echo "Built javadocs for all source files in the repository."
echo "in $DEST"
echo ""
echo "By the way, before a putback to hdcookbook on dev.java.net, I do:"
echo "    cp -r $DEST/* ../../../www/javadocs/grin"
echo ""


#!/bin/sh

source vars.sh
DEST=$HDC_BUILD_DIR/bdjo

#
#  Please excuse this hack.  The BDJO builder requires JDK 1.6, but
#  as of this writing, the JDK 1.6 for OS/X available at developer.apple.com
#  didn't have a JIT for PowerPC.  This makes it undesirable to use as the 
#  default JDK, so this special bit of magic looks for JDK 1.6 in a place 
#  where it only exists under OS/X.
#
#  This should be harmless for windows users.

JAVA=/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Commands/java
JAVAC=/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Commands/javac
JAVADOC=/System/Library/Frameworks/JavaVM.framework/Versions/1.6/Commands/javadoc
if [ ! -f $JAVA ] ; then
    JAVA=java
    JAVAC=javac
    JAVADOC=javadoc
fi

rm -rf $DEST
mkdir -p $DEST
SCRATCH=$HDC_BUILD_DIR/tmp_file
rm -f $SCRATCH

cd $HDC_REPOSITORY/src
echo com/hdcookbook/tools/bdjo/*.java > $SCRATCH
echo "Running javac..."
$JAVAC -d $DEST @$SCRATCH
if [[ $? != 0 ]] ; then
    exit 1;
fi
rm -f $SCRATCH
cp com/hdcookbook/tools/bdjo/jaxb.index $DEST/com/hdcookbook/tools/bdjo

$JAVA -cp $DEST com.hdcookbook.tools.bdjo.Converter \
	com/hdcookbook/bookmenu/bdjo/main_bdjo.xml  \
	$HDC_DISC_BDMV/BDJO/00000.bdjo
echo "Wrote $HDC_DISC_BDMV/BDJO/00000.bdjo"
if [[ $? != 0 ]] ; then
    exit 1;
fi
rm -f $HDC_DISC_BDMV/BACKUP/BDJO/00000.bdjo
# just in case


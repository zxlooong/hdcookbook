#!/bin/sh

source vars.sh

MOSAIC=yes   # Set to "yes" for a slower build and faster xlet load

mkdir -p $HDC_DISC_BDMV/JAR
mkdir -p $HDC_DISC_BDMV/AUXDATA
SCRATCH=$HDC_BUILD_DIR/tmp_file
rm -f $SCRATCH
mkdir -p $HDC_BUILD_DIR

if [[ $MOSAIC == yes ]] ; then
    echo ""
    echo "**********  Mosaic Builder  *************"
    cd $HDC_REPOSITORY/src
    DEST=$HDC_BUILD_DIR/mosaic_builder
    rm -rf $DEST
    mkdir -p $DEST
    echo "Running javac..."
    javac -d $DEST com/hdcookbook/grin/build/mosaic/*.java
    if [[ $? != 0 ]] ; then
	exit 1;
    fi
    MOSAIC_CP=$DEST
fi

echo ""
echo "*************  Menu xlet  *****************"
DEST=$HDC_BUILD_DIR/menuxlet
cd $HDC_REPOSITORY/src
echo com/hdcookbook/grin/*.java > $SCRATCH
echo com/hdcookbook/grin/commands/*.java >> $SCRATCH
echo com/hdcookbook/grin/features/*.java >> $SCRATCH
echo com/hdcookbook/grin/input/*.java >> $SCRATCH
echo com/hdcookbook/grin/parser/*.java >> $SCRATCH
echo com/hdcookbook/grin/util/*.java >> $SCRATCH
echo com/hdcookbook/bookmenu/*.java >> $SCRATCH
echo com/hdcookbook/bookmenu/MonitorIXCInterface.java >> $SCRATCH
echo com/hdcookbook/bookmenu/menu/*.java >> $SCRATCH
echo com/hdcookbook/bookmenu/menu/commands/*.java >> $SCRATCH

rm -rf $DEST
mkdir -p $DEST/classes
echo "Running javac..."
javac -source 1.3 -target 1.3 -bootclasspath $HDC_BDJ_PLATFORM_CLASSES \
	-d $DEST/classes @$SCRATCH
if [[ $? != 0 ]] ; then
    exit 1;
fi
rm -f $SCRATCH
cp com/hdcookbook/bookmenu/menu/*.perm $DEST/classes/com/hdcookbook/bookmenu/menu
if [[ $? != 0 ]] ; then
    exit 1;
fi
cd $DEST/classes
echo "Building jar..."
jar cf ../00002.jar * 
cd ..
cp 00002.jar $HDC_DISC_BDMV/JAR
if [[ $? != 0 ]] ; then
    exit 1;
fi
echo "Created `pwd`/00002.jar and copied to $HDC_DISC_BDMV/JAR"

cd $HDC_REPOSITORY/src
DEST=$HDC_BUILD_DIR/menuassets
rm -rf $DEST
mkdir -p $DEST
echo "Copying assets to $DEST..."
ASSETS="com/hdcookbook/bookmenu/assets"
(cd $ASSETS ; tar cf - * --exclude .svn) | (cd $DEST ; tar xf -)

if [[ $MOSAIC == yes ]] ; then
    echo "Making mosaic"
    java -Xmx512m -cp $MOSAIC_CP \
    	    com.hdcookbook.grin.build.mosaic.Main \
	    -show menu.txt \
	    -assets $DEST \
	    -out $DEST/
    if [[ $? != 0 ]] ; then
	exit 1;
    fi
    rm -rf $DEST/Graphics
fi
cd $DEST
if [[ $? != 0 ]] ; then
    exit 1
fi
find . -name .DS_Store -exec rm {} ";"
rm $HDC_DISC_BDMV/AUXDATA/*
mv sound.bdmv $HDC_DISC_BDMV/AUXDATA
if [[ $? != 0 ]] ; then
    exit 1
fi
mv Font/Lisa.ttf $HDC_DISC_BDMV/AUXDATA/00000.otf
if [[ $? != 0 ]] ; then
    exit 1
fi
mv Font/dvb.fontindex $HDC_DISC_BDMV/AUXDATA
if [[ $? != 0 ]] ; then
    exit 1
fi
rmdir Font
if [[ $? != 0 ]] ; then
    exit 1
fi
echo "Building jar..."
rm -f $HDC_DISC_BDMV/JAR/00004.jar
jar cf $HDC_DISC_BDMV/JAR/00004.jar * 
cd ..
echo "Created $HDC_DISC_BDMV/JAR/00004.jar";

echo ""
echo "*************  Monitor xlet  *****************"

DEST=$HDC_BUILD_DIR/monitorxlet

cd $HDC_REPOSITORY/src
echo com/hdcookbook/grin/util/Debug.java  > $SCRATCH
echo com/hdcookbook/bookmenu/monitor/*.java >> $SCRATCH
echo com/hdcookbook/bookmenu/MonitorIXCInterface.java >> $SCRATCH

rm -rf $DEST
mkdir -p $DEST/classes
echo "Running javac..."
javac -source 1.3 -target 1.3 -bootclasspath $HDC_BDJ_PLATFORM_CLASSES \
	-d $DEST/classes @$SCRATCH
if [[ $? != 0 ]] ; then
    exit 1;
fi
rm -f $SCRATCH
cp com/hdcookbook/bookmenu/monitor/*.perm $DEST/classes/com/hdcookbook/bookmenu/monitor
if [[ $? != 0 ]] ; then
    exit 1;
fi

cd $DEST/classes
echo "Building jar..."
jar cf ../00001.jar * 
cd ..
cp 00001.jar $HDC_DISC_BDMV/JAR
NEEDS_SIGNING="`pwd`/00001.jar"
echo "Created `pwd`/00001.jar"
echo "***  This JAR file needs to be signed  ***"


echo ""
echo "*************  Game xlet  *****************"
DEST=$HDC_BUILD_DIR/gamexlet

cd $HDC_REPOSITORY/src
echo com/hdcookbook/grin/util/Debug.java > $SCRATCH
echo com/hdcookbook/gunbunny/*.java >> $SCRATCH
echo com/hdcookbook/gunbunny/util/*.java >> $SCRATCH

rm -rf $DEST
mkdir -p $DEST/classes
echo "Running javac..."
javac -source 1.3 -target 1.3 -bootclasspath $HDC_BDJ_PLATFORM_CLASSES \
	-d $DEST/classes @$SCRATCH
if [[ $? != 0 ]] ; then
    exit 1;
fi
rm -f $SCRATCH

echo "Copying assets..."
ASSETS="com/hdcookbook/gunbunny/assets"
tar cf - $ASSETS --exclude .svn | (cd $DEST/classes ; tar xf -)

cd $DEST/classes
find . -name .DS_Store -exec rm {} ";"
echo "Building jar..."
jar cf ../00003.jar * 
cd ..
cp 00003.jar $HDC_DISC_BDMV/JAR
echo "Created `pwd`/00003.jar"

echo ""
echo "Be sure you remember to sign the monitor xlet"
echo "It's in $NEEDS_SIGNING"
echo ""

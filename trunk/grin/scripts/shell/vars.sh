#!/bin/sh -x

if [ "$HDC_NOSETVARS" != "yes" ] ; then
    HDC_REPOSITORY=~/java.net/hdcookbook
    HDC_BUILD_DIR=~/projects/build
    HDC_DISC_BDMV=/Volumes/192.168.64.106/BookDiscBD/BDMV
    HDC_BDJ_PLATFORM_CLASSES=~/bd-j/local/bdj_stubs/classes/interactive/classes.zip
    HDC_MOUNT_POINT=/Volumes/192.168.64.106
fi

if [ "$HDC_NOCHECK_MOUNT" != "yes" ] ; then
    if [ ! -d $HDC_MOUNT_POINT ] ; then
	    echo "$HDC_MOUNT_POINT does not exist"
	    exit 1
    fi
fi


#!/bin/sh

cd ../ant/
ant -f build_hdcookbook_xlets.xml build-menu-xlet
if [[ $? != 0 ]] ; then
    exit 1;
fi

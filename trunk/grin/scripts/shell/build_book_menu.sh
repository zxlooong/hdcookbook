#!/bin/sh

cd ../../..
ant build-grinview-menu
if [[ $? != 0 ]] ; then
    exit 1;
fi

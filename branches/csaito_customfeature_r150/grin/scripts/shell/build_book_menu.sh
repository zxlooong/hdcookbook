#!/bin/sh

cd ../../..
ant build-grinview
if [[ $? != 0 ]] ; then
    exit 1;
fi

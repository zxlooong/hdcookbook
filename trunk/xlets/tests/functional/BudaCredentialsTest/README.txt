

This directory contains projects to build three disc images
that are used to exercise BUDA credentials.  Each is signed
with a different root certificate, and uses a different org ID.
The three disc images are:

Creator:  This image uses a BUDA credential to create a file in the
directory owned by "Middle".

Middle:  This image doesn't use a credential, and checks for the
file created by Creator.  It then creates another file.

Destroyer:  This image uses a BUDA credential containing a wildcard.
It checks for the two files that should be there after Middle has
successfully run.  It then does some read and write tests, and then
deletes the various files out of Middle's directory.



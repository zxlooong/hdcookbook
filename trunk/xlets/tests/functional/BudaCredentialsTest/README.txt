

This directory contains projects to build five disc images
that are used to exercise BUDA credentials.  Each is signed
with a different root certificate, and uses a different org ID.
The five four disc images are:

Creator:  This image uses a BUDA credential to create a file in the
directory owned by "Middle".

Middle:  This image doesn't use a credential, and checks for the
file created by Creator.  It then creates another file.

Destroyer:  This image uses a BUDA credential containing a wildcard.
It checks for the two files that should be there after Middle has
successfully run.  It then does some read and write tests, and then
deletes the various files out of Middle's directory.

Root:  This image is outside of the Creator->Middle->Destroyer test
sequence.  It has the same org ID as "middle", but uses a different
root certificate (whereas the other disc images all have different root
certificates and different org IDs).  See root/README.txt for more information.

Wildcard:  This image is outside of the Creator->Middle->Destroyer test
sequence.  It is meant to be run first, and it exercises wildcarded
credentials, both the "/*" and "/-" variants.  Like "creator," it results
in the file "output_1.txt" being left in the place where "middle" expects
it to be, which allows easy verification that a new file is created in the
directory determined by the root cert of the grantor, even with a wildcard
certificate.

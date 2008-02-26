#!/bin/sh -x

# Recreates an hdcookbook disc image from the build bundle.
# Adjust two variables below to point to the promotion and the 
# rest of the disc image.
bundle=c:/hdcookbook/hdcookbook-bin-bundle.zip 
nojar_image=c:/2007_10_hdcookbook_disc_image_no_jar_bdjo.zip
dist_dir=tmp

if [ ! -f $bundle ] ; then
	echo "Files not found: $bundle ";
        exit 1;
fi

if [ ! -f $nojar_image ]  ; then
	echo "Files not found: $nojar_image ";
        exit 1;
fi


mkdir -p $dist_dir
cd $dist_dir
jar xvf $nojar_image
jar xvf $bundle
cd 2007_10_hdcookbook_disc_image_no_jar_bdjo/CERTIFICATE
cp ../../discroot/CERTIFICATE/*.crt .
cp ../../discroot/CERTIFICATE/*.crt BACKUP
cd ../BDMV
cp -r ../../discroot/JAR .
cp ../../discroot/BDJO/* BDJO

echo "Created disc image at $dist_dir."

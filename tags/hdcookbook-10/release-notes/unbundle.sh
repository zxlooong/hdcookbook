#!/bin/sh -x

# Recreates a hdcookbook disc image from a release bundle, by adding JAR, 
# CERTIFICATE and BDJO to the disc image.
# Adjust "nojar_image" to the root of the hdcookbook disc image downloaded
# from http://hdcookbook.dev.java.net/servlets/ProjectDocumentList.


bundle=`pwd`/HDCookbook-DiscImage
nojar_image=c:/2007_10_hdcookbook_disc_image_no_jar_bdjo.zip

dist_dir=`pwd`/HDCookbook-DiscImage-complete

if [ ! -f $bundle ] ; then
	echo "File not found: $bundle ";
        exit 1;
fi

if [ ! -f $nojar_image ]  ; then
	echo "Disc image not found. $nojar_image ";
	echo "Image can be downloaded from hdcookbook.dev.java.net/servlets/ProjectDocumentList"
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

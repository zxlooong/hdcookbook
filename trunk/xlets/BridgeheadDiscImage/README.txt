
This directory provides a bootstrap xlet that can download a 
new disc image from a PC on network, and perform a VFS update
and start the new image.  This can simplify the development 
process by eliminating the need to burn a physical disc to 
test out a disc image.  Of course, it's limited to BD-Live
(profile 2) players.

client.jar is a simple UI program that can be used on the PC
to perform the download.  It also has options for running from the
command line, if you prefer.  See the SampleDiscImage directory for 
a sample disc image that can be downloaded.

The bridgehead xlet can be programmed to always download
an image from a fixed network address, by configuring it with
xlet arguments (in the BDJO that launches it).  This probably
only makes sense if you launch the bridgehead xlet from some
other xlet.  In this mode, bridgehead still takes over the
screen and gives you and indication of progress; it's still
meant as an engineering and prototyping tool, and not something
you'd include verbatim on a production disc.


USING THE TOOL
=============

  1) Invoke ant on the toplevel.  The disc image will be created 
     in the "BridgeheadDiscImage/dist" directory.

  2)  Burn the bridgehead Disc image to a BD-RE or BD-R and put it 
      in your Blu-ray player.  Press 1 to start the VFS update process.

  3)  Build HelloTVXlet under the SampleDiscImage dir.  This generates 
      a disc image for download by the bridgehead Xlet on the player.  
      This is a sample disc image to be substituted with your own image.

  4)  Use the client application in the "Client/dist" dir, e.g with
      "java -jar Client/dist/client.jar".  It will need the IP address
      of your player.  For example, the IP address of a PS/3 can be 
      found under it's system menu -> System information menu section.
      The upload directory is the directory dist/VFS.  In this example,
      that's SampleDiscImage/dist/VFS.

  5)  You should now have the uploaded xlet running on your player.
-------

Below are some guidelines for making your own disc image directory for 
download.
 
1.  Currently the bridgehead xlet expects the bumf xml and the bumf
signature file to be named as "manifest.xml" and "manifest.sf" and 
to be placed at the root of the disc image.  The bumfgenerator
tool generates a VFS director with these names.

2.  The CERTIFICATE directory can't be replaced during the VFS update, 
so the new disc image will be executed using this BridgeheadXlet's 
CERTIFICATE information.  This means that the BD-J application(s) to 
be downloaded must be signed with some certificate authenticated with 
the root certificate of the bridgehead disc image. 

3. The bridgehead Disc Image's index.bdmv currently sets 00000.bdjo as 
the first title, and TopMenu immediately launches this first title.  
You'll probably need to overwrite at least the bdjo.

4. Currently the bridgeHead xlet is launched from 90000.bdjo, and this 
is marked as a First Playback item in the Bridgehead Disc Image's 
index.bdmv file.  The new disc image downloaded to a BD player should 
not include a new bdjo file named as "90000" or any jar files referred 
by this 90000.bdjo.  Also, if the new disc image provides its own 
index.bdmv, it should still list the bridgehead xlet's 90000.bdjo as 
the first playback item, so that the bridgehead can make it possible to
undo the VFS update, or do a further VFS update when the disc is restarted.
If the new disc image downloaded to a BD player fails to meet these 
requirements, then the Bridgehead Xlet will refuse to perform the VFS 
update with the downloaded image.  (By the way, you can convert index.bdmv 
file to an xml format and back using a tool under tools/index in hdcookbook.)

5.  The bridghead xlet allows you to select a title from its menu; this should
be a title that starts the disc image to be tested.  By default, it's title
1 (bd://1), but the bridgehead xlet lets you use a different title, by
putting a locator string in the file "title.txt" in same BUDA directory
as the manifest file.

If you have a disc image you want to test, you can take modify the
index.bdmv file of that disc image, moving its first play title to the
end of index.bdmv, and setting the bridgehead xlet as the first play
title.  Then, make a locator to the old first play title, and put that
locator string in title.txt.  This way the disc under test always boots
to the bridgehead xlet, so you can always undo the VFS update and download
changes to the disc image under test.

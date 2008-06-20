 
Provides a bootstrap xlet that can download a new disc image off the network, performs VFS update and starts the new image.
This can simplify the development process by eliminating the need to burn a physical disc to test out the disc image against Profile 2 players.


Use client.jar to initiate the download.
See the SampleXlet directory for a sample zip file that can be downloaded.

A step-by-step instruction on how to use this tool.
----------
1) Invoke ant on the toplevel.  The disc image will be created in the "BridgeheadDiscImage/dist" dir.

2) Burn the bridgehead Disc image to a BD-RE and stick it in your Blu-ray player.  Press 1 to start the VFS update process.

3) Build HelloTVXlet under the SampleXlet dir, which generates the zip file for download by the bridgehead Xlet on the player.  This is a sample disc image to be substituted with your own image.

4) Use the client application in the "Client/dist" dir that sources sample (your) Xlet to the bridgehead xlet running on the player waiting for VFS update.  The IP address of the PS/3 can be found under it's system menu -> System information menu section.
Run the client application:
"java -jar <path to client.jar>".  
The client application can take the IP address and the location of the zip file as optional arguments.

5) You should now have either the sample Xlet (HelloTVXlet) or your own Xlet running on your player.
-------

Below are some guidelines for making your own zip file for download.
 
1. Currently the bridgehead xlet expects the bumf xml and the bu signature file to be named as "sample.xml" and "sample.sf" and be placed at the root of the zip.

2. CERTIFICATE directory can't be replaced during the VFS update, so the new disc image will be executed using this BridgeheadXlet's CERTIFICATE information.  This means that your BD-J application to be downloaded needs to be signed with some certificate authenticated with the root certificate of the bridgehead disc image. 

3. The bridgehead Disc Image's index.bdmv currently sets 00000.bdjo as the first title, and TopMenu immediately launches this first title.  It is recommended to overwrite this bdjo with your own title.

4. Currently the bridgeHead xlet is launched from 90000.bdjo, and this is marked as a First Playback item in the Bridgehead Disc Image's index.bdmv file.  The new disc image downloaded to a BD player should not include a new bdjo file named as "90000" or any jar files referred by this 90000.bdjo.  Also, if the new disc image provides it's own index.bdmv, it should still list the bridgehead xlet's 90000.bdjo as the first playback item, so that the bridgehead can provide ongoing support to cancel or do further VFS updates upon disc restart.  If the new disc image downloaded to a BD player fails to meet these requirements, then the Bridgehead Xlet will not be performing the VFS update with the downloaded image. (You can convert index.bdmv file to an xml format and back using a tool under tools/index in hdcookbook.)




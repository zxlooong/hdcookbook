 
Provides a bootstrap xlet that can download a new disc image off the network, performs VFS update and starts the new image.
This can simplify the development process by eliminating the need to burn a physical disc to test out the disc image against Profile 2 players.

 * Some notes:
 * 1. Currently the xlet expects the bumf xml and the bu signature file to be
 * named as "sample.xml" and "sample.sf" and be placed at the root of the zip.
 * 2. CERTIFICATE directory can't be replaced during the VFS update,
 * so the new disc image will be executed using this BridgeheadXlet's
 * CERTIFICATE information.

Use client.jar to initiate the download.
See the samples directory for a sample zip file that can be downloaded.


A step-by-step instruction on how to use this tool.
----------
1) Invoke ant on the toplevel.  Files will be created in the "dist" dir.

2) Burn the bridgehead Xlet onto a disk and stick it in your Blu-ray player.

3) Build HelloTVXlet under the samples dir, which generates the zip file for download by the bridgehead Xlet on the player.  This is a sample disc image to be substituted with your own image.

4) Use the client application in the "dist" dir that sources sample (your) Xlet to the bridgehead xlet running on the player waiting for VFS download. 
Run the client application:
"java -jar  <path to client.jar>"

5) You should now have either the sample Xlet (HelloTVXlet) or your own Xlet
running on your player.
-------

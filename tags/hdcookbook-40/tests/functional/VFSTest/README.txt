This is a test for VFS update.

The test xlet (VFSUpdateXlet) downloading a new 00000.jar to replace itself after the update.

To try this test:

1. Before building, adjust the variables in build.properties.
2. Open src/VFSUpdateXlet/hellotvxlet/HelloTVXlet.java and adjust the HOSTDIR value.
3. After building the test with ant, upload three files (sample.xml, sample.sf and 00000.jar) created in "dist/Upload" directory to the http server you've specified at step 2.
4. The disc image is created in the "dist/DiscImage" dir.  

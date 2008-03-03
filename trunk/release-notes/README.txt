
Thank you for your interest in the hdcookbook project.  

This bundle contains a build snapshot of the hdcookbook.dev.java.net/svn/trunk code repository.  


- GrinJavaDocs
  Javadocs for the current GRIN APIs.  
  "javase" includes APIs available during the grin compile time on a desktop.
  "javame" is an API set available during the xlet runtime.

- GrinLibraries
  Compiled version of the current GRIN libraries.  
  "javase" is a library set for the grin compile time on a desktop.
  "javame" is a library set that is expected to be bundled with an xlet and be on a disc.  This is compiled with right javac options to work against PBP 1.0.

- GrinTools
  Includes tools for compiling and executing grin show scripts on a desktop.
  
- BDTools
  Includes tools that are helpful in making a valid blu-ray disc image, such as generating a bdjo file and signing a jar file.  
  These tools work independently from the grin framwork.

- HDCookbook-DiscImage
  Holds a hdcookbook sample xlets needed to create a disc image.  
  Use "unbundle.sh" to create a complete, working blu-ray disc image sample from it.

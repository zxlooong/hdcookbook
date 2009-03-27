
This directory holds GRIN extension feature for FontStrip.

FontStrip extension and the image generation tool lets an application 
to use set of image files containing pre-rendered characters for
rendering text.  
The tool user specifies which set of characters should be pre-rendered into 
the character image mosaic, and FontMetrics information for each character 
is preserved so that they can be positioned correctly to form arbitrary strings
at runtime.  FontStrip eliminates the need to include real font files 
(*.otf files in the AUXDATA directory for BD-J) in the application.  Unlike 
font files, FontStrip carries no licensing compilication, since FontStrip 
comes with characters needed for the associated application only, 
which is typically much less than all available characters in a font set.  
This makes FontStrip image of a given disc impossible to be applied for 
a generic use.

Also, FontStrip provides an opportunity for a content designer to 
enhance and customize the look of the font, by updating the generated
font image file (by adding dropshadows and so on), then generating
final font image mosaic from the modified file.


How To Generate Font Image Mosaics
==================================
The tool to generate images is included in the "fontimagemaker.jar".  To 
generate the intermediate font image file and the final font image mosaic, do

java -jar fontimagemaker.jar -config <name of the configuratin file>

Invoke the jar without any argument to see all possible tool options.  

By default, the FontImageMaker tool generates both intermediate design images 
and final character image mosaics, unless the intermediate images specified in 
the configuration file are found in the asset directory.   If you are planning 
to update the look of the font for the disc, then use the FontImageMaker to 
first generate the design image files, update those image files, and then 
re-run the FontImageMaker tool with an identical configuration file passed 
in the first time, but by placing the updated files in the asset directory.
 The final image will be based on the modified design images as opposed to 
the auto-generated ones.

The schema file for the configuration file is at 
jdktools/tools/fontstrip-config.xsd.

An example of the configuration file can be seen at 
<hdcookbook>/xlets/tests/functional/FontStrip/src/assets/input1.xml.

The final character image mosaics are associated with an information file, 
"fontstrip.inf".  This file includes FontMetrics information for the set of 
characters in all the mosaics, as well as other essential data for properly 
rendering characters from the mosaics at application runtime.  Make sure to 
include this information file to the final disc as well as image files. 


How To Use FontStrip Extension Feature
======================================
FontStrip extension is just like any GRIN extension feature.

The BNF describing the syntax of the extension feature is:

fontstrip_text = feature "extension" "fontstrip:text" name 
                 font_name text_pos text_strings 
                 ["hspace" integer] 
                 ["background" color_entry ] ";"

font_name :: = string # Name of the font mosaic image file 
                      # as defined in configuration file's "finalImage" element.

This BNF syntax is a modification of GRIN's Text feature.  Please look at 
the standard GRIN documentation for the description of other elements.  
"hspace" is a optional argument used to add horizontal and vertical spacing 
between each characters, in pixels.

The workspace creates two jars, "sefontstrip.jar" and "fontstrip.jar", to 
support the extension.  "sefontstrip.jar" contains files needed for show's 
binary compiliation and GrinView run.  The FontStrip's fully qualified 
ExtensionParser classname is 
"com.hdcookbook.grin.fontstrip.FontStripExtensionCompiler".  "fontstrip.jar" is
a set of classes needed to support fontstrip extension feature for GRIN, and it
is meant to be added to the grin library on a disc together with the 
"fontstrip.inf" and fontstrip image mosaic files.

To find an example of FontStrip extension being used in the show, go to:
<hdcookbook>/xlets/tests/functional/FontStrip/

May 6th 2009:

This directory holds a first approximation of a tool for
capturing profiling data, and displaying it.  As of this
writing (May 6, 2009), it's just enough for a command-line
programmer to use to make slides for his upcoming JavaOne talk :-)

June 26th 2009:
A new browser for viewing the profiling data is now available.
This browser is in a class called: ProfileBrowser

About Profile Browser:
---------------------

1. This browser reads the saved profiling data from the given file
and displays it. A tigher integration with the capturing tool
will happen soon.

2. This browser uses an open source visualization library called
Prefuse: http://prefuse.org
The prefuse.jar must be made available on the classpath
both during compilation and when running the browser.
For your convenience, we have made prefuse.jar available on hdcookbook
java.net webpage. In order to download it:

1. Go to: http://hdcookbook.dev.java.net

2. Click on 'Document & files'
   (From the left side menu, under the 'Project tools', the third
    section has 'Documents & files').

3. click on 'Tools and Libraries' you can see prefuse.jar (it's around 770KB file).
   (Expand hdcookbook folder on this page, to see Tools and Libraries folder).

A property in the build file lets you choose between the two displays-
a first approximation UI or the advanced browser.
In the user.vars.properties file, set advanced.ui to (yes or no) accordingly.

Profile Browser's features:
---------------------------

1. The vertical bars represent the execution times for each method. The RED
patches indicate the execution times that are out side the standard window of
execution times for a given method.  
If you see many red colored patches, you can increase the deviation factor
say by:  2 ( i.e 2 * standard deviation) or more to identify execution times
that are taking way too longer than the standard execution times for that method. 

2. You can select the time unit that is convenient for analyzing the data at hand.
We have set Microseconds as a default time unit. You can switch between micro, nano,
millis and secs without resulting in any loss of precision during switching.

3. The range slider (right side bar; looks like scroll bar but with arrows pointing
in opposite directions) allows zoom-in and zoom-out (narrowing down)
of the time scale with up and down mouse dragging.
That means you could zoom into a time range of interest by just dragging the mouse.

4. Method Filtering: You can provide an expression to filter the data on the display
(the editable box is on the left at the bottom) and get the timeline for selected
method/s.  For example, you could type in an expression: a | b
This will only plot methods names starting with letters a or b. 
This is useful for focussing on the individual method/s of interest to see how the
timeline looks for them.

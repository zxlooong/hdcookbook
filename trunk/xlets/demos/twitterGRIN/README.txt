

This directory contains a little Twitter viewing xlet.  It was
developed in a little under a day, to demonstrate building a
"Yahoo widgets"-style widget using GRIN.  It was inspired by a
JavaFX widget, but the Twitter connection stuff was taken from
a CLDC twitter widget we found by googling "twitter CLDC client".
Those files all retain their original copyright, and the copyright
notice is kept intact.

A word about the JSON libraries here:  I think eventually we'd want 
a JSON parser as a single class.  The syntax is easy enough - a
chart is at http://json.org.  The thing about the library
here is that the JSONObject type hierarchy just adds classload 
overhead.  It would be more efficient to just have the parser
emit Java types, like HashSet<String, Object>, Object[], String, 
Long and Double.  For throwing togeter this demo, the JSON libraries
I downloaded are adequate, but I'd want a more efficient single-class
parser before promoting JSON to a shared library.


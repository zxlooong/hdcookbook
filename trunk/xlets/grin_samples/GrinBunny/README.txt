

This is an arcade-style game called "GrinBunny."  It is a rewrite
of the Gun Bunny game that is included in the HD cookbook
disc image.  This version is built using the GRIN framework, 
and shows how Java code can drive a GRIN scene graph to produce 
this kind of game.

For the original, see xlets/hdcookbook_discimage/gunbunny.

The xlet that drives the game was made generic.  To adapt to
a different game, you should only need to change vars.properties
a bit, and then look for occurances of "bunny" or "Bunny" in
places like the classname of GrinBunnyXlet, and change it to
something suitable.

The game xlet has a debug screen you can get to with the popup
menu key.  This lets you suppress KEY_RELEASED events, so you can test
how a game would play on a player that doesn't generate them.  The
GrinBunny game doesn't do anything on KEY_RELEASED, so when you do this
test with GrinBunny you'll find that its behavior is unchanged.


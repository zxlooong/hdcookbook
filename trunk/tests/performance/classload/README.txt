
This test was designed to measure two things:

  *  Get an estimate of classload time for xlets on various players
     in general
  *  Specifically measure the effect of combining multiple GRIN commands
     into one class by using switch(), as opposed to having seperate classes
     for each.

A real xlet will use obfuscated class names, so the classes being measured
all have very terse names.

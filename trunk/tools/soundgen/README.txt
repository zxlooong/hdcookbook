This is soundgen tool. This tool converts a set audio files in any formats supported by 
javax.sound.sampled API to a single "sound.bdmv" file. In BD-J, interactive sound(s) 
are stored in a file named "sound.bdmv". Please refer to section 5.6 of BD-ROM System Description 
Part 3 Version 2.02 for the specification of sound.bdmv.


Tool usage:

    java -jar soundgen.jar [-debug] <input sound files> <output sound.bdmv file>

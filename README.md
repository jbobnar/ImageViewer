# ImageViewer
Java based image viewer primarily intended for image presentation.

## Description
The intention was to provide an image presenting software with features that
are needed for presentation (highlight, drawing, soft transitions etc.). The
viewer also supports full color management, valuing the embedded icc profiles
as well as the profile selected for the output device on which the application
is shown (either by using the OS default or profile selected in the application).
Software supports most of the common image formats, such as jpeg, bmp, png, tiff, 
psd, cr2, dng, nef and others.

## Dependencies
Project depends on a slightly modified TwelveMonekys ImageIO plugins, which are
available [here](https://github.com/jbobnar/TwelveMonkeys) (mainly modified to 
allow using monitor profiles). 

It also depends on the it.tidalwave.imageio.raw plugins which provides the IO
for raw image formats. The ImageViewer uses the official 1.7-ALPHA-2 version, 
except that the cr2 and nef plugins were suppressed due to rendering issues
(for those two formats the TwelveMonkeys plugins take over).

The viewer also depends on the JavaGraphics project, the sources of which are
provided in this github project under a special LICENSE. The code of Java Graphics
has been stripped from everything that is not related to image transitions.

Other dependencies are used as they can be found on the central maven repository.

## Compile
mvn compile will do. You can find the jar file in the target folder.

## License
See [here](https://github.com/jbobnar/ImageViewer/blob/master/ImageViewer/lib/LICENSE.info).

## Released version
Released version of software is available in the Release section. The software is
compiled with Java 1.8.0 and compiled to windows executable using the 
[launch4j](http://launch4j.sourceforge.net/). [InnoSetup](http://www.jrsoftware.org/isinfo.php) 
is used for creating the installer.

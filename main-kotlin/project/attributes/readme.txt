Attribute Icons
===============

You can find good icons on thenounproject.com.

Be sure to only use CC0 and PD licensed icons. If you use CC-BY licensed icons, we must attribute the designer in the about box in c:geo.

An xml list of OC icons can be downloaded here: http://www.opencaching.de/xml/ocxml11.php?modifiedsince=20060320000000&user=0&session=0&attrlist=1
An html overview is in ocicons.html


1. Creating SVGs with Inkscape
------------------------------

Old script, does not work with current server structure - let them as reference and for later change:
    For SVGs from thenounproject.com you can use the script ./nounwork
    Example: ./nounwork day http://thenounproject.com/download/zipped/svg_76.zip
    This will download the SVG and start Inkscape. Edit it and save. The canvas color is set automatically (just that you can see the painted things later on) and the resulting day.svg will be put into the folder "new".

Editing in Inkscape:
 - if not done with script: Set the canvas color (pagecolor) "#009674", just that you can see the painted things later on
 - edit if you have to
 - if you need a cache box, take it from other_cache.svg
 - don't use a background or so. Background of the later rendered icons will be black be black.
 - after editing make everything white - exeptions are ok, see field_puzzle.svg or maintenance.svg
 - Select everything with Strg-a
 - go to File > Document Properties
 - let Inkscape automatically adjust the canvas to the selection
 - save

Put the result into the folder "svgs" and don't forget to add it to git (git add svgs/*).


2. create the icons
-------------------

./makeicons1res.sh

An icon is only created when it is not present or the corresponding svg-file is newer than the icon
Icons are written to directory ./drawable-mdpi. Copy them to res/drawable-mdpi when done.


3. edit iconlist.txt
--------------------

Add a new line or change an existing one.
Don't mess up the structure of this file. It is used to automatically create missing strings, html-pages and enumerations.
The first column contains the name of the attribute. In case of geocaching.com, this also is their internal id and is used for parsing websites in c:geo. So don't change any IDs!
The second is the "groundspeak:attribute id" from a gpx file. You can get them from a cache gpx export.
The third is the ACODE, the "opencaching id". This you will get from their attribute-definitions.xml. To prepare this part follow ../attributes_okapi/readme.txt.


4. create a list of strings that are (not) missing
--------------------------------------------------

./listEnStrings.sh

will list all attribute strings from strings.xml and creates empty string tags for missing strings.
Create the missing strings in values/strings.xml.
The output of the missing string tags lack a ">" sign so that you get a compiler error as a reminder, when you inserted them into values/strings.xml.


5. create the Enums for CacheAttributes.java
--------------------------------------------

./makeEnums.sh

will print out the enums. Use this output and paste it into CacheAttribute.java. Exchange the last comma with a semikolon.


6. create an html page with all icons to check your work
--------------------------------------------------------

./makehtmlpage1res.sh

In the current version of OKAPI (roughly rev. 890 and onwards) are attributes returned with a unified id, the acode.
The metadata file to define the unified attributed is available under:
https://github.com/opencaching/okapi/blob/master/okapi/services/attrs/attribute-definitions.xml
which can serve as a starting point for the generation of an attribute list.

A second point is the wiki entry https://wiki.opencaching.eu/index.php?title=Cache_attributes which describes also the state or planned changes.

If attribute-definitions.xml will be updated, we need of course to check first if it is structurally compatible to the previous version.
If present it seems to be necessary to remove the BOM at the beginning of the file. Then you can run GenerateAttributes
and copy new/changed lines from attrlist.txt to the iconlist.txt in the attributes folder and continue there with the proper generation
of icons and enumerations and such.

to generate attrlist.txt, do:

    javac -d AttrGen AttrGen/src/GenerateAttributes.java
    java -cp AttrGen GenerateAttributes attribute-definitions.xml > attrlist.txt
    rm AttrGen/*.class 2>/dev/null

or for Windows:

    "%JAVA_HOME%\bin\javac" -d AttrGen AttrGen\src\GenerateAttributes.java
    "%JAVA_HOME%\bin\java" -cp AttrGen GenerateAttributes attribute-definitions.xml > attrlist.txt
    del AttrGen\*.class

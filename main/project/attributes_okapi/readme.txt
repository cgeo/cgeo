In the current version of OKAPI (roughly rev. 890 and onwards) are attributes returned with a unified id, the acode.
The metadata file to define the unified attributed is available under:
http://opencaching-api.googlecode.com/svn/trunk/okapi/services/attrs/attribute-definitions.xml
which do not officially publish as a stable definition, but which can serve as a starting point for the generation of an attribute list.
In the future we can get an updated list also from okapi, but this does not contain references between the local oc ids and the acodes.

If attributes.xml will be updated, we need of course to check first if it is structurally compatible to the previous version.
If present it seems to be necessary to remove the BOM at the beginning of the file. Then you can run genattr.sh
and copy new lines from attrlist.txt to the iconlist.txt in the attributes folder and continue there with the proper generation
of icons and enumerations and such.

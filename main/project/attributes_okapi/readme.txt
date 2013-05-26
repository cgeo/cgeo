In the current version of OKAPI (rev. 798) are attributes not returned with a unified id but only with the localized text.
Luckily a metadata file to prepare the unification of these attributes has already been prepared by the OKAPI project
(http://code.google.com/p/opencaching-api/source/browse/trunk/etc/attributes.xml), which do not officially publish as a stable definition,
but which can serve as an easier starting point for the generation of a parser class.
To allow the representation with icons we need to map these localized texts to our internal ids which is done with a parser
generated from the aforementioned file. Soo the AttrGen project for more details.

If attributes.xml will be updated, we need of course to check first if it is structurally compatible to the previous version.
If present it seems to be necessary to remove the BOM at the beginning of the file. Then you can run genattr.sh
and copy the generated AttributeParser.java to the appropriate location (connector.oc).

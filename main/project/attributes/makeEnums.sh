#!/bin/bash
#
# creates Enums from iconslist.txt
#
# structure:
#   UNKNOWN(0, 0, "unknown", R.drawable.attribute_unknown, R.string.attribute_unknown_yes, R.string.attribute_unknown_no),

require () {
    hash "$1" 2>&- || { echo >&2 "I require $1 but it's not installed.  Aborting."; exit 1; }
}

require sed
require cut

# shellcheck disable=SC2002
cat iconlist.txt | grep -v "^#" | while read -r l; do
    name=$(echo "$l" | cut -d "|" -f 1 | sed "s/ *//g")
    gcid=$(echo "$l" | cut -d "|" -f 2 | sed "s/ *//g")
    ocid=$(echo "$l" | cut -d "|" -f 4 | sed "s/ *//g")
    enum=$(echo "$name" | tr '[:lower:]' '[:upper:]')
    yes=R.string.attribute_${name}_yes
    no=R.string.attribute_${name}_no

    [ -z "$gcid" ] && gcid=-1
    [ -z "$ocid" ] && ocid=-1

    echo "    ${enum}($gcid, $ocid, \"$name\", R.drawable.attribute_$name, $yes, $no),"
done

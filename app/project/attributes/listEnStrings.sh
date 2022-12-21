#!/bin/bash

require () {
    hash "$1" 2>&- || { echo >&2 "I require $1 but it's not installed.  Aborting."; exit 1; }
}

require sed
require cut

stringfile="../../res/values/strings.xml"

# shellcheck disable=SC2002
cat iconlist.txt | grep -v "^#" | while read -r l; do
    name=$(echo "$l" | cut -d "|" -f 1 | sed "s/ *//g")
    att=attribute_${name}_
    for yn in yes no; do
        line="$(grep "${att}"${yn} $stringfile)"
        if [ -z "$line" ]; then
            echo "  <string name=\"${att}${yn}\"</string>"
        else
            echo "$line"
        fi
    done
done

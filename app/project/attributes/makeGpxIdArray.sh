#!/bin/bash

require () {
    hash "$1" 2>&- || { echo >&2 "I require $1 but it's not installed.  Aborting."; exit 1; }
}

require sed

echo "private final int[] CACHE_ATTRIBUTES = {"

# shellcheck disable=SC2002
cat iconlist.txt | grep -v "^#" \
        | cut -d "|" -f 1,2 \
        | sed "s/ *$//" \
        | grep "[0-9]$" \
        | sort -nk 3 \
        | sed "s/^\([^ ]*\)[ \|]*\([0-9]*\)$/    R.string.attribute_\1_yes, \/\/ GPX-ID \2/" \
        > "$0".tmp

maxid=$(tail -n 1 "$0".tmp | sed "s/^.* \([0-9]*\)$/\1/")

for n in $(seq 0 "$maxid"); do
    l=$(grep " $n$" "$0".tmp)
    if [ -z "$l" ]; then
        echo "    -1, // GPX-ID $n"
    else
        echo "$l"
    fi
done

echo "};"

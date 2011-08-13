#!/bin/sh

getnames () {
    grep "<string\(-array\)*\s*name" "$1" | sed "s/^.*<string\(-array\)*\s*name\s*=\s*\"\([^\"]*\)\".*$/\2/"
}

finddiffs () {
    echo "Missing translations for language '$1':" > $1.missing
    diff -y en.str $1.str > tmp.str
    echo "Only in values/strings.xml (this doesn't mean, that everything has to be translated):" >> $1.missing
    grep "<\||" tmp.str | cut -d " " -f 1 | while read s; do
        grep "string\s*name\s*=\s*\"$s\"" ../../res/values/strings.xml
    done >> $1.missing
    echo "Only in values-$1/strings.xml:" >> $1.missing
    grep ">\||" tmp.str | sed "s/^/x/;s/\s\s*/ /g" | cut -d " " -f 3 | while read s; do
        grep "string\s*name\s*=\s*\"$s\"" ../../res/values-$1/strings.xml
    done >> $1.missing
    rm tmp.str
    [ `cat $1.missing | wc -l` -lt 4 ] && rm $1.missing
}

getnames ../../res/values/strings.xml > en.str
for l in `find ../../res/values-* -name "strings.xml" | sed "s/^.*values-\(..\).*$/\1/"`; do
    getnames ../../res/values-$l/strings.xml > $l.str
    finddiffs $l
done
rm *.str
echo "missing translations:"
wc -l *.missing | sed "s/\.missing//"

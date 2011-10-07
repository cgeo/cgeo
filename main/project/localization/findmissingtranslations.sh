#!/bin/sh

getnames () {
    sed -ne 's/^.*<string\s*name\s*=\s*"\([^\"]*\)".*$/\1/p' $1
}

finddiffs () {
    echo "translations missing or not in the right place for language '$1':" > $1.missing
    diff -y en.str $1.str > tmp.str
    echo "Only in values/strings.xml:" >> $1.missing
    grep "<\||" tmp.str | cut -d " " -f 1 | while read s; do
        grep "<string" ../../res/values/strings.xml | grep "name=\"$s\""
    done | egrep -v '<string name="(contributors|changelog)">'>> $1.missing
    echo "Only in values-$1/strings.xml:" >> $1.missing
    grep ">\||" tmp.str | sed "s/^/x/;s/\s\s*/ /g" | cut -d " " -f 3 | while read s; do
        grep "<string" ../../res/values-$1/strings.xml | grep "name=\"$s\""
    done >> $1.missing
    rm tmp.str
}

usage() {
    echo "Usage: $0 [ <lang-code> | all ]"
    echo "  where <lang-code> is one of:"
    echo "$alllangs"
    exit 1
}

cd `dirname "$0"`

alllangs=`find ../../res/values-* -name "strings.xml" | sed "s/^.*values-\(..\).*$/    \1/"`

if [ $# -ne 1 ]; then
    usage
elif [ "$1" != "all" -a ! -f ../../res/values-$1/strings.xml ]; then
    echo "language file res/values-$1/strings.xml not present"
    echo
    usage
fi

if [ "$1" = "all" ]; then
    langs=$alllangs
else
    langs=$1
fi

echo processing en...
getnames ../../res/values/strings.xml > en.str
for l in $langs; do
    echo processing $l...
    getnames ../../res/values-$l/strings.xml > $l.str
    finddiffs $l
done
rm *.str
echo "missing translations:"

for l in $langs; do
    # Do not count 3 comments
    wc -l $l.missing | sed "s/\.missing//" | awk '{print $2": "$1-3}'
done

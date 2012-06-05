#!/bin/sh

cd `dirname "$0"`

. ./funcs.sh

sourcedir=../../src/cgeo/geocaching
sourcefiles=`cd $sourcedir && find . -name '*.java'`
xmlfiles=`echo ../../res/*/*.xml ../../AndroidManifest.xml`
first=true
if [ x$1 = x-f ]; then
    remove=true
elif [ x$1 = x-n ]; then
    remove=false
else
    echo "Usage: findextratranslations.sh [ -n | -f]" >&2
    echo "         -n: only display" >&2
    echo "         -f: force unused strings removal" >&2
    exit 1
fi

checkpresent() {
    # Status messages are dynamically referenced by name, so they will
    # not appear in the source.
    if [ -z `echo $1 | sed -e 's/^status_.*$//'` ]; then
        return 0
    fi
    # Attributes are a special case, where in fact only the _yes is
    # referenced in the source while the _no should be kept as well
    res=`echo $1 | sed -e 's/^\(attribute_.*_\)no/\1yes/'`
    (cd $sourcedir && grep -m 1 R.string.$res $sourcefiles > /dev/null) || \
      grep -m 1 @string/$1 $xmlfiles > /dev/null
}

checkname() {
    if ! checkpresent $1; then
        checkfirst
        echo "   - $1"
	if $remove; then
	    remove_from_strings $1
	fi
    fi
}

remove_from_strings() {
   sed -i -e "/<string name=\"$1\">/d" ../../res/values*/strings.xml
}

checkfirst() {
    if $first; then
        echo Possibly unreferenced names:
	first=false
    fi
}

for name in `getnames ../../res/values/strings.xml`; do
    checkname $name
done

if $first; then
    echo All names seem to be in use
fi

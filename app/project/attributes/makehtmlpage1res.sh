#!/bin/sh
#
# this script generates a html-page with all icons on it

OUT=./iconlist1res.html
LIST=./iconlist.txt
BG0=#c0c0c0
BG1=#a0a0a0
BG=0
BGCOLOR=$BG0

addrow () {
    attrname=attribute_$1
    # shellcheck disable=SC2129
    echo "<tr bgcolor='$BGCOLOR'>" >> "${OUT}"
    echo "<td><img src='./drawable-mdpi/${attrname}.png'>" >> "${OUT}"
    echo "<img style='background:url(./drawable-mdpi/${attrname}.png' src='./drawable-mdpi/attribute__strikethru.png'>" >> "${OUT}"
    echo "</td>" >> "${OUT}"
    for f in 2 4; do
        id=$(grep "^\<$1\>" $LIST | cut -d "|" -f $f | sed "s/ *//g")
        [ -z "$id" ] && id="&nbsp;"
        echo "<td align=center>$id</td>" >> "${OUT}"
    done
    descyes=$(grep "${attrname}_yes" ../../res/values/strings.xml | sed "s/^.*>\(.*\)<.*$/\1/")
    descno=$(grep "${attrname}_no" ../../res/values/strings.xml | sed "s/^.*>\(.*\)<.*$/\1/")
    echo "<td>$descyes<br>$descno</td><td>${attrname}_yes<br>${attrname}_no</td></tr>" >> "${OUT}"
    BG=$((BG + 1))
    [ $BG -eq 2 ] && BG=0
    [ $BG -eq 0 ] && BGCOLOR=$BG0
    [ $BG -eq 1 ] && BGCOLOR=$BG1
}

echo "<html><body bgcolor='#b0b0b0'>" > "${OUT}"
echo "<table border=1 cellpadding=2><tr><th>icon</th><th>GC-ID</th><th>ACODE</th><th>description</th><th>resource name</th></tr>" >> "${OUT}"

# shellcheck disable=SC2002
cat iconlist.txt | grep -v "^#" | while read -r i; do
    name=$(echo "$i" | cut -d "|" -f 1 | sed "s/ //g")
    addrow "$name"
done

echo "</table></body></html>" >> "${OUT}"

echo "generated file $OUT"

#!/bin/sh
#
# this script generates a html-page with all icons on it

OUT=iconlist1res.html
BG0=#c0c0c0
BG1=#a0a0a0
BG=0
BGCOLOR=$BG0

addrow () {
    echo "<tr bgcolor='$BGCOLOR'>" >> "${OUT}"
    echo "<td><img src='./drawable/${1}.png'></td>" >> "${OUT}"
    desc=`grep "${1}_yes" ../../res/values/strings.xml | sed "s/^.*>\(.*\)<.*$/\1/"`
    echo "<td>$desc</td><td>$1</td></tr>" >> "${OUT}"
    BG=$(( $BG + 1 ))
    [ $BG -eq 2 ] && BG=0
    [ $BG -eq 0 ] && BGCOLOR=$BG0
    [ $BG -eq 1 ] && BGCOLOR=$BG1
}

echo "<html><body bgcolor='#b0b0b0'>" > "${OUT}"
echo "<table border=1 cellpadding=2><tr><th>icon</th><th>description</th><th>resource name</th></tr>" >> "${OUT}"

cat iconlist.txt | grep -v "^#" | while read i; do
    name=`echo $i | cut -d "|" -f 1 | sed "s/ //g"`
    addrow attribute_$name
done
addrow strikethru

echo "</table></body></html>" >> "${OUT}"

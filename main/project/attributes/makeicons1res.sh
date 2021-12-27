#!/bin/bash
#
# creates attribute icons in one resolution only

require () {
    hash "$1" 2>&- || { echo >&2 "I require $1 but it's not installed.  Aborting."; exit 1; }
}

require optipng
#part of ImageMagick package
require convert
#part of ImageMagick package
require composite
require sed

# directory for icons
ICONDIR="./drawable-xhdpi"
# size of the image itself (inside border)
IMGSIZE=64
# size of the whole icon
ICONSIZE=96
# distance of border from edge of icon
BDIST=2
# thickness of border
BSTROKE=2
# size of the round edges
BROUND=8
# color of the border
FCOL=white
# background color of the icon
BCOL=black
# thickness of the strikethru bar
SSTROKE=5
# color of the strikethru bar
SCOL=\#c00000
# file name of strike thru bar
SFNAME="$ICONDIR/attribute__strikethru.png"

# calculated values
BNDIST=$((ICONSIZE - BDIST))

# create output directory if missing
[ -d $ICONDIR ] || mkdir $ICONDIR

# create border
echo "drawing border"
#convert -size ${ICONSIZE}x${ICONSIZE} xc:none -fill ${BCOL} -strokewidth 1 \
#    -draw "roundrectangle ${BDIST},${BDIST} ${BNDIST},${BNDIST} ${BROUND},${BROUND}" \
#    -strokewidth ${BSTROKE} -stroke ${FCOL} \
#    -draw "roundrectangle ${BDIST},${BDIST} ${BNDIST},${BNDIST} ${BROUND},${BROUND}" \
#    border.png
convert -size ${ICONSIZE}x${ICONSIZE} xc:none border.png

# create strike-thru bar as overlay for _no images
echo "drawing ${SFNAME}"
convert -size ${ICONSIZE}x${ICONSIZE} xc:none -fill ${BCOL} -strokewidth 1 \
    -draw "roundrectangle ${BDIST},${BDIST} ${BNDIST},${BNDIST} ${BROUND},${BROUND}" \
    mask1.png
convert -size ${ICONSIZE}x${ICONSIZE} xc:none -fill none -strokewidth ${BSTROKE} -stroke ${FCOL} \
    -draw "roundrectangle ${BDIST},${BDIST} ${BNDIST},${BNDIST} ${BROUND},${BROUND}" \
    mask2.png
convert -size ${ICONSIZE}x${ICONSIZE} xc:none -stroke "${SCOL}" -strokewidth ${SSTROKE} \
    -draw "line 0,0 ${ICONSIZE},${ICONSIZE}" mask1.png -compose DstIn -composite tmp.png
convert tmp.png mask2.png -compose DstOut -composite -depth 8 ${SFNAME}
optipng -quiet ${SFNAME}

if [ $# -gt 0 ]; then
    svgs="$@"
else
    svgs="svgs/*.svg"
fi
for s in $svgs; do
    n=$ICONDIR/attribute_$(basename "$s" | sed "s/\.svg//")

    # don't draw icons if svg is older than icon
    [ -f "${n}.png" ] && [ "$s" -ot "${n}.png" ] && continue

    echo "drawing $n"

    # draw icons
    convert -density 200 -background none "$s" -resize ${IMGSIZE}x${IMGSIZE} tmp.png
    composite -gravity center tmp.png border.png -depth 8 "${n}.png"
    optipng -quiet "${n}.png"
done


rm tmp.png border.png mask1.png mask2.png


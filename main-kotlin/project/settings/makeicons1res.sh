#!/bin/bash
#
# creates attribute icons in one resolution only

require () {
    hash $1 2>&- || { echo >&2 "I require $1 but it's not installed.  Aborting."; exit 1; }
}

require optipng
#part of ImageMagick package
require convert
#part of ImageMagick package
require composite
require sed

# directory for icons
ICONDIR="./drawable-mdpi"
# size of the image itself (inside border)
IMGSIZE=24
# size of the whole icon
ICONSIZE=48

# create output directory if missing
[ -d $ICONDIR ] || mkdir $ICONDIR

if [ $# -gt 0 ]; then
    svgs="$@"
else
    svgs="svgs/*.svg"
fi

convert -size ${ICONSIZE}x${ICONSIZE} xc:none canvas.png

for s in $svgs; do
    n=$ICONDIR/settings_`basename "$s" | sed "s/\.svg//"`

    # don't draw icons if svg is older than icon
    [ -f "${n}.png" ] && [ "$s" -ot "${n}.png" ] && continue

    echo "drawing $n"

    # white

    sed -e "s/fill:#....../fill:#ffffff/g" "$s" > tmp.svg
    convert -density 200 -background none tmp.svg -fill black -resize ${IMGSIZE}x${IMGSIZE} tmp.png
    composite -gravity center tmp.png canvas.png "${n}_white.png"
    optipng -quiet "${n}_white.png"

    # black

    sed -e "s/fill:#....../fill:#000000/g" "$s" > tmp.svg
    convert -density 200 -background none tmp.svg -fill black -resize ${IMGSIZE}x${IMGSIZE} tmp.png
    composite -gravity center tmp.png canvas.png "${n}_black.png"
    optipng -quiet "${n}_black.png"
done


rm canvas.png tmp.png tmp.svg


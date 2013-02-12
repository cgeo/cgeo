#/!bin/bash

if [ $# -ne 2 ]; then
    echo "Usage: $0 <iconname> <zip-link>"
    exit
fi

TMPDIR=`date +"%N"`
mkdir $TMPDIR
mkdir new
wget -O $TMPDIR/x.zip $2
unzip $TMPDIR/x.zip -d $TMPDIR
rm $TMPDIR/x.zip
mv $TMPDIR/*.svg new/$1.svg
rm -rf $TMPDIR
inkscape new/$1.svg
chmod 664 new/$1.svg
sed -i 's/pagecolor="#......"/pagecolor="#009674"/g;s/pageopacity="[^"]*"/pageopacity="1"/g' new/$1.svg

ls new

#! /bin/sh
#

RXJAVA=0.19.6

cd $(git rev-parse --show-toplevel)/main/libs

updatelib () {
  vendor="$1"
  name="$2"
  version="$3"
  rm -f $name*.jar src/$name*.jar
  l=$name-$version.jar
  wget -O $l "http://search.maven.org/remotecontent?filepath=$vendor/$name/$version/$l"
  s=$name-$version-sources.jar
  wget -O src/$s "http://search.maven.org/remotecontent?filepath=$vendor/$name/$version/$s"
  d=$name-$version-javadoc.jar
  wget -O src/$d "http://search.maven.org/remotecontent?filepath=$vendor/$name/$version/$d"
  echo "src=src/$s" > $l.properties
  echo "doc=src/$d" >> $l.properties
  git add $l src/$s src/$d $l.properties
}

updatelib com/netflix/rxjava rxjava-core $RXJAVA
updatelib com/netflix/rxjava rxjava-android $RXJAVA
updatelib com/netflix/rxjava rxjava-async-util $RXJAVA

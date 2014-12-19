#! /bin/sh
#

RXJAVA=1.0.3
RXANDROID=0.23.0
RXJAVAASYNCUTIL=0.21.0
JACKSONCORE=2.4.4
JACKSONDATABIND=2.4.4
JACKSONANNOTATIONS=2.4.4
COMMONSCOLLECTIONS4=4.0
COMMONSLANG3=3.3.2
COMMONSIO=2.4

cd $(git rev-parse --show-toplevel)/main/libs

updatelib () {
  vendor="$1"
  name="$2"
  version="$3"
  rm -f $name*.jar src/$name*.jar $name*.jar.properties
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

fixgradle() {
  var="$1"
  version="$2"
  sed -i "/def $var =/s/.*/def $var = '$version'/" ../build.gradle
}

updatelib io/reactivex rxjava $RXJAVA
fixgradle RXJavaVersion $RXJAVA
updatelib io/reactivex rxandroid $RXANDROID
fixgradle RXAndroidVersion $RXANDROID
updatelib io/reactivex rxjava-async-util $RXJAVAASYNCUTIL
fixgradle RXJavaAsyncUtilVersion $RXJAVAASYNCUTIL

updatelib com/fasterxml/jackson/core jackson-core $JACKSONCORE
fixgradle JacksonCoreVersion $JACKSONCORE
updatelib com/fasterxml/jackson/core jackson-databind $JACKSONDATABIND
fixgradle JacksonDatabindVersion $JACKSONDATABIND
updatelib com/fasterxml/jackson/core jackson-annotations $JACKSONANNOTATIONS
fixgradle JacksonAnnotationsVersion $JACKSONANNOTATIONS

updatelib org/apache/commons commons-collections4 $COMMONSCOLLECTIONS4
fixgradle CommonsCollections4Version $COMMONSCOLLECTIONS4
updatelib org/apache/commons commons-lang3 $COMMONSLANG3
fixgradle CommonsLang3Version $COMMONSLANG3
updatelib commons-io commons-io $COMMONSIO
fixgradle CommonsIoVersion $COMMONSIO

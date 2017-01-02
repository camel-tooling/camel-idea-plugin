#!/bin/sh
# This script will install all files in IntelliJ IDEA's lib/ folder to the local maven .m2 repository. This way we can use them during the build
#
# Usage:
#   ./install-intellij-libs.sh 2016.3.2 /Users/JOE/Applications/IntelliJ IDEA CE.app/Contents

IDEA_VERSION=$1
INTELLIJ_HOME=$2

if [ -z "$INTELLIJ_HOME" ]
then
  echo "Please provide the version and path to the IntelliJ home directory. For example: ./install-intellij-libs.sh 2016.3.2 /Users/JOE/Applications/IntelliJ IDEA CE.app/Contents/"
  exit 1
fi

if [ ! -d "$INTELLIJ_HOME" ]
then
  echo "Directory does not exist: $INTELLIJ_HOME"
  exit 1
fi

echo 'Installing IntelliJ artifacts to Maven local repository'
echo "Intellij home: $INTELLIJ_HOME"

for i in "${INTELLIJ_HOME}"/lib/*.jar
do
    JAR_FILE=$(basename "$i")
    ARTIFACT_ID="${JAR_FILE%.*}"
    mvn install:install-file -Dfile="$i" -DgroupId=com.intellij -DartifactId=${ARTIFACT_ID} -Dversion=${IDEA_VERSION} -Dpackaging=jar
done

mvn install:install-file -Dfile="${INTELLIJ_HOME}/plugins/properties/lib/properties.jar" -DgroupId=com.intellij.plugins -DartifactId=properties -Dversion=${IDEA_VERSION} -Dpackaging=jar
mvn install:install-file -Dfile="${INTELLIJ_HOME}/plugins/properties/lib/resources_en.jar" -DgroupId=com.intellij.plugins -DartifactId=resources_en -Dversion=${IDEA_VERSION} -Dpackaging=jar
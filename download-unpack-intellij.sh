#!/bin/sh
# This script will download the intellij CE version and unpack to the specified destination
#
# Usage:
#   ./download-unpack-intellij.sh 2017.1 ~/Download/IDEA idea-IC-171.3780.107 /User/JOE/.m2
#   version         : version of the file to download
#   destination     : Download and unpack destination directory
#   source-version  : Directory version to unpack from TAR ball
#   m2-home         : M2 home directory

IDEA_VERSION=$1
INTELLIJ_DEST=$2
INTELLIJ_DEST_VERSION=$3
DOWNLOAD_CMD="curl -L -o ideaCI.tar.gz https://download.jetbrains.com/idea/ideaIC-${IDEA_VERSION}.tar.gz"
M2_REPO_ARTIFACT=$4/repository/com/intellij/idea/${IDEA_VERSION}/idea-${IDEA_VERSION}.jar

if [ -z "$INTELLIJ_DEST" ]
then
  echo "Please provide the version and path where Intellij should be unpacked. For example: ./install-intellij-libs.sh 2016.3.3 ~/Download/IDEA idea-IC-163.11103.6 /User/JOE/.m2"
  exit 1
fi

echo "Checking if IntelliJ artifact is already installed $M2_REPO_ARTIFACT"
if [ -f "$M2_REPO_ARTIFACT" ]
then
  echo "Artifacts for Intellij ${IDEA_VERSION} already installed, exit script "
  exit 0
fi

if [ ! -d "$INTELLIJ_DEST/" ]
then
  echo "Directory does not exist: $INTELLIJ_DEST"
  exit 1
fi

echo 'Downloading IntelliJ...'
${DOWNLOAD_CMD}

echo "Unpack intelliJ to ${INTELLIJ_DEST}"
tar -xvf ideaCI.tar.gz --directory ${INTELLIJ_DEST} --strip=1 ${INTELLIJ_DEST_VERSION}/lib/ ${INTELLIJ_DEST_VERSION}/plugins/properties ${INTELLIJ_DEST_VERSION}/plugins/yaml
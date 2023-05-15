#!/bin/bash

echo "## Building with version $1..."
./gradlew --no-daemon clean
./gradlew --no-daemon --no-parallel -PideaVersion="$1" build

status=$?
if [[ $status -ne 0 ]]; then
    echo "## Build for version $1 failed. Exiting." >&2
    exit 1
fi

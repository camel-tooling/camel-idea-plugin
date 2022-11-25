#!/bin/bash

for v in "2022.1.4" "2022.2.4"; do
    echo "## Building with version $v..."
    ./gradlew --no-daemon -PideaVersion="$v" clean build

    status=$?
    if [[ $status -ne 0 ]]; then
        echo "## Build for version $v failed. Exiting." >&2
        exit 1
    fi
done

#!/bin/bash

for v in "2022.2.5" "2022.3.3" "2023.1.1"; do
    echo "## Building with version $v..."
    ./gradlew --no-daemon clean
    ./gradlew --no-daemon --no-parallel -PideaVersion="$v" build

    status=$?
    if [[ $status -ne 0 ]]; then
        echo "## Build for version $v failed. Exiting." >&2
        exit 1
    fi
done

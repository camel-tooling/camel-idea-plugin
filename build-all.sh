#!/bin/bash

for v in "2023.1.3" "2023.2.5" "2023.3.6" "2024.1.4"; do
  ./build.sh $v
done

#!/bin/bash

for v in "2023.1.7" "2023.2.7" "2023.3.7" "2024.1.6"; do
  ./build.sh $v
done

#!/bin/bash

for v in "2023.2.8" "2023.3.8" "2024.1.7" "2024.2.5" "2024.3.1"; do
  ./build.sh $v
done

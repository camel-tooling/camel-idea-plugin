#!/bin/bash

for v in "2022.3.3" "2023.1.3" "2023.2.3" "2023.3"; do
  ./build.sh $v
done

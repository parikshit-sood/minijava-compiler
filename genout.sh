#!/bin/bash

for file in testcases/hw5/*.sparrow-v; do
    filename=$(basename "$file")
    outfile="testcases/hw5/${filename}.out"
    gradle run -q < "$file" | java -jar misc/venus.jar > "$outfile"
done


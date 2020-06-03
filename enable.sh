#!/bin/bash

cd "$(dirname "$0")"
PROJECTROOT="$(pwd -P)"
TESTSPATH="$PROJECTROOT/analysis/src/test/java/ch/ethz/rse/integration/tests"

echo "$TESTSPATH"

cat testSettings.txt | while read line || [[ -n $line ]];
do
    if [[ "${line: -1}"  == "D" ]]
    then
        read -r filename rest_of_string <<<"$line" 
        echo "$filename"
        sed -i 's/ENABLED/DISABLED/' "$TESTSPATH/$filename.java"
    else
        read -r filename rest_of_string <<<"$line" 
        echo "$filename"
        sed -i 's/DISABLED/ENABLED/' "$TESTSPATH/$filename.java"
    fi
done
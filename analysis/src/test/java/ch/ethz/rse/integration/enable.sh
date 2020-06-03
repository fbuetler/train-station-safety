#!/bin/bash

cat testSettings.txt | while read line || [[ -n $line ]];
do
    if [[ "${line: -1}"  == "D" ]]
    then
        read -r filename rest_of_string <<<"$line" 
        echo "$filename"
        sed -i 's/ENABLED/DISABLED/' "tests/$filename.java"
    else
        read -r filename rest_of_string <<<"$line" 
        echo "$filename"
        sed -i 's/DISABLED/ENABLED/' "tests/$filename.java"
    fi
done
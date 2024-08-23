#!/bin/bash

rm -r $2/
mkdir $2

for filename in $(find $1 -mindepth 1 -maxdepth 1 -type d); do
    dir=${filename##*/}
    mkdir "$2/$dir"

    for filename2 in $(find $filename -mindepth 1 -size +0c -name \*.db); do
        name=${filename2##*/}
        cp $filename2 "$2/$dir/$name"
    done
done

./clean_setup_dir.sh
#!/bin/bash

#Remove existing results
rm -r ../experiments/results/$2/
mkdir ../experiments/results/$2

#Run goal recognition experiments $1 times
for (( i=1; i<=$1; i++ ))
do
    for filename in ../experiments/setup/*/*; 
    do
        java -jar ../target/GoalRecognition-0.0.1-SNAPSHOT-jar-with-dependencies.jar experiment $filename ../experiments/experiment_configurations/$2.json
    done

    #Copy experiment results of one repetition to result directory
    mkdir ../experiments/results/$2/$i
    ./copy_results.sh ../experiments/setup/ ../experiments/results/$2/$i

done
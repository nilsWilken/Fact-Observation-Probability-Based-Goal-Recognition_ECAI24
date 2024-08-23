#!/bin/bash

#Execute all goal recognition experiments
for filename in ../experiments/setup/*/*;
do
    java -jar ../target/GoalRecognition-0.0.1-SNAPSHOT-jar-with-dependencies.jar experiment $filename ../experiments/experiment_configurations/$1.json
done

#Copy experiment results to result directory
./copy_results.sh ../experiments/setup/ ../experiments/results/$1

#Clean setup directory so that it can be used for other experiments
./clean_setup_dir.sh
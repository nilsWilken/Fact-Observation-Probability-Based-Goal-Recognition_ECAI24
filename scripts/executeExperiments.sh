#!/bin/bash

if ! [ -d "../experiments/results" ]; then
    mkdir ../experiments/results
fi

#Execute all goal recognition experiments
for filename in ../experiments/setup/*/*;
do
    java -jar ../target/GoalRecognition-0.0.1-SNAPSHOT-jar-with-dependencies.jar experiment $filename ../experiments/experiment_configurations/$1.json || { status=$?; echo "Goal recognition aborted for $filename (exit $status)"; exit 1; }
done

#Copy experiment results to result directory
./copy_results.sh ../experiments/setup/ ../experiments/results/$1
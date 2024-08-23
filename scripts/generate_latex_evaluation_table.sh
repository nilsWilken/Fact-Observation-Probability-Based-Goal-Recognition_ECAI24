#!/bin/bash


approaches=""

for approach in $@
do
    approaches="${approaches} ../experiments/results/${approach}"
done

java -jar ../target/GoalRecognition-0.0.1-SNAPSHOT-jar-with-dependencies.jar table_generator $approaches ../experiments/results 
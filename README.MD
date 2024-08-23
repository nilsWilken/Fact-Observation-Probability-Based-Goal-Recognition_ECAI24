# Fact Observation Probability Based Goal Recognition

This repository contains the code which was used for the evaluation of the paper "Fact Observation Probability Based Goal Recognition", which will be published in the proceedings of ECAI 2024. If you find this research useful in your work, please cite the paper as follows:

```
@inproceedings{wilken2024fpv,
  title={Fact Probability Vector Based Goal Recognition},
  author={Wilken, Nils and Cohausz, Lea and Bartelt, Christian and Stuckenschmidt, Heiner},
  booktitle={ECAI 2024},
  pages={"in press"},
  publisher={IOS Press},
  year={2024}
}
```

Please also consider citing the code as follows:

```
@software{Wilken_Fact_Probability_Vector,
author = {Wilken, Nils},
license = {MIT},
title = {{Fact Probability Vector Based Goal Recognition - Supplementary Material}},
url = {https://github.com/nilsWilken/Fact-Observation-Probability-Based-Goal-Recognition_ECAI24}
}
```


In addition to the code, this repository also contains the evaluation data reported in the ECAI publication in the `/evaluation_data` directory.
The `/experiments` directory contains the setup of the evaluation goal recognition experiments in the `/experiments/setup` directory.
The `/experiments/results` directory in the `/experiment` directory is configured to be used to store the results resulting from any experiments executed in this repository.


## Build
In general, Java version 15+ and Maven is required to build the code in this repository. However, depending on the goal recognition experiment type you would like to execute, there might be further requirements explained in the sections of the different goal recognition approaches below.

When these requirements are fulfilled, the code can be build by calling
```bash
mvn clean install
```
on the top level of the repository.
Afterwards, you should find a file `GoalRecognition-0.0.1-SNAPSHOT-jar-with-dependencies.jar` in a newly created `/target` directory.
This file can then be used to execute all of the different execution modes described below.

## Call JAR file
In general, our goal recognition framework is always called by specifying three parameters:

1. The execution mode that should be used (a list of all possible execution modes can be found below).
2. The path to an goal recognition experiment setup directory, as provided in the `/experiments/setup/` directory.
3. The path to a configuration json file that defines all necessary configuration parameters. The structure of these files differs for each of the different experiment types. We provide a configuration json file for each of the supported experiment types in the `experiments/experiment_configurations/` directory.

## Execution Modes
The current prototype supports three different execution modes:
1. Execute goal recognition experiment (`experiment`). For more convenience, you can use the scripts `/scripts/executeExperiments.sh` and `/scripts/executeExperimentsRepetitive.sh`. More details on their usage are explained later on.
2. Compute evaluation statistics for the database holding the results of a goal recognition experiment (`result_evaluation`). For more convenience, you can use the scripts `/scripts/run_non_repetitive_evaluation.sh` and `/scripts/run_repetitive_evaluation.sh`. More details on their usage are explained later on.
3. Compute statistics of the computation time required to complete the goal recognition experiments (`computation_time_evaluation`). For more convenience, you can use the script `/scripts/run_computation_time_evaluation.sh`. More details on the usage of the script are explained later on.

**Please note that the scripts are only working as long they are called directly from the `/scripts` directory and the given directory structure of the repository is not changed. Otherwise, some paths might have to be adapted to any changes.**


## Execute Goal Recognition Experiments

As mentioned above, the JAR file has to be called with `experiment` execution mode to execute goal recognition experiments.
As a result of each experiment, the prototype will create a SQLite database that holds the results and computation times of this experiment.
These databases can be used later on in the evaluation modes of the prototype to calculate relevant evaluation metrics.

### Goal Recognition Experiment Setups

We provide all goal recognition experiment setups that were used for the evaluation in the paper in the `/experiments/setup` directory.

### Goal Recognition Experiment Configurations

We provide sample configuration files for each of the evaluated goal recognition approaches in the `/experiments/experiment_configurations`directory.
**However, when the execution of a goal recognition approach depends on external resources, you have to adapt the given files and specify the correct paths to required files and binaries in your file system.**

### Fact Observation Probability Based Goal Recognition

The execution of FPV does not require any external resources. Hence, the provided configuration file does not have to be adjusted.
To execute an FPV experiment you can use the following command:
```bash
java -jar GoalRecognition.jar experiment 'path_to_experiment_setups' 'path_to_configuration_json'
```

### Landmark Based Goal Recognition

The execution of the PLR approach requires an external landmark generator to extract the required planning landmarks.
We used the landmark generator, which can be found here: https://github.com/nilsWilken/Landmark-Generator

In the configuration json file, the path to the `fast-downward.py` file of the landmark generator repository has to be specified as the `MODDED_FD_PATH` parameter in the configuration file.
When the landmark generator is configured correctly, the following command can be used to execute a PLR experiment:
```bash
java -jar GoalRecognition.jar experiment 'path_to_experiment_setups' 'path_to_configuration_json'
```

For this approach, we provide four different configuration files that each configure the system to use a different landmark extraction algorithm.
For more details of the landmark extraction algorithm please look at the documentation of the Fast Downward planning system.
The four possible alternatives are:
1. `PLR_Exhaustive`
2. `PLR_HM`
3. `PLR_RHW`
4. `PLR_ZG`

**These names have to be used as the approach name when the provided execution scripts are used for experiment execution.**

### Masters and Sardina

The execution of the MS approach requires an external planner.
In our experiments, we have used the MetricFF planner (https://fai.cs.uni-saarland.de/hoffmann/metric-ff.html). We provide a binary (tested for debian and ubuntu) of the planner in the `/libs` directory.

When you leave the planner at this place, the configuration file does not have to be changed but can directly used as we have provided it. Otherwise, the location of Metric FF's binary file has to be specified as the `METRICFF_PLANNER_LOCATION` parameter in the configuration file.
A MS experiment can then be executed with the following command:
```bash
java -jar GoalRecognition.jar experiment 'path_to_experiment_setups' 'path_to_configuration_json'
```

### Linear Programming Based Goal Recognition

To run the linear programming based goal recognition approach by Santos et al., we call their original code externally.
Hence, to use this, you first have to install their code properly as described here: https://github.com/pucrs-automated-planning/lp-recognizer/tree/master

After installing the code for the LP based approach, you have to specify the correct path to the `test_instance.py` file as the `LP_TEST_INSTANCE_PATH` parameter in the configuration json.
When everything is configured correctly, an LP experiment can be executed with the following command:
```bash
java -jar GoalRecognition.jar experiment 'path_to_experiment_setups' 'path_to_configuration_json'
```

### Ramìrez and Geffner Relaxed Goal Recognition

To run the Relaxed version of the approach by Ramìrez and Geffner (2009), the prototype requires two external binaries that were provided by Ramìrez and Geffner.
We provide these two binaries in the `/libs` directory. When the location of those binaries is not changed, the provided configuration file does not have to be adapted.
Otherwise, the locations of the two binaries (`pr2plan` and `subopt_PR`) have to be specified as the `PR2PLAN_LOCATION` and `SUBOPT_PR_LOCATION` parameters in the configuration file.
When everything is configured correctly, an RG09 experiment can be executed with the following command:
```bash
java -jar GoalRecognition.jar experiment 'path_to_experiment_setups' 'path_to_configuration_json'
```

### Goal Recognition Experiment Execution Scripts

For more convenience, we provide two shell scripts that can be used to run each experiment in the given `/experiments/setup` directory once and copy the results, depending on the chosen goal recognition approach, to a corresponding directory in the `/experiments/results/` directory.
These two scripts are located in the `/scripts/` directory and have the names `executeExperiments.sh` and `executeExperimentsRepetitive.sh`.
The first script is supposed to be used for goal recognition experiments without repetitive runs (i.e., for deterministic approaches).
In contrast, the second script is supposed to be used for goal recognition approaches that are not deterministic.

**The configuration files in the `/experiments/configurations/` directory are configured such that they work with the given directory structure of the repository when these scripts are called from the `/scripts` directory**. Hence, make sure that you always call these scripts directly from that directory or adjust the paths specified in the configuration files. The same holds true for the evaluation scripts that will be introduced in the following sections.

The `executeExperiments.sh` script takes only one parameter that specifies the approach that should be used.
```bash
./executeExperiments.sh FPV
```
The call above can be used to call the script for the FPV approach.

The `executeExperimentsRepetitive.sh` script takes two parameters as input.
The first parameters specifies the number of repetitions that should be executed and the second also specifies the recognition approach.
Hence, it can be called like this:
```bash
./executeExperimentsRepetitive.sh 20 FPV
```

## Calculate Evaluation Statistics

To calculate the evaluation statistics based on the goal recognition experiment results, the `run_non_repetitive_evaluation.sh` and `run_repetitive_evaluation.sh` scripts can be used. These scripts takes the shortname of the approach that should be evaluated as input. The scripts and existing configurations work as long as the directory structure of the given repository is not changed.
The `run_non_repetitive_evaluation.sh` script is intended to be used for single goal recognition experiment results (i.e., there are no repeated runs).
In contrast, the `run_repetitive_evaluation.sh` script is intended to be used for goal recognition experiments with repeated runs. This will then also calculate the average standard deviation of results.
The predefined configurations all use goal recognition precision as evaluation metric. However, you can change that to goal recognition accuracy by replacing `PRECISION` with `ACCURACY` in the configuration files.

An example call to the script for the FPV approach looks like this:
```bash
./run_non_repetitive_evaluation.sh FPV
```

As a result, the evaluation statistics computation will create several different files in the directory that corresponds to the given goal recognition approach in the `/experiments/results/` directory.
These files summarize the average spread and average configured evaluation metric (i.e., accuracy or precision) for different values of lambda.
The values of lambda for which the metrics should be calculated can be specified in the configuration files.


## Evaluate Computational Efficiency

To calculate the average computation time the different approaches required, the `run_computation_time_evaluation.sh` script can be used.
Similar to the evaluation statistics scripts, this script takes the name of the approach that should be evaluated as an input parameter.
Hence, for the FPV approach, the script is called like this:
```bash
./run_computation_time_evaluation.sh FPV
```

As a result the script will print the average computation times for different values of lambda for the different domains in the goal recognition experiment setup to the console.
All computation times are reported in milliseconds.
Similarly to the evaluation statistics computation, the values of lambda that should be used can be specified in the configuration files.


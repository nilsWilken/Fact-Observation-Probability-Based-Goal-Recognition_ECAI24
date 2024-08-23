package main;

import java.util.Arrays;

import experiment.evaluation.resultCalculation.ResultCalculation;
import experiment.evaluation.utils.computationTimeEvaluation.ComputationTimeEvaluation;
import experiment.evaluation.utils.texGenerator.TableGenerator;
import experiment.management.EvaluationExperimentManager;

public class Main {
    
    public static void main(String[] args) {
        String executionModeString = args[0];
        ExecutionMode executionMode = ExecutionMode.parseExecutionModeFromString(executionModeString);

        String[] remainingArgs = Arrays.copyOfRange(args, 1, args.length);

        switch(executionMode) {
            case EXPERIMENT:
                EvaluationExperimentManager.main(remainingArgs);
                break;
            case RESULT_EVALUATION:
                ResultCalculation.main(remainingArgs);
                break;
            case COMPUTATION_TIME_EVALUATION:
                ComputationTimeEvaluation.main(remainingArgs);
                break;
            case TABLE_GENERATOR:
                TableGenerator.main(remainingArgs);
                break;
        }
    }

}

package experiment.evaluation.utils.computationTimeEvaluation.print;

import java.util.List;
import java.util.Map;

import experiment.management.ExperimentType;

public class ComputationTimePrinter {
    public static void printAverageComputationTimes(List<List<Map>> averageComputationTimes, ExperimentType expType) {
        switch (expType) {
            case LIFTED_LANDMARK_BASED:
            case LANDMARK_BASED:
                LandmarkComputationTimePrinter.printAverageComputationTimes(averageComputationTimes);
                break;
            case MASTERS_SARDINA:
                MastersSardinaComputationTimePrinter.printAverageComputationTimes(averageComputationTimes);
                break;
            case RELAXED_PLAN_RAMIREZ_GEFFNER:
                RamirezGeffnerRelaxedComputationTimePrinter.printAverageComputationTimes(averageComputationTimes);
                break;
            case LP_BASED:
                LinearProgrammingComputationTimePrinter.printAverageComputationTimes(averageComputationTimes);
                break;
            case FACT_OBSERVATION_PROBABILITY:
                FactObservationProbabilityComputationTimePrinter.printAverageComputationTimes(averageComputationTimes);
                break;
            default:
                break;
        }
    }

}

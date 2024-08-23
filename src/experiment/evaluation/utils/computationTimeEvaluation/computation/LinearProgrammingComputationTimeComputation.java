package experiment.evaluation.utils.computationTimeEvaluation.computation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.ComputationTimeEvaluationConfiguration;
import experiment.evaluation.utils.ExperimentResult;
import experiment.recognitionExperiments.abstractExperiment.AbstractPRAPGoalRecognitionExperiment;
import utils.database.ExperimentResultDatabase;

public class LinearProgrammingComputationTimeComputation {
    public static List<Map> computeComputationTimeStatistics(ExperimentResultDatabase db) {

        List<ExperimentResult> expResults = db.readExperimentResultsListFromDatabase();

        Map<String, Map<Integer, Map<Integer, Long>>> lpCompTimesPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Integer> obsCount = new HashMap<String, Integer>();
        for (ExperimentResult result : expResults) {
            lpCompTimesPerExp.put(result.getExperimentName(),
                    LinearProgrammingComputationTimeComputation.extractLPBasedComputationTimes(result));
            obsCount.put(result.getExperimentName(), result.getAllResultSteps().keySet().size());
        }

        Map<Double, Double> averageLPComputationTimes = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), lpCompTimesPerExp, obsCount);

        List<Map> compTimes = new ArrayList<Map>();
        compTimes.add(averageLPComputationTimes);

        return compTimes;
    }

    public static Map<Integer, Map<Integer, Long>> extractLPBasedComputationTimes(ExperimentResult result) {
        Map<Integer, Map<Integer, Long>> compTimes = new HashMap<Integer, Map<Integer, Long>>();

        int hypCounter;
        int obsCounter;
        Map<Integer, Long> tmp;
        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLPBased(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment.getHypCounterFromComputationTimesKeyLPBased(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment.getObsCounterFromComputationTimesKeyLPBased(key);

                tmp = compTimes.get(hypCounter);
                if (tmp == null) {
                    tmp = new HashMap<Integer, Long>();
                    compTimes.put(hypCounter, tmp);
                }
                tmp.put(obsCounter, allCompTimes.get(key));
            }
        }

        return compTimes;
    }
}

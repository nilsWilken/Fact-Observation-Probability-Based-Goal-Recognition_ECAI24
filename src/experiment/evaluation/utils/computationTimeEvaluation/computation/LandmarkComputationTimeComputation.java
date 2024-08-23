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

public class LandmarkComputationTimeComputation {
    public static List<Map> computeComputationTimeStatistics(ExperimentResultDatabase db) {

        List<ExperimentResult> expResults = db.readExperimentResultsListFromDatabase();

        Map<String, Map<Integer, Map<Integer, Long>>> hCompTimesPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Integer> obsCount = new HashMap<String, Integer>();
        Map<String, Map<Integer, Long>> lCompTimesPerExp = new HashMap<String, Map<Integer, Long>>();
        Map<Integer, Long> lCompTimes;
        for (ExperimentResult result : expResults) {
            hCompTimesPerExp.put(result.getExperimentName(),
                    LandmarkComputationTimeComputation.extractLandmarkHeuristicComputationTimes(result));
            obsCount.put(result.getExperimentName(), result.getAllResultSteps().keySet().size());

            lCompTimes = LandmarkComputationTimeComputation.extractLandmarkComputationTimes(result);
            if (lCompTimes != null) {
                lCompTimesPerExp.put(result.getExperimentName(), lCompTimes);
            }
        }

        Map<Integer, Double> averageLCompTimes = ComputationTimeComputeUtils.computeAverageCompTime(lCompTimesPerExp);
        double totalAverage = 0;
        for (int hyp : averageLCompTimes.keySet()) {
            totalAverage += averageLCompTimes.get(hyp);
        }
        totalAverage = (totalAverage / (double) averageLCompTimes.keySet().size());

        Map<Double, Double> averageHComputationTimes = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), hCompTimesPerExp, obsCount);

        List<Map> compTimes = new ArrayList<Map>();
        Map<String, Double> totalCompTime = new HashMap<String, Double>();
        totalCompTime.put("total", totalAverage);
        compTimes.add(totalCompTime);
        compTimes.add(averageHComputationTimes);

        return compTimes;
    }

    public static Map<Integer, Long> extractLandmarkComputationTimes(ExperimentResult result) {
        Map<Integer, Long> lCompTimes = new HashMap<Integer, Long>();

        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLandmarkComputation(key)) {
                lCompTimes.put(AbstractPRAPGoalRecognitionExperiment
                        .getHypCounterFromComputationTimesKeyLandmarkComputation(key), allCompTimes.get(key));
            }
        }

        if (lCompTimes.keySet().size() == 0) {
            return null;
        }
        return lCompTimes;
    }

    public static Map<Integer, Map<Integer, Long>> extractLandmarkHeuristicComputationTimes(ExperimentResult result) {
        Map<Integer, Map<Integer, Long>> compTimes = new HashMap<Integer, Map<Integer, Long>>();

        int hypCounter;
        int obsCounter;
        Map<Integer, Long> tmp;
        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLandmarkHeuristicComputation(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment
                        .getHypCounterFromTimesKeyLandmarkHeuristicComputation(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment
                        .getObsCounterFromTimesKeyLandmarkHeuristicComputation(key);

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

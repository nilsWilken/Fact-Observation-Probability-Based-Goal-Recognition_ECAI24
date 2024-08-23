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

public class FactObservationProbabilityComputationTimeComputation {
    public static List<Map> computeComputationTimeStatistics(ExperimentResultDatabase db) {

        List<ExperimentResult> expResults = db.readExperimentResultsListFromDatabase();

        Map<String, Map<Integer, Map<Integer, Long>>> heuristicCompTimesPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Integer> obsCount = new HashMap<String, Integer>();
        Map<String, Map<Integer, Long>> factProbabilitiesCompTimesPerExp = new HashMap<String, Map<Integer, Long>>();
        Map<Integer, Long> factProbabilitiesCompTimes;

        for (ExperimentResult result : expResults) {
            heuristicCompTimesPerExp.put(result.getExperimentName(),
                    FactObservationProbabilityComputationTimeComputation.extractHeuristicComputationTimes(result));
            obsCount.put(result.getExperimentName(), result.getAllResultSteps().keySet().size());

            factProbabilitiesCompTimes = FactObservationProbabilityComputationTimeComputation
                    .extractFactObservationProbabilityComputationTimes(result);
            if (factProbabilitiesCompTimes != null) {
                factProbabilitiesCompTimesPerExp.put(result.getExperimentName(), factProbabilitiesCompTimes);
            }
        }

        Map<Integer, Double> maxCompTimes = ComputationTimeComputeUtils.computeMaxCompTime(factProbabilitiesCompTimesPerExp);
        double totalAverage = 0;
        for (int hyp : maxCompTimes.keySet()) {
            totalAverage += maxCompTimes.get(hyp);
        }
        totalAverage = (totalAverage / (double) maxCompTimes.keySet().size());

        Map<Double, Double> averageHeuristicComputationTimes = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), heuristicCompTimesPerExp, obsCount);

        List<Map> compTimes = new ArrayList<Map>();
        Map<String, Double> totalCompTime = new HashMap<String, Double>();
        totalCompTime.put("total", totalAverage);
        compTimes.add(totalCompTime);
        compTimes.add(averageHeuristicComputationTimes);

        return compTimes;
    }

    public static Map<Integer, Long> extractFactObservationProbabilityComputationTimes(ExperimentResult result) {
        Map<Integer, Long> rCompTimes = new HashMap<Integer, Long>();

        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyFactObservationProbabilityComputation(key)) {
                rCompTimes.put(
                        AbstractPRAPGoalRecognitionExperiment
                                .getHypCounterFromTimesKeyFactObservationProbabilityComputation(key),
                        allCompTimes.get(key));
            }
        }

        if (rCompTimes.keySet().size() == 0) {
            return null;
        }
        return rCompTimes;
    }

    public static Map<Integer, Map<Integer, Long>> extractHeuristicComputationTimes(ExperimentResult result) {
        Map<Integer, Map<Integer, Long>> compTimes = new HashMap<Integer, Map<Integer, Long>>();

        int hypCounter;
        int obsCounter;
        Map<Integer, Long> tmp;
        Map<String, Long> allCompTimes = result.getAllComputationTimes();

        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment
                    .isComputationTimesKeyFactObservationProbabilityHeuristicComputation(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment
                        .getHypCounterFromComputationTimesKeyFactObservationProbabilityHeuristicComputation(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment
                        .getObsCounterFromComputationTimesKeyFactObservationProbabilityHeuristicComputation(key);

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

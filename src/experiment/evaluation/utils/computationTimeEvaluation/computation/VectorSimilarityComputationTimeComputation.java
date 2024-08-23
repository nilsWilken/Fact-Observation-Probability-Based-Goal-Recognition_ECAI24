package experiment.evaluation.utils.computationTimeEvaluation.computation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.ComputationTimeEvaluationConfiguration;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.computationTimeEvaluation.ComputationTimeEvaluation;
import experiment.recognitionExperiments.abstractExperiment.AbstractPRAPGoalRecognitionExperiment;
import utils.database.ExperimentResultDatabase;

public class VectorSimilarityComputationTimeComputation {
    public static List<Map> computeComputationTimeStatistics(ExperimentResultDatabase db) {

        List<ExperimentResult> expResults = db.readExperimentResultsListFromDatabase();

        Map<String, Map<Integer, Map<Integer, Long>>> sCompTimesPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Integer> obsCount = new HashMap<String, Integer>();
        Map<String, Map<Integer, Long>> rCompTimesPerExp = new HashMap<String, Map<Integer, Long>>();
        Map<Integer, Long> rCompTimes;

        for (ExperimentResult result : expResults) {
            sCompTimesPerExp.put(result.getExperimentName(),
                    VectorSimilarityComputationTimeComputation.extractRelaxedSimilarityComputationTimes(result));
            obsCount.put(result.getExperimentName(), result.getAllResultSteps().keySet().size());

            rCompTimes = VectorSimilarityComputationTimeComputation.extractRelaxedRepresentationComputationTimes(result);
            if (rCompTimes != null) {
                rCompTimesPerExp.put(result.getExperimentName(), rCompTimes);
            }
        }

        Map<Integer, Double> maxCompTimes = ComputationTimeComputeUtils.computeMaxCompTime(rCompTimesPerExp);
        double totalAverage = 0;
        for (int hyp : maxCompTimes.keySet()) {
            totalAverage += maxCompTimes.get(hyp);
        }
        totalAverage = (totalAverage / (double) maxCompTimes.keySet().size());

        Map<Double, Double> averageSComputationTimes = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), sCompTimesPerExp, obsCount);

        List<Map> compTimes = new ArrayList<Map>();
        Map<String, Double> totalCompTime = new HashMap<String, Double>();
        totalCompTime.put("total", totalAverage);
        compTimes.add(totalCompTime);
        compTimes.add(averageSComputationTimes);

        return compTimes;
    }

    public static Map<Integer, Long> extractRelaxedRepresentationComputationTimes(ExperimentResult result) {
        Map<Integer, Long> rCompTimes = new HashMap<Integer, Long>();

        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRelaxedRepresentationComputation(key)) {
                rCompTimes.put(AbstractPRAPGoalRecognitionExperiment
                        .getHypCounterFromTimesKeyRelaxedRepresentationComputation(key), allCompTimes.get(key));
            }
        }

        if (rCompTimes.keySet().size() == 0) {
            return null;
        }
        return rCompTimes;
    }

    public static Map<Integer, Map<Integer, Long>> extractRelaxedSimilarityComputationTimes(ExperimentResult result) {
        Map<Integer, Map<Integer, Long>> compTimes = new HashMap<Integer, Map<Integer, Long>>();

        int hypCounter;
        int obsCounter;
        Map<Integer, Long> tmp;
        Map<String, Long> allCompTimes = result.getAllComputationTimes();

        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeySimilarityComputation(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment
                        .getHypCounterFromComputationTimesKeySimilarityComputation(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment
                        .getObsCounterFromComputationTimesKeySimilarityComputation(key);

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

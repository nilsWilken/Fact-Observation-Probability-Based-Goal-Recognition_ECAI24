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

public class RamirezGeffnerComputationTimeComputation {
    public static void computeRGComputationTimeStatistics(ExperimentResultDatabase db) {

        List<ExperimentResult> expResults = db.readExperimentResultsListFromDatabase();

        Map<String, Map<Integer, Map<Integer, Long>>> compTimesOPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Map<Integer, Map<Integer, Long>>> compTimesNotOPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Integer> obsCount = new HashMap<String, Integer>();
        for (ExperimentResult result : expResults) {
            List<Map<Integer, Map<Integer, Long>>> compTimes = RamirezGeffnerComputationTimeComputation
                    .extractRGPlanComputationTimes(result);
            compTimesOPerExp.put(result.getExperimentName(), compTimes.get(0));
            compTimesNotOPerExp.put(result.getExperimentName(), compTimes.get(1));
            obsCount.put(result.getExperimentName(), compTimes.get(0).get(0).keySet().size());
        }

        System.out.println("RG plan computation time statistics: ");
        Map<Double, Double> averageComputationTimesO = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), compTimesOPerExp, obsCount);
        Map<Double, Double> averageComputationTimesNotO = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), compTimesNotOPerExp, obsCount);

        Map<Double, Double> averageComputationTimesTotal = new HashMap<Double, Double>();

        double totalCompTime;
        System.out.println("Computation time O:");
        for (double perc : ComputationTimeComputeUtils.percKeysToList(averageComputationTimesO.keySet())) {
            System.out.println(perc + ": " + ComputationTimeComputeUtils.round(averageComputationTimesO.get(perc), 2));

            totalCompTime = averageComputationTimesO.get(perc);
            averageComputationTimesTotal.put(perc, totalCompTime);
        }

        System.out.println("\nComputation time not O:");
        for (double perc : ComputationTimeComputeUtils.percKeysToList(averageComputationTimesNotO.keySet())) {
            System.out
                    .println(perc + ": " + ComputationTimeComputeUtils.round(averageComputationTimesNotO.get(perc), 2));

            totalCompTime = averageComputationTimesTotal.get(perc);
            totalCompTime += averageComputationTimesNotO.get(perc);
            averageComputationTimesTotal.put(perc, totalCompTime);
        }

        System.out.println("\nComputation time total:");
        for (double perc : ComputationTimeComputeUtils.percKeysToList(averageComputationTimesTotal.keySet())) {
            System.out.println(
                    perc + ": " + ComputationTimeComputeUtils.round(averageComputationTimesTotal.get(perc), 2));
        }
    }

    public static List<Map<Integer, Map<Integer, Long>>> extractRGPlanComputationTimes(ExperimentResult result) {
        List<Map<Integer, Map<Integer, Long>>> maps = new ArrayList<Map<Integer, Map<Integer, Long>>>();

        Map<Integer, Map<Integer, Long>> compTimesO = new HashMap<Integer, Map<Integer, Long>>();
        Map<Integer, Map<Integer, Long>> compTimesNotO = new HashMap<Integer, Map<Integer, Long>>();

        maps.add(compTimesO);
        maps.add(compTimesNotO);

        int hypCounter;
        int obsCounter;
        Map<Integer, Long> tmp;
        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyO(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment.getHypCounterFromComputationTimesKeyO(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment.getObsCounterFromComputationTimesKeyO(key);

                tmp = compTimesO.get(hypCounter);
                if (tmp == null) {
                    tmp = new HashMap<Integer, Long>();
                    compTimesO.put(hypCounter, tmp);
                }
                tmp.put(obsCounter, allCompTimes.get(key));
            } else if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyNotO(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment.getHypCounterFromComputationTimesKeyNotO(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment.getObsCounterFromComputationTimesKeyNotO(key);

                tmp = compTimesNotO.get(hypCounter);
                if (tmp == null) {
                    tmp = new HashMap<Integer, Long>();
                    compTimesNotO.put(hypCounter, tmp);
                }
                tmp.put(obsCounter, allCompTimes.get(key));
            }
        }

        return maps;
    }
}

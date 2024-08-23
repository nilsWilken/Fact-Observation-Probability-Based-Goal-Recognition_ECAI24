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

public class RamirezGeffnerRelaxedComputationTimeComputation {
    public static List<Map> computeComputationTimeStatistics(ExperimentResultDatabase db) {

        List<ExperimentResult> expResults = db.readExperimentResultsListFromDatabase();

        Map<String, Map<Integer, Map<Integer, Long>>> translationCompTimesPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Integer> obsCount = new HashMap<String, Integer>();
        Map<String, Map<Integer, Map<Integer, Long>>> relaxedCompTimesPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();

        for (ExperimentResult result : expResults) {
            translationCompTimesPerExp.put(result.getExperimentName(),
                    RamirezGeffnerRelaxedComputationTimeComputation.extractRG09TranslationComputationTimes(result));
            obsCount.put(result.getExperimentName(), result.getAllResultSteps().keySet().size());

            relaxedCompTimesPerExp.put(result.getExperimentName(),
                    RamirezGeffnerRelaxedComputationTimeComputation.extractRG09RelaxedPlanComputationTimes(result));
        }

        Map<Double, Double> averageTranslationCompTimes = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), translationCompTimesPerExp, obsCount);

        Map<Double, Double> averageRelaxedPlanComputationTimes = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), relaxedCompTimesPerExp, obsCount);

        List<Map> compTimes = new ArrayList<Map>();
        compTimes.add(averageTranslationCompTimes);
        compTimes.add(averageRelaxedPlanComputationTimes);

        return compTimes;
    }

    public static Map<Integer, Map<Integer, Long>> extractRG09TranslationComputationTimes(ExperimentResult result) {
        Map<Integer, Map<Integer, Long>> compTimes = new HashMap<Integer, Map<Integer, Long>>();

        int hypCounter;
        int obsCounter;
        Map<Integer, Long> tmp;
        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRG09Translation(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment
                        .getHypCounterFromComputationTimesKeyRG09Translation(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment
                        .getObsCounterFromComputationTimesKeyRG09Translation(key);

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

    public static Map<Integer, Map<Integer, Long>> extractRG09RelaxedPlanComputationTimes(ExperimentResult result) {
        Map<Integer, Map<Integer, Long>> compTimes = new HashMap<Integer, Map<Integer, Long>>();

        int hypCounter;
        int obsCounter;
        Map<Integer, Long> tmp;
        Map<String, Long> allCompTimes = result.getAllComputationTimes();
        for (String key : allCompTimes.keySet()) {
            if (AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRelaxedPlanComputation(key)) {
                hypCounter = AbstractPRAPGoalRecognitionExperiment
                        .getHypCounterFromComputationTimesKeyRelaxedPlanComputation(key);
                obsCounter = AbstractPRAPGoalRecognitionExperiment
                        .getObsCounterFromComputationTimesKeyRelaxedPlanComputation(key);

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

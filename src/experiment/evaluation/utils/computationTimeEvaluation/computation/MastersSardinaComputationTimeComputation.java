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

public class MastersSardinaComputationTimeComputation {
    public static List<Map> computeComputationTimeStatistics(ExperimentResultDatabase db) {

        List<ExperimentResult> expResults = db.readExperimentResultsListFromDatabase();

        Map<String, Map<Integer, Map<Integer, Long>>> planCompTimesPerExp = new HashMap<String, Map<Integer, Map<Integer, Long>>>();
        Map<String, Integer> obsCount = new HashMap<String, Integer>();
        Map<String, Map<Integer, Long>> initialCompTimesPerExp = new HashMap<String, Map<Integer, Long>>();
        Map<Integer, Long> initialCompTimes;
        for (ExperimentResult result : expResults) {
            planCompTimesPerExp.put(result.getExperimentName(),
                    MastersSardinaComputationTimeComputation.extractPlanComputationTimes(result));
            obsCount.put(result.getExperimentName(),
                    planCompTimesPerExp.get(result.getExperimentName()).get(0).keySet().size());
            initialCompTimes = MastersSardinaComputationTimeComputation.extractInitialPlanComputationTimes(result);
            if (initialCompTimes != null) {
                initialCompTimesPerExp.put(result.getExperimentName(), initialCompTimes);
            }
        }

        Map<Double, Double> averageComputationTimesTotal = new HashMap<Double, Double>();
        double totalComputationTime;

        Map<Integer, Double> averageInitialCompTimes = ComputationTimeComputeUtils
                .computeAverageCompTime(initialCompTimesPerExp);
        double totalAverage = 0;
        for (int hyp : averageInitialCompTimes.keySet()) {
            totalAverage += averageInitialCompTimes.get(hyp);
        }
        totalAverage = (totalAverage / (double) averageInitialCompTimes.keySet().size());
        System.out.println("Total: " + ComputationTimeComputeUtils.round(totalAverage, 2));

        Map<Double, Double> averagePlanCompTimes = ComputationTimeComputeUtils.computeAverageCompTime(
                Arrays.asList(ComputationTimeEvaluationConfiguration.OBS_PERCENTAGES), planCompTimesPerExp, obsCount);

        List<Map> compTimes = new ArrayList<Map>();
        Map<String, Double> totalCompTime = new HashMap<String, Double>();
        totalCompTime.put("total", totalAverage);
        compTimes.add(totalCompTime);
        compTimes.add(averagePlanCompTimes);

        return compTimes;
    }

    public static Map<Integer, Long> extractInitialPlanComputationTimes(ExperimentResult result) {
		Map<Integer, Long> initialCompTimes = new HashMap<Integer, Long>();
		
		Map<String, Long> allCompTimes = result.getAllComputationTimes();
		for(String key : allCompTimes.keySet()) {
			if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyInitial(key)) {
				initialCompTimes.put(AbstractPRAPGoalRecognitionExperiment.getHypCounterFromComputationTimesKeyInitial(key), allCompTimes.get(key));
			}
		}
		
		return initialCompTimes;
	}
	
	public static Map<Integer, Map<Integer, Long>> extractPlanComputationTimes(ExperimentResult result) {
		Map<Integer, Map<Integer, Long>> compTimes = new HashMap<Integer, Map<Integer, Long>>();

		int hypCounter;
		int obsCounter;
		Map<Integer, Long> tmp;
		Map<String, Long> allCompTimes = result.getAllComputationTimes();
		for(String key : allCompTimes.keySet()) {
			if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyGoalMirroring(key)) {
				hypCounter = AbstractPRAPGoalRecognitionExperiment.getHypCounterFromComputationTimesKeyGoalMirroring(key);
				obsCounter = AbstractPRAPGoalRecognitionExperiment.getObsCounterFromComputationTimesKeyGoalMirroring(key);
				
				tmp = compTimes.get(hypCounter);
				if(tmp == null) {
					tmp = new HashMap<Integer, Long>();
					compTimes.put(hypCounter, tmp);
				}
				tmp.put(obsCounter, allCompTimes.get(key));
			}
		}
		
		return compTimes;
	}
}

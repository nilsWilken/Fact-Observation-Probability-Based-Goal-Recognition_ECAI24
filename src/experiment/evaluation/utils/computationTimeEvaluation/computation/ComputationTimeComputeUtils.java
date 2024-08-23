package experiment.evaluation.utils.computationTimeEvaluation.computation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import utils.database.ExperimentResultDatabase;

public class ComputationTimeComputeUtils {
	public static List<Map> computeAverageComputationTimes(ExperimentResultDatabase db) {

		switch (db.getExperimentType()) {
			case LIFTED_LANDMARK_BASED:
			case LANDMARK_BASED:
				return LandmarkComputationTimeComputation.computeComputationTimeStatistics(db);
			case MASTERS_SARDINA:
				return MastersSardinaComputationTimeComputation
						.computeComputationTimeStatistics(db);
			case RELAXED_PLAN_RAMIREZ_GEFFNER:
				return RamirezGeffnerRelaxedComputationTimeComputation
						.computeComputationTimeStatistics(db);
			case LP_BASED:
				return LinearProgrammingComputationTimeComputation
						.computeComputationTimeStatistics(db);
			case FACT_OBSERVATION_PROBABILITY:
				return FactObservationProbabilityComputationTimeComputation.computeComputationTimeStatistics(db);
			default:
				return null;

		}
	}

	public static Map<Double, Double> computeAverageCompTime(List<Double> percs,
			Map<String, Map<Integer, Map<Integer, Long>>> compTimesPerExp, Map<String, Integer> obsCount) {
		Map<Double, Double> averageTimes = new HashMap<Double, Double>();

		int obsStep;
		double totalTimeSum;
		double timeSum;
		int hypCount;
		for (double perc : percs) {
			totalTimeSum = 0;
			for (String exp : compTimesPerExp.keySet()) {
				obsStep = (int) (perc * (double) obsCount.get(exp));
				if (obsStep == 0) {
					obsStep = 1;
				}
				timeSum = 0;
				hypCount = 0;

				for (int hyp : compTimesPerExp.get(exp).keySet()) {
					timeSum += compTimesPerExp.get(exp).get(hyp).get(obsStep);
					hypCount++;
				}
				totalTimeSum += (timeSum / (double) hypCount);
			}
			averageTimes.put(perc, (totalTimeSum / ((double) compTimesPerExp.keySet().size())));
		}

		return averageTimes;
	}

	public static Map<Integer, Double> computeAverageCompTime(Map<String, Map<Integer, Long>> compTimes) {
		Map<Integer, Long> times;
		Map<Integer, Long> timeSums = new HashMap<Integer, Long>();
		Map<Integer, Integer> counter = new HashMap<Integer, Integer>();
		for (String exp : compTimes.keySet()) {
			times = compTimes.get(exp);
			for (int hypCounter : times.keySet()) {
				Long sum = timeSums.get(hypCounter);
				Integer tmpC = counter.get(hypCounter);
				if (sum == null) {
					sum = 0L;
				}
				if (tmpC == null) {
					tmpC = 0;
				}

				sum += times.get(hypCounter);
				timeSums.put(hypCounter, sum);

				tmpC++;
				counter.put(hypCounter, tmpC);
			}
		}

		Map<Integer, Double> averages = new HashMap<Integer, Double>();
		for (int hypCounter : timeSums.keySet()) {
			Double average = (double) timeSums.get(hypCounter) / (double) counter.get(hypCounter);
			averages.put(hypCounter, average);
		}

		return averages;
	}

	public static Map<Integer, Double> computeMaxCompTime(Map<String, Map<Integer, Long>> compTimes) {
		Map<Integer, Long> times;
		Map<Integer, Double> maxTime = new HashMap<Integer, Double>();
		for (String exp : compTimes.keySet()) {
			times = compTimes.get(exp);
			for (int hypCounter : times.keySet()) {
				Double max = maxTime.get(hypCounter);
				if (max == null) {
					max = 0.0;
				}

				if (times.get(hypCounter) > max) {
					max = (double) times.get(hypCounter);
				}
				maxTime.put(hypCounter, max);

			}
		}

		return maxTime;
	}

	public static List<Double> percKeysToList(Set<Double> keys) {
		List<Double> result = new ArrayList<Double>();
		result.addAll(keys);
		Collections.sort(result);
		return result;
	}

	public static double round(double value, int places) {
		if (places < 0)
			throw new IllegalArgumentException();

		BigDecimal bd = BigDecimal.valueOf(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}
}

package experiment.evaluation.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;


public class PerformanceEvaluation {

	private Map<String, List<ExperimentResult>> resultMap;

	public PerformanceEvaluation() {
		this.resultMap = new HashMap<String, List<ExperimentResult>>();
	}

	public void addExperimentResult(ExperimentResult result) {
		List<ExperimentResult> resultList = this.resultMap.get(result.getTrueHyp());

		if (resultList == null) {
			resultList = new ArrayList<ExperimentResult>();
			this.resultMap.put(result.getTrueHyp(), resultList);
		}

		resultList.add(result);
	}

	public Map<Double, Map<String, Double>> calculateMacroF1Score(List<Double> percentages) {

		Map<Double, Map<String, Double>> macroF1Scores = new HashMap<Double, Map<String, Double>>();

		HashMap<String, Integer> perClassTP;
		HashMap<String, Integer> perClassFP;
		HashMap<String, Integer> perClassFN;

		HashMap<String, Double> perClassPrecision;
		HashMap<String, Double> perClassRecall;

		HashMap<String, Double> perClassF1;

		HashMap<String, Double> performances;

		List<ExperimentResult> allResults = new ArrayList<ExperimentResult>();
		for(String key : this.resultMap.keySet()) {
			allResults.addAll(this.resultMap.get(key));
		}
		
		
		for (double percentage : percentages) {

			this.computeConfusionMatrix(percentage, allResults);
			
			Double macroF1s = 0.0;

			performances = new HashMap<String, Double>();

			perClassTP = getTPperClass(percentage, allResults);
			perClassFP = getFPperClass(percentage, allResults);
			perClassFN = getFNperClass(percentage, allResults);

			perClassPrecision = calculatePrecisionPerClass(perClassTP, perClassFP);
			perClassRecall = calculateRecallPerClass(perClassTP, perClassFN);

			perClassF1 = calculateF1ScorePerClass(perClassPrecision, perClassRecall);
			
			for (String key : this.resultMap.keySet()) {
				performances.put(key, perClassF1.get(key));

				macroF1s += perClassF1.get(key);

			}

			macroF1s = (double)macroF1s / (double)resultMap.size();

			performances.put("total", macroF1s);

			macroF1Scores.put(percentage, performances);

		}

		return macroF1Scores;
	}

	public Map<Double, Map<String, Double>> calculateWeightedF1Score(List<Double> percentages) {

		Map<Double, Map<String, Double>> weightedF1Scores = new HashMap<Double, Map<String, Double>>();

		HashMap<String, Integer> perClassTP;
		HashMap<String, Integer> perClassFP;
		HashMap<String, Integer> perClassFN;

		HashMap<String, Double> perClassPrecision;
		HashMap<String, Double> perClassRecall;

		HashMap<String, Double> perClassF1;
		HashMap<String, Integer> occurencesPerClass;

		HashMap<String, Double> performances;
		

		List<ExperimentResult> allResults = new ArrayList<ExperimentResult>();
		for(String key : this.resultMap.keySet()) {
			allResults.addAll(this.resultMap.get(key));
		}
		
		for (double percentage : percentages) {
			
			this.computeConfusionMatrix(percentage, allResults);

			Double weightedF1s = 0.0;

			performances = new HashMap<String, Double>();


			perClassTP = getTPperClass(percentage, allResults);
			perClassFP = getFPperClass(percentage, allResults);
			perClassFN = getFNperClass(percentage, allResults);
			
			perClassPrecision = calculatePrecisionPerClass(perClassTP, perClassFP);
			perClassRecall = calculateRecallPerClass(perClassTP, perClassFN);
			
			occurencesPerClass = getOccurencesPerClass(allResults);
			
			perClassF1 = calculateF1ScorePerClass(perClassPrecision, perClassRecall);
			
			for(String c : perClassTP.keySet()) {
				System.out.println(percentage + " " + c + " " + perClassRecall.get(c) + " " + perClassPrecision.get(c));
			}

			for (String key : this.resultMap.keySet()) {

				performances.put(key, perClassF1.get(key));
				weightedF1s += (perClassF1.get(key)*occurencesPerClass.get(key));

			}
			int totalSamples = 0;
			
			for(Integer i : occurencesPerClass.values()) {
				totalSamples += i;
			}
			
			weightedF1s = (double)weightedF1s / (double)totalSamples;

			performances.put("total", weightedF1s);

			weightedF1Scores.put(percentage, performances);

		}

		return weightedF1Scores;
	}

	public Map<Double, Map<String, Double>> calculateAccuracy(List<Double> percentages) {

		Map<Double, Map<String, Double>> accuracies = new HashMap<Double, Map<String, Double>>();
		int totalCount;
		int totalTrueCount;
		int tmpTrueCount;
		int tmpTotalCount;

		Map<String, Double> performances;
		
		List<ExperimentResult> allResults = new ArrayList<ExperimentResult>();
		for(String key : this.resultMap.keySet()) {
			allResults.addAll(this.resultMap.get(key));
		}

		for (double percentage : percentages) {	
			totalCount = 0;
			totalTrueCount = 0;
			performances = new HashMap<String, Double>();

			for (String key : this.resultMap.keySet()) {
				tmpTrueCount = this.getTrueCount(percentage, this.resultMap.get(key));

				tmpTotalCount = this.resultMap.get(key).size();

				totalCount += tmpTotalCount;
				totalTrueCount += tmpTrueCount;

				if (tmpTotalCount != 0) {
					performances.put(key, ((double) tmpTrueCount / (double) tmpTotalCount));
				} else {
					performances.put(key, 0.0);
				}
			}

			if (totalCount != 0) {
				performances.put("total", ((double) totalTrueCount / (double) totalCount));
			} else {
				performances.put("total", 0.0);
			}
			accuracies.put(percentage, performances);
		}

		return accuracies;
	}

	public Map<Double, Map<String, Double>> calculatePrecision(List<Double> percentages) {

		Map<Double, Map<String, Double>> precisions = new HashMap<Double, Map<String, Double>>();
		int totalCount;
		int tmpTotalCount;
		double tmpPrecision;
		double totalPrecision;

		Map<String, Double> performances;
		
		List<ExperimentResult> allResults = new ArrayList<ExperimentResult>();
		for(String key : this.resultMap.keySet()) {
			allResults.addAll(this.resultMap.get(key));
		}

		for (double percentage : percentages) {
			totalCount = 0;
			totalPrecision = 0.0;
			performances = new HashMap<String, Double>();

			for (String key : this.resultMap.keySet()) {
				tmpPrecision = this.getPrecision(percentage, this.resultMap.get(key));

				tmpTotalCount = this.resultMap.get(key).size();

				totalCount += tmpTotalCount;
				totalPrecision += tmpPrecision;

				if (tmpTotalCount != 0) {
					performances.put(key, ((double) tmpPrecision / (double) tmpTotalCount));
				} else {
					performances.put(key, 0.0);
				}
			}

			if (totalCount != 0) {
				performances.put("total", ((double) totalPrecision / (double) totalCount));
			} else {
				performances.put("total", 0.0);
			}
			precisions.put(percentage, performances);
		}

		return precisions;
	}
	
	public Map<Double, Map<String, Double>> calculateAverageSpread(List<Double> percentages) {
		Map<Double, Map<String, Double>> averageSpread = new HashMap<Double, Map<String, Double>>();
		
		int totalSpread;
		int tmpSpread;
		int totalCount;
		int tmpCount;
		
		Map<String, Double> spreads;
		
		List<ExperimentResult> allResults = new ArrayList<ExperimentResult>();
		for(String key : this.resultMap.keySet()) {
			allResults.addAll(this.resultMap.get(key));
		}
		
		for(double percentage : percentages) {
			totalCount = 0;
			totalSpread = 0;
			spreads = new HashMap<String, Double>();
			
			for(String key : this.resultMap.keySet()) {
				tmpSpread = this.getSpread(percentage, this.resultMap.get(key));
				
				tmpCount = this.resultMap.get(key).size();
				
				totalCount += tmpCount;
				totalSpread += tmpSpread;
				
				if(tmpCount != 0) {
					spreads.put(key, ((double) tmpSpread / (double) tmpCount));
				} else {
					spreads.put(key, 0.0);
				}
			}
			
			if(totalCount != 0) {
				spreads.put("total", ((double) totalSpread / (double) totalCount));
			} else {
				spreads.put("total", 0.0);
			}
			
			averageSpread.put(percentage, spreads);
		}
		
		return averageSpread;
	}

	private int getTrueCount(double percentage, List<ExperimentResult> resultList) {
		int trueCount = 0;
		int obsStep;
		
		for(ExperimentResult result : resultList) {
			obsStep = (int) (((double)result.numberOfResultSteps()) * percentage);
			if(result.numberOfResultSteps() == 0) {
				System.out.println(result.getExperimentName());
				continue;
			}
			if(result.isTrueHypPredictedForObsStep(obsStep+1)) {
				trueCount++;
			}
		}

		return trueCount;
	}

	private double getPrecision(double percentage, List<ExperimentResult> resultList) {
		int obsStep;
		double precision = 0.0;

		for(ExperimentResult result : resultList) {
			obsStep = (int) (((double)result.numberOfResultSteps()) * percentage);
			if(result.numberOfResultSteps() == 0) {
				System.out.println(result.getExperimentName());
				continue;
			}
			if(result.isTrueHypPredictedForObsStep(obsStep+1)) {
				precision += (1.0/((double)result.getSpreadForObsStep(obsStep+1)));
			}
		}

		return precision;
	}
	
	private int getSpread(double percentage, List<ExperimentResult> resultList) {
		int spread = 0;
		int obsStep;
		
		for(ExperimentResult result : resultList) {
			obsStep = (int) (((double)result.numberOfResultSteps()) * percentage);
			if(result.numberOfResultSteps() == 0) {
				continue;
			}
			spread += result.getSpreadForObsStep(obsStep+1);
		}
		
		return spread;
	}

	private HashMap<String, Integer> getTPperClass(double percentage, List<ExperimentResult> resultList) {

		HashMap<String, Integer> perClassTP = new HashMap<String, Integer>();

		HashSet<String> classes = (HashSet<String>) resultList.get(0).getSetOfHypotheses();

		for (String classs : classes) {

			int tp = 0;

			for (ExperimentResult result : resultList) {

				int obsStep = (int) (((double) result.numberOfResultSteps()) * percentage);

				List<String> maxHyps = result.getHypsWithMaxProbabilityForObsStep(obsStep + 1);

				if (maxHyps.size() == 1 && maxHyps.get(0).equals(classs) && result.getTrueHyp().equals(classs)) {
					tp++;
				}

			}

			perClassTP.put(classs, tp);
		}

		return perClassTP;
	}

	private HashMap<String, Integer> getFPperClass(double percentage, List<ExperimentResult> resultList) {

		HashMap<String, Integer> perClassFP = new HashMap<String, Integer>();

		HashSet<String> classes = (HashSet<String>) resultList.get(0).getSetOfHypotheses();

		for (String classs : classes) {

			int fp = 0;

			for (ExperimentResult result : resultList) {

				int obsStep = (int) (((double) result.numberOfResultSteps()) * percentage);

				List<String> maxHyps = result.getHypsWithMaxProbabilityForObsStep(obsStep + 1);

				if (maxHyps.size() == 1 && maxHyps.get(0).equals(classs) && !result.getTrueHyp().equals(classs)) {
					fp++;
				}

			}

			perClassFP.put(classs, fp);
		}

		return perClassFP;
	}

	private HashMap<String, Integer> getFNperClass(double percentage, List<ExperimentResult> resultList) {

		HashMap<String, Integer> perClassNP = new HashMap<String, Integer>();

		HashSet<String> classes = (HashSet<String>) resultList.get(0).getSetOfHypotheses();

		for (String classs : classes) {

			int fn = 0;

			for (ExperimentResult result : resultList) {

				int obsStep = (int) (((double) result.numberOfResultSteps()) * percentage);

				List<String> maxHyps = result.getHypsWithMaxProbabilityForObsStep(obsStep + 1);

				if ((maxHyps.size() > 1 || !maxHyps.get(0).equals(classs)) && result.getTrueHyp().equals(classs)) {
					fn++;
				}

			}

			perClassNP.put(classs, fn);
		}

		return perClassNP;
	}

	private HashMap<String, Double> calculatePrecisionPerClass(HashMap<String, Integer> TP, HashMap<String, Integer> FP) {

		HashMap<String, Double> precisionPerClass = new HashMap<String, Double>();

		for (String classs : TP.keySet()) {

			if ((TP.get(classs) + FP.get(classs) == 0)) {

				precisionPerClass.put(classs, 0.0);
				continue;

			}

			double precision = (double)TP.get(classs) / (double)(TP.get(classs) + FP.get(classs));

			precisionPerClass.put(classs, precision);

		}

		return precisionPerClass;

	}

	private HashMap<String, Double> calculateRecallPerClass(HashMap<String, Integer> TP, HashMap<String, Integer> FN) {

		HashMap<String, Double> recallPerClass = new HashMap<String, Double>();

		for (String classs : TP.keySet()) {

			if ((TP.get(classs) + FN.get(classs) == 0)) {

				recallPerClass.put(classs, 0.0);
				continue;

			}

			double recall = (double)TP.get(classs) / (double)(TP.get(classs) + FN.get(classs));

			recallPerClass.put(classs, recall);

		}

		return recallPerClass;

	}

	private HashMap<String, Double> calculateF1ScorePerClass(HashMap<String, Double> precision, HashMap<String, Double> recall) {

		HashMap<String, Double> F1ScorePerClass = new HashMap<String, Double>();

		for (String classs : precision.keySet()) {

			Double p = precision.get(classs);
			Double r = recall.get(classs);

			if ((p + r) == 0) {
				F1ScorePerClass.put(classs, 0.0);
				continue;
			}

			Double score = (double)2 * (double)(p * r) / (double)(p + r);

			F1ScorePerClass.put(classs, score);
		}

		return F1ScorePerClass;

	}

	private HashMap<String, Integer> getOccurencesPerClass(List<ExperimentResult> resultList) {

		HashMap<String, Integer> occurencesPerClass = new HashMap<String, Integer>();

		HashSet<String> classes = (HashSet<String>) resultList.get(0).getSetOfHypotheses();

		for (String classs : classes) {

			occurencesPerClass.put(classs, 0);
		}

		for (ExperimentResult result : resultList) {

			String trueHyp = result.getTrueHyp();

			int occurences = occurencesPerClass.get(trueHyp);

			occurencesPerClass.put(trueHyp, occurences + 1);

		}
		
		for(String classs : classes) {
			System.out.println(classs + " " + occurencesPerClass.get(classs));
		}

		return occurencesPerClass;
	}
	
	private Map<String, Map<String, Integer>> computeConfusionMatrix(double percentage, List<ExperimentResult> allResults) {
		Map<String, Map<String, Integer>> cMatrix = new HashMap<String, Map<String, Integer>>();
		
		for(String key : this.resultMap.keySet()) {
			cMatrix.put(key, new HashMap<String, Integer>());
		}
		
		List<String> predictedHyps;
		int obsStep;
		int count;
		for(ExperimentResult result : allResults) {
			obsStep = (int) (((double) result.numberOfResultSteps()) * percentage);
			
			predictedHyps = result.getHypsWithMaxProbabilityForObsStep(obsStep + 1);
			
			if(predictedHyps.size() > 1) {
				if(cMatrix.get(result.getTrueHyp()).get("none") == null) {
					count = 0;
				}
				else {
					count = cMatrix.get(result.getTrueHyp()).get("none");
				}
				count++;
				cMatrix.get(result.getTrueHyp()).put("none", count);
			}
			else {
				if(cMatrix.get(result.getTrueHyp()).get(predictedHyps.get(0)) == null) {
					count = 0;
				}
				else {
					count = cMatrix.get(result.getTrueHyp()).get(predictedHyps.get(0));
				}
				count++;
				cMatrix.get(result.getTrueHyp()).put(predictedHyps.get(0), count);
			}
		}
		
		System.out.println(percentage);
		this.printConfusionMatrix(cMatrix);
		
		return cMatrix;
	}
	
	private void printConfusionMatrix(Map<String, Map<String, Integer>> cMatrix) {
		int count = 0;
		for(String actualHyp : cMatrix.keySet()) {
			if(count == 0) {
				System.out.print("\t");
				for(String predHyp : this.resultMap.keySet()) {
					System.out.print(predHyp + "\t");
				}
				System.out.print("none");
				System.out.println();
				count++;
			}
			System.out.print(actualHyp + "\t");
			for(String predHyp : this.resultMap.keySet()) {
				if(cMatrix.get(actualHyp).get(predHyp) != null) {
					System.out.print(cMatrix.get(actualHyp).get(predHyp) + "\t");
				}
				else {
					System.out.print("0\t");
				}
			}
			if(cMatrix.get(actualHyp).get("none") != null) {
				System.out.print(cMatrix.get(actualHyp).get("none") + "\t");
			}
			else {
				System.out.print("0\t");
			}
			System.out.print("\n");
		}
		System.out.println("\n");
	}

}

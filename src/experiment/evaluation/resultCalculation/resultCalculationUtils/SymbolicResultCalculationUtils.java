package experiment.evaluation.resultCalculation.resultCalculationUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import config.EvaluationConfiguration;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.PerformanceEvaluation;
import experiment.evaluation.wrapper.AveragePerformanceWrapper;
import utils.JSONUtils;
import utils.database.ExperimentResultDatabase;

public class SymbolicResultCalculationUtils {

    public static PerformanceEvaluation calculateSymbolicExperimentPerformance(List<Double> obsPercentages, ExperimentResultDatabase expDB, File outFile) {
		PerformanceEvaluation performanceEval = SymbolicResultCalculationUtils.calculateSymbolicExperimentPerformance(obsPercentages, expDB);

		//Write averaged results to file
		SymbolicResultCalculationUtils.writeAverageResultsToFile(performanceEval, obsPercentages, outFile);

		//Write average spread to file
		SymbolicResultCalculationUtils.writeSpreadToFile(performanceEval, obsPercentages, outFile);

		return performanceEval;
	}

    public static PerformanceEvaluation calculateSymbolicExperimentPerformance(List<Double> obsPercentages, ExperimentResultDatabase expDB) {
		PerformanceEvaluation performanceEval = new PerformanceEvaluation();
		
		//Loop through all experiment results in the database
		for(ExperimentResult expResult : expDB.readExperimentResultsListFromDatabase()) {
			performanceEval.addExperimentResult(expResult);
			System.out.println("TRUE HYP: " + expResult.getTrueHyp());
		}

		return performanceEval;
	}

    public static void writeAverageResultsToFile(PerformanceEvaluation performanceEval, List<Double> obsPercentages, File outFile) {
		JSONUtils.writeObjectToJSON(new AveragePerformanceWrapper(GeneralResultCalculationUtils.calculatePerformanceMeasure(performanceEval, obsPercentages)), outFile);	
	}

	public static void writeSpreadToFile(PerformanceEvaluation performanceEval, List<Double> obsPercentages, File outFile) {
		String spreadFilePath = outFile.getAbsolutePath().replace(".json", "");
		spreadFilePath += "_spread.json";
		JSONUtils.writeObjectToJSON(new AveragePerformanceWrapper(performanceEval.calculateAverageSpread(obsPercentages)), new File(spreadFilePath));
	}

    public static List<Map<Double, Double>> calculateAveragePerformanceMeasuresForSymbolicResultDirectory(File resultDir) {
		ExperimentResultDatabase symbolicDB;

		List<PerformanceEvaluation> performanceEvals = new ArrayList<PerformanceEvaluation>();
		for (File f : resultDir.listFiles()) {
			if (!f.getName().contains(".db")) {
				continue;
			}
			symbolicDB = new ExperimentResultDatabase(f.getAbsolutePath());
			performanceEvals.add(SymbolicResultCalculationUtils.calculateSymbolicExperimentPerformance(
					Arrays.asList(EvaluationConfiguration.OBS_PERCENTAGES), symbolicDB));
		}

		Map<Double, Double> averagePerformances = new TreeMap<Double, Double>();
		Map<Double, Double> averageSpreads = new TreeMap<Double, Double>();
		Map<Double, Map<String, Double>> perfMeasures;
		Map<Double, Map<String, Double>> spreads;
		double averagePerformance;
		double averageSpread;
		for (PerformanceEvaluation perfEval : performanceEvals) {
			perfMeasures = GeneralResultCalculationUtils.calculatePerformanceMeasure(perfEval,
					Arrays.asList(EvaluationConfiguration.OBS_PERCENTAGES));
			spreads = perfEval.calculateAverageSpread(Arrays.asList(EvaluationConfiguration.OBS_PERCENTAGES));

			for (double obsPerc : perfMeasures.keySet()) {
				if (averagePerformances.get(obsPerc) == null) {
					averagePerformances.put(obsPerc, 0.0);
				}
				if (averageSpreads.get(obsPerc) == null) {
					averageSpreads.put(obsPerc, 0.0);
				}

				averagePerformance = averagePerformances.get(obsPerc);
				averagePerformance += perfMeasures.get(obsPerc).get("total");
				averagePerformances.put(obsPerc, averagePerformance);

				averageSpread = averageSpreads.get(obsPerc);
				averageSpread += spreads.get(obsPerc).get("total");
				averageSpreads.put(obsPerc, averageSpread);
			}
		}

		for (double obsPerc : averagePerformances.keySet()) {
			averagePerformance = averagePerformances.get(obsPerc);
			averagePerformance = averagePerformance / ((double) performanceEvals.size());
			averagePerformances.put(obsPerc, averagePerformance);

			averageSpread = averageSpreads.get(obsPerc);
			averageSpread = averageSpread / ((double) performanceEvals.size());
			averageSpreads.put(obsPerc, averageSpread);
		}

		GeneralResultCalculationUtils.writeAverageDirectoryPerformanceToFile(averagePerformances,
				Paths.get(resultDir.getAbsolutePath(), "averageResults.txt").toFile());

        GeneralResultCalculationUtils.writeAverageDirectorySpreadToFile(averageSpreads,
				Paths.get(resultDir.getAbsolutePath(), "averageSpreads.txt").toFile());

        GeneralResultCalculationUtils.writeAveragePerformanceToLatexFile(averagePerformances,
				Paths.get(resultDir.getAbsolutePath(), "averageResults.tex").toFile());

		GeneralResultCalculationUtils.writeAverageDirectoryPerformanceToTableFile(averagePerformances, Paths.get(resultDir.getAbsolutePath(), "averageResults_table.tex").toFile());

		GeneralResultCalculationUtils.writeAveragePerformanceToLatexFile(averageSpreads,
				Paths.get(resultDir.getAbsolutePath(), "averageSpreads.tex").toFile());

		List<Map<Double, Double>> result = new ArrayList<Map<Double, Double>>();
		result.add(averagePerformances);
		result.add(averageSpreads);
		
		return result;
	}

    public static Map<Double, Double> calculateStandardDeviation(List<Map<Double, Double>> values, Map<Double, Double> averageValues) {
		Map<Double, Double> deviation = new HashMap<Double, Double>();

		Double v;
		for(Map<Double, Double> pMap : values) {
			for(Double obsPerc : pMap.keySet()) {
				v = deviation.get(obsPerc);
				if(v == null) {
					v = 0.0;
					deviation.put(obsPerc, v);
				}
				v += Math.pow(pMap.get(obsPerc) - averageValues.get(obsPerc), 2);
				deviation.put(obsPerc, v);
			}
		}
		for(Double obsPerc : deviation.keySet()) {
			v = deviation.get(obsPerc);
			deviation.put(obsPerc, Math.sqrt(v/values.size()));
		}

		return deviation;
	}

    public static Map<Double, Double> calculateAverage(List<Map<Double, Double>> values) {
		Map<Double, Double> averageValues = new HashMap<Double, Double>();

		Double v;
		for(Map<Double, Double> pMap : values) {
			for(Double obsPerc : pMap.keySet()) {
				v = averageValues.get(obsPerc);
				if(v == null) {
					v = 0.0;
					averageValues.put(obsPerc, v);
				}
				v += pMap.get(obsPerc);
				averageValues.put(obsPerc, v);
			}
		}
		for(Double obsPerc : averageValues.keySet()) {
			v = averageValues.get(obsPerc);
			averageValues.put(obsPerc, v/values.size());
		}

		return averageValues;
	}

    public static List<Map<Double, Double>> calculateAverage(Map<String, List<Map<Double, Double>>> values) {
		List<Map<Double, Double>> averageValues = new ArrayList<Map<Double, Double>>();

		int listSize = values.get(values.keySet().iterator().next()).size();
		int numDomains = values.keySet().size();
		for(int i = 0; i < listSize; i++) {
			averageValues.add(new HashMap<Double, Double>());
		}

		Map<Double, Double> avg;
		Double v;
		for(int i = 0; i < listSize; i++) {
			avg = averageValues.get(i);

			for(String key : values.keySet()) {
				for(Double obsPerc : values.get(key).get(i).keySet()) {
					v = avg.get(obsPerc);
					if(v == null) {
						v = 0.0;
					}
					v += values.get(key).get(i).get(obsPerc);
					avg.put(obsPerc, v);
				}
			}
		}

		for(int i = 0; i < listSize; i++) {
			avg = averageValues.get(i);

			for(Double obsPerc : avg.keySet()) {
				v = avg.get(obsPerc);
				v = v/(double)numDomains;
				avg.put(obsPerc, v);
			}
		}

		return averageValues;
	}
    
}

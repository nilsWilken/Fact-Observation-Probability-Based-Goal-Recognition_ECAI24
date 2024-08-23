package experiment.evaluation.resultCalculation.resultCalculationUtils;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.EvaluationConfiguration;
import experiment.evaluation.utils.PerformanceEvaluation;

public class GeneralResultCalculationUtils {

    public static Map<Double, Map<String, Double>> calculatePerformanceMeasure(PerformanceEvaluation performanceEval, List<Double> obsPercentages) {
		switch(EvaluationConfiguration.PERFORMANCE_MEASURE) {
		case ACCURACY:
			return performanceEval.calculateAccuracy(obsPercentages);
		case PRECISION:
			return performanceEval.calculatePrecision(obsPercentages);
		case MACRO_F1_SCORE:
			return performanceEval.calculateMacroF1Score(obsPercentages);
		case WEIGHTED_F1_SCORE:
			return performanceEval.calculateWeightedF1Score(obsPercentages);
		default:
			return null;
		}
	}

    

    public static Map<Double, Map<String, Double>> averageExpPerformances(List<Map<Double, Map<String, Double>>> expPerformances) {
		Map<Double, Map<String, Double>> averagePerformances = new HashMap<Double, Map<String, Double>>();

		//Helper variables
		Map<String, Double> perfSums;
		Map<String, Double> expCounters;
		Map<String, Double> expPerformance;
		Map<String, Double> averagePerformance;
		double perfSum;
		double expCounter;

		for(double perc : expPerformances.get(0).keySet()) {
			perfSums = new HashMap<String, Double>();
			expCounters = new HashMap<String, Double>();

			for(int i=0; i < expPerformances.size(); i++) {
				expPerformance = expPerformances.get(i).get(perc);
				for(String key : expPerformance.keySet()) {
					if(perfSums.get(key) != null) {
						perfSum = perfSums.get(key);
					}
					else {
						perfSum = 0.0;
					}
					if(expCounters.get(key) != null) {
						expCounter = expCounters.get(key);
					}
					else {
						expCounter = 0.0;
					}
					perfSum += expPerformance.get(key);
					perfSums.put(key, perfSum);

					expCounter++;
					expCounters.put(key, expCounter);
				}
			}
			averagePerformance = new HashMap<String, Double>();
			for(String key : perfSums.keySet()) {
				averagePerformance.put(key, perfSums.get(key)/expCounters.get(key));
			}
			averagePerformances.put(perc, averagePerformance);
		}

		return averagePerformances;
	}

    public static void writeAverageDirectoryPerformanceToFile(Map<Double, Double> averagePerformances, File outFile) {
		StringBuffer outString = new StringBuffer();

		for(double obsPerc : averagePerformances.keySet()) {
			outString.append(obsPerc + ": " + averagePerformances.get(obsPerc) + "\n");
		}

		try {
			if(outFile.exists()) {
				outFile.delete();
			}
			Files.writeString(Paths.get(outFile.getAbsolutePath()), outString.toString(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static void writeAverageDirectoryPerformanceToTableFile(Map<Double, Double> averagePerformances, File outFile) {
		StringBuffer outString = new StringBuffer();

		List<Double> percs = new ArrayList<Double>();
		percs.add(0.1);
		percs.add(0.2);
		percs.add(0.3);
		percs.add(0.4);
		percs.add(0.5);
		percs.add(0.6);
		percs.add(0.7);
		percs.add(0.8);
		percs.add(0.9);
		percs.add(0.99);


		outString.append("&");
		BigDecimal bd;
		for(double obsPerc : percs) {
			bd = new BigDecimal(averagePerformances.get(obsPerc)).setScale(2, RoundingMode.HALF_UP);
			outString.append(bd.doubleValue() + "   &");
		}

		try {
			if(outFile.exists()) {
				outFile.delete();
			}
			Files.writeString(Paths.get(outFile.getAbsolutePath()), outString.toString(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static void writeAverageDirectoryDeviationToTableFile(Map<Double, Double> averagePerformances, File outFile) {
		StringBuffer outString = new StringBuffer();

		List<Double> percs = new ArrayList<Double>();
		percs.add(0.1);
		percs.add(0.2);
		percs.add(0.3);
		percs.add(0.4);
		percs.add(0.5);
		percs.add(0.6);
		percs.add(0.7);
		percs.add(0.8);
		percs.add(0.9);
		percs.add(0.99);


		outString.append("&");
		BigDecimal bd;
		for(double obsPerc : percs) {
			bd = new BigDecimal(averagePerformances.get(obsPerc)).setScale(2, RoundingMode.HALF_UP);
			outString.append(bd.doubleValue() + "   &");
		}

		try {
			if(outFile.exists()) {
				outFile.delete();
			}
			Files.writeString(Paths.get(outFile.getAbsolutePath()), outString.toString(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static void writeAverageDirectorySpreadToFile(Map<Double, Double> averageSpreads, File outFile) {
		StringBuffer outString = new StringBuffer();

		double totalAverageSpread = 0.0;
		for(double obsPerc : averageSpreads.keySet()) {
			outString.append(obsPerc + ": " + averageSpreads.get(obsPerc) + "\n");
			totalAverageSpread += averageSpreads.get(obsPerc);
		}
		totalAverageSpread = totalAverageSpread/(double)averageSpreads.keySet().size();
		outString.append("total: " + totalAverageSpread);

		try {
			if(outFile.exists()) {
				outFile.delete();
			}
			Files.writeString(Paths.get(outFile.getAbsolutePath()), outString.toString(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

    public static String writeAveragePerformanceToLatexFile(Map<Double, Double> averagePerformance, File outFile) {
		StringBuffer plot = new StringBuffer();
		plot.append("\t\t\t\t\tcoordinates { ");
		
		BigDecimal bd;
		int counter = 1;
		for(double key : averagePerformance.keySet()) {
			bd = new BigDecimal(averagePerformance.get(key)).setScale(2, RoundingMode.HALF_UP);
			plot.append("(" + ((double)counter++) + "," + bd.doubleValue() + ")");
		}
		
		plot.append("};");

		try {
			if(outFile.exists()) {
				outFile.delete();
			}
			Files.writeString(Paths.get(outFile.getAbsolutePath()), plot.toString(), StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return plot.toString();
	}
    
}

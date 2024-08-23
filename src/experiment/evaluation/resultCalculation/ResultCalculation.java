package experiment.evaluation.resultCalculation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.ConfigurationManager;
import config.EvaluationConfiguration;
import config.GoalRecognitionConfiguration;
import experiment.evaluation.resultCalculation.resultCalculationUtils.GeneralResultCalculationUtils;
import experiment.evaluation.resultCalculation.resultCalculationUtils.SymbolicResultCalculationUtils;
import utils.database.ExperimentResultDatabase;
import utils.plotting.LatexFigureUtils;

public class ResultCalculation {
	public static void main(String[] args) {
		//Check whether arguments are valid.
		if(args.length > 0) {
			try {
				System.out.println("Initialize configs");
				ConfigurationManager.initializeEvaluationConfiguration(new File(args[0]));
			} catch(IOException e) {
				e.printStackTrace();
			}
		}

		//Execute result calculation depending on the configured calculation type.
		switch(EvaluationConfiguration.CALCULATION_TYPE) {
		case SYMBOLIC:
			ResultCalculation.runSymbolicResultCalculation();
			break;
		case SYMBOLIC_DIRECTORY:
			ResultCalculation.runSymbolicDirectoryResultCalculation();
			break;
		case DIRECTORY_STANDARD_DEVIATION:
			ResultCalculation.runSymbolicDirectoryStandardDeviationResultCalculation();
			break;
		default:
			break;
		}		
	}

	public static void runSymbolicResultCalculation() {
		ExperimentResultDatabase symbolicDB = null;
		if(EvaluationConfiguration.SYMBOLIC_DB_PATH != null && !EvaluationConfiguration.SYMBOLIC_DB_PATH.equals("")) {
			symbolicDB = new ExperimentResultDatabase(EvaluationConfiguration.SYMBOLIC_DB_PATH);
		}
		if(symbolicDB == null) {
			return;
		}
		SymbolicResultCalculationUtils.calculateSymbolicExperimentPerformance(Arrays.asList(EvaluationConfiguration.OBS_PERCENTAGES), symbolicDB, new File(EvaluationConfiguration.OUT_FILE_PATH));

		if(EvaluationConfiguration.WRITE_RESULTS_TO_LATEX_FIGURE) {
			GoalRecognitionConfiguration.ROUNDING_SCALE = 3;
			LatexFigureUtils.writeAverageSinglePerformanceToLatexFigure(new File(EvaluationConfiguration.OUT_FILE_PATH), new File(EvaluationConfiguration.LATEX_OUT_FILE_PATH));
		}
	}

	public static void runSymbolicDirectoryResultCalculation() {
		for (File resultDir : new File(EvaluationConfiguration.SYMBOLIC_DB_DIRECTORY_PATH).listFiles()) {
			if(!resultDir.isDirectory()) {
				continue;
			}
			SymbolicResultCalculationUtils.calculateAveragePerformanceMeasuresForSymbolicResultDirectory(resultDir);	
		}
	}

	public static void runSymbolicDirectoryStandardDeviationResultCalculation() {
		String fileName;
			Map<String, List<Map<Double, Double>>> performances = new HashMap<String, List<Map<Double, Double>>>();
			Map<String, List<Map<Double, Double>>> spreads = new HashMap<String, List<Map<Double, Double>>>();
			List<Map<Double, Double>> averageMetrics;
			List<Map<Double, Double>> tmp;
			for(File testRun : new File(EvaluationConfiguration.SYMBOLIC_DB_DIRECTORY_PATH).listFiles()) {
				if(!testRun.isDirectory()) {
					continue;
				}
				for(File resultDir : testRun.listFiles()) {
					if(!resultDir.isDirectory()) {
						continue;
					}
					fileName = resultDir.getName();
					averageMetrics = SymbolicResultCalculationUtils.calculateAveragePerformanceMeasuresForSymbolicResultDirectory(resultDir);

					tmp = performances.get(fileName);
					if(tmp == null) {
						tmp = new ArrayList<Map<Double, Double>>();
						performances.put(fileName, tmp);
					}
					tmp.add(averageMetrics.get(0));

					tmp = spreads.get(fileName);
					if(tmp == null) {
						tmp = new ArrayList<Map<Double, Double>>();
						spreads.put(fileName, tmp);
					}
					tmp.add(averageMetrics.get(1));
				}
			}

			Map<String, Map<Double, Double>> averagePerformances = new HashMap<String, Map<Double, Double>>();
			Map<String, Map<Double, Double>> performanceDeviations = new HashMap<String, Map<Double, Double>>();

			Map<String, Map<Double, Double>> averageSpreads = new HashMap<String, Map<Double, Double>>();
			Map<String, Map<Double, Double>> spreadDeviations = new HashMap<String, Map<Double, Double>>();

			File deviationDir = Paths.get(EvaluationConfiguration.SYMBOLIC_DB_DIRECTORY_PATH, "standardDeviationPerformance").toFile();
			if(!deviationDir.exists()) {
				deviationDir.mkdir();
			}

			File devDir;
			for(String key : performances.keySet()) {
				averagePerformances.put(key, SymbolicResultCalculationUtils.calculateAverage(performances.get(key)));	
				performanceDeviations.put(key, SymbolicResultCalculationUtils.calculateStandardDeviation(performances.get(key), averagePerformances.get(key)));

				averageSpreads.put(key, SymbolicResultCalculationUtils.calculateAverage(spreads.get(key)));
				spreadDeviations.put(key, SymbolicResultCalculationUtils.calculateStandardDeviation(spreads.get(key), averageSpreads.get(key)));
			
				devDir = Paths.get(deviationDir.getAbsolutePath(), key).toFile();
				if(!devDir.exists()) {
					devDir.mkdir();
				}

				GeneralResultCalculationUtils.writeAverageDirectoryPerformanceToFile(averagePerformances.get(key),
					Paths.get(devDir.getAbsolutePath(), "averageResults.txt").toFile());

				GeneralResultCalculationUtils.writeAverageDirectorySpreadToFile(averageSpreads.get(key),
					Paths.get(devDir.getAbsolutePath(), "averageSpreads.txt").toFile());

				GeneralResultCalculationUtils.writeAverageDirectorySpreadToFile(spreadDeviations.get(key), Paths.get(devDir.getAbsolutePath(), "averageSpreadsDeviation.txt").toFile());

				GeneralResultCalculationUtils.writeAveragePerformanceToLatexFile(averagePerformances.get(key),
					Paths.get(devDir.getAbsolutePath(), "averageResults.tex").toFile());

				GeneralResultCalculationUtils.writeAverageDirectoryPerformanceToTableFile(averagePerformances.get(key), Paths.get(devDir.getAbsolutePath(), "averageResults_table.tex").toFile());

				GeneralResultCalculationUtils.writeAverageDirectoryDeviationToTableFile(performanceDeviations.get(key), Paths.get(devDir.getAbsolutePath(), "averageResultsDeviation_table.tex").toFile());

				GeneralResultCalculationUtils.writeAveragePerformanceToLatexFile(averageSpreads.get(key),
					Paths.get(devDir.getAbsolutePath(), "averageSpreads.tex").toFile());
			}

			devDir = Paths.get(deviationDir.getAbsolutePath(), "average").toFile();
			if(!devDir.exists()) {
				devDir.mkdir();
			}

			List<Map<Double, Double>> totalAveragePerformanceValues = SymbolicResultCalculationUtils.calculateAverage(performances);
			Map<Double, Double> totalAveragePerformance = SymbolicResultCalculationUtils.calculateAverage(totalAveragePerformanceValues);

			List<Map<Double, Double>> totalAverageSpreadValues = SymbolicResultCalculationUtils.calculateAverage(spreads);
			Map<Double, Double> totalAverageSpread = SymbolicResultCalculationUtils.calculateAverage(totalAverageSpreadValues);

			Map<Double, Double> totalPerformanceDeviation = SymbolicResultCalculationUtils.calculateStandardDeviation(totalAveragePerformanceValues, totalAveragePerformance);
			Map<Double, Double> totalSpreadDeviation = SymbolicResultCalculationUtils.calculateStandardDeviation(totalAverageSpreadValues, totalAverageSpread);

			GeneralResultCalculationUtils.writeAverageDirectoryPerformanceToTableFile(totalAveragePerformance, Paths.get(devDir.getAbsolutePath(), "averageResults_table.tex").toFile());
			
			GeneralResultCalculationUtils.writeAverageDirectoryDeviationToTableFile(totalPerformanceDeviation, Paths.get(devDir.getAbsolutePath(), "averageResultsDeviation_table.tex").toFile());

			GeneralResultCalculationUtils.writeAverageDirectorySpreadToFile(totalAverageSpread,	Paths.get(devDir.getAbsolutePath(), "averageSpreads.txt").toFile());

			GeneralResultCalculationUtils.writeAverageDirectorySpreadToFile(totalSpreadDeviation, Paths.get(devDir.getAbsolutePath(), "averageSpreadsDeviation.txt").toFile());
	}

}

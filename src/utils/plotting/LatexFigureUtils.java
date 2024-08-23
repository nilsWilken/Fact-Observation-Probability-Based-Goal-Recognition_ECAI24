package utils.plotting;

import java.io.File;
import java.util.Map;

import config.GoalRecognitionConfiguration;
import experiment.evaluation.wrapper.AveragePerformanceWrapper;
import experiment.evaluation.wrapper.AveragePerformancesWrapper;
import utils.JSONUtils;

public class LatexFigureUtils {
	
	public static void writeAverageSinglePerformanceToLatexFigure(File averagePerfFile, File outputFile) {
		Map<Double, Map<String, Double>> prapAveragePerformances = ((AveragePerformanceWrapper) JSONUtils.readObjectFromJSON(AveragePerformanceWrapper.class, averagePerfFile)).getPerformance();
		(new AveragePerformanceWrapper(prapAveragePerformances)).writeTotalPerformanceToLatexStrings(GoalRecognitionConfiguration.PRAP_MARK, GoalRecognitionConfiguration.PRAP_COLOR, outputFile);
	}

}

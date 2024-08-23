package utils.plotting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.github.sh0nk.matplotlib4j.Plot;
import com.github.sh0nk.matplotlib4j.PythonExecutionException;
import com.github.sh0nk.matplotlib4j.builder.PlotBuilder;

public class ResultPlotter {
	
	public static void plotResults(String goal, int n, Map<Double, Map<String, Double>> symbolicResults, Map<Integer, Map<Double, Map<String, Double>>> dataDrivenResults, Map<Integer, Map<Double, Map<String, Double>>> combinedResults, List<Double> obsPercentages) {
		Plot plt = Plot.create();
				
		List<Double> plotValuesPRAP = new ArrayList<Double>();
		for(double perc : obsPercentages) {
			plotValuesPRAP.add(symbolicResults.get(perc).get(goal));
		}
		
		List<Double> plotValuesBN = new ArrayList<Double>();
		for(double perc : obsPercentages) {
			plotValuesBN.add(dataDrivenResults.get(n).get(perc).get(goal));
		}
		
		List<Double> plotValuesCombined = new ArrayList<Double>();
		for(double perc : obsPercentages) {
			plotValuesCombined.add(combinedResults.get(n).get(perc).get(goal));
		}
		
		plt.plot().add(obsPercentages, plotValuesPRAP).label("PRAP").linestyle("dashed");
		plt.plot().add(obsPercentages, plotValuesBN).label("BN").linestyle("dashdot");
		plt.plot().add(obsPercentages, plotValuesCombined).label("Combined").linestyle("solid");
		plt.xlabel("obs percentage");
		plt.ylabel("performance");
		plt.title("n = " + n);
		plt.legend();
		try {
			plt.show();
		} catch (IOException | PythonExecutionException e) {
			e.printStackTrace();
		}
	}
	
	
	public static void plotResults(String goal, Map<Double, Map<String, Double>> results, List<Double> obsPercentages) {
		Plot plt = Plot.create();
		PlotBuilder pltBuilder = plt.plot();
		
		List<Double> plotValues = new ArrayList<Double>();
		for(double perc : obsPercentages) {
			plotValues.add(results.get(perc).get(goal));
		}
		
		pltBuilder.add(obsPercentages, plotValues).label(goal).linestyle("--");
		plt.xlabel("obs percentage");
		plt.ylabel("performance");
		plt.legend();
		try {
			plt.show();
		} catch (IOException | PythonExecutionException e) {
			e.printStackTrace();
		}
	}

}

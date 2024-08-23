package experiment.evaluation.wrapper;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import config.GoalRecognitionConfiguration;

public class AveragePerformanceWrapper {
	
	private Map<Double, Map<String, Double>> averagePerformance;
	
	public AveragePerformanceWrapper(Map<Double, Map<String, Double>> averagePerformance) {
		this.averagePerformance = new TreeMap<Double, Map<String, Double>>();
		
		Map<String, Double> tmp;
		Map<String, Double> averagePerf;
		
		for(double key : averagePerformance.keySet()) {
			tmp = new TreeMap<String, Double>();
			averagePerf = averagePerformance.get(key);
			for(String key2 : averagePerf.keySet()) {
				tmp.put(key2, averagePerf.get(key2));
			}
			this.averagePerformance.put(key, tmp);
		}
		
	}
	
	public Map<Double, Map<String, Double>> getPerformance() {
		return this.averagePerformance;
	}
	
	public void writeTotalPerformanceToLatexStrings(String mark, String color, File outFile) {
		try {
			Files.writeString(Paths.get(outFile.getAbsolutePath()), this.generateAveragePerformanceLatexString(mark, color), new OpenOption[0]);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	protected String generateAveragePerformanceLatexString(String mark, String color) {
		StringBuffer latex = new StringBuffer();
		
		List<Double> x = new ArrayList<Double>();
		for(int i=1; i <= this.averagePerformance.keySet().size(); i++) {
			x.add((double)i);
		}
		
		List<Double> y = new ArrayList<Double>();
		List<Double> sortedPercs = new ArrayList<Double>();
		for(Double perc : this.averagePerformance.keySet()) {
			sortedPercs.add(perc);
		}
		
		Collections.sort(sortedPercs);
		for(int i=0; i < sortedPercs.size(); i++) {
			y.add(this.averagePerformance.get(sortedPercs.get(i)).get("total"));
		}
		latex.append(AveragePerformanceWrapper.generatePlotString(mark, color, x, y));
		
		return latex.toString();
	}
	
	protected static String generatePlotString(String mark, String color, List<Double> x, List<Double> y) {
		StringBuffer plot = new StringBuffer();
		plot.append("\\addplot[\n");
		plot.append("\t\t\t\t\tcolor=" + color + ",\n");
		plot.append("\t\t\t\t\tmark=" + mark + ",\n");
		plot.append("\t\t\t\t\t]\n");
		plot.append("\t\t\t\t\tcoordinates { ");
		
		BigDecimal bd;
		for(int i=0; i < x.size(); i++) {
			bd = new BigDecimal(y.get(i)).setScale(GoalRecognitionConfiguration.ROUNDING_SCALE, RoundingMode.HALF_UP);
			plot.append("(" + x.get(i) + "," + bd.doubleValue() + ")");
		}
		
		plot.append("};");
		
		return plot.toString();
	}

}

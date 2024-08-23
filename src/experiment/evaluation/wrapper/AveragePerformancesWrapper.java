package experiment.evaluation.wrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

public class AveragePerformancesWrapper {
	
	private Map<Integer, Map<Double, Map<String, Double>>> averagePerformances;
	
	public AveragePerformancesWrapper(Map<Integer, Map<Double, Map<String, Double>>> averagePerformances) {
		this.averagePerformances = new TreeMap<Integer, Map<Double, Map<String, Double>>>();
		
		Map<Double, Map<String, Double>> tmp;
		Map<String, Double> tmp2;
		Map<Double, Map<String, Double>> tmp12;
		Map<String, Double> tmp22;
		for(int key : averagePerformances.keySet()) {
			tmp12 = averagePerformances.get(key);
			tmp = new TreeMap<Double, Map<String, Double>>();
			for(double key2 : tmp12.keySet()) {
				tmp22 = tmp12.get(key2);
				tmp2 = new TreeMap<String, Double>();
				for(String key3 : tmp22.keySet()) {
					tmp2.put(key3, tmp22.get(key3));
				}
				tmp.put(key2, tmp2);
			}
			this.averagePerformances.put(key, tmp);
		}
	}
	
	public Map<Integer, Map<Double, Map<String, Double>>> getPerformances() {
		return this.averagePerformances;
	}
	
	public void writeTotalAveragePerformancesToFile(String mark, String color, File outFile) {
		StringBuffer latex = new StringBuffer();
		
		for(int n : this.averagePerformances.keySet()) {
			latex.append("n = " + n + ": \n");
			latex.append((new AveragePerformanceWrapper(this.averagePerformances.get(n)).generateAveragePerformanceLatexString(mark, color)));
			latex.append("\n");
		}
		
		try {
			Files.writeString(Paths.get(outFile.getAbsolutePath()), latex.toString(), new OpenOption[0]);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

}

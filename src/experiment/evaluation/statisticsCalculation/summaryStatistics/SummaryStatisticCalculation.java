package experiment.evaluation.statisticsCalculation.summaryStatistics;

import java.util.List;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

public class SummaryStatisticCalculation {
	
	public static double calculateSummaryStatistic(List<Double> sample, SummaryStatistic statistic) {
		SummaryStatistics stats = SummaryStatisticCalculation.generateSummaryStatisticsFromSample(sample);
		
		switch(statistic) {
		case MAXIMUM:
			return stats.getMax();
		case MEAN:
			return stats.getMean();
		case MINIMUM:
			return stats.getMin();
		case VARIANCE:
			return stats.getVariance();
		default:
			return Double.NaN;
		
		}
	}
	
	public static SummaryStatistics generateSummaryStatisticsFromSample(List<Double> sampleA) {
		SummaryStatistics resultStats = new SummaryStatistics();
		
		for(double d : sampleA) {
			resultStats.addValue(d);
		}
		
		return resultStats;
	}

}

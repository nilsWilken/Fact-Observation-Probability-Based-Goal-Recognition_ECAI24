package symbolic.vectorUtils.distanceMeasures;

import java.util.List;

public class DistanceMeasureCalculation {
	
	public static double calculateDistance(List<Double> v1, List<Double> v2, DistanceMeasure measure) {
		switch(measure) {
		case EUCLIDEAN_DISTANCE:
			return EuclideanDistance.calculateEuclideanDistance(v1, v2);
		case HAMMING_DISTANCE:
			return HammingDistance.calculateHammingDistance(v1, v2);
		default:
			return 0;
		
		}
	}
}

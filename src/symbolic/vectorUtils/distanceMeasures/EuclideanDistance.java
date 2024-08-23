package symbolic.vectorUtils.distanceMeasures;

import java.util.ArrayList;
import java.util.List;

import symbolic.vectorUtils.VectorOperations;

public class EuclideanDistance {

	public static double calculateEuclideanDistance(List<Double> v1, List<Double> v2) {
		List<Double> direction = VectorOperations.calculateDirection(v1, v2);
		
		double sum = 0.0;
		for(double val : direction) {
			sum += Math.pow(val, 2);
		}
		return Math.sqrt(sum);
	}
	
	public static void main(String[] args) {
		List<Double> v1 = new ArrayList<Double>();
		List<Double> v2 = new ArrayList<Double>();
		
		v1.add(1.1);
		v1.add(1.1);
		
		v2.add(1.1);
		v2.add(1.1);
		
		System.out.println(EuclideanDistance.calculateEuclideanDistance(v1, v2));
	}
}

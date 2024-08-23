package symbolic.vectorUtils.similarityMeasures;

import java.util.List;

import weka.core.Instance;

public class CosineSimilarity {
	
	protected static double calculateCosineSimilarity(Instance inst1, Instance inst2) {
		
		double productSum = 0.0;
		double product;
		
		for(int i=0; i < inst1.numAttributes(); i++) {
			product = inst1.value(inst1.attribute(i)) * inst2.value(inst2.attribute(i));
			productSum += product;
		}
		
		double euclideanNorm = (CosineSimilarity.calculateEuclideanNorm(inst1) * CosineSimilarity.calculateEuclideanNorm(inst2));
		
		if(euclideanNorm > 0) {
			return (productSum/euclideanNorm);
		}
		else {
			return 0;
		}
	}
	
	protected static double calculateCosineSimilarity(List<Double> v1, List<Double> v2) {
		double dotProduct = DotProduct.calculateDotProduct(v1, v2);

		double euclideanNorm = (CosineSimilarity.calculateEuclideanNorm(v1) * CosineSimilarity.calculateEuclideanNorm(v2));
		if(euclideanNorm > 0) {
			return (dotProduct/euclideanNorm);
		}
		else {
			if(CosineSimilarity.calculateEuclideanNorm(v1) == 0 && CosineSimilarity.calculateEuclideanNorm(v2) == 0) {
				System.out.println("FAIL " + euclideanNorm);
			}
			return 0;
		}
	}
	
	public static double calculateEuclideanNorm(Instance inst) {
		double sum = 0;
		
		for(int i=0; i < inst.numAttributes(); i++) {
			sum += Math.pow(inst.value(inst.attribute(i)), 2);
		}
		
		return Math.sqrt(sum);
	}
	
	public static double calculateEuclideanNorm(List<Double> v) {
		double sum = 0;
		
		for(double ele : v) {
			sum += Math.pow(ele, 2);
		}
		
		return Math.sqrt(sum);
	}

}

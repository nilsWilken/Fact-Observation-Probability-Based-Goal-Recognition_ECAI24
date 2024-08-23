package symbolic.vectorUtils.similarityMeasures;

import java.util.List;

public class DotProduct {
	
	protected static double calculateDotProduct(List<Double> v1, List<Double> v2) {
		double productSum = 0.0;
		double product;
		
		for(int i=0; i < v1.size(); i++) {
			product = v1.get(i) * v2.get(i);
			productSum += product;
		}
		
		return productSum;
	}
}

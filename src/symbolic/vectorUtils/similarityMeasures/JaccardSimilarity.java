package symbolic.vectorUtils.similarityMeasures;

import java.util.List;

public class JaccardSimilarity {

	protected static double calculateJaccardSimilarity(List<Double> v1, List<Double> v2) {
		double dotProduct = DotProduct.calculateDotProduct(v1, v2);
		double dotProductV1 = DotProduct.calculateDotProduct(v1, v1);
		double dotProductV2 = DotProduct.calculateDotProduct(v2, v2);
		
		return dotProduct/(dotProductV1 + dotProductV2 - dotProduct);
	}
}

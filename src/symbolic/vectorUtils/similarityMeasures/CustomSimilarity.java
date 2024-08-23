package symbolic.vectorUtils.similarityMeasures;

import java.util.List;

public class CustomSimilarity {
	
	protected static double calculateCustomSimilarity(List<Double> v1, List<Double> v2) {
		double dotProduct = DotProduct.calculateDotProduct(v1, v2);
		
		double v1Count = 0;
		for(int i=0; i < v1.size(); i++) {
			if(v1.get(i) != 0 && v2.get(i) != v1.get(i)) {
				v1Count++;
			}
		}
		
		return dotProduct/v1Count;
	}

}

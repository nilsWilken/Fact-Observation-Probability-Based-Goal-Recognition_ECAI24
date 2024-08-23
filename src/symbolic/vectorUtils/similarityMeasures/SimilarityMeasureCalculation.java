package symbolic.vectorUtils.similarityMeasures;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import symbolic.vectorUtils.distanceMeasures.DistanceMeasure;
import symbolic.vectorUtils.distanceMeasures.DistanceMeasureCalculation;
import utils.GeneralUtils;

public class SimilarityMeasureCalculation {
	
	public static double calculateSimilarity(List<Double> v1, List<Double> v2, SimilarityMeasure measure) {
		switch(measure) {
		case COSINE:
			return GeneralUtils.roundDouble(CosineSimilarity.calculateCosineSimilarity(v1, v2), 4);
		case DOT_PRODUCT:
			return GeneralUtils.roundDouble(DotProduct.calculateDotProduct(v1, v2), 4);
		case JACCARD:
			return GeneralUtils.roundDouble(JaccardSimilarity.calculateJaccardSimilarity(v1, v2), 4);
		case EUCLIDEAN_DISTANCE:
			return  DistanceMeasureCalculation.calculateDistance(v1, v2, DistanceMeasure.EUCLIDEAN_DISTANCE);
		case KULLBACK_LEIBLER_DIVERGENCE:
			return 1.0 - GeneralUtils.roundDouble(KullbackLeiblerDivergence.calculateKullbackLeiblerDivergence(v1, v2), 4);
		case CUSTOM:
			return GeneralUtils.roundDouble(CustomSimilarity.calculateCustomSimilarity(v1, v2), 4);
		default:
			return 0;
		
		}
	}
	
	public static double getMaxSimilarity(Map<Integer, Double> similarities) {
		double maxValue = Double.MIN_VALUE;
		
		for(int key : similarities.keySet()) {
			if(similarities.get(key) > maxValue) {
				maxValue = similarities.get(key);
			}
		}
		
		return maxValue;
	}
	
	public static Map<Integer, Double> combineSimilarityMaps(Map<Integer, Double> dotProduct, Map<Integer, Double> cosine) {
		Map<Integer, Double> similarities = new HashMap<Integer, Double>();
		
		double maxValue = SimilarityMeasureCalculation.getMaxSimilarity(dotProduct);
		
		int count = 0;
		for(Double v : dotProduct.values()) {
			if(v == maxValue) {
				count++;
			}
		}
		
		if(count > 1) {
			for(int key : dotProduct.keySet()) {
				if(dotProduct.get(key) == maxValue) {
					similarities.put(key, dotProduct.get(key));
				}
				else {
					similarities.put(key, 0.0);
				}
			}
		}
		else {
			for(int key : cosine.keySet()) {
				similarities.put(key, cosine.get(key));
			}
		}
		
		return similarities;
	}

}

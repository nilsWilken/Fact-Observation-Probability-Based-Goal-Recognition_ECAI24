package symbolic.vectorUtils.similarityMeasures;

public enum SimilarityMeasure {
	
	COSINE,
	JACCARD,
	DOT_PRODUCT,
	EUCLIDEAN_DISTANCE,
	CUSTOM,
	KULLBACK_LEIBLER_DIVERGENCE;

	public static SimilarityMeasure parseFromString(String similarityMeasure) {
		switch(similarityMeasure.toLowerCase()) {
			case "cosine":
				return COSINE;
			case "jaccard":
				return JACCARD;
			case "dotproduct":
				return DOT_PRODUCT;
			case "euclideandistance":
				return EUCLIDEAN_DISTANCE;
			case "custom":
				return CUSTOM;
			case "kullbackleiblerdivergence":
				return KULLBACK_LEIBLER_DIVERGENCE;
			default:
				return COSINE;
		}
	}

}

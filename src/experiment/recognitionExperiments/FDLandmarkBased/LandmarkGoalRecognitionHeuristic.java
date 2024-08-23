package experiment.recognitionExperiments.FDLandmarkBased;

public enum LandmarkGoalRecognitionHeuristic {
    COMPLETION,
	UNIQUENESS;
	
	public static String heuristicToString(LandmarkGoalRecognitionHeuristic heuristic) {
		switch(heuristic) {
		case COMPLETION:
			return "COMPLETION";
		case UNIQUENESS:
			return "UNIQUENESS";
		default:
			return "NO VALID HEURISTIC";
		
		}
	}
	
	public static LandmarkGoalRecognitionHeuristic parseFromString(String heuristic) {
		switch(heuristic.toLowerCase()) {
			case "completion":
				return COMPLETION;
			case "uniqueness":
				return UNIQUENESS;
			default:
				return COMPLETION;
		}
	} 
}

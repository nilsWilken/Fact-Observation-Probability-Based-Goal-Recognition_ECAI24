package experiment.management;

public enum ExperimentType {
	
	MASTERS_SARDINA,
	LANDMARK_BASED,
	LIFTED_LANDMARK_BASED,
	RELAXED_PLAN_RAMIREZ_GEFFNER,
	LP_BASED,
	FACT_OBSERVATION_PROBABILITY;
	
	public static String getDatabaseSummaryName(ExperimentType expType) {
		switch(expType) {
		case MASTERS_SARDINA:
			return "MS";
		case LANDMARK_BASED:
			return "Landmark";
		case LIFTED_LANDMARK_BASED:
			return "LiftedLandmark";
		case RELAXED_PLAN_RAMIREZ_GEFFNER:
			return "RelaxedPlanRamirezGeffner";
		case LP_BASED:
			return "LPBased";
		case FACT_OBSERVATION_PROBABILITY:
			return "FactObservationProbability";
		default:
			return null;
		}
	}
	
	public static ExperimentType parseFromString(String expType) {
		switch(expType.toLowerCase()) {
		case "masterssardina":
			return MASTERS_SARDINA;
		case "landmarkbased":
			return LANDMARK_BASED;
		case "liftedlandmarkbased":
			return LIFTED_LANDMARK_BASED;
		case "relaxedplanramirezgeffner":
			return RELAXED_PLAN_RAMIREZ_GEFFNER;
		case "lpbased":
			return LP_BASED;
		case "factobservationprobability":
			return FACT_OBSERVATION_PROBABILITY;
		default:
			return LANDMARK_BASED;	
		}
	}
	
	public static ExperimentType parseFromSummaryName(String summaryName) {
		for(ExperimentType expType : ExperimentType.values()) {
			if(ExperimentType.getDatabaseSummaryName(expType).contains(summaryName.split("_")[0])) {
				return expType;
			}
		}
		return null;
	}
}

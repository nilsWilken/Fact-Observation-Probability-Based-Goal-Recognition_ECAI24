package utils.database;

public enum ColumnType {
	
	COMPUTATION_TIME,
	COMPUTATION_TIME_KEY,
	HYPOTHESIS,
	OBSERVATION_STEP,
	GOAL_PROBABILITY,
	EXPERIMENT_NAME,
	TRUE_HYPOTHESIS,
	PLAN_LENGTH_KEY,
	PLAN_LENGTH,
	ATTRIBUTE_NAME,
	ATTRIBUTE_VALUE;
	
	public static String convertToStringName(ColumnType type) {
		switch(type) {
		case HYPOTHESIS:
			return "hypothesis";
		case TRUE_HYPOTHESIS:
			return "trueHypothesis";
		case OBSERVATION_STEP:
			return "observationStep";
		case GOAL_PROBABILITY:
			return "goalProbability";
		case COMPUTATION_TIME:
			return "computationTime";
		case COMPUTATION_TIME_KEY:
			return "computationTimeKey";
		case EXPERIMENT_NAME:
			return "experimentName";
		case PLAN_LENGTH_KEY:
			return "planLengthKey";
		case PLAN_LENGTH:
			return "planLength";
		case ATTRIBUTE_NAME:
			return "attributeName";
		case ATTRIBUTE_VALUE:
			return "attributeValue";
		default:
			return "";
		}
	}
	
	public static ColumnType parseFromString(String type) {
		for(ColumnType cType : ColumnType.values()) {
			if(ColumnType.convertToStringName(cType).equals(type.trim())) {
				return cType;
			}
		}
		return null;
	}
	
	public static String getDatabaseVarType(ColumnType type) {
		switch(type) {
		case HYPOTHESIS:
			return "text";
		case TRUE_HYPOTHESIS:
			return "text";
		case OBSERVATION_STEP:
			return "int(4)";
		case GOAL_PROBABILITY:
			return "real";
		case COMPUTATION_TIME:
			return "int(8)";
		case COMPUTATION_TIME_KEY:
			return "text";
		case EXPERIMENT_NAME:
			return "text";
		case PLAN_LENGTH_KEY:
			return "text";
		case PLAN_LENGTH:
			return "int(4)";
		case ATTRIBUTE_NAME:
			return "text";
		case ATTRIBUTE_VALUE:
			return "text";
		default:
			return "";
		}
	}
	
	public static VarType getJavaVarType(ColumnType type) {
		switch(type) {
		case HYPOTHESIS:
			return VarType.TEXT;
		case TRUE_HYPOTHESIS:
			return VarType.TEXT;
		case OBSERVATION_STEP:
			return VarType.INT;
		case GOAL_PROBABILITY:
			return VarType.DOUBLE;
		case COMPUTATION_TIME:
			return VarType.LONG;
		case COMPUTATION_TIME_KEY:
			return VarType.TEXT;
		case EXPERIMENT_NAME:
			return VarType.TEXT;
		case PLAN_LENGTH_KEY:
			return VarType.TEXT;
		case PLAN_LENGTH:
			return VarType.INT;
		case ATTRIBUTE_NAME:
			return VarType.TEXT;
		case ATTRIBUTE_VALUE:
			return VarType.TEXT;
		default:
			return null;
		}
	}
	
	public static EvaluationTable getEvaluationTable(ColumnType type) {
		for(EvaluationTable table : EvaluationTable.values()) {
			for(ColumnType cType : EvaluationTable.getTableSchema(table)) {
				if(cType == type) {
					return table;
				}
			}
		}
		return null;
	}
	

}

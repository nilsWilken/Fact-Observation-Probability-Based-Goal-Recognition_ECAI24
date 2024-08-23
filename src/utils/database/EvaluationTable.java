package utils.database;

public enum EvaluationTable {

	SUMMARY, TOTAL_COMPUTATION_TIMES, EXPERIMENT_COMPUTATION_TIMES, PLAN_LENGTHS, EXPERIMENT_RESULT, CONFIGURATION;

	public static String getTableName(EvaluationTable type) {
		switch (type) {
		case SUMMARY:
			return "Summary";
		case TOTAL_COMPUTATION_TIMES:
			return "TotalComputationTimes";
		case EXPERIMENT_RESULT:
			return "ExperimentResult";
		case EXPERIMENT_COMPUTATION_TIMES:
			return "ExperimentComputationTimes";
		case PLAN_LENGTHS:
			return "PlanLengths";
		case CONFIGURATION:
			return "Configuration";
		default:
			return "";
		}
	}

	public static EvaluationTable parseFromString(String tableName) {
		for (EvaluationTable table : EvaluationTable.values()) {
			if (tableName.contains(EvaluationTable.getTableName(table))) {
				return table;
			}
		}
		return null;
	}

	public static ColumnType[] getTableSchema(EvaluationTable evaluationTable) {
		switch (evaluationTable) {
		case SUMMARY:
			return new ColumnType[] { ColumnType.EXPERIMENT_NAME, ColumnType.COMPUTATION_TIME, ColumnType.TRUE_HYPOTHESIS };
		case TOTAL_COMPUTATION_TIMES:
			return new ColumnType[] { ColumnType.EXPERIMENT_NAME, ColumnType.COMPUTATION_TIME };
		case EXPERIMENT_RESULT:
			return new ColumnType[] { ColumnType.OBSERVATION_STEP, ColumnType.HYPOTHESIS, ColumnType.GOAL_PROBABILITY };
		case EXPERIMENT_COMPUTATION_TIMES:
			return new ColumnType[] { ColumnType.COMPUTATION_TIME_KEY, ColumnType.COMPUTATION_TIME };
		case PLAN_LENGTHS:
			return new ColumnType[] { ColumnType.PLAN_LENGTH_KEY, ColumnType.PLAN_LENGTH };
		case CONFIGURATION:
			return new ColumnType[] { ColumnType.ATTRIBUTE_NAME, ColumnType.ATTRIBUTE_VALUE };
		default:
			return null;
		}
	}

}

package main;

public enum ExecutionMode {
    EXPERIMENT,
    RESULT_EVALUATION,
    COMPUTATION_TIME_EVALUATION,
    TABLE_GENERATOR;

    public static ExecutionMode parseExecutionModeFromString(String executionMode) {
        switch(executionMode.toLowerCase()) {
            case "experiment":
                return EXPERIMENT;
            case "result_evaluation":
                return RESULT_EVALUATION;
            case "computation_time_evaluation":
                return COMPUTATION_TIME_EVALUATION;
            case "table_generator":
                return TABLE_GENERATOR;
        }
        System.err.println("No valid execution type given! Please choose one of experiment, result_evaluation, computation_time_evaluation, or table_generator");
        System.exit(0);
        return null;
    }
}

package config;

public enum Configuration {
	GOAL_RECOGNITION_CONFIGURATION,
	EVALUATION_CONFIGURATION;
	
	public static String getConfigName(Configuration config) {
		switch(config) {
		case GOAL_RECOGNITION_CONFIGURATION:
			return "GoalRecognitionConfiguration";
		case EVALUATION_CONFIGURATION:
			return "EvaluationConfiguration";
		default:
			return "";
		}
	}

	public static String[] getConfigs() {

		String[] configs = new String[values().length];
		for(int i=0;i<configs.length;i++){
			configs[i] = values()[i].toString();
		}
		return configs;
	}

	public static Class getConfigClass(Configuration config) {
		switch(config) {
		case GOAL_RECOGNITION_CONFIGURATION:
			return GoalRecognitionConfiguration.class;
		case EVALUATION_CONFIGURATION:
			return EvaluationConfiguration.class;
		default:
			return null;
		}
	}
}

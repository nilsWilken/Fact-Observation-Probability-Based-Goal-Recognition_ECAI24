package config.plannerConfiguration;

import config.GoalRecognitionConfiguration;
import symbolic.planning.planners.classicPlanners.IClassicalPlanner;
import symbolic.planning.planners.classicPlanners.MetricFFPlanner;

public enum AvailablePlanners {
	
	METRIC_FF;
	
	public static IClassicalPlanner getActivePlanner() {
		if(GoalRecognitionConfiguration.ACTIVE_PLANNER == null) {
			return null;
		}
		switch(GoalRecognitionConfiguration.ACTIVE_PLANNER) {
		case METRIC_FF:
			return new MetricFFPlanner();
		default:
			return new MetricFFPlanner();
		}
	}

}

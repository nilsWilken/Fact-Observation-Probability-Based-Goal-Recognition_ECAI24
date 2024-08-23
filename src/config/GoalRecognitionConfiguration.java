package config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import config.plannerConfiguration.AvailablePlanners;
import experiment.recognitionExperiments.FDLandmarkBased.LandmarkExtractionType;
import experiment.recognitionExperiments.FDLandmarkBased.LandmarkGoalRecognitionHeuristic;
import symbolic.relaxedPlanning.bestSupporterFunctions.BestSupporterSelectionFunction;

public class GoalRecognitionConfiguration {

    // ####################### GENERAL GOAL RECOGNITION CONFIGURATION ###########################
	public static String RELAXED_PLANNING_HEURISTIC = "greedy";
	public static String EXPERIMENT_TYPE;

	public static String CURRENT_STATE_HANDLER;

	public static String PLANNING_SETUP_DIR_NAME;
	public static String PLANNING_COMMON_FILES_DIR_NAME;

	public static String SERIALIZED_AVERAGE_PERFORMANCE_FILE_NAME;
	public static String SERIALIZED_AVERAGE_PRAP_PERFORMANCE_FILE_NAME;

	public static String CSV_SEPARATOR;

	public static Double[] OBS_PERCENTAGES;
	public static double PLAN_RECOGNITION_BETA;

	public static LandmarkExtractionType LANDMARK_EXTRACTION_TYPE;

	// Latex output configs
	public static String PRAP_COLOR;
	public static String PRAP_MARK;
	public static int ROUNDING_SCALE;

	public static boolean USE_INITIAL_STATE_LANDMARKS;
	public static boolean SIMPLIFY_GROUNDING;
	public static String LANDMARK_RECOGNITION_HEURISTIC;
	public static String MODDED_FD_PATH;

	public static String MAXIMUM_EXTERNAL_PROCESS_MEMORY;
	public static String EXECUTE_EXTERNAL_COMMAND_SCRIPT_LOCATION;

	public static boolean USE_EXTERNAL_EXECUTION_SCRIPT;
	public static boolean USE_BASH_C;

	public static boolean USE_COMPLETE_OBSERVATION_SEQUENCE;

	public static int FPV_ITERATIONS;
	public static int FPV_L_NORM;

	public static String LP_TEST_INSTANCE_PATH;

    public static String generateGenericHypName(int counter) {
		return "hyp" + counter;
	}

	public static String generateInitialResultDirName() {
		return "initialProblemResults";
	}

	public static String generateObsResultDirName(int obsCounter) {
		return "obs" + obsCounter;
	}

	public static String generateHypResultDirName(int hypCounter) {
		return GoalRecognitionConfiguration.generateGenericHypName(hypCounter);
	}

	public static double calculateLikelihoodRatio(double planCostO, double planCostNotO) {
		if (planCostO == 0) {
			return 0.0;
		}
		if (planCostNotO == 0) {
			return 1.0;
		}

		double likelihoodRatio = planCostO - planCostNotO;
		likelihoodRatio = (Math.exp((-GoalRecognitionConfiguration.PLAN_RECOGNITION_BETA) * likelihoodRatio)
				/ (1 + Math.exp((-GoalRecognitionConfiguration.PLAN_RECOGNITION_BETA) * likelihoodRatio)));

		System.out.println(likelihoodRatio);
		return likelihoodRatio;
	}

	/**
	 * Calculates the probabilities for all hypotheses from the likelihood ratios of
	 * the hypotheses.
	 * 
	 * @param likelihoodRatios Map of the likelihood ratios. Keys correspond to
	 *                         hypotheses.
	 * @return Map of the probabilities. Keys correspond to the hypotheses.
	 */
	public static Map<Integer, Double> calculateProbabilitiesFromLikelihoods(Map<Integer, Double> likelihoodRatios) {
		Map<Integer, Double> result = new HashMap<Integer, Double>();
		double likelihoodSum = 0;

		for (int key : likelihoodRatios.keySet()) {
			likelihoodSum += likelihoodRatios.get(key).doubleValue();
		}

		for (int key : likelihoodRatios.keySet()) {
			if (likelihoodSum > 0) {
				result.put(key, likelihoodRatios.get(key) / likelihoodSum);
			} else {
				result.put(key, 0.0);
			}
		}

		return result;
	}

	public static int getExpNumberFromExperimentDatabaseName(String expDatabaseName) {
		return Integer.parseInt(expDatabaseName.split("_")[2]);
	}

	public static LandmarkGoalRecognitionHeuristic getLandmarkHeuristic() {
		return LandmarkGoalRecognitionHeuristic.parseFromString(GoalRecognitionConfiguration.LANDMARK_RECOGNITION_HEURISTIC);
	}

	public ArrayList<ArrayList> getConfigFields() throws IllegalAccessException {

		ArrayList configFields = new ArrayList();

		Class c = this.getClass();
		Field[] fields = c.getDeclaredFields();
		for(int i = 0; i< fields.length; i++) {

			ArrayList field = new ArrayList();


			String name = fields[i].getName();
			String type = fields[i].getType().getSimpleName();
			Object value = fields[i].get(this);
			Object[] options = null;

			field.add(name);
			field.add(type);
			field.add(value);
			field.add(options);

			configFields.add(field);

		}

		return configFields;
	}

    /**
	 * Specifies how long the main thread is paused between checks of max waiting
	 * time and whether the planning process already finished.
	 */
	public static long WAITING_TIME_MILLISECONDS;

	/**
	 * Max runtime of the planner for one planning problem.
	 */
	public static long MAX_TIME_MILLISECONDS;

	/**
	 * Specifies which planner is used.
	 */
	public static AvailablePlanners ACTIVE_PLANNER;

	// MetricFF CONFIGURATION SETTINGS

	/**
	 * Location of the compiled MetricFF planner
	 */
	public static String METRICFF_PLANNER_LOCATION;

	/**
	 * Specifies the strategy that MetricFF uses to solve the problem
	 */
	public static String METRICFF_PLANNER_STRATEGY;
	
	public static boolean DELETE_PLAN_AND_LOG_FILES;

	

	//RG09 CONFIGURATION SETTINGS
	public static String PR2PLAN_LOCATION;

	public static String SUBOPT_PR_LOCATION;

    // ####################### PLANNER CONFIGURATION ###########################
    public static String DOMAIN_FILE_NAME;
	public static String GROUNDED_DOMAIN_FILE_NAME;
	public static String TEMPLATE_FILE_NAME;
	public static String ALL_HYPS_FILE_NAME;
	public static String REAL_HYP_FILE_NAME;
	public static String OBS_FILE_NAME;
	public static String RESULTS_DIR_NAME;
	public static String ARFF_OUTPUT_DIR_NAME;
	public static String TRUE_HYP_FILE_NAME;

	public static String PLANNING_RESULTS_DIR_NAME;
	public static String SERIALIZED_PLANNING_RESULTS_FILE_NAME;

	public static int MAX_PLANNING_THREADS;

	public static int maxPlanningThreads;

	public static double MODEL_PROBABILITY_THRESHOLD;


	public static int MAX_PLANNING_THREADS() {
		return GoalRecognitionConfiguration.maxPlanningThreads;
	}

	public static boolean RUN_PLANNER;

	public static String generateInitialProblemFileName(String expName, int hypCounter) {
		return expName + "_" + GoalRecognitionConfiguration.generateGenericHypName(hypCounter) + "_problem.pddl";
	}

	public static String generateObsProblemFileName(String expName, int hypCounter, int obsCounter) {
		return expName + "_" + GoalRecognitionConfiguration.generateHypCounterProblemFileName(hypCounter) + "_obs" + obsCounter + "_problem.pddl";
	}

	public static String generateObsGoalProblemFileName(String expName, int hypCounter, int obsCounter) {
		return expName + "_" + GoalRecognitionConfiguration.generateHypCounterProblemFileName(hypCounter) + "_obs" + obsCounter + "_goal_problem.pddl";
	}

	public static String generateObsNotGoalProblemFileName(String expName, int hypCounter, int obsCounter) {
		return expName + "_" + GoalRecognitionConfiguration.generateHypCounterProblemFileName(hypCounter) + "_obs" + obsCounter + "_notGoal_problem.pddl";
	}

	public static String generateObsDomainFileName(String expName, int obsCounter) {
		return expName + "_obs" + obsCounter + "_domain_grounded.pddl";
	}

	public static String generateObsDomainFileName(String expName, int hypCounter, int obsCounter) {
		return expName + "_hyp" + hypCounter + "_obs" + obsCounter + "_domain.pddl";
	}

	public static String generateHypSetupDirName(int hypCounter) {
		return "problems_hyp" + hypCounter;
	}

	public static String generateHypDomainDirName(int hypCounter) {
		return "domains_hyp" + hypCounter;
	}

	public static String generateGroundedDomainsDirName() {
		return "groundedDomains";
	}

	public static String generateHypCounterProblemFileName(int hypCounter) {
		return GoalRecognitionConfiguration.generateGenericHypName(hypCounter);
	}

	public static String generateHypPlanFileName(int hypCounter) {
		return GoalRecognitionConfiguration.generateGenericHypName(hypCounter) + "_plan.txt";
	}

	public static String generateHypPlanDirName(int hypCounter) {
		return GoalRecognitionConfiguration.generateGenericHypName(hypCounter) + "_plans";
	}

	public static String generateHypLogFileName(int hypCounter) {
		return GoalRecognitionConfiguration.generateGenericHypName(hypCounter) + ".log";
	}

	public static String generateProbabilityReportFileName(int obsCounter) {
		return "goal_probabilities_obs" + obsCounter + ".dat";
	}

	public static String generateSampledPlansDirNameForGoal(String goal) {
		return "sampledPlans_" + goal;
	}

	public static String generateSampledPlanFileName(String goal, int counter) {
		return goal + "_" + counter + ".dat";
	}

	public static BestSupporterSelectionFunction getRelaxedPlanningHeuristic() {
		return BestSupporterSelectionFunction.parseFromString(GoalRecognitionConfiguration.RELAXED_PLANNING_HEURISTIC);
	}
    
}

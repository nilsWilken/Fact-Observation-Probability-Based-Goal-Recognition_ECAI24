package experiment.recognitionExperiments.abstractExperiment;

import java.io.File;
import java.util.Map;

import config.plannerConfiguration.AvailablePlanners;
import symbolic.planning.planners.classicPlanners.IClassicalPlanner;
import symbolic.planning.planningUtils.PDDLProblemTemplate;
import utils.DirectoryManager;

public abstract class AbstractPRAPGoalRecognitionExperiment {
	public String name;
	protected DirectoryManager dirManager;
	protected Map<Integer, String> hypMap;
	protected int numberObs;
	protected PDDLProblemTemplate problemTemplate;
	protected IClassicalPlanner planner;

	public AbstractPRAPGoalRecognitionExperiment(String name, String rootDir) {
		this.name = name;
		this.dirManager = DirectoryManager.getInstance(new File(rootDir));

		this.problemTemplate = new PDDLProblemTemplate(this.dirManager.getTemplateFile(this.name));
		
		this.planner = AvailablePlanners.getActivePlanner();
	}
	
	public boolean checkObservationFile() {
		return this.dirManager.getObsFile(this.name).exists();
	}

	public void runPlannerForInitialProblem(int hypCounter) {
		this.planner.runPlanner(this.dirManager.getGroundedDomainFile(), this.dirManager.getInitialProblemFile(this.name, hypCounter), this.dirManager.getInitialPlanFile(hypCounter, this.name), this.dirManager.getInitialPlanningLogFile(hypCounter, this.name));
	}

	public void runPlannerForObsProblem(int obsCounter, int hypCounter) {
		this.planner.runPlanner(this.dirManager.getGroundedDomainFile(), this.dirManager.getProblemFileForObs(this.name, hypCounter, obsCounter), this.dirManager.getObsHypPlanFile(obsCounter, hypCounter, this.name), this.dirManager.getPlanningObsHypLogFile(hypCounter, obsCounter, this.name));
	}
	
	public void runPlannerWithObsDomain(int obsCounter, int hypCounter) {
		this.planner.runPlanner(this.dirManager.getDomainFileForObs(this.name, obsCounter), this.dirManager.getInitialProblemFile(this.name, hypCounter), this.dirManager.getObsHypPlanFile(obsCounter, hypCounter, this.name), this.dirManager.getPlanningObsHypLogFile(hypCounter, obsCounter, this.name));
	}
	
	public void runPlannerWithObsDomainAndGoalProblem(int obsCounter, int hypCounter) {
		this.planner.runPlanner(this.dirManager.getDomainFileForObs(this.name, obsCounter), this.dirManager.getGoalProblemFileForObs(this.name, hypCounter, obsCounter), this.dirManager.getObsHypPlanFile(obsCounter, hypCounter, this.name), this.dirManager.getPlanningObsHypLogFile(hypCounter, obsCounter, this.name));
	}
	
	public void runPlannerWithObsDomainAndNotGoalProblem(int obsCounter, int hypCounter) {
		this.planner.runPlanner(this.dirManager.getDomainFileForObs(this.name, obsCounter), this.dirManager.getNotGoalProblemFileForObs(this.name, hypCounter, obsCounter), this.dirManager.getObsHypPlanFile(obsCounter, hypCounter, this.name), this.dirManager.getPlanningObsHypLogFile(hypCounter, obsCounter, this.name));
	}
	
	public String getExperimentName() {
		return this.name;
	}
	
	public static String generateComputationTimesKeyInitial(int hypCounter) {
		return "initialTime_" + hypCounter;
	}
	
	public static boolean isComputationTimesKeyInitial(String key) {
		if(key.split("_").length == 2 && key.split("_")[0].equals("initialTime")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromComputationTimesKeyInitial(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyInitial(key)) {
			return Integer.parseInt(key.split("_")[1]);
		}
		return -1;
	}
	
	public static String generateComputationTimesKeyGMMS(int hypCounter, int obsCounter) {
		return "computationTime_" + hypCounter + "_" + obsCounter;
	}
	
	public static boolean isComputationTimesKeyGoalMirroring(String key) {
		if(key.split("_").length == 3 && key.split("_")[1].matches("\\d")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromComputationTimesKeyGoalMirroring(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyGoalMirroring(key)) {
			return Integer.parseInt(key.split("_")[1]);
		}
		return -1;
	}
	
	public static int getObsCounterFromComputationTimesKeyGoalMirroring(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyGoalMirroring(key)) {
			return Integer.parseInt(key.split("_")[2]);
		}
		return -1;
	}
	
	public static String generateComputationTimesKeyO(int hypCounter, int obsCounter) {
		return "computationTime_O_" + hypCounter + "_" + obsCounter;
	}
	
	public static boolean isComputationTimesKeyO(String key) {
		if(key.split("_").length == 4 && key.split("_")[1].equals("O")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromComputationTimesKeyO(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyO(key)) {
			return Integer.parseInt(key.split("_")[2]);
		}
		return -1;
	}
	
	public static int getObsCounterFromComputationTimesKeyO(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyO(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}
	
	public static String generateComputationTimesKeyNotO(int hypCounter, int obsCounter) {
		return "computationTime_notO_" + hypCounter + "_" + obsCounter;
	}
	
	public static boolean isComputationTimesKeyNotO(String key) {
		if(key.split("_").length == 4 && key.split("_")[1].equals("notO")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromComputationTimesKeyNotO(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyNotO(key)) {
			return Integer.parseInt(key.split("_")[2]);
		}
		return -1;
	}
	
	public static int getObsCounterFromComputationTimesKeyNotO(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyNotO(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}
	
	public static String generateComputationTimesKeyLandmarkComputation(int hypCounter) {
		return "computationTime_landmark_" + hypCounter;
	}

	public static String generateComputationTimesKeyRelaxedPlanComputation(int hypCounter, int obsCounter) {
		return "computationTime_relaxedPlan_" + hypCounter + "_" + obsCounter;
	}

	public static boolean isComputationTimesKeyRelaxedPlanComputation(String key) {
		if(key.split("_").length == 4 && key.split("_")[1].equals("relaxedPlan")) {
			return true;
		}
		return false;
	}

	public static int getHypCounterFromComputationTimesKeyRelaxedPlanComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRelaxedPlanComputation(key)) {
			return Integer.parseInt(key.split("_")[2]);	
		}
		return -1;
	}

	public static int getObsCounterFromComputationTimesKeyRelaxedPlanComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRelaxedPlanComputation(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}

	public static String generateComputationTimesKeyRG09Translation(int hypCounter, int obsCounter) {
		return "computationTime_rg09translation_" + hypCounter + "_" + obsCounter;
	}

	public static boolean isComputationTimesKeyRG09Translation(String key) {
		if(key.split("_").length == 4 && key.split("_")[1].equals("rg09translation")) {
			return true;
		}
		return false;
	}

	public static int getHypCounterFromComputationTimesKeyRG09Translation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRG09Translation(key)) {
			return Integer.parseInt(key.split("_")[2]);
		}
		return -1;
	}

	public static int getObsCounterFromComputationTimesKeyRG09Translation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRG09Translation(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}
	
	public static boolean isComputationTimesKeyLandmarkComputation(String key) {
		if(key.split("_").length == 3 && key.split("_")[1].equals("landmark")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromComputationTimesKeyLandmarkComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLandmarkComputation(key)) {
			return Integer.parseInt(key.split("_")[2]);
		}
		return -1;
	}
	
	public static String generateComputationTimesKeyLandmarkHeuristicComputation(int hypCounter, int obsCounter) {
		return "computationTime_landmark_heuristic_" + hypCounter + "_" + obsCounter;
	}

	public static String generateComputationTimesKeyRelaxedPlanHeuristicComputation(int hypCounter, int obsCounter) {
		return "computationTime_relaxedPlan_heuristic_" + hypCounter + "_" + obsCounter;
	}

	public static String generateComputationTimesKeyGraphInteractionHeuristicComputation(int hypCounter, int obsCounter) {
		return "computationTime_graphInteraction_heuristic_" + hypCounter + "_" + obsCounter;
	}

	public static String generateComputationTimesKeyGraphBasedComputation(int hypCounter, int obsCounter) {
		return "computationTime_graph_based_" + hypCounter + "_" + obsCounter;
	}
	
	public static boolean isComputationTimesKeyLandmarkHeuristicComputation(String key) {
		if(key.split("_").length == 5 && key.split("_")[2].equals("heuristic")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromTimesKeyLandmarkHeuristicComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLandmarkHeuristicComputation(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}
	
	public static int getObsCounterFromTimesKeyLandmarkHeuristicComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLandmarkHeuristicComputation(key)) {
			return Integer.parseInt(key.split("_")[4]);
		}
		return -1;
	}

	public static String generateComputationTimesKeyRelaxedRepresentationComputation(int hypCounter) {
		return "computationTime_relaxed_representation_" + hypCounter;
	}

	public static boolean isComputationTimesKeyRelaxedRepresentationComputation(String key) {
		if(key.split("_").length == 4 && key.split("_")[2].equals("representation")) {
			return true;
		}
		return false;
	}

	public static int getHypCounterFromTimesKeyRelaxedRepresentationComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyRelaxedRepresentationComputation(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}

	public static String generateComputationTimesKeyFactObservationProbabilitiesComputation(int hypCounter) {
		return "computationTime_fact_observation_probability_" + hypCounter;
	}

	public static boolean isComputationTimesKeyFactObservationProbabilityComputation(String key) {
		if(key.split("_").length == 5 && key.split("_")[1].equals("fact")) {
			return true;
		}
		return false;
	}

	public static int getHypCounterFromTimesKeyFactObservationProbabilityComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyFactObservationProbabilityComputation(key)) {
			return Integer.parseInt(key.split("_")[4]);
		}
		return -1;
	}

	public static String generateComputationTimesKeySimilarityComputation(int hypCounter, int obsCounter) {
		return "computationTime_similarity_computation_" + hypCounter + "_" + obsCounter;
	}

	public static boolean isComputationTimesKeySimilarityComputation(String key) {
		if(key.split("_").length == 5 && key.split("_")[1].equals("similarity")) {
			return true;
		}
		return false;
	}

	public static int getHypCounterFromComputationTimesKeySimilarityComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeySimilarityComputation(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}

	public static int getObsCounterFromComputationTimesKeySimilarityComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeySimilarityComputation(key)) {
			return Integer.parseInt(key.split("_")[4]);
		}
		return -1;
	}

	public static String generateComputationTimesKeyFactObservationProbabilityHeuristicComputation(int hypCounter, int obsCounter) {
		return "computationTime_fact_observation_probability_heuristic_computation_" + hypCounter + "_" + obsCounter;
	}

	public static boolean isComputationTimesKeyFactObservationProbabilityHeuristicComputation(String key) {
		if(key.split("_").length == 8 && key.split("_")[4].equals("heuristic")) {
			return true;
		}
		return false;
	}

	public static int getHypCounterFromComputationTimesKeyFactObservationProbabilityHeuristicComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyFactObservationProbabilityHeuristicComputation(key)) {
			return Integer.parseInt(key.split("_")[6]);
		}
		return -1;
	}

	public static int getObsCounterFromComputationTimesKeyFactObservationProbabilityHeuristicComputation(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyFactObservationProbabilityHeuristicComputation(key)) {
			return Integer.parseInt(key.split("_")[7]);
		}
		return -1;
	}
	
	public static String generateGMMSInitialPlanLengthKey(int hypCounter) {
		return "initialPlanLength_" + hypCounter;
	}
	
	public static boolean isGoalMirroringInitialPlanLengthKey(String key) {
		if(key.split("_").length == 2 && key.split("_")[0].equals("initialPlanLength")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromGoalMirroringInitialPlanLengthKey(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isGoalMirroringInitialPlanLengthKey(key)) {
			return Integer.parseInt(key.split("_")[1]);
		}
		return -1;
	}
	
	public static String generateGMMSPlanLengthKey(int hypCounter, int obsCounter) {
		return "planLength_" + hypCounter + "_" + obsCounter;
	}
	
	public static boolean isGoalMirroringPlanLengthKey(String key) {
		if(key.split("_").length == 3 && key.split("_")[0].equals("planLength")) {
			return true;
		}
		return false;
	}
	
	public static int getHypCounterFromGoalMirroringPlanLengthKey(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isGoalMirroringPlanLengthKey(key)) {
			return Integer.parseInt(key.split("_")[1]);
		}
		return -1;
	}
	
	public static int getObsCounterFromGoalMirroringPlanLengthKey(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isGoalMirroringPlanLengthKey(key)) {
			return Integer.parseInt(key.split("_")[2]);
		}
		return -1;
	}
	
	public static String generateComputationTimesKeyLandmarkTotalTime() {
		return "computationTime_landmark_total";
	}

	public static String generateComputationTimesKeyLPBased(int hypCounter, int obsCounter) {
		return "computationTime_lpbased_total_" + hypCounter + "_" + obsCounter;
	}

	public static boolean isComputationTimesKeyLPBased(String key) {
		String[] split = key.split("_");
		if(split[1].equals("lpbased") && split.length == 5) {
			return true;
		}
		return false;
	}

	public static int getHypCounterFromComputationTimesKeyLPBased(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLPBased(key)) {
			return Integer.parseInt(key.split("_")[3]);
		}
		return -1;
	}

	public static int getObsCounterFromComputationTimesKeyLPBased(String key) {
		if(AbstractPRAPGoalRecognitionExperiment.isComputationTimesKeyLPBased(key)) {
			return Integer.parseInt(key.split("_")[4]);
		}
		return -1;
	}
}

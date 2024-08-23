package experiment.recognitionExperiments.FDLandmarkBased;

import java.io.File;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import com.hstairs.ppmajal.conditions.ComplexCondition;
import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;

import config.GoalRecognitionConfiguration;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.ResultObsStep;
import experiment.recognitionExperiments.abstractExperiment.AbstractEvaluationPRAPGoalRecognitionExperiment;
import experiment.recognitionExperiments.abstractExperiment.AbstractPRAPGoalRecognitionExperiment;
import symbolic.planning.stateHandler.obsConversion.GMPDDLStateHandler;
import symbolic.landmarkExtraction.LGG;
import symbolic.landmarkExtraction.LandmarkExtraction;
import utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction.ExhaustiveLandmarkGenerator;
import utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction.HMLandmarkGenerator;
import utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction.HoffmannOrdersLandmarkGenerator;
import utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction.ILandmarkGenerator;
import utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction.RHWLandmarkGenerator;
import utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction.ZGLandmarkGenerator;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class FDLandmarkBasedExperiment extends AbstractEvaluationPRAPGoalRecognitionExperiment implements Callable<FDLandmarkBasedExperiment> {
    private Map<Integer, Map<Integer, Double>> goalScores;
	
	private Map<String, Collection<GroundAction>> filteredActionsPerGoal;
	private Map<String, LandmarkExtraction> landmarkExtractionPerGoal;
	private Map<ComplexCondition, LGG> landmarksPerGoal;
	private Map<ComplexCondition, Map<Condition, List<Integer>>> liftedLandmarkMappingsPerGoal;
	private Map<ComplexCondition, Map<Integer, String[]>> liftedLandmarksPerGoal;
	private Set<Predicate> initialStatePredicates;
	private EPddlProblem problem;
	Map<Integer, Double> uniquenessScores;
	
	private Map<Integer, Long> landmarkComputationTimes;
	private Map<Integer, Long> heuristicComputationTimes;
	private long totalComputationTime;

	private Map<ComplexCondition, Graph<String, DefaultEdge>> landmarkGraphPerGoal;
	
	public FDLandmarkBasedExperiment(String name, String rootDir) {
		super(name, rootDir);
		
		this.goalScores = new HashMap<Integer, Map<Integer, Double>>();
		
		this.landmarkComputationTimes = new HashMap<Integer, Long>();
		this.heuristicComputationTimes = new HashMap<Integer, Long>();
	}
	
	public FDLandmarkBasedExperiment(String name, String rootDir, Map<String, Collection<GroundAction>> filteredActions) {
		super(name, rootDir);
		
		this.goalScores = new HashMap<Integer, Map<Integer, Double>>();
		
		this.filteredActionsPerGoal = filteredActions;
		
		this.landmarkComputationTimes = new HashMap<Integer, Long>();
		this.heuristicComputationTimes = new HashMap<Integer, Long>();
	}

	@Override
	public FDLandmarkBasedExperiment call() {
		this.runExperiment();
		return this;
	}
	
	public Map<ComplexCondition, LGG> getLandmarksPerGoal() {
		return this.landmarksPerGoal;
	}

	public Map<ComplexCondition, Map<Integer, String[]>> getLiftedLandmarksPerGoal() {
		return this.liftedLandmarksPerGoal;
	}

	public Map<ComplexCondition, Map<Condition, List<Integer>>> getLiftedLandmarkMappingsPerGoal() {
		return this.liftedLandmarkMappingsPerGoal;
	}

	public Map<ComplexCondition, Graph<String, DefaultEdge>> getLandmarkGraphPerGoal() {
		return this.landmarkGraphPerGoal;
	}
	
	public void setLandmarkGraphPerGoal(Map<ComplexCondition, Graph<String, DefaultEdge>> landmarkGraphPerGoal) {
		this.landmarkGraphPerGoal = landmarkGraphPerGoal;
	}
	
	public void setLandmarksPerGoal(Map<ComplexCondition, LGG> landmarksPerGoal) {
		this.landmarksPerGoal = landmarksPerGoal;
	}

	public void setLiftedLandmarksPerGoal(Map<ComplexCondition, Map<Integer, String[]>> liftedLandmarksPerGoal) {
		this.liftedLandmarksPerGoal = liftedLandmarksPerGoal;
	}

	public void setLiftedLandmarkMappingsPerGoal(Map<ComplexCondition, Map<Condition, List<Integer>>> liftedLandmarkMappingsPerGoal) {
		this.liftedLandmarkMappingsPerGoal = liftedLandmarkMappingsPerGoal;
	}
	
	private void runExperiment() {
		long totalStartTime = System.currentTimeMillis();
		
		//Generate problem files (basically replace the goals in the given template)
		this.generateProblemFiles();
		
		//Compute landmarks for all goals
		this.computeLandmarks();
		
		try {
			//Read observations for this experiment from file
			GMPDDLStateHandler stateHandler = new GMPDDLStateHandler(this.dirManager.getDomainFile().getAbsolutePath(), this.dirManager.getInitialProblemFile(this.name, 0).getAbsolutePath());
			List<GroundAction> observations = stateHandler.retrieveActionListFromObsFile(this.dirManager.getObsFile(this.name).getAbsolutePath());
			
			//Compute landmark based goal scores for each amount of observations (starting at 1 to max amount)
			List<GroundAction> cObservations = new ArrayList<GroundAction>();
			Map<Integer, Double> goalScores;
			for(int i=0; i < observations.size(); i++) {
				cObservations.add(observations.get(i));
				
				long startTime = System.currentTimeMillis();
				goalScores = this.getGoalScoresForObs(cObservations);

				this.heuristicComputationTimes.put(i, System.currentTimeMillis()-startTime);
				
				this.goalScores.put(i+1, goalScores);	
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e);
		}
		
		this.totalComputationTime = System.currentTimeMillis() - totalStartTime;
	}
	
	@Override
	public void generateProblemFiles() {
		this.hypMap = new HashMap<Integer, String>();
		
		File outputDir;
		String initialProblemFilePath = "";
		
		List<String> initialProblemFiles = new ArrayList<String>();
		
		int hypCounter = 0;
		
		for(String hyp : this.dirManager.getPlanningHyps()) {
			outputDir = this.dirManager.generateProblemDir(this.name, hypCounter);
						
			if(outputDir.listFiles().length == 0) {
				initialProblemFilePath = this.problemTemplate.generateInitialProblemFile(hyp, outputDir.getAbsolutePath(), this.name, hypCounter);
				initialProblemFiles.add(initialProblemFilePath);
			}
			
			this.dirManager.addProblemDir(this.name, hypCounter, outputDir);
			this.hypMap.put(hypCounter, hyp);
			hypCounter++;
		}
	}

	@Override
	public void generateGoalProbabilityReports() {
		
	}

	@Override
	public ExperimentResult generateExperimentResult() {
		ExperimentResult expResult = new ExperimentResult(this.name);
		expResult.setTrueHyp(this.dirManager.getTrueTextGoalForExperiment(this.name));

		ResultObsStep cStep;
		for (int obs : this.goalScores.keySet()) {
			cStep = new ResultObsStep();
			for (int hyp : this.hypMap.keySet()) {
				cStep.addGoalProbability(this.dirManager.getTextGoalFromPlanningGoal(this.hypMap.get(hyp)),
						this.goalScores.get(obs).get(hyp));
			}
			expResult.addResultStep(obs, cStep);
		}

		for (int hyp : this.hypMap.keySet()) {
			if (this.landmarkComputationTimes.get(hyp) != null) {
				expResult.addComputationTimeInMillis(
						AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyLandmarkComputation(hyp),
						this.landmarkComputationTimes.get(hyp));
			}
			for (int obs : this.heuristicComputationTimes.keySet()) {
				expResult
						.addComputationTimeInMillis(
								AbstractPRAPGoalRecognitionExperiment
										.generateComputationTimesKeyLandmarkHeuristicComputation(hyp, obs),
								(this.heuristicComputationTimes.get(obs) / this.hypMap.keySet().size()));
			}
		}
		//		expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyLandmarkTotalTime(), totalComputationTime);
		expResult.setTotalComputationTimeInMillis(this.totalComputationTime);
		return expResult;
	}
	
	private void computeLandmarks() {
		if(this.liftedLandmarksPerGoal != null && this.liftedLandmarkMappingsPerGoal != null) {
			System.out.println("Landmarks already calculated!");
			this.initializeProblemInstance();
			return;
		}

		this.landmarkExtractionPerGoal = new HashMap<String, LandmarkExtraction>();
		this.liftedLandmarkMappingsPerGoal = new HashMap<ComplexCondition, Map<Condition, List<Integer>>>();
		this.liftedLandmarksPerGoal = new HashMap<ComplexCondition, Map<Integer, String[]>>();
		this.landmarkGraphPerGoal = new HashMap<ComplexCondition, Graph<String, DefaultEdge>>();
		
        ILandmarkGenerator lmGen;
		try {
			int liftedCount = 0;
			int conjCount = 0;
			int disjCount = 0;
			int groundedCount = 0;
			int[] counts;
			List<Integer> lmCounts = new ArrayList<Integer>();
			for (int hypCounter : this.hypMap.keySet()) {
                //Run landmark generator
				switch(GoalRecognitionConfiguration.LANDMARK_EXTRACTION_TYPE) {
					case EXHAUSTIVE:
						lmGen = new ExhaustiveLandmarkGenerator();
						break;
					case HM:
						lmGen = new HMLandmarkGenerator();
						break;
					case RHW:
						lmGen = new RHWLandmarkGenerator();
						break;
					case ZG:
						lmGen = new ZGLandmarkGenerator();
						break;
					case HOFFMANN_ORDERS_EXHAUSTIVE:
					case HOFFMANN_ORDERS_HM:
					case HOFFMANN_ORDERS_RHW:
					case HOFFMANN_ORDERS_ZG:
						lmGen = new HoffmannOrdersLandmarkGenerator();
						break;
					default:
						lmGen = new HoffmannOrdersLandmarkGenerator();
						break;
				}
                lmGen.runLandmarkGenerator(this.dirManager.getDomainFile(), this.dirManager.getInitialProblemFile(this.name, hypCounter), new File("lmGen.log"));
					
				//Move landmark file
				String probFileDir = this.dirManager.getProblemFileDir(this.name, hypCounter).getAbsolutePath();
				
				File LMFile = Paths.get(probFileDir, "LMs.txt").toFile();
				if(LMFile.exists()) {
					LMFile.delete();
				}
				Files.move(Paths.get("lmGen.log"), Paths.get(LMFile.getAbsolutePath()), new CopyOption[0]);


				//Convert Landmarks per Goal into String Array
				Map<Integer, String[]> landmarks = lmGen.getLandmarks();


				PddlDomain domain = new PddlDomain(this.dirManager.getDomainFile().getAbsolutePath());
				EPddlProblem problem = new EPddlProblem(this.dirManager.getInitialProblemFile(this.name, hypCounter).getAbsolutePath(), domain.getConstants(), domain.types, domain);

				domain.substituteEqualityConditions();

				problem.transformGoal();
				problem.groundingActionProcessesConstraints();
				problem.simplifyAndSetupInit(false, false);

				ComplexCondition goal = problem.getGoals();
				this.liftedLandmarkMappingsPerGoal.put(goal, LandmarkBasedGoalRecognitionUtils.translateLiftedStringLandmarks(landmarks, problem));
				this.liftedLandmarksPerGoal.put(goal, landmarks);
				this.landmarkComputationTimes.put(hypCounter, lmGen.getRuntime());

				if(GoalRecognitionConfiguration.LANDMARK_EXTRACTION_TYPE == LandmarkExtractionType.HOFFMANN_ORDERS_EXHAUSTIVE ||
					GoalRecognitionConfiguration.LANDMARK_EXTRACTION_TYPE == LandmarkExtractionType.HOFFMANN_ORDERS_HM ||
					GoalRecognitionConfiguration.LANDMARK_EXTRACTION_TYPE == LandmarkExtractionType.HOFFMANN_ORDERS_RHW ||
					GoalRecognitionConfiguration.LANDMARK_EXTRACTION_TYPE == LandmarkExtractionType.HOFFMANN_ORDERS_ZG) {
					
						this.landmarkGraphPerGoal.put(goal , ((HoffmannOrdersLandmarkGenerator)lmGen).getLandmarkGraph());
				}

				counts = this.getLandmarkCounts(landmarks);
				liftedCount += counts[0];
				conjCount += counts[1];
				disjCount += counts[2];
				groundedCount += counts[3];

				lmCounts.add(landmarks.size());
		    }


            //Initialize initial state predicates
			this.initializeProblemInstance();

			System.out.println("Landmark counts for " + this.name + ": ");
			for(int i = 0; i < lmCounts.size(); i++) {
				System.out.println(i + ": " + lmCounts.get(i));
			}

			System.out.println("\tLifted Landmark count: " + liftedCount);
			System.out.println("\tConjunctive Landmark count: " + conjCount);
			System.out.println("\tDisjunctive Landmark count: " + disjCount);
			System.out.println("\tGrounded Landmark count: " + groundedCount);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void initializeProblemInstance() {
		PddlDomain domain = new PddlDomain(this.dirManager.getDomainFile().getAbsolutePath());
		
		String problemFile = this.dirManager.getInitialProblemFile(this.name, 0).getAbsolutePath();
		EPddlProblem problem = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);

		domain.substituteEqualityConditions();

		try {
			problem.transformGoal();
			problem.groundingActionProcessesConstraints();
			problem.simplifyAndSetupInit(false, false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.problem = problem;
		this.initialStatePredicates = new LinkedHashSet<Predicate>();

		for (Predicate p : problem.getPredicatesInvolvedInInit()) {
			this.initialStatePredicates.add(p);
		}
	}
	
	private Map<Integer, Double> getGoalScoresForObs(List<GroundAction> obs)
			throws Exception {
		Map<ComplexCondition, Double> scores = new HashMap<ComplexCondition, Double>();
		
		switch(GoalRecognitionConfiguration.getLandmarkHeuristic()) {
		case COMPLETION:
			scores = LandmarkBasedGoalRecognitionUtils.computeCompletion(obs, this.problem, this.liftedLandmarkMappingsPerGoal, this.liftedLandmarksPerGoal, this.landmarkGraphPerGoal);
			break;
		case UNIQUENESS:
			if(this.uniquenessScores == null) {
				this.uniquenessScores = LandmarkBasedGoalRecognitionUtils.computeUniquenessScores(this.liftedLandmarkMappingsPerGoal);
			}
			scores = LandmarkBasedGoalRecognitionUtils.computeUniquenessHeuristic(obs, this.problem, this.liftedLandmarkMappingsPerGoal, this.uniquenessScores);
			break;
		default:
			break;
		}
		
		Map<Integer, Double> convertedScores = new HashMap<Integer, Double>();
		for(ComplexCondition goal : scores.keySet()) {
			for(int hyp : this.hypMap.keySet()) {
				if(this.removeSpacesFromHyp(this.hypMap.get(hyp)).equalsIgnoreCase(this.convertGoalConditionToString(goal))) {
					convertedScores.put(hyp, scores.get(goal));
					break;
				}
			}
		}
		
		return convertedScores;
	}

	
	private String convertGoalConditionToString(ComplexCondition goal) {
		String goalString = goal.toString().toLowerCase();
		goalString = goalString.replaceFirst("\\(and ", "");
		
		return goalString.substring(0, goalString.length()-1);
	}
	
	private String removeSpacesFromHyp(String hyp) {
		return hyp.replace(") (", ")(");
	}

	private int[] getLandmarkCounts(Map<Integer, String[]> landmarkSet) {
		int liftedCount = 0;
		int conjCount = 0;
		int disjCount = 0;
		int groundedCount = 0;

		String[] lm;
		for(int key : landmarkSet.keySet()) {
			// System.out.println("KEY: " + key);
			lm = landmarkSet.get(key);
			if((List.of(lm)).contains("?")) {
				liftedCount++;
			}
			else if(lm[0].equals("conj")) {
				conjCount++;
			}
			else if(lm[0].equals("disj")) {
				disjCount++;
			}
			else {
				groundedCount++;
			}
		}

		return new int[] {liftedCount, conjCount, disjCount, groundedCount};
	}
}

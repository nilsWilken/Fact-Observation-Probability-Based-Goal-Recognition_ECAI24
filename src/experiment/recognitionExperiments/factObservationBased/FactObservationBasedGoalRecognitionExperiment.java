package experiment.recognitionExperiments.factObservationBased;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;
import com.hstairs.ppmajal.conditions.Condition;

import config.GoalRecognitionConfiguration;
import dataDriven.bayes.netTranslation.bifXML.BNBIFXMLHandler;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.ResultObsStep;
import experiment.recognitionExperiments.abstractExperiment.AbstractEvaluationPRAPGoalRecognitionExperiment;
import experiment.recognitionExperiments.abstractExperiment.AbstractPRAPGoalRecognitionExperiment;
import symbolic.planning.stateHandler.planSampling.SimplePlanSamplingStateHandler;
import symbolic.vectorUtils.VectorOperations;
import symbolic.vectorUtils.VectorUtils;
import symbolic.vectorUtils.PDDLUtils;
import utils.ObsUtils;


public class FactObservationBasedGoalRecognitionExperiment extends AbstractEvaluationPRAPGoalRecognitionExperiment implements Callable<FactObservationBasedGoalRecognitionExperiment> {

	//Stores estimated goal vector similarities
	private Map<Integer, Map<Integer, Double>> goalSimilarities;
	
	//Stores computation times
	private Map<Integer, Long> factObservationProbabilitiesComputationTimes;
	private Map<Integer, Long> factObservationProbabilitiesHeuristicTimes;

	private List<Map<Integer, Map<String, Double>>> hypPredicatesMaps;
	private Map<Integer, Set<Predicate>> allPredicatesPerHyp;
	
	private boolean runOnlyOnLastObservation = false;
	
	public FactObservationBasedGoalRecognitionExperiment(String name, String rootDir) {
		super(name, rootDir);
	
		this.goalSimilarities = new HashMap<Integer, Map<Integer, Double>>();

		this.factObservationProbabilitiesComputationTimes = new HashMap<Integer, Long>();
		this.factObservationProbabilitiesHeuristicTimes = new HashMap<Integer, Long>();

		this.hypPredicatesMaps = null;
		this.allPredicatesPerHyp = null;
	}

	public void setRunOnlyOnLastObservation(boolean runOnlyOnLastObservation) {
		this.runOnlyOnLastObservation = runOnlyOnLastObservation;
	}

	@Override
	public FactObservationBasedGoalRecognitionExperiment call() {
		this.runExperiment();
		return this;
	}
	
	private void runExperiment() {

		//Generate problem files
		this.generateProblemFiles();
		
		
		//Domain and problem for which relaxed plans should be generated
		PddlDomain domain;
		EPddlProblem problem = null;
		

		List<Map<Integer, Map<GroundAction, Double>>> hypActionsProbsMaps = new ArrayList<Map<Integer, Map<GroundAction, Double>>>();

		Map<Integer, Map<String, Double>> hypPredicates = new HashMap<Integer, Map<String, Double>>();

		Map<Integer, Map<GroundAction, Double>> hypActionsProbs = new HashMap<Integer, Map<GroundAction, Double>>();

		Map<Integer, List<Map<GroundAction, Double>>> sampledActionProbs = new HashMap<Integer, List<Map<GroundAction, Double>>>();

		Set<Predicate> allPredicates = new HashSet<Predicate>();

		Map<Integer, Set<String>> goalsPerHyp = new HashMap<Integer, Set<String>>();


		List<Predicate> allPredicatesList = new ArrayList<Predicate>();

		if(this.hypPredicatesMaps == null) {
			this.hypPredicatesMaps = new ArrayList<Map<Integer, Map<String, Double>>>();
			this.allPredicatesPerHyp = new HashMap<Integer, Set<Predicate>>();

			Set<Predicate> goalPredicates = new HashSet<Predicate>();
			for(int hypCounter = 0; hypCounter < this.dirManager.getNumberOfProblemDirs(this.name); hypCounter++) {
				
				//Initialize PDDLDomain and problem file according to currently considered hypothesis
				domain = new PddlDomain(this.dirManager.getDomainFile().getAbsolutePath());
				problem = new EPddlProblem(
					this.dirManager.getInitialProblemFile(this.name, hypCounter).getAbsolutePath(),
					domain.getConstants(), domain.types, domain);
				domain.substituteEqualityConditions();

				try {
					problem.transformGoal();
					problem.groundingActionProcessesConstraints();

					problem.simplifyAndSetupInit(false, false);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
				goalPredicates.addAll(problem.getGoals().getInvolvedPredicates());
				allPredicates.addAll(this.determineRelevantPredicates(problem));

				Set<String> g = new HashSet<String>();
				for(Predicate p : problem.getGoals().getInvolvedPredicates()) {
					g.add(p.pddlPrint(false));
				}
				goalsPerHyp.put(hypCounter, g);
			}
			for(int hypCounter = 0; hypCounter < this.dirManager.getNumberOfProblemDirs(this.name); hypCounter++) {
				this.allPredicatesPerHyp.put(hypCounter, allPredicates);
			}


			long representationsStartTime;

			//Generate a certain number (sample) of relaxed plans per hypothesis
			for(int hypCounter = 0; hypCounter < this.dirManager.getNumberOfProblemDirs(this.name); hypCounter++) {
				
				//Initialize PDDLDomain and problem file according to currently considered hypothesis
				domain = new PddlDomain(this.dirManager.getDomainFile().getAbsolutePath());
				problem = new EPddlProblem(
					this.dirManager.getInitialProblemFile(this.name, hypCounter).getAbsolutePath(),
					domain.getConstants(), domain.types, domain);
				domain.substituteEqualityConditions();

				try {
					problem.transformGoal();
					problem.groundingActionProcessesConstraints();

					problem.simplifyAndSetupInit(false, false);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			
				representationsStartTime = System.currentTimeMillis();


				List<Map<GroundAction, Double>> actionProbs = FactObservationBasedGoalRecognitionUtils.getActionProbabilitiesModelBased2(domain, problem, goalPredicates);
				sampledActionProbs.put(hypCounter, actionProbs);
				
				this.factObservationProbabilitiesComputationTimes.put(hypCounter, System.currentTimeMillis() - representationsStartTime);
			}

			List<String> predicateNames = new ArrayList<String>();
			for(Predicate p : allPredicates) {
				predicateNames.add(p.pddlPrint(false));
			}

			for(Predicate p : allPredicates) {
				allPredicatesList.add(p);
			}

			List<Condition> allPAsConditions = new ArrayList<Condition>();
			for(Predicate p : allPredicates) {
				allPAsConditions.add(p);
			}


			//Create BN that models the probabilistic structure of the actions and facts. This is used afterwards to derive fact probabilities from action probabilities.
			System.out.println("CREATE BN NETWORKS");
			Map<Integer, List<BNBIFXMLHandler>> bnNetworks = FactObservationBasedGoalRecognitionUtils.createBNBIFXMLHandlerFromActionProbabilitiesForVS(sampledActionProbs, goalsPerHyp);

			//Derive fact probabilities from the sampled action probabilities, using the previously established BN structure.
			System.out.println("CALCULATE FACT PROBABILTIES");
			for(int i=0; i < sampledActionProbs.get(0).size(); i++) {
				hypActionsProbs = new HashMap<Integer, Map<GroundAction, Double>>();
				hypPredicates = new HashMap<Integer, Map<String, Double>>();
				for(int hypCounter = 0; hypCounter < this.dirManager.getNumberOfProblemDirs(this.name); hypCounter++) {
	
					hypActionsProbs.put(hypCounter, sampledActionProbs.get(hypCounter).get(i));
	
					List<Pair<GroundAction, Double>> actionProbs = new ArrayList<Pair<GroundAction, Double>>();
					for(GroundAction a : sampledActionProbs.get(hypCounter).get(i).keySet()) {
						actionProbs.add(new ImmutablePair<GroundAction, Double>(a, sampledActionProbs.get(hypCounter).get(i).get(a)));
					}
	
					domain = new PddlDomain(this.dirManager.getDomainFile().getAbsolutePath());
					problem = new EPddlProblem(
						this.dirManager.getInitialProblemFile(this.name, hypCounter).getAbsolutePath(),
						domain.getConstants(), domain.types, domain);
					domain.substituteEqualityConditions();
	
					try {
						problem.transformGoal();
						problem.groundingActionProcessesConstraints();
	
						problem.simplifyAndSetupInit(false, false);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
					hypPredicates.put(hypCounter, FactObservationBasedGoalRecognitionUtils.getPossiblePredicatesByBNBIFXMLHandler(bnNetworks.get(hypCounter).get(i), sampledActionProbs.get(hypCounter).get(i), predicateNames, goalsPerHyp.get(hypCounter)));
				}

				this.hypPredicatesMaps.add(hypPredicates);
				hypActionsProbsMaps.add(hypActionsProbs);
			}
		}
		else {
			domain = new PddlDomain(this.dirManager.getDomainFile().getAbsolutePath());
			problem = new EPddlProblem(
				this.dirManager.getInitialProblemFile(this.name, 0).getAbsolutePath(),
				domain.getConstants(), domain.types, domain);
			domain.substituteEqualityConditions();

			try {
				problem.transformGoal();
				problem.groundingActionProcessesConstraints();

				problem.simplifyAndSetupInit(false, false);
			} catch (Exception e1) {
				e1.printStackTrace();
			}

			for(Predicate p : this.allPredicatesPerHyp.get(0)) {
				allPredicatesList.add(p);
			}
		}
		
		
		//Parse sequence of observations from file
		List<String> observations = this.parseObservations();
		
		
		//Convert initial state to a list of strings that represent the present prediate names
		List<String> predicatesInitial = PDDLUtils.getPredicates((PDDLState)problem.getInit(), problem);
		
		
		//Convert initial state to vector
		List<Double> vInitial = VectorUtils.convertRelaxedStateToVector(predicatesInitial, allPredicatesList);

		
		//State handler that is used to maintain the current relaxed state
		SimplePlanSamplingStateHandler handler = new SimplePlanSamplingStateHandler(this.dirManager.getDomainFile().getAbsolutePath(), this.dirManager.getInitialProblemFile(this.name, 0).getAbsolutePath());
		
		//Keeps track of the currently considered observation step
		int obsCounter = 0;
		
		//Maps that store relaxed state and direction vectors per hypothesis
		Map<Integer, List<Double>> vHyps;
		Map<Integer, List<Double>> directions;

		//Vector that represents the last observed relaxed state
		List<Double> vCurrent;
		
		//Relaxed state that represents the last observed planning state
		RelState cRelState;
		
		long heuristicStartTime;

		//List that stores the converted vectors which correspond to the converted plans
		List<Map<Integer, List<Double>>> vHypsList = new ArrayList<Map<Integer, List<Double>>>();
		
		//Convert each generated relaxed plan
		Map<Integer, Map<String, Double>> hPredicates;
		for(int i=0; i < hypPredicatesMaps.size(); i++) {
			hPredicates = hypPredicatesMaps.get(i);
			vHyps = new HashMap<Integer, List<Double>>();

			//Convert relaxed plan for each hypothesis
			for(int hyp : hPredicates.keySet()) {
				vHyps.put(hyp, VectorUtils.convertRelaxedStateToVector(hPredicates.get(hyp), allPredicatesList));
			}
			vHypsList.add(vHyps);
		}

		List<Map<Integer, List<Double>>> dInitialHypList = new ArrayList<Map<Integer, List<Double>>>();
		Map<Integer, List<Double>> dInitialHyp;
		for(Map<Integer, List<Double>> hyps : vHypsList) {
			dInitialHyp = new HashMap<Integer, List<Double>>();
			for(int hyp : hyps.keySet()) {
				dInitialHyp.put(hyp, VectorOperations.calculateDirection(VectorOperations.elementWiseMultiplicationCurrent(vInitial, hyps.get(hyp)), hyps.get(hyp)));
			}
			dInitialHypList.add(dInitialHyp);
		}

		
		//If only the last observation should be used.
		if(this.runOnlyOnLastObservation) {
			String lastObs = observations.get(observations.size()-1);
			observations = new ArrayList<String>();
			observations.add(lastObs);
			this.numberObs = 1;
		}

		//Iterate through all observations
		System.out.println("PROCESS OBSERVATIONS");
		for(String obs : observations) {
			obs = ObsUtils.parseObservationFileLine(obs);
			obsCounter++;

			heuristicStartTime = System.currentTimeMillis();
			
			//Advance relaxed state with currently considered observation
			handler.advanceCurrentRelaxedState(obs);
			cRelState = handler.getCurrentRelaxedState();
						
			//Convert current relaxed state to vector
			vCurrent = VectorUtils.convertRelaxedStateToVector(PDDLUtils.getPredicates(cRelState, problem), allPredicatesList);

			//Calculate directions between the initial state and the sample relaxed states
			List<Map<Integer, List<Double>>> directionsList = new ArrayList<Map<Integer, List<Double>>>();

			List<Map<Integer, List<Double>>> currentHypDirectionsList = new ArrayList<Map<Integer, List<Double>>>();
			Map<Integer, List<Double>> cHypDirections;
			
			
			for(Map<Integer, List<Double>> hyps : vHypsList) {
				directions = new HashMap<Integer, List<Double>>();
				cHypDirections = new HashMap<Integer, List<Double>>();
				for(int hyp : hyps.keySet()) {
					//Compute directions
					directions.put(hyp, VectorOperations.calculateDirection(vInitial, hyps.get(hyp)));
					cHypDirections.put(hyp, VectorOperations.calculateDirection(VectorOperations.elementWiseMultiplicationCurrent(vCurrent, hyps.get(hyp)), hyps.get(hyp)));
				}

				//Add computed direction maps to lists
				directionsList.add(directions);
				currentHypDirectionsList.add(cHypDirections);
			}
				
			double heuristic;
			Map<Integer, Double> heuristicValues = new HashMap<Integer, Double>();
						
			Map<Integer, List<Double>> dir;
			Map<Integer, List<Double>> cHypDir;
			
			//Compute heuristic value for all computed direction vectors
			for(int i = 0; i < directionsList.size(); i++) {
				dir = directionsList.get(i);
				cHypDir = currentHypDirectionsList.get(i);
				for(int hyp : dir.keySet()) {

					//Calculate norm of direction between initial state and fact observation probabilities for goal "hyp".
					double initDistance = VectorOperations.calculateNorm(dInitialHypList.get(i).get(hyp), GoalRecognitionConfiguration.FPV_L_NORM);

					//Calculate norm of direction between currently observed state and fact observation probabilities for goal "hyp".
					double cDistance = VectorOperations.calculateNorm(cHypDir.get(hyp), GoalRecognitionConfiguration.FPV_L_NORM);

					//Calculate heuristic value.
					heuristic = (initDistance - cDistance);

					if(heuristic == Double.NaN) {
						System.out.println("HEURISTIC IS NAN!");
					}

					//Save heuristic value.
					if(heuristicValues.get(hyp) == null) {
						heuristicValues.put(hyp, heuristic);
					}
					else if(heuristicValues.get(hyp).doubleValue() < heuristic) {
						heuristicValues.put(hyp, heuristic);
					}
				}
			}
			//Compute heuristic computation times
			this.factObservationProbabilitiesHeuristicTimes.put(obsCounter, System.currentTimeMillis() - heuristicStartTime);
			
			//Save heursitc values for current observation step.
			this.goalSimilarities.put(obsCounter, heuristicValues);
		}
	}

	@Override
	public void generateProblemFiles() {
		this.hypMap = new HashMap<Integer, String>();
		
		File outputDir;
		List<String> outputFilePaths = new ArrayList<String>();
		List<String> outputFileNames = new ArrayList<String>();
		List<String> initialProblemFiles = new ArrayList<String>();
		String initialProblemFilePath = "";
		
		int hypCounter = 0;
		
		for(String line : this.dirManager.getPlanningHyps()) {
			outputDir = this.dirManager.generateProblemDir(this.name, hypCounter);
			
			outputFilePaths.add(outputDir.getAbsolutePath());
			outputFileNames.add(this.name);
			
			initialProblemFilePath = this.problemTemplate.generateInitialProblemFile(line, outputDir.getAbsolutePath(), this.name, hypCounter);
			initialProblemFiles.add(initialProblemFilePath);
			
			
			this.dirManager.addProblemDir(this.name, hypCounter, outputDir);
			this.hypMap.put(hypCounter, line);
			hypCounter++;
		}
		
		this.numberObs = hypCounter;
	}

	@Override
	public void generateGoalProbabilityReports() {		
		
	}

	@Override
	public ExperimentResult generateExperimentResult() {
		ExperimentResult expResult = new ExperimentResult(this.name);
		
		ResultObsStep cStep;
		for(int obs : this.goalSimilarities.keySet()) {
			cStep = new ResultObsStep();
			for(int hyp : this.hypMap.keySet()) {
				cStep.addGoalProbability(this.dirManager.getTextGoalFromPlanningGoal(this.hypMap.get(hyp)), this.goalSimilarities.get(obs).get(hyp));
			}
			expResult.addResultStep(obs, cStep);
		}
		
		expResult.setTrueHyp(this.dirManager.getTrueTextGoalForExperiment(this.name));
		
		for(int hyp : this.hypMap.keySet()) {
			if(this.factObservationProbabilitiesComputationTimes.get(hyp) != null) {
				expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyFactObservationProbabilitiesComputation(hyp), this.factObservationProbabilitiesComputationTimes.get(hyp));
			}
			for(int obs : this.factObservationProbabilitiesHeuristicTimes.keySet()) {
				expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyFactObservationProbabilityHeuristicComputation(hyp, obs), this.factObservationProbabilitiesHeuristicTimes.get(obs)/this.hypMap.keySet().size());
			}
		}

		return expResult;
	}

	public int getNumberOfObs() {
		return this.numberObs;
	}
	
	private List<String> parseObservations() {
		List<String> observations = new ArrayList<String>();
		try {
			observations = Files.readAllLines(this.dirManager.getObsFileAsPath(this.name));
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return observations;
	}
	
	private List<Predicate> determineRelevantPredicates(EPddlProblem problem) {
		List<Predicate> allPredicates = new ArrayList<Predicate>();
		Predicate p;
		for(Object o : problem.getActualFluents().keySet()) {
			if(o instanceof Predicate) {
				p = (Predicate)o;
				allPredicates.add(p);
			}
		}
		
		return allPredicates;
	}

	private List<GroundAction> determineRelevantActions(EPddlProblem problem) {
		List<GroundAction> allActions = new ArrayList<GroundAction>();

		for(GroundAction a : (Collection<GroundAction>)problem.getActions()) {
			allActions.add(a);
		}

		return allActions;
	}

	private static String vectorToString(List<Double> vector) {
		StringBuffer buff = new StringBuffer();
		buff.append("(");
		if(vector.size() > 0) {
			buff.append(vector.get(0));
		}

		for(int i=1; i < vector.size(); i++) {
			buff.append(", " + vector.get(i));
		}

		buff.append(")");

		return buff.toString();
	}

	public List<Map<Integer, Map<String, Double>>> getHypPredicatesMaps() {
		return this.hypPredicatesMaps;
	}

	public void setHypPredicatesMaps(List<Map<Integer, Map<String, Double>>> hypPredicatesMaps) {
		this.hypPredicatesMaps = hypPredicatesMaps;
	}

	public Map<Integer, Set<Predicate>> getAllPredicates() {
		return this.allPredicatesPerHyp;
	}

	public void setAllPredicatesPerHyp(Map<Integer, Set<Predicate>> allPredicatesPerHyp) {
		this.allPredicatesPerHyp = allPredicatesPerHyp;
	}
}

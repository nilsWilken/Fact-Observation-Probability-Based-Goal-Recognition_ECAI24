package experiment.recognitionExperiments.factObservationBased;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.hstairs.ppmajal.conditions.AndCond;
import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.NotCond;
import com.hstairs.ppmajal.conditions.PDDLObject;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;

import config.GoalRecognitionConfiguration;
import dataDriven.bayes.netTranslation.bifXML.BNBIFXMLHandler;
import eu.amidst.core.distribution.ConditionalDistribution;
import eu.amidst.core.distribution.Multinomial;
import eu.amidst.core.distribution.Multinomial_MultinomialParents;
import eu.amidst.core.distribution.UnivariateDistribution;
import eu.amidst.core.inference.ImportanceSamplingRobust;
import eu.amidst.core.inference.InferenceEngine;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.HashMapAssignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;
import symbolic.landmarkExtraction.LandmarkExtraction;
import symbolic.planning.stateHandler.AbstractStateHandler;
import symbolic.vectorUtils.PDDLUtils;
import symbolic.vectorUtils.similarityMeasures.SimilarityMeasure;
import symbolic.vectorUtils.similarityMeasures.SimilarityMeasureCalculation;
import utils.GeneralUtils;
import utils.ObsUtils;

public class FactObservationBasedGoalRecognitionUtils {
    public static List<Map<GroundAction, Double>> getActionProbabilitiesModelBased2(PddlDomain domain, EPddlProblem problem, Set<Predicate> possibleGoals) {
		//List that will hold the final action probabilities
		List<Pair<GroundAction, Double>> result = new ArrayList<Pair<GroundAction, Double>>();
		
		//Calculate relaxed planning graph
		LandmarkExtraction le1 = new LandmarkExtraction();
		try {
			le1.createRPGWithAllActions(domain, problem);
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}

		//Initialize list that contains all goal predicates
		List<Predicate> goalPredicates = new ArrayList<Predicate>();
		for(Predicate p : problem.getGoals().getInvolvedPredicates()) {
			goalPredicates.add(p);
		}

		//Initialize list that contains all initial predicates
		PDDLState initialState = (PDDLState) problem.getInit().clone();
		RelState relState = initialState.relaxState();
		List<Predicate> initialPredicates = new ArrayList<Predicate>();
		for(Object o : problem.getPredicatesInvolvedInInit()) {
			if(o instanceof Predicate) {
				if(relState.canBeTrue((Predicate)o)) {
					initialPredicates.add(((Predicate)o));
				}
			}
		}

		//Initialize list that will contain all actions that are part of the previously calculated RPG
		ArrayList<GroundAction> completeActions = new ArrayList<GroundAction>();
		for(int key : le1.action_levels.keySet()) {
			for(GroundAction a : le1.action_levels.get(key)) {
				completeActions.add(a);
			}
		}

		//Set that holds all found supporters
		Set<GroundAction> sups = new HashSet<GroundAction>();

		//List that holds all new potential supporters
		List<GroundAction> newSups;

		//Map that will hold all action probabilities
		Map<GroundAction, Double> pMap = new HashMap<GroundAction, Double>();
		
		//Holds action probability temporarily
		Double p;

		//Calculate set of relevant supporting actions per goal predicate "VS_ITERATIONS" times
		for(int j = 0; j < GoalRecognitionConfiguration.FPV_ITERATIONS; j++) {
			System.out.println("Iteration progress: " + ((double)j/(double)GoalRecognitionConfiguration.FPV_ITERATIONS));
			
			//Clear supporter set
			sups.clear();

			//Iterate through all goal predicates
			for(int i=0; i < goalPredicates.size(); i++) {
				System.out.println("Subgoal progress: " + ((double)i/(double)goalPredicates.size()));
				
				//Check whether current goal predicate is already fulfilled in initial state. If true no supporters are calculated
				if(!initialPredicates.contains(goalPredicates.get(i))) {

					/*
					 * Check whether a sufficient number of samples were already calculated for current goal predicate.
					 * If true, already calculated supporters are used 
					 */
					if(PDDLUtils.SAMPLED_PLANS.get(goalPredicates.get(i)) == null || PDDLUtils.SAMPLED_PLANS.get(goalPredicates.get(i)).size() <= j) {
						
						//Calculate supporters for current goal predicate
						newSups = PDDLUtils.findSupportersForGoal(goalPredicates.get(i), completeActions, initialPredicates, problem, le1);
						sups.addAll(newSups);

						//Update ACTION_COUNTS
						for(GroundAction sup : newSups) {
							PDDLUtils.ACTION_COUNTS.get(goalPredicates.get(i)).put(sup, PDDLUtils.ACTION_COUNTS.get(goalPredicates.get(i)).get(sup) + 1);
						}
						if(PDDLUtils.SAMPLED_PLANS.get(goalPredicates.get(i)) == null) {
							PDDLUtils.SAMPLED_PLANS.put(goalPredicates.get(i), new ArrayList<List<GroundAction>>());
						}
						PDDLUtils.SAMPLED_PLANS.get(goalPredicates.get(i)).add(newSups);
					}
					else {
						sups.addAll(PDDLUtils.SAMPLED_PLANS.get(goalPredicates.get(i)).get(j));
					}
				}
			}

			//Update action counts for all found supporters
			for(GroundAction a : sups) {
				p = pMap.get(a);
				if(p == null) {
					p = 0.0;
				}
				p++;
				pMap.put(a, p);
			}
		}

		//Calculate action probabilities by dividing action counts by VS_ITERATIONS
		for(GroundAction key : pMap.keySet()) {
			pMap.put(key, pMap.get(key)/(double)GoalRecognitionConfiguration.FPV_ITERATIONS);
		}

		//Set a probability of 0 for all actions that were not sampled
		for(GroundAction a : completeActions) {
			if(pMap.get(a) == null) {
				pMap.put(a, 0.0);
			}
		}

		List<Map<GroundAction, Double>> pMapList = new ArrayList<Map<GroundAction, Double>>();
		pMapList.add(pMap);

		return pMapList;
	}

    public static Map<Integer, List<BNBIFXMLHandler>> createBNBIFXMLHandlerFromActionProbabilitiesForVS(Map<Integer, List<Map<GroundAction, Double>>> actionProbsPerHyp, Map<Integer, Set<String>> goalsPerHyp) {
		Map<Integer, List<BNBIFXMLHandler>> bnPerHyp = new HashMap<Integer, List<BNBIFXMLHandler>>();
		
		Set<String> goalsSet = new HashSet<String>();
		for(int hyp : goalsPerHyp.keySet()) {
			goalsSet.addAll(goalsPerHyp.get(hyp));
		}

		//Initialize goal prior probabilities
		int numberOfHyps = actionProbsPerHyp.keySet().size();
		double[] goalPriors = new double[numberOfHyps];
		for(int i=0; i < numberOfHyps; i++) {
			goalPriors[i] = 1.0/((double)numberOfHyps);
		}

		//Initialize list with all goal values
		List<String> goals = new ArrayList<String>();
		List<Integer> goalInts = new ArrayList<Integer>();
		for(int key : actionProbsPerHyp.keySet()) {
			goals.add("" + key);
			goalInts.add(key);
		}

		//Initialize list with action values
		List<String> actionValues = new ArrayList<String>();
		actionValues.add("false");
		actionValues.add("true");

		//Initialize list with fact values
		List<String> factValues = new ArrayList<String>();
		factValues.add("false");
		factValues.add("true");

		Map<Condition, List<GroundAction>> relevantActionsPerFact = new HashMap<Condition, List<GroundAction>>();
		List<GroundAction> relActions;
		
		for(int hCounter = 0; hCounter < actionProbsPerHyp.keySet().size(); hCounter++) {
			//Generate bayesian networks
			List<BNBIFXMLHandler> networks = new ArrayList<BNBIFXMLHandler>();
			BNBIFXMLHandler bnHandler;
			String actionString;

			Set<String> addedActionsNodes = new HashSet<String>();
			Set<String> addedFactNodes = new HashSet<String>();

			Set<String> relevantFacts = new HashSet<String>();
			Set<Condition> relevantFactConds = new HashSet<Condition>();
			for(Map<GroundAction, Double> probs : actionProbsPerHyp.get(hCounter)) {
				for(GroundAction a : probs.keySet()) {
					if(probs.get(a) > 0.0) {
						for(Predicate e : a.getAddList().getInvolvedPredicates()) {
							relActions = relevantActionsPerFact.get(e);
							if(relActions == null) {
								relActions = new ArrayList<GroundAction>();
								relevantActionsPerFact.put(e, relActions);
							}
							relActions.add(a);

							relevantFacts.add(e.pddlPrint(false));
							relevantFactConds.add(e);
						}
					}
				}
			}

			for(int i = 0; i < actionProbsPerHyp.get(0).size(); i++) {
				bnHandler = new BNBIFXMLHandler("GeneratedNetwork_" + i);
				bnHandler.addNode("Goal", goals);

				for(String obs : relevantFacts) {
					bnHandler.addNode(obs, factValues);
					addedFactNodes.add(obs);
				}

				Set<Predicate> tc = new HashSet<Predicate>();
				for(int key : actionProbsPerHyp.keySet()) {
					for(Condition p : relevantFactConds) {
						for(GroundAction a : relevantActionsPerFact.get(p)) {
							actionString = AbstractStateHandler.convertActionToActionString(a);
							if(bnHandler.getConnections().get(p.pddlPrint(false)).contains(actionString)) {
								continue;
							}
							
							tc.clear();
							for(Object c : a.getPreconditions().sons) {
								PDDLUtils.processComplexCond((Condition)c, tc, new HashSet<Predicate>(), new LinkedList<Predicate>(), new ArrayList<Predicate>());
							}
							
							if(a.getAddList().getInvolvedPredicates().contains(p) || tc.contains(p)) {
								if(addedActionsNodes.add(actionString)) {
									bnHandler.addNode(actionString, actionValues);
									bnHandler.addArc("Goal", actionString);
								}
								bnHandler.addArc(actionString, p.pddlPrint(false));
							}
						}
					}
				}

				networks.add(bnHandler);
			}
			bnPerHyp.put(hCounter, networks);
		}

		return bnPerHyp;
	}

	public static Map<Integer, List<BayesianNetwork>> createBayesianNetworkFromActionProbabilitiesForVS(Map<Integer, List<Map<GroundAction, Double>>> actionProbsPerHyp, Map<Integer, Set<String>> goalsPerHyp) {
		Map<Integer, List<BayesianNetwork>> bnPerHyp = new HashMap<Integer, List<BayesianNetwork>>();
		
		Set<String> goalsSet = new HashSet<String>();
		for(int hyp : goalsPerHyp.keySet()) {
			goalsSet.addAll(goalsPerHyp.get(hyp));
		}

		//Initialize goal prior probabilities
		int numberOfHyps = actionProbsPerHyp.keySet().size();
		double[] goalPriors = new double[numberOfHyps];
		for(int i=0; i < numberOfHyps; i++) {
			goalPriors[i] = 1.0/((double)numberOfHyps);
		}

		//Initialize list with all goal values
		List<String> goals = new ArrayList<String>();
		List<Integer> goalInts = new ArrayList<Integer>();
		for(int key : actionProbsPerHyp.keySet()) {
			goals.add("" + key);
			goalInts.add(key);
		}

		//Initialize list with action values
		List<String> actionValues = new ArrayList<String>();
		actionValues.add("false");
		actionValues.add("true");

		//Initialize list with fact values
		List<String> factValues = new ArrayList<String>();
		factValues.add("false");
		factValues.add("true");

		
		for(int hCounter = 0; hCounter < actionProbsPerHyp.keySet().size(); hCounter++) {
			//Generate bayesian networks
			List<BayesianNetwork> networks = new ArrayList<BayesianNetwork>();
			BNBIFXMLHandler bnHandler;
			ConditionalDistribution cond;
			String actionString;
			BayesianNetwork bn;
			Variables vars;
			Assignment assignment;
			Multinomial_MultinomialParents mCond;
			Multinomial mult;
			double[] probabilities;
			double prob;

			Set<String> addedActionsNodes = new HashSet<String>();
			Set<String> addedFactNodes = new HashSet<String>();

			Set<String> relevantFacts = new HashSet<String>();
			Set<Condition> relevantFactConds = new HashSet<Condition>();
			for(Map<GroundAction, Double> probs : actionProbsPerHyp.get(hCounter)) {
				for(GroundAction a : probs.keySet()) {
					if(probs.get(a) > 0.0) {
						for(Predicate e : a.getAddList().getInvolvedPredicates()) {
							relevantFacts.add(e.pddlPrint(false));
							relevantFactConds.add(e);
						}
					}
				}
			}

			for(int i = 0; i < actionProbsPerHyp.get(0).size(); i++) {
				bnHandler = new BNBIFXMLHandler("GeneratedNetwork_" + i);
				bnHandler.addNode("Goal", goals);

				for(String obs : relevantFacts) {
					bnHandler.addNode(obs, factValues);
					addedFactNodes.add(obs);
				}

				for(Condition p : relevantFactConds) {
					if(goalsSet.contains(p.pddlPrint(false))) {
						bnHandler.addArc("Goal", p.pddlPrint(false));
						continue;
					}
					for(int key : actionProbsPerHyp.keySet()) {
						for(GroundAction a : actionProbsPerHyp.get(key).get(i).keySet()) {
							if(a.getAddList().getInvolvedPredicates().contains(p)) {
								actionString = AbstractStateHandler.convertActionToActionString(a);
								if(addedActionsNodes.add(actionString)) {
									bnHandler.addNode(actionString, actionValues);
									bnHandler.addArc("Goal", actionString);
								}
								bnHandler.addArc(actionString, p.pddlPrint(false));
							}
						}
					}
				}

				//Generate BayesianNetwork object from structure specification
				bn = bnHandler.convertToAmidstBN();
				vars = bn.getVariables();

				List<String> variableNames = new ArrayList<String>();
				for(Variable var : bn.getVariables().getListOfVariables()) {
					if(!var.getName().equals("Goal")) {
						variableNames.add(var.getName());
					}
				}

				//Set goal priors
				cond = bn.getConditionalDistribution(vars.getVariableByName("Goal"));
				((Multinomial)cond).setProbabilities(goalPriors);

				//Set fact probabilities
				List<Variable> parents;
				String binary;
				for(String fact : addedFactNodes) {
					if(goalsSet.contains(fact)) {
						for(int hyp : actionProbsPerHyp.keySet()) {
							assignment = new HashMapAssignment(1);
							assignment.setValue(vars.getVariableByName("Goal"), goalInts.indexOf(hyp));

							mCond = bn.getConditionalDistribution(vars.getVariableByName(fact));
							mult = mCond.getMultinomial(assignment);
							if(goalsPerHyp.get(hyp).contains(fact)) {
								probabilities = new double[] {0.01, 0.99};
							}
							else {
								probabilities = new double[] {0.99, 0.01};
							}
							mult.setProbabilities(probabilities);
						}
						continue;
					}
					parents = bn.getDAG().getParentSet(vars.getVariableByName(fact)).getParents();
					if(parents.size() == 0) {
						continue;
					}

					mCond = bn.getConditionalDistribution(vars.getVariableByName(fact));
					if(mCond.getNumberOfParentAssignments() == 0) {
						System.out.println("CONTINUE");
						continue;
					}
					for(int v = 0; v < mCond.getNumberOfParentAssignments(); v++) {
						binary = String.format("%" + parents.size() + "s", Integer.toBinaryString(v)).replace(" ", "0");
						assignment = new HashMapAssignment(parents.size());
						for(int b = 0; b < parents.size(); b++) {
							assignment.setValue(parents.get(b), Double.parseDouble("" + binary.charAt(b)));
						}
						mult = mCond.getMultinomial(assignment);
					
						if(v > 0) {
							probabilities = new double[] {0.01, 0.99};
						}
						else {
							probabilities = new double[] {0.99, 0.01};
						}
					
						mult.setProbabilities(probabilities);
					}
				}


				//Set action probabilties
				Map<String, GroundAction> actionStrings = new HashMap<String, GroundAction>();
				for(int hyp : actionProbsPerHyp.keySet()) {
					assignment = new HashMapAssignment(1);
					assignment.setValue(vars.getVariableByName("Goal"), goalInts.indexOf(hyp));

					actionStrings.clear();

					for(GroundAction a : actionProbsPerHyp.get(hyp).get(i).keySet()) {
						actionString = AbstractStateHandler.convertActionToActionString(a);
						actionStrings.put(actionString, a);
					}
					for(String varName : addedActionsNodes) {
						mCond = bn.getConditionalDistribution(vars.getVariableByName(varName));
						mult = mCond.getMultinomial(assignment);
						if(actionStrings.keySet().contains(varName)) {
							prob = actionProbsPerHyp.get(hyp).get(i).get(actionStrings.get(varName));
							if(prob == 0.0) {
								probabilities = new double[] {0.99, 0.01};
							}
							else {
								probabilities = new double[] {1.0 - prob, prob};
							}
						}
						else {
							probabilities = new double[] {0.99, 0.01};
						}
						mult.setProbabilities(probabilities);
					}
				}
				networks.add(bn);
			}
			bnPerHyp.put(hCounter, networks);
		}

		return bnPerHyp;
	}

	public static List<BayesianNetwork> createBayesianNetworkFromActionProbabilitiesForFactObservations(Map<Integer, List<Map<GroundAction, Double>>> actionProbsPerHyp, List<String> observations, List<Condition> observedConditions, Map<Integer, Set<String>> goalsPerHyp) {
		
		Set<String> goalsSet = new HashSet<String>();
		for(int hyp : goalsPerHyp.keySet()) {
			goalsSet.addAll(goalsPerHyp.get(hyp));
		}

		//Initialize goal prior probabilities
		int numberOfHyps = actionProbsPerHyp.keySet().size();
		double[] goalPriors = new double[numberOfHyps];
		for(int i=0; i < numberOfHyps; i++) {
			goalPriors[i] = 1.0/((double)numberOfHyps);
		}

		//Initialize list with all goal values
		List<String> goals = new ArrayList<String>();
		List<Integer> goalInts = new ArrayList<Integer>();
		for(int key : actionProbsPerHyp.keySet()) {
			goals.add("" + key);
			goalInts.add(key);
		}

		//Initialize list with action values
		List<String> actionValues = new ArrayList<String>();
		actionValues.add("false");
		actionValues.add("true");

		//Initialize list with fact values
		List<String> factValues = new ArrayList<String>();
		factValues.add("false");
		factValues.add("true");

		//Generate bayesian networks
		List<BayesianNetwork> networks = new ArrayList<BayesianNetwork>();
		BNBIFXMLHandler bnHandler;
		ConditionalDistribution cond;
		String actionString;
		BayesianNetwork bn;
		Variables vars;
		Assignment assignment;
		Multinomial_MultinomialParents mCond;
		Multinomial mult;
		double[] probabilities;
		double prob;

		Set<String> addedActionsNodes = new HashSet<String>();
		Set<String> addedFactNodes = new HashSet<String>();

		for(int i = 0; i < actionProbsPerHyp.get(0).size(); i++) {
			bnHandler = new BNBIFXMLHandler("GeneratedNetwork_" + i);
			bnHandler.addNode("Goal", goals);

			for(String obs : observations) {
				bnHandler.addNode(obs, factValues);
				addedFactNodes.add(obs);
			}

			for(Condition p : observedConditions) {
				if(goalsSet.contains(p.pddlPrint(false))) {
					bnHandler.addArc("Goal", p.pddlPrint(false));
					continue;
				}
				for(int key : actionProbsPerHyp.keySet()) {
					for(GroundAction a : actionProbsPerHyp.get(key).get(i).keySet()) {
						if(a.getAddList().getInvolvedPredicates().contains(p)) {
							actionString = AbstractStateHandler.convertActionToActionString(a);
							if(addedActionsNodes.add(actionString)) {
								bnHandler.addNode(actionString, actionValues);
								bnHandler.addArc("Goal", actionString);
							}
							bnHandler.addArc(actionString, p.pddlPrint(false));
						}
					}
				}
			}

			//Generate BayesianNetwork object from structure specification
			bn = bnHandler.convertToAmidstBN();
			vars = bn.getVariables();

			List<String> variableNames = new ArrayList<String>();
			for(Variable var : bn.getVariables().getListOfVariables()) {
				if(!var.getName().equals("Goal")) {
					variableNames.add(var.getName());
				}
			}

			//Set goal priors
			cond = bn.getConditionalDistribution(vars.getVariableByName("Goal"));
			((Multinomial)cond).setProbabilities(goalPriors);

			//Set fact probabilities
			List<Variable> parents;
			String binary;
			for(String fact : addedFactNodes) {
				if(goalsSet.contains(fact)) {
					for(int hyp : actionProbsPerHyp.keySet()) {
						assignment = new HashMapAssignment(1);
						assignment.setValue(vars.getVariableByName("Goal"), goalInts.indexOf(hyp));

						mCond = bn.getConditionalDistribution(vars.getVariableByName(fact));
						mult = mCond.getMultinomial(assignment);
						if(goalsPerHyp.get(hyp).contains(fact)) {
							probabilities = new double[] {0.01, 0.99};
						}
						else {
							probabilities = new double[] {0.99, 0.01};
						}
						mult.setProbabilities(probabilities);
					}
					continue;
				}
				parents = bn.getDAG().getParentSet(vars.getVariableByName(fact)).getParents();
				if(parents.size() == 0) {
					continue;
				}

				mCond = bn.getConditionalDistribution(vars.getVariableByName(fact));
				if(mCond.getNumberOfParentAssignments() == 0) {
					System.out.println("CONTINUE");
					continue;
				}
				for(int v = 0; v < mCond.getNumberOfParentAssignments(); v++) {
					binary = String.format("%" + parents.size() + "s", Integer.toBinaryString(v)).replace(" ", "0");
					assignment = new HashMapAssignment(parents.size());
					for(int b = 0; b < parents.size(); b++) {
						assignment.setValue(parents.get(b), Double.parseDouble("" + binary.charAt(b)));
					}
					mult = mCond.getMultinomial(assignment);
					
					if(v > 0) {
						probabilities = new double[] {0.01, 0.99};
					}
					else {
						probabilities = new double[] {0.99, 0.01};
					}
					
					mult.setProbabilities(probabilities);
				}
			}


			//Set action probabilties
			Map<String, GroundAction> actionStrings = new HashMap<String, GroundAction>();
			for(int hyp : actionProbsPerHyp.keySet()) {
				assignment = new HashMapAssignment(1);
				assignment.setValue(vars.getVariableByName("Goal"), goalInts.indexOf(hyp));

				actionStrings.clear();

				for(GroundAction a : actionProbsPerHyp.get(hyp).get(i).keySet()) {
					actionString = AbstractStateHandler.convertActionToActionString(a);
					actionStrings.put(actionString, a);
				}
				for(String varName : addedActionsNodes) {
					mCond = bn.getConditionalDistribution(vars.getVariableByName(varName));
					mult = mCond.getMultinomial(assignment);
					if(actionStrings.keySet().contains(varName)) {
						prob = actionProbsPerHyp.get(hyp).get(i).get(actionStrings.get(varName));
						if(prob == 0.0) {
							probabilities = new double[] {0.99, 0.01};
						}
						else {
							probabilities = new double[] {1.0 - prob, prob};
						}
					}
					else {
						probabilities = new double[] {0.99, 0.01};
					}
					mult.setProbabilities(probabilities);
				}
			}
			networks.add(bn);
		}

		return networks;
	}

	public static List<BayesianNetwork> createBayesianNetworkFromActionProbabilitiesForObservations(Map<Integer, List<Map<GroundAction, Double>>> actionProbsPerHyp, List<String> observations) {
		
		//Initialize goal prior probabilities
		int numberOfHyps = actionProbsPerHyp.keySet().size();
		double[] goalPriors = new double[numberOfHyps];
		for(int i=0; i < numberOfHyps; i++) {
			goalPriors[i] = 1.0/((double)numberOfHyps);
		}

		//Initialize list with all goal values
		List<String> goals = new ArrayList<String>();
		List<Integer> goalInts = new ArrayList<Integer>();
		for(int key : actionProbsPerHyp.keySet()) {
			goals.add("" + key);
			goalInts.add(key);
		}

		//Initialize list with action values
		List<String> actionValues = new ArrayList<String>();
		actionValues.add("true");
		actionValues.add("false");

		//Generate bayesian networks
		List<BayesianNetwork> networks = new ArrayList<BayesianNetwork>();
		BNBIFXMLHandler bnHandler;
		ConditionalDistribution cond;
		String actionString;
		BayesianNetwork bn;
		Variables vars;
		Assignment assignment;
		Multinomial_MultinomialParents mCond;
		Multinomial mult;
		double[] probabilities;
		double prob;

		Set<String> addedActionsNodes = new HashSet<String>();

		for(int i = 0; i < actionProbsPerHyp.get(0).size(); i++) {
			bnHandler = new BNBIFXMLHandler("GeneratedNetwork_" + i);
			bnHandler.addNode("Goal", goals);

			for(int key : actionProbsPerHyp.keySet()) {
				for(String obs : observations) {;
					if(addedActionsNodes.add(obs)) {
						bnHandler.addNode(obs, actionValues);
						bnHandler.addArc("Goal", obs);
					}
				}
			}

			//Generate BayesianNetwork object from structure specification
			bn = bnHandler.convertToAmidstBN();
			vars = bn.getVariables();

			List<String> variableNames = new ArrayList<String>();
			for(Variable var : bn.getVariables().getListOfVariables()) {
				if(!var.getName().equals("Goal")) {
					variableNames.add(var.getName());
				}
			}

			//Set goal priors
			cond = bn.getConditionalDistribution(vars.getVariableByName("Goal"));
			((Multinomial)cond).setProbabilities(goalPriors);

			//Set action probabilties
			Map<String, GroundAction> actionStrings = new HashMap<String, GroundAction>();
			for(int hyp : actionProbsPerHyp.keySet()) {
				assignment = new HashMapAssignment(1);
				assignment.setValue(vars.getVariableByName("Goal"), goalInts.indexOf(hyp));

				actionStrings.clear();

				for(GroundAction a : actionProbsPerHyp.get(hyp).get(i).keySet()) {
					actionString = AbstractStateHandler.convertActionToActionString(a);
					actionStrings.put(actionString, a);
				}
				for(String varName : variableNames) {
					mCond = bn.getConditionalDistribution(vars.getVariableByName(varName));
					mult = mCond.getMultinomial(assignment);
					if(actionStrings.keySet().contains(varName)) {
						prob = actionProbsPerHyp.get(hyp).get(i).get(actionStrings.get(varName));
						if(prob == 0.0) {
							probabilities = new double[] {0.01, 0.99};
						}
						else {
							probabilities = new double[] {prob, 1.0 - prob};
						}
					}
					else {
						probabilities = new double[] {0.01, 0.99};
					}
					mult.setProbabilities(probabilities);
				}
			}
			networks.add(bn);
		}

		return networks;
	}

	public static List<BayesianNetwork> createBayesianNetworkFromActionProbabilities(Map<Integer, List<Map<GroundAction, Double>>> actionProbsPerHyp) {
		
		//Initialize goal prior probabilities
		int numberOfHyps = actionProbsPerHyp.keySet().size();
		double[] goalPriors = new double[numberOfHyps];
		for(int i=0; i < numberOfHyps; i++) {
			goalPriors[i] = 1.0/((double)numberOfHyps);
		}

		//Initialize list with all goal values
		List<String> goals = new ArrayList<String>();
		List<Integer> goalInts = new ArrayList<Integer>();
		for(int key : actionProbsPerHyp.keySet()) {
			goals.add("" + key);
			goalInts.add(key);
		}

		//Initialize list with action values
		List<String> actionValues = new ArrayList<String>();
		actionValues.add("true");
		actionValues.add("false");

		//Generate bayesian networks
		List<BayesianNetwork> networks = new ArrayList<BayesianNetwork>();
		BNBIFXMLHandler bnHandler;
		ConditionalDistribution cond;
		String actionString;
		BayesianNetwork bn;
		Variables vars;
		Assignment assignment;
		Multinomial_MultinomialParents mCond;
		Multinomial mult;
		double[] probabilities;
		double prob;

		Set<String> addedActionsNodes = new HashSet<String>();

		for(int i = 0; i < actionProbsPerHyp.get(0).size(); i++) {
			bnHandler = new BNBIFXMLHandler("GeneratedNetwork_" + i);
			bnHandler.addNode("Goal", goals);

			for(int key : actionProbsPerHyp.keySet()) {
				for(GroundAction a : actionProbsPerHyp.get(key).get(0).keySet()) {
					actionString = AbstractStateHandler.convertActionToActionString(a);
					if(addedActionsNodes.add(actionString)) {
						bnHandler.addNode(actionString, actionValues);
						bnHandler.addArc("Goal", actionString);
					}
				}
			}

			//Generate BayesianNetwork object from structure specification
			bn = bnHandler.convertToAmidstBN();
			vars = bn.getVariables();

			List<String> variableNames = new ArrayList<String>();
			for(Variable var : bn.getVariables().getListOfVariables()) {
				if(!var.getName().equals("Goal")) {
					variableNames.add(var.getName());
				}
			}

			//Set goal priors
			cond = bn.getConditionalDistribution(vars.getVariableByName("Goal"));
			((Multinomial)cond).setProbabilities(goalPriors);

			//Set action probabilties
			Map<String, GroundAction> actionStrings = new HashMap<String, GroundAction>();
			for(int hyp : actionProbsPerHyp.keySet()) {
				assignment = new HashMapAssignment(1);
				assignment.setValue(vars.getVariableByName("Goal"), goalInts.indexOf(hyp));

				actionStrings.clear();

				for(GroundAction a : actionProbsPerHyp.get(hyp).get(i).keySet()) {
					actionString = AbstractStateHandler.convertActionToActionString(a);
					actionStrings.put(actionString, a);
				}
				for(String varName : variableNames) {
					mCond = bn.getConditionalDistribution(vars.getVariableByName(varName));
					mult = mCond.getMultinomial(assignment);
					if(actionStrings.keySet().contains(varName)) {
						prob = actionProbsPerHyp.get(hyp).get(i).get(actionStrings.get(varName));
						if(prob == 0.0) {
							probabilities = new double[] {0.01, 0.99};
						}
						else {
							probabilities = new double[] {prob, 1.0 - prob};
						}
					}
					else {
						probabilities = new double[] {0.01, 0.99};
					}
					mult.setProbabilities(probabilities);
				}
			}
			networks.add(bn);
		}

		return networks;
	}

    public static Map<String, Double> getPossiblePredicatesByBNBIFXMLHandler(BNBIFXMLHandler bn, Map<GroundAction, Double> actionProbs, List<String> allPredicates, Set<String> goalPredicates) {
        double[] probabilities;
		Map<String, Double> predicateProbs = new HashMap<String, Double>();

		HashMap<String, List<String>> connections = bn.getConnections();
        List<String> parents;
		List<List<String>> goalParents = new ArrayList<List<String>>();
		String actionString;
		double prob;
        for(String p : allPredicates) {
			parents = connections.get(p);

			if(goalPredicates.contains(p)) {
				predicateProbs.put(p, 1.0);
				goalParents.add(parents);
			}
			else if(parents == null || parents.size() == 0) {
				predicateProbs.put(p, 0.0);
			}
			else {
				prob = 1.0;
				for(GroundAction a : actionProbs.keySet()) {
					actionString = AbstractStateHandler.convertActionToActionString(a);
					if(parents.contains(actionString)) {
						prob *= (1.0 - actionProbs.get(a));
					}
				}

				if((1.0 - prob) > GoalRecognitionConfiguration.MODEL_PROBABILITY_THRESHOLD) {
					predicateProbs.put(p, 1.0 - prob);
					// predicateProbs.put(p, 1.0 - finalProb);
				} else {
					predicateProbs.put(p, 0.0);
				}
				
			}
		}

		List<String> p0 = goalParents.get(0);
		if(p0 != null) {
			List<String> toRemove = new ArrayList<String>();

			for(int i=1; i < goalParents.size(); i++) {
				if(goalParents.get(i) == null) {
					continue;
				}
				for(String p : p0) {
					if(!goalParents.get(i).contains(p)) {
						toRemove.add(p);
					}
				}
				if(toRemove.size() == p0.size()) {
					break;
				}
			}

			p0.removeAll(toRemove);

			for(String p : p0) {
				predicateProbs.put(p, 1.0);
			}
		}

		return predicateProbs;
	}

	public static Map<String, Double> getPossiblePredicatesByBayesianNetwork(BayesianNetwork bn, int hypCounter, List<String> allPredicates) {
        double[] probabilities;
        Assignment obsAssignment;
        UnivariateDistribution posterior;

		Map<String, Double> predicateProbs = new HashMap<String, Double>();

        ImportanceSamplingRobust sampling = new ImportanceSamplingRobust();
        sampling.setSampleSize(100);
        InferenceEngine.setInferenceAlgorithm(sampling);

        obsAssignment = new HashMapAssignment(1);
		obsAssignment.setValue(bn.getVariables().getVariableByName("Goal"), (double)hypCounter);
            
        for(String p : allPredicates) {
			try {
				posterior = InferenceEngine.getPosterior(bn.getVariables().getVariableByName(p), bn, obsAssignment); 
        		probabilities = ((Multinomial)posterior).getProbabilities();
				predicateProbs.put(p, probabilities[1]);
			}catch(Exception e) {
				predicateProbs.put(p, 0.0);
			}
		}

		return predicateProbs;
	}

	public static Map<String, Double> getPossiblePredicatesByActionProbabilitiesAsMap(PddlDomain domain, EPddlProblem problem, File domainFile, File problemFile, List<Pair<GroundAction, Double>> actionProbs, List<Predicate> allPredicates) {
		PDDLState initialState1 = (PDDLState) problem.getInit().clone();
		RelState relState1 = initialState1.relaxState();

		Map<String, Double> predicateProbs = new HashMap<String, Double>();
		Map<String, Double> negativePredicateProbs = new HashMap<String, Double>();
		double predProb;
		double negPredProb;
		double maxProb = 0.0;
		List<String> addEffects;
		String pName;
		for(Pair<GroundAction, Double> pair : actionProbs) {
			if(pair.getRight() >= GoalRecognitionConfiguration.MODEL_PROBABILITY_THRESHOLD) {
				GroundAction a = pair.getLeft();
				relState1.apply(a);

				addEffects = PDDLUtils.getAddEffects(pair.getLeft());
				for(Predicate p : allPredicates) {
					pName = p.pddlPrint(false);

					if(addEffects.contains(pName)) {
						if(predicateProbs.get(pName) == null) {
							predicateProbs.put(pName, GeneralUtils.roundDouble(pair.getRight(), 6));
						}
						else {
							predProb = predicateProbs.get(pName);
	
							predProb += pair.getRight();
							predicateProbs.put(pName, predProb);
						}

						if(negativePredicateProbs.get(pName) == null) {
							negativePredicateProbs.put(pName, GeneralUtils.roundDouble((1.0 - pair.getRight()), 6));
						}
						else {
							negPredProb = negativePredicateProbs.get(pName);

							negPredProb += (1.0 - pair.getRight());
							negativePredicateProbs.put(pName, negPredProb);
						}
					}
				}
			}
		}

		for(Predicate p : allPredicates) {
			pName = p.pddlPrint(false);
			if(predicateProbs.get(pName) == null) {
				predProb = 0.0;
			}
			else {
				predProb = predicateProbs.get(pName);
			}
			if(negativePredicateProbs.get(pName) == null) {
				negPredProb = 0.0;
			}
			else {
				negPredProb = negativePredicateProbs.get(pName);
			}

			maxProb = predProb + negPredProb;

			if(maxProb > 0) {
				predProb /= maxProb;
			}

			predicateProbs.put(pName, predProb);
		}

		return predicateProbs;
	}

    public static Double computeActionProbability(Map<Predicate, Double> predicateScores, GroundAction action) {
		Predicate pEffect;

		double max = 0.0;
		for(Object e : action.getAddList().sons) {
			pEffect = (Predicate)e;
			if(predicateScores.keySet().contains(pEffect)) {
				max += predicateScores.get(pEffect);
			}
		}

		return max/(double)action.getAddList().sons.size();
	}

    public static GroundAction sampleActionByProbability(Map<GroundAction, Double> probMap) {
		Map<Double, GroundAction> sampleMap = new TreeMap<Double, GroundAction>();

		double prob = 0.0;
		for(GroundAction key : probMap.keySet()) {
			prob += probMap.get(key);
			sampleMap.put(prob, key);
		}

		Random random = new Random();
		double rand = random.nextDouble(prob);

		double prevKey = 0.0;
		for(Double key : sampleMap.keySet()) {
			if(rand < key && rand >= prevKey) {
				return sampleMap.get(key);
			}
			prevKey = key;
		}

		return null;
	}

	public static List<GroundAction> samplePlanFromSupporterActions(List<Predicate> supportedPredicates, List<GroundAction> actions, List<GroundAction> selectedActions, Condition goal, PDDLState init, List<Predicate> initialPredicates) {
		if(PDDLUtils.SAMPLED_PLANS.get(goal) == null) {
			PDDLUtils.SAMPLED_PLANS.put(goal, new ArrayList<List<GroundAction>>());
		}

		List<GroundAction> plan = new ArrayList<GroundAction>();
		List<GroundAction> possibleActions = new ArrayList<GroundAction>();

		PDDLState initClone = init.clone();
		RelState relInit = init.relaxState();


		Random random = new Random();
		List<GroundAction> relActions;
		List<GroundAction> selectedRelActions;
		boolean added;
		Set<Predicate> foundPredicates = new HashSet<Predicate>();
		Set<PDDLObject> usedObjects = new HashSet<PDDLObject>();
		int iterationCounter = 0;
		int iterationLimit = 20;
		int failCounter = 0;
		int failLimit = 10;
		while(!relInit.satisfy(goal)) {
			relActions = PDDLUtils.getRelevantActions(relInit, actions);
			selectedRelActions = PDDLUtils.getRelevantActions(relInit, selectedActions);
			possibleActions.clear();
			added = false;
			for(GroundAction relA : selectedRelActions) {
				for(Predicate p : supportedPredicates) {
					if(relA.getAddList().sons.contains(p) && !foundPredicates.contains(p)) {
						added = true;
						possibleActions.add(relA);
						
						for(Predicate aP : relA.getAddList().getInvolvedPredicates()) {
							foundPredicates.add(aP);
							for(Object o : aP.getInvolvedVariables()) {
								PDDLObject po = (PDDLObject)o;
								usedObjects.add(po);
							}
						}
						for(Condition prec : relA.getPreconditions().getTerminalConditions()) {
							if(!(prec instanceof NotCond)) {
								for(Object o : prec.getInvolvedVariables()) {
									PDDLObject po = (PDDLObject)o;
									usedObjects.add(po);
								}
							}
						}

					}
				}
			}
			if(!added) {
				for(GroundAction relA : relActions) {
					for(Predicate p : supportedPredicates) {
						if(relA.getAddList().sons.contains(p) && !foundPredicates.contains(p)) {
							added = true;
							possibleActions.add(relA);
						
							for(Predicate aP : relA.getAddList().getInvolvedPredicates()) {
								foundPredicates.add(aP);
								for(Object o : aP.getInvolvedVariables()) {
									PDDLObject po = (PDDLObject)o;
									usedObjects.add(po);
								}
							}
							for(Condition prec : relA.getPreconditions().getTerminalConditions()) {
								if(!(prec instanceof NotCond)) {
									for(Object o : prec.getInvolvedVariables()) {
										PDDLObject po = (PDDLObject)o;
										usedObjects.add(po);
									}
								}
							}
						}
					}
				}
			}
			for(GroundAction posA : possibleActions) {
				relInit.apply(posA);
				plan.add(posA);
			}
			if(!added) {
				if(relActions.size() == 0) {
					relInit = initClone.clone().relaxState();
					plan.clear();
					iterationCounter = 0;
					foundPredicates.clear();
					usedObjects.clear();

					failCounter++;
					if(failCounter > failLimit) {
						failCounter = 0;
						iterationCounter = 0;
						iterationLimit += 10;
					}

					continue;
				}

				if(selectedRelActions.size() > 0) {
					plan.add(PDDLUtils.selectAction(selectedRelActions, initialPredicates, foundPredicates, new ArrayList<Predicate>()));
				}
				else {
					plan.add(PDDLUtils.selectAction(relActions, initialPredicates, foundPredicates, new ArrayList<Predicate>()));
				}
				for(Predicate aP : plan.get(plan.size()-1).getAddList().getInvolvedPredicates()) {
					foundPredicates.add(aP);
					for(Object o : aP.getInvolvedVariables()) {
						PDDLObject po = (PDDLObject)o;
						usedObjects.add(po);
					}
				}
				for(Condition prec : plan.get(plan.size()-1).getPreconditions().getTerminalConditions()) {
					if(!(prec instanceof NotCond)) {
						for(Object o : prec.getInvolvedVariables()) {
							PDDLObject po = (PDDLObject)o;
							usedObjects.add(po);
						}
					}
				}
				relInit.apply(plan.get(plan.size()-1));
			}
			if(iterationCounter > iterationLimit) {
				relInit = initClone.clone().relaxState();
				plan.clear();
				iterationCounter = 0;
				foundPredicates.clear();
				usedObjects.clear();

				failCounter++;
				if(failCounter > failLimit) {
					failCounter = 0;
					iterationCounter = 0;
					iterationLimit += 10;
				}
			}
			iterationCounter++;
		}
		PDDLUtils.SAMPLED_PLANS.get(goal).add(plan);

		return plan;
	}

    public static void normalizeProbMap(Map<GroundAction, Double> pMap) {
		double maxProb = 0.0;

		for(GroundAction key : pMap.keySet()) {
			if(pMap.get(key) > maxProb) {
				maxProb = pMap.get(key);
			}
		}

		for(GroundAction key : pMap.keySet()) {
			pMap.put(key, pMap.get(key)/maxProb);
		}
	}
    
    public static Pair<GroundAction, Double> sampleAction(List<Pair<GroundAction, Double>> actions) {
		Map<Double, Pair<GroundAction, Double>> sumKeys = new TreeMap<Double, Pair<GroundAction, Double>>();
		double sum = 0.0;

		for(Pair<GroundAction, Double> action : actions) {
			sum += action.getRight();
			sumKeys.put(sum, action);
		}
		double random = ThreadLocalRandom.current().nextDouble(0.0, sum);

		for(Double key : sumKeys.keySet()) {
			if(random <= key) {
				return sumKeys.get(key);
			}
		}

		return null;
	}

    public static Pair<GroundAction, Double> sampleActionMax(List<Pair<GroundAction, Double>> actions) {
		Map<Double, Pair<GroundAction, Double>> sumKeys = new TreeMap<Double, Pair<GroundAction, Double>>();
		double max = 0.0;

		for(Pair<GroundAction, Double> action : actions) {
			System.out.println(action.getLeft().toEcoString(true) + ": " + action.getRight());
			if(action.getRight() > max) {
				max = action.getRight();
			}
			sumKeys.put(action.getRight(), action);
		}

		return sumKeys.get(max);
	}
}

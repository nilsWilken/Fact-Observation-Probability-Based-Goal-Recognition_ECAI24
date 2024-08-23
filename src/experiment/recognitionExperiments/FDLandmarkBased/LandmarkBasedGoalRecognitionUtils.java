package experiment.recognitionExperiments.FDLandmarkBased;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.hstairs.ppmajal.conditions.AndCond;
import com.hstairs.ppmajal.conditions.ComplexCondition;
import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.OrCond;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;
import com.hstairs.ppmajal.problem.State;

import config.GoalRecognitionConfiguration;
import symbolic.vectorUtils.PDDLUtils;
import utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction.FastDownwardLMExtractionUtils;

import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;


public class LandmarkBasedGoalRecognitionUtils {
	public static HashMap<ComplexCondition, Double> computeCompletion(List<GroundAction> observations, EPddlProblem problem, Map<ComplexCondition, Map<Condition, List<Integer>>> goalsPlusLiftedLMs, Map<ComplexCondition, Map<Integer, String[]>> liftedLandmarksPerGoal, Map<ComplexCondition, Graph<String, DefaultEdge>> landmarkGraphPerGoal) {

		HashMap<ComplexCondition, Double> recognizedGoals = new HashMap<ComplexCondition, Double>();

		
		// Map<ComplexCondition, List<Integer>> achievedLMsInObservations = computeAchievedLandmarksInObservations(observations, problem, goalsPlusLiftedLMs, landmarkGraphPerGoal);


		//Compute achieved landmarks per subgoal to compute completion heuristic
		Map<ComplexCondition, Map<Condition, List<Integer>>> achievedLMsInObservations = computeAchievedLandmarksPerSubgoalInObservations(observations, problem, goalsPlusLiftedLMs, landmarkGraphPerGoal);

		Map<ComplexCondition, Map<Condition, List<Integer>>> lmsPerSubgoalPerGoal = new HashMap<ComplexCondition, Map<Condition, List<Integer>>>();
		Map<Condition, List<Integer>> lmMap;
		Map<Condition, List<Integer>> lmsPerSubgoal;
		for(ComplexCondition goal : goalsPlusLiftedLMs.keySet()) {
			lmMap = goalsPlusLiftedLMs.get(goal);
			lmsPerSubgoal = new HashMap<Condition, List<Integer>>();
			
			for(Condition subGoal : goal.getInvolvedPredicates()) {
				if(lmMap.get(subGoal) == null) {
					continue;
				}
				if(landmarkGraphPerGoal.get(goal) == null) {
					List<Integer> l = new ArrayList<Integer>();
					l.add(lmMap.get(subGoal).get(0));
					lmsPerSubgoal.put(subGoal, l);
				}
				else {
					lmsPerSubgoal.put(subGoal, LandmarkBasedGoalRecognitionUtils.getAllGraphPredecessors(landmarkGraphPerGoal.get(goal), LandmarkBasedGoalRecognitionUtils.getStringLMID(lmMap.get(subGoal).get(0))));
					lmsPerSubgoal.get(subGoal).add(lmMap.get(subGoal).get(0));
				}
			}
			lmsPerSubgoalPerGoal.put(goal, lmsPerSubgoal);
		}
		
		double denominator;
		double numerator;
		for(ComplexCondition goal : achievedLMsInObservations.keySet()) {
			// denominator = (double)goalsPlusLiftedLMs.get(goal).size();
			// denominator = liftedLandmarksPerGoal.get(goal).keySet().size();	
			// numerator = (double) achievedLMsInObservations.get(goal).size();
			
			// double h_gc = numerator/denominator;

			double h_gc = 0.0;
			for(Condition subGoal : goal.getInvolvedPredicates()) {
				if(lmsPerSubgoalPerGoal.get(goal).get(subGoal) == null) {
					continue;
				}

				denominator = (double)lmsPerSubgoalPerGoal.get(goal).get(subGoal).size();
				numerator = (double)achievedLMsInObservations.get(goal).get(subGoal).size();
				h_gc += (numerator/denominator);
			}
			recognizedGoals.put(goal, (h_gc/(double)goal.getInvolvedPredicates().size()));
		}

		return recognizedGoals;
	}

	public static Map<Integer, Double> computeUniquenessScores(Map<ComplexCondition, Map<Condition, List<Integer>>> goalsPlusLiftedLMs) {
		// Map<ComplexCondition, Map<Integer, Double>> uniquenessScores = new HashMap<ComplexCondition, Map<Integer, Double>>();

		Map<Integer, Integer> occurences = new HashMap<Integer, Integer>();
		Map<Condition, List<Integer>> lmMap;

		Integer occurence;

		//Counts occurences of each landmark
		for(ComplexCondition goal : goalsPlusLiftedLMs.keySet()) {
			lmMap = goalsPlusLiftedLMs.get(goal);
			for(Condition cond : lmMap.keySet()) {
				for(Integer key : lmMap.get(cond)) {
					occurence = occurences.get(key);
					if(occurence == null) {
						occurence = 0;
					}
					occurences.put(key, ++occurence);
				}
			}
		}

		Map<Integer, Double> uniquenessScores = new HashMap<Integer, Double>();
		for(int key : occurences.keySet()) {
			uniquenessScores.put(key, 1.0/((double)occurences.get(key)));
		}

		// Map<Integer, Double> scores;
		// for(ComplexCondition goal : goalsPlusLiftedLMs.keySet()) {
		// 	scores = new HashMap<Integer, Double>();
		// 	for(Condition cond : goalsPlusLiftedLMs.get(goal).keySet()) {
		// 		for(Integer key : goalsPlusLiftedLMs.get(goal).get(cond)) {
		// 			if(scores.get(key) == null) {
		// 				scores.put(key, landmarkScores.get(key));
		// 			}
		// 		}
		// 	}
		// 	uniquenessScores.put(goal, scores);
		// }

		return uniquenessScores;
	}

	public static Map<ComplexCondition, Double> computeUniquenessHeuristic(List<GroundAction> observations, EPddlProblem problem, Map<ComplexCondition, Map<Condition, List<Integer>>> goalsPlusLiftedLMs, Map<Integer, Double> uniquenessScores) {

		Map<ComplexCondition, Double> recognizedGoals = new HashMap<ComplexCondition, Double>();

		Map<ComplexCondition, List<Integer>> achievedLMsInObservations = computeAchievedLandmarksInObservations(observations, problem, goalsPlusLiftedLMs, null);

		double numerator;
		double denominator;
		List<Integer> achievedLMs;
		for(ComplexCondition goal : goalsPlusLiftedLMs.keySet()) {
			numerator = 0.0;
			denominator = 0.0;

			achievedLMs = achievedLMsInObservations.get(goal);

			for(Integer key : uniquenessScores.keySet()) {
				if(achievedLMs.contains(key)) {
					numerator += uniquenessScores.get(key);
				}
				denominator += uniquenessScores.get(key);
			}

			recognizedGoals.put(goal, numerator/denominator);
		}

		return recognizedGoals;
	}

	public static Map<ComplexCondition, Map<Integer, List<Integer>>> computeAchievedLandmarksInObservationsWithPosition(List<GroundAction> observations, EPddlProblem problem, Map<ComplexCondition, Map<Condition, List<Integer>>> goalsPlusLiftedLMs, Map<ComplexCondition, Graph<String, DefaultEdge>> landmarkGraphPerGoal) {
			
		Map<ComplexCondition, Map<Integer, List<Integer>>> goalsPlusAchievedLMs = new HashMap<ComplexCondition, Map<Integer, List<Integer>>>();			
		 
		for (ComplexCondition goal : goalsPlusLiftedLMs.keySet()) {
			RelState state = ((PDDLState) problem.getInit().clone()).relaxState();
			RelState init = ((PDDLState) problem.getInit().clone()).relaxState();
			State init2 = problem.getInit().clone();
			List<Condition> initPredicates = PDDLUtils.getPossiblePredicatesAsConditions(init, problem);

			HashSet<Condition> prePlusAdd = new HashSet<Condition>();

			// for each observed action o in O do
			int obsCounter = 0;
			Map<Condition, List<Integer>> lmMap = goalsPlusLiftedLMs.get(goal);
			for (GroundAction observation : observations) {
				Set<Integer> AL = new HashSet<Integer>();

				//If there are no landmarks for one goal
				if (lmMap.isEmpty()) {
					goalsPlusAchievedLMs.put(goal, new HashMap<Integer, List<Integer>>());
					break;
				}

				
				if(state.satisfy(observation.getPreconditions())) {
					observation.apply(state);
				}
				else {
					for(Object o : observation.getPreconditions().sons) {
						Condition c = (Condition)o;
						if(!c.isSatisfied(state)) {
							state.makePositive((Predicate)c);
						}
					}
					state.apply(observation);
				}
				
				if(init2.satisfy(observation.getPreconditions())) {
					init2.apply(observation, init2);
				}

				for(Condition lm : lmMap.keySet()) {
					if(lm instanceof ComplexCondition) {
						if(init2.satisfy(lm)) {
							for(Integer lmIndex : lmMap.get(lm)) {
								AL.add(lmIndex);
							}
						}
					}
				}

				List<Condition> predicates = PDDLUtils.getPossiblePredicatesAsConditions(state, problem);
				if(!GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
					List<Condition> toRemove = new ArrayList<Condition>();
					for(Condition c : predicates) {
						if(initPredicates.contains(c)) {
							toRemove.add(c);
						}
					}
					predicates.removeAll(toRemove);
				}

				for(Condition lm : lmMap.keySet()) {
					if(lm instanceof ComplexCondition) {
						continue;
					}
					else if(predicates.contains(lm)) {
						for(Integer lmIndex : lmMap.get(lm)) {
							AL.add(lmIndex);
						}
					}
				}

				if(landmarkGraphPerGoal.get(goal) != null) {
					Set<Integer> idsToAdd = new HashSet<Integer>();
					for(int al : AL) {
						idsToAdd.addAll(LandmarkBasedGoalRecognitionUtils.getAllGraphPredecessors(landmarkGraphPerGoal.get(goal), LandmarkBasedGoalRecognitionUtils.getStringLMID(al)));
					}
					AL.addAll(idsToAdd);
				}
			
				List<Integer> alList = new ArrayList<Integer>();
				alList.addAll(AL);

				//Store achieved LMs with goal
				Map<Integer, List<Integer>> ALperObs = goalsPlusAchievedLMs.get(goal);
				if(ALperObs == null) {
					ALperObs = new HashMap<Integer, List<Integer>>();
					goalsPlusAchievedLMs.put(goal, ALperObs);
				}
				for(int i = 0; i < obsCounter; i++) {
					alList.removeAll(ALperObs.get(i));
				}
				ALperObs.put(obsCounter++, alList);
			}
		}

		return goalsPlusAchievedLMs;
	}

	public static Condition translateLandmarkToCondition(Map<Condition, List<Integer>> lmMappings, int lm) {
		for(Condition key : lmMappings.keySet()) {
			if(lmMappings.get(key).contains(lm)) {
				return key;
			}
		}
		return null;
	}

	public static Map<ComplexCondition, Map<Condition, List<Integer>>> computeAchievedLandmarksPerSubgoalInObservations(List<GroundAction> observations, EPddlProblem problem, Map<ComplexCondition, Map<Condition, List<Integer>>> goalsPlusLiftedLMs, Map<ComplexCondition, Graph<String, DefaultEdge>> landmarkGraphPerGoal) {
			
		Map<ComplexCondition, Map<Condition, List<Integer>>> goalsPlusAchievedLMs = new HashMap<ComplexCondition, Map<Condition, List<Integer>>>();			
		 
		for (ComplexCondition goal : goalsPlusLiftedLMs.keySet()) {
			Map<Condition, List<Integer>> lmMap = goalsPlusLiftedLMs.get(goal);

			//If there are no landmarks for one goal
			if (lmMap.isEmpty()) {
				goalsPlusAchievedLMs.put(goal, new HashMap<Condition, List<Integer>>());
				continue;
			}


			Set<Integer> AL = new HashSet<Integer>();
			RelState state = ((PDDLState) problem.getInit().clone()).relaxState();
			RelState init = ((PDDLState) problem.getInit().clone()).relaxState();
			State init2 = problem.getInit().clone();
			List<Condition> initPredicates = PDDLUtils.getPossiblePredicatesAsConditions(init, problem);

			HashSet<Condition> prePlusAdd = new HashSet<Condition>();

			// for each observed action o in O do
			for (GroundAction observation : observations) {
				// for(Condition preCond : observation.getPreconditions().getTerminalConditions()) {
				// 	if(preCond instanceof Predicate) {
				// 		prePlusAdd.add(preCond);
				// 	}
				// }

				//Consider all facts that are part of the add list of the currently checked action as potentially achieved
				//getTerminalConditions() bricht add lIste in einzelne fakten
				// for (Condition terminalCondition : observation.getAddList().getTerminalConditions()) {
				// 	if (terminalCondition instanceof Predicate && !initPredicates.contains(terminalCondition)) {
				// 		prePlusAdd.add(terminalCondition);
				// 	}
				// }

				//Handle conditional effects of currently checked action
				//Preconditions werden hinzugefügt
				// if (observation.cond_effects != null) {
				// 	for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) observation.cond_effects.sons) {
				// 		if (state.satisfy(cEffect.activation_condition)) {
				// 			prePlusAdd.addAll(((AndCond) cEffect.effect).getTerminalConditions());	
				// 		}
				// 	}
				// }

				
				if(state.satisfy(observation.getPreconditions())) {
					observation.apply(state);
				}
				else {
					for(Object o : observation.getPreconditions().sons) {
						Condition c = (Condition)o;
						if(!c.isSatisfied(state)) {
							state.makePositive((Predicate)c);
						}
					}
					state.apply(observation);
				}
				
				if(init2.satisfy(observation.getPreconditions())) {
					init2.apply(observation, init2);
				}

				for(Condition lm : lmMap.keySet()) {
					if(lm instanceof ComplexCondition) {
						if(init2.satisfy(lm)) {
							for(Integer lmIndex : lmMap.get(lm)) {
								AL.add(lmIndex);
							}
						}
					}
				}
				// observation.apply(state);
			}

			List<Condition> predicates = PDDLUtils.getPossiblePredicatesAsConditions(state, problem);
			if(!GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
				List<Condition> toRemove = new ArrayList<Condition>();
				for(Condition c : predicates) {
					if(initPredicates.contains(c)) {
						toRemove.add(c);
					}
				}
				predicates.removeAll(toRemove);
			}

			for(Condition lm : lmMap.keySet()) {
				if(lm instanceof ComplexCondition) {
					continue;
				}
				else if(predicates.contains(lm)) {
					for(Integer lmIndex : lmMap.get(lm)) {
						AL.add(lmIndex);
					}
				}
			}

			if(landmarkGraphPerGoal.get(goal) != null) {
				Set<Integer> idsToAdd = new HashSet<Integer>();
				for(int al : AL) {
					if(al == -1) {
						continue;
					}
					idsToAdd.addAll(LandmarkBasedGoalRecognitionUtils.getAllGraphPredecessors(landmarkGraphPerGoal.get(goal), LandmarkBasedGoalRecognitionUtils.getStringLMID(al)));
				}
				AL.addAll(idsToAdd);
			}
			
			List<Integer> alList = new ArrayList<Integer>();
			alList.addAll(AL);

			Map<Condition, List<Integer>> alPerSubgoal = new HashMap<Condition, List<Integer>>();
			List<Integer> alPerSubgoalList;
			List<Integer> preds;
			for(Condition subGoal : goal.getInvolvedPredicates()) {
				alPerSubgoalList = new ArrayList<Integer>();
				preds = LandmarkBasedGoalRecognitionUtils.getAllGraphPredecessors(landmarkGraphPerGoal.get(goal), LandmarkBasedGoalRecognitionUtils.getStringLMID(lmMap.get(subGoal).get(0)));
				preds.add(lmMap.get(subGoal).get(0));

				for(int pred : preds) {
					if(alList.contains(pred)) {
						alPerSubgoalList.add(pred);
					}
				}
				alPerSubgoal.put(subGoal, alPerSubgoalList);
			}
			
			//Store achieved LMs with Goal
			goalsPlusAchievedLMs.put(goal, alPerSubgoal);
		}

		
		return goalsPlusAchievedLMs;
	}


	public static Map<ComplexCondition, List<Integer>> computeAchievedLandmarksInObservations(List<GroundAction> observations, EPddlProblem problem, Map<ComplexCondition, Map<Condition, List<Integer>>> goalsPlusLiftedLMs, Map<ComplexCondition, Graph<String, DefaultEdge>> landmarkGraphPerGoal) {
			
		Map<ComplexCondition, List<Integer>> goalsPlusAchievedLMs = new HashMap<ComplexCondition, List<Integer>>();			
		 
		for (ComplexCondition goal : goalsPlusLiftedLMs.keySet()) {
			Map<Condition, List<Integer>> lmMap = goalsPlusLiftedLMs.get(goal);

			//If there are no landmarks for one goal
			if (lmMap.isEmpty()) {
				goalsPlusAchievedLMs.put(goal, new ArrayList<Integer>());
				continue;
			}


			Set<Integer> AL = new HashSet<Integer>();
			RelState state = ((PDDLState) problem.getInit().clone()).relaxState();
			RelState init = ((PDDLState) problem.getInit().clone()).relaxState();
			State init2 = problem.getInit().clone();
			List<Condition> initPredicates = PDDLUtils.getPossiblePredicatesAsConditions(init, problem);

			HashSet<Condition> prePlusAdd = new HashSet<Condition>();

			// for each observed action o in O do
			for (GroundAction observation : observations) {
				// for(Condition preCond : observation.getPreconditions().getTerminalConditions()) {
				// 	if(preCond instanceof Predicate) {
				// 		prePlusAdd.add(preCond);
				// 	}
				// }

				//Consider all facts that are part of the add list of the currently checked action as potentially achieved
				//getTerminalConditions() bricht add lIste in einzelne fakten
				// for (Condition terminalCondition : observation.getAddList().getTerminalConditions()) {
				// 	if (terminalCondition instanceof Predicate && !initPredicates.contains(terminalCondition)) {
				// 		prePlusAdd.add(terminalCondition);
				// 	}
				// }

				//Handle conditional effects of currently checked action
				//Preconditions werden hinzugefügt
				// if (observation.cond_effects != null) {
				// 	for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) observation.cond_effects.sons) {
				// 		if (state.satisfy(cEffect.activation_condition)) {
				// 			prePlusAdd.addAll(((AndCond) cEffect.effect).getTerminalConditions());	
				// 		}
				// 	}
				// }

				
				if(state.satisfy(observation.getPreconditions())) {
					observation.apply(state);
				}
				else {
					for(Object o : observation.getPreconditions().sons) {
						Condition c = (Condition)o;
						if(!c.isSatisfied(state)) {
							state.makePositive((Predicate)c);
						}
					}
					state.apply(observation);
				}
				
				if(init2.satisfy(observation.getPreconditions())) {
					init2.apply(observation, init2);
				}

				for(Condition lm : lmMap.keySet()) {
					if(lm instanceof ComplexCondition) {
						if(init2.satisfy(lm)) {
							for(Integer lmIndex : lmMap.get(lm)) {
								AL.add(lmIndex);
							}
						}
					}
				}
				// observation.apply(state);
			}

			List<Condition> predicates = PDDLUtils.getPossiblePredicatesAsConditions(state, problem);
			if(!GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
				List<Condition> toRemove = new ArrayList<Condition>();
				for(Condition c : predicates) {
					if(initPredicates.contains(c)) {
						toRemove.add(c);
					}
				}
				predicates.removeAll(toRemove);
			}

			for(Condition lm : lmMap.keySet()) {
				if(lm instanceof ComplexCondition) {
					continue;
				}
				else if(predicates.contains(lm)) {
					for(Integer lmIndex : lmMap.get(lm)) {
						AL.add(lmIndex);
					}
				}
			}

			if(landmarkGraphPerGoal.get(goal) != null) {
				Set<Integer> idsToAdd = new HashSet<Integer>();
				for(int al : AL) {
					if(al == -1) {
						continue;
					}
					idsToAdd.addAll(LandmarkBasedGoalRecognitionUtils.getAllGraphPredecessors(landmarkGraphPerGoal.get(goal), LandmarkBasedGoalRecognitionUtils.getStringLMID(al)));
				}
				AL.addAll(idsToAdd);
			}
			
			List<Integer> alList = new ArrayList<Integer>();
			alList.addAll(AL);

			//Store achieved LMs with Goal
			goalsPlusAchievedLMs.put(goal, alList);
		}

		
		return goalsPlusAchievedLMs;
	}

	public static String getStringLMID(int lmIndex) {
		return "lm" + lmIndex;
	}

	public static int getCountFromID(String id) {
        id = id.replace("lm", "");
        return Integer.parseInt(id);
    }

	public static List<Integer> getAllGraphPredecessors(Graph<String, DefaultEdge> landmarkGraph, String vertex) {
		Set<String> predecessors = new HashSet<String>();

		List<String> newPredecessors;
		List<String> queue = new ArrayList<String>();
		queue.add(vertex);
		String cVertex;
		do {
			cVertex = queue.get(0);
			queue.remove(cVertex);

			newPredecessors = Graphs.predecessorListOf(landmarkGraph, cVertex);
			newPredecessors.removeAll(predecessors);
			predecessors.addAll(newPredecessors);

			queue.addAll(newPredecessors);
		}while(queue.size() > 0);

		List<Integer> achievedPreds = new ArrayList<Integer>();
		for(String predecessor : predecessors) {
			achievedPreds.add(LandmarkBasedGoalRecognitionUtils.getCountFromID(predecessor));
		}

		return achievedPreds;
	}

	public static List<Integer> getAllGraphSuccesors(Graph<String, DefaultEdge> landmarkGraph, String vertex) {
		Set<String> succesors = new HashSet<String>();

		List<String> newSuccesors;
		List<String> queue = new ArrayList<String>();
		queue.add(vertex);
		String cVertex;
		do {
			cVertex = queue.get(0);
			queue.remove(cVertex);

			newSuccesors = Graphs.successorListOf(landmarkGraph, cVertex);
			newSuccesors.removeAll(succesors);
			succesors.addAll(newSuccesors);

			queue.addAll(newSuccesors);
		}while(queue.size() > 0);

		List<Integer> succesorLMs = new ArrayList<Integer>();
		for(String succesor : succesors) {
			succesorLMs.add(LandmarkBasedGoalRecognitionUtils.getCountFromID(succesor));
		}

		return succesorLMs;
	}

	public static List<Integer> getAllPreviouslyAchievedLMs(int currentObs, Map<Integer, List<Integer>> achievedLMs) {
		List<Integer> prevAchievedLMs = new ArrayList<Integer>();
		for(int i=0; i < currentObs; i++) {
			for(Integer lm : achievedLMs.get(i)) {
				prevAchievedLMs.add(lm);
			}
		}
		return prevAchievedLMs;
	}

	public static Map<Condition, List<Integer>> translateLiftedStringLandmarks(Map<Integer, String[]> liftedLandmarks, EPddlProblem problem) {
		Map<Condition, List<Integer>> translatedLandmarks = new HashMap<Condition, List<Integer>>();

		Map<Integer, List<String[]>> conjLandmarks = new HashMap<Integer, List<String[]>>();
		Map<Integer, List<String[]>> disjLandmarks = new HashMap<Integer, List<String[]>>();
		Set<Integer> foundLMs = new HashSet<Integer>();


		LandmarkBasedGoalRecognitionUtils.extractConjAndDisjLandmarks(liftedLandmarks, conjLandmarks, disjLandmarks);
		LandmarkBasedGoalRecognitionUtils.translateConjunctiveLandmarks(translatedLandmarks, conjLandmarks, problem);
		LandmarkBasedGoalRecognitionUtils.translateDisjLandmarks(translatedLandmarks, disjLandmarks, problem);

		foundLMs.addAll(conjLandmarks.keySet());
		foundLMs.addAll(disjLandmarks.keySet());

		Predicate c;
		PDDLState state = (PDDLState)problem.getInit().clone();

		//Iterate over all facts in the given planning problem
		for(Object o : problem.getActualFluents().keySet()) {
			
			//Check whether currently considered fluent is of type Predicate
			if(o instanceof Predicate) {
				c = (Predicate)o;
			
				//Filter initial state landmarks if they are not supposed to be used
				if(!state.satisfy(c) || GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
					
					//Check for each found (lifted) landmark whether it is fulfilled by the currently considered fact
					String[] lm;
					List<Integer> lmList;
					for(int i=0; i < liftedLandmarks.keySet().size(); i++) {
						lm = liftedLandmarks.get(i);

						//If considered landmark is conjunctive or disjunctive
						if(lm[0].equals("conj") || lm[0].equals("disj")) {
							continue;
						}
						else if(conditionEqualsLM(c, lm)) {

							//If fact fulfills landmark lm, then add the index of lm to the list of indices that are fulfilled by fact c
							lmList = translatedLandmarks.get(c);
							if(lmList == null) {
								lmList = new ArrayList<Integer>();
								translatedLandmarks.put(c, lmList);
							}
							lmList.add(i);
							foundLMs.add(i);
						}
					}
				}
			}
		}

		//Remove all landmarks for which no fulfilling facts were found (i.e., initial state landmarks)
		List<Integer> keyRemove = new ArrayList<Integer>();
		for(int key : liftedLandmarks.keySet()) {
			if(!foundLMs.contains(key)) {
				keyRemove.add(key);
			}
		}
		for(int key : keyRemove) {
			liftedLandmarks.remove(key);
		}

		return translatedLandmarks;
	}

	private static void translateConjunctiveLandmarks(Map<Condition, List<Integer>> translatedLandmarks, Map<Integer, List<String[]>> conjLandmarks, EPddlProblem problem) {
		Map<Integer, Condition> andConds = new HashMap<Integer, Condition>();

		for(int key : conjLandmarks.keySet()) {
			andConds.put(key, new AndCond());
		}

		Condition c;
		AndCond and;

		//Iterate over all facts in the given planning problem
		for(Object o : problem.getActualFluents().keySet()) {
			
			//Check whether currently considered fluent is of type Predicate
			if(o instanceof Predicate) {
				c = (Predicate)o;
			
				for(Integer key : conjLandmarks.keySet()) {
					for(String[] lmCandidate : conjLandmarks.get(key)) {
						if(conditionEqualsLM(c, lmCandidate)) {
							and = (AndCond)andConds.get(key);
							and.addConditions(c);
							break;
						}
					}
				}		
			}
		}

		PDDLState init = (PDDLState)problem.getInit().clone();
		for(int key : andConds.keySet()) {
			c = andConds.get(key);
			// if(!init.satisfy(c) || GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
				List<Integer> indices = new ArrayList<Integer>();
				indices.add(key);
				translatedLandmarks.put(c, indices);
			// }
		}
	}

	private static void translateDisjLandmarks(Map<Condition, List<Integer>> translatedLandmarks, Map<Integer, List<String[]>> disjLandmarks, EPddlProblem problem) {
		Map<Integer, Condition> orConds = new HashMap<Integer, Condition>();

		for(int key : disjLandmarks.keySet()) {
			orConds.put(key, new OrCond());
		}

		Condition c;
		OrCond or;

		//Iterate over all facts in the given planning problem
		for(Object o : problem.getActualFluents().keySet()) {
			
			//Check whether currently considered fluent is of type Predicate
			if(o instanceof Predicate) {
				c = (Predicate)o;
			
				for(Integer key : disjLandmarks.keySet()) {
					for(String[] lmCandidate : disjLandmarks.get(key)) {
						if(conditionEqualsLM(c, lmCandidate)) {
							or = (OrCond)orConds.get(key);
							or.addConditions(c);
							break;
						}
					}
				}		
			}
		}

		PDDLState init = (PDDLState)problem.getInit().clone();
		for(int key : orConds.keySet()) {
			c = orConds.get(key);
			
			// if(!init.satisfy(c) || GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
				List<Integer> indices = new ArrayList<Integer>();
				indices.add(key);
				translatedLandmarks.put(c, indices);
			// }
		}
	}

	private static void extractConjAndDisjLandmarks(Map<Integer, String[]> landmarks, Map<Integer, List<String[]>> conjLandmarks, Map<Integer, List<String[]>> disjLandmarks) {
		List<Integer> keysToRemove = new ArrayList<Integer>();

		for(int key : landmarks.keySet()) {
			if(landmarks.get(key)[0].equals("conj")) {
				conjLandmarks.put(key, LandmarkBasedGoalRecognitionUtils.parseComplexLandmark(landmarks.get(key)));
				keysToRemove.add(key);
			}
			else if(landmarks.get(key)[0].equals("disj")) {
				disjLandmarks.put(key, LandmarkBasedGoalRecognitionUtils.parseComplexLandmark(landmarks.get(key)));
				keysToRemove.add(key);
			}
		}

		// for(int key : keysToRemove) {
		// 	landmarks.remove(key);
		// }
	}

	private static List<String[]> parseComplexLandmark(String[] complexLandmark) {
		List<String[]> parsedLMs = new ArrayList<String[]>();

		List<String> tmp = new ArrayList<String>();
		for(int i=2; i < complexLandmark.length; i++) {
			if(!complexLandmark[i].equals(FastDownwardLMExtractionUtils.FORMULA_LANDMARK_SEPARATOR)) {
				tmp.add(complexLandmark[i]);
			}
			else {
				String[] tmp2 = new String[tmp.size()];
				for(int j=0; j < tmp.size(); j++) {
					tmp2[j] = tmp.get(j);
				}
				parsedLMs.add(tmp2);
				tmp = new ArrayList<String>();
			}
		}

		return parsedLMs;
	}

	private static boolean conditionEqualsLM(Condition c, String[] LM) {
		String cString = c.toString();
		cString = cString.replace("(", "");
		cString = cString.replace(")", "");

		String[] cArr = cString.split(" ");

		if(cArr.length != LM.length) {
			return false;
		}
		
		//Check if they are the same predicate
		if (cArr[0].equalsIgnoreCase(LM[0])) {
			//For all variables
			for (int i = 1; i < cArr.length; i++) {
				if (LM[i].equals("?") || cArr[i].equalsIgnoreCase(LM[i])) {
					continue;
				} else {
					return false;
				}
			}
		} else {
			return false;	
		}
		return true;
	}
}

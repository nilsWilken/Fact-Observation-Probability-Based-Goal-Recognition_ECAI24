package symbolic.vectorUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang3.tuple.Pair;

import com.hstairs.ppmajal.conditions.AndCond;
import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.NotCond;
import com.hstairs.ppmajal.conditions.PDDLObject;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.conditions.ConditionalEffect;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.heuristics.advanced.h1;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;

import symbolic.landmarkExtraction.LandmarkExtraction;
import symbolic.planning.stateHandler.planSampling.SimplePlanSamplingStateHandler;
import utils.ObsUtils;

public class PDDLUtils {

	public static Map<Condition, List<List<GroundAction>>> SAMPLED_PLANS = new HashMap<Condition, List<List<GroundAction>>>();
	public static Map<Predicate, Map<GroundAction, Integer>> ACTION_COUNTS = new HashMap<Predicate, Map<GroundAction, Integer>>();
	
	public static Map<Integer, List<List<String>>> getPossibleGoalStatesForGoal(PddlDomain domain, EPddlProblem problem, List<AndCond> goalConditions) {
		h1 heuristic = new h1(problem);
		heuristic.setup(problem.getInit());
		
		Set<RelState> goalStates = new HashSet<RelState>();
		
		Set<RelState> visitedStates = new HashSet<RelState>();
		Set<RelState> newStates;
		
		Map<Float, List<RelState>> newStatesMap;
		Map<Float, List<RelState>> oldNewStatesMap;
		
		PDDLState init = (PDDLState)problem.getInit();
		RelState relInit = init.relaxState();
		RelState cState;
		List<RelState> cStatesList;
		List<RelState> newStatesList;
		float heuristicEstimate;
		
		
		int maxExpandedHValues = 1;
		int maxExpandedStates = 10;
		int actionLayer = 0;
		int expandedHValues;
		int expandedStates;
		
		Map<Integer, List<List<String>>> resultStates = new HashMap<Integer, List<List<String>>>();
		List<List<String>> cResultStates;
		boolean oneGoal;
		List<GroundAction> actions;
		
		while (goalStates.size() < 200) {
			cStatesList = new ArrayList<RelState>();
			cStatesList.add(((PDDLState)problem.getInit()).relaxState());
			
			oldNewStatesMap = new TreeMap<Float, List<RelState>>();
			oldNewStatesMap.put(heuristic.computeEstimate(problem.getInit()), cStatesList);
			
			do {
				newStates = new HashSet<RelState>();
				newStatesMap = new TreeMap<Float, List<RelState>>();
				expandedHValues = 0;

				for (float key : oldNewStatesMap.keySet()) {
					cStatesList = oldNewStatesMap.get(key);
					Collections.shuffle(cStatesList);
					expandedStates = 0;

					for (RelState nState : cStatesList) {
						actions = PDDLUtils.getRelevantActions(nState,
								(Collection<GroundAction>) problem.getActions());
						Collections.shuffle(actions);
						for (GroundAction a : actions) {
							cState = nState.clone();
							cState.apply(a);

							if (!visitedStates.contains(cState)) {
								if (PDDLUtils.checkGoalSatisfaction(cState, goalConditions)) {
									visitedStates.add(cState);
									continue;
								} else if (cState.satisfy(problem.getGoals())) {
									goalStates.add(cState);
									visitedStates.add(cState);
								} else {
									if (newStates.add(cState)) {
										heuristicEstimate = heuristic.computeEstimate(cState.concretizeState(problem));
										newStatesList = newStatesMap.get(heuristicEstimate);
										if (newStatesList == null) {
											newStatesList = new ArrayList<RelState>();
											newStatesMap.put(heuristicEstimate, newStatesList);
										}
										newStatesList.add(cState);
									}
								}
							}
						}
						expandedStates++;
						if (expandedStates == maxExpandedStates) {
							break;
						}
					}
					expandedHValues++;
					if (expandedHValues == maxExpandedHValues) {
						break;
					}
				}

				oldNewStatesMap = newStatesMap;
				visitedStates.addAll(newStates);
				actionLayer++;
			} while (newStates.size() != 0);
		}
		
		int counter;
		int minCounter = Integer.MAX_VALUE;
		RelState minState = null;
		for(RelState s : goalStates) {
			counter = 0;
			for(int key : s.possBollValues.keySet()) {
				if(s.possBollValues.get(key) > 0) {
					counter++;
				}
			}
			if(counter < minCounter) {
				minCounter = counter;
				minState = s;
			}
		}
		
		cResultStates = new ArrayList<List<String>>();
		cResultStates.add(PDDLUtils.getPossiblePredicates(minState, problem));
		resultStates.put(0, cResultStates);
		System.out.println(goalStates.size());
		
		return resultStates;
	}
	
	public static boolean checkGoalSatisfaction(RelState state, List<AndCond> possibleConds) {
		for(AndCond cond : possibleConds) {
			if(state.satisfy(cond)) {
				return true;
			}
		}
		return false;
	}
	
	public static List<GroundAction> getRelevantActions(RelState state, Collection<GroundAction> actions) {
		List<GroundAction> relevantActions = new ArrayList<GroundAction>();
		
		List<GroundAction> applicableActions = PDDLUtils.getApplicableActions(state, actions);
		RelState stateClone;
		
		for(GroundAction a : applicableActions) {
			stateClone = state.clone();
			if(!(stateClone.apply(a)).equals(state)) {
				relevantActions.add(a);
			}
		}
		
		return relevantActions;
	}
	
	public static List<GroundAction> getApplicableActions(RelState state, Collection<GroundAction> actions) {
		List<GroundAction> applicableActions = new ArrayList<GroundAction>();
		
		for(GroundAction a : actions) {
			if(state.satisfy(a.getPreconditions())) {
				applicableActions.add(a);
			}
		}

		return applicableActions;
	}
	
	public static List<String> getPossiblePredicates(RelState state, EPddlProblem problem) {
		List<String> pPredicates = new ArrayList<String>();
		
		Predicate p;
		for(Object o : problem.getActualFluents().keySet()) {
			if(o instanceof Predicate) {
				p = (Predicate)o;
				if(state.canBeTrue(p)) {
					pPredicates.add(p.pddlPrint(false));
				}
			}
		}
		
		return pPredicates;
	}

	public static List<Condition> getPossiblePredicatesAsConditions(RelState state, EPddlProblem problem) {
		List<Condition> pPredicates = new ArrayList<Condition>();

		Predicate c;
		for(Object o : problem.getActualFluents().keySet()) {
			if(o instanceof Predicate) {
				c = (Predicate)o;
				if(state.canBeTrue(c)) {
					pPredicates.add(c);
				}
			}
		}

		return pPredicates;
	}

	public static List<String> getDifferentPredicates(RelState rel1, RelState rel2, EPddlProblem problem) {
		List<String> diff = new ArrayList<String>();

		Predicate p;
		for(Object o : problem.getActualFluents().keySet()) {
			if(o instanceof Predicate) {
				p = (Predicate)o;
				if((rel1.canBeTrue(p) && !rel2.canBeTrue(p)) || (!rel1.canBeTrue(p) && rel2.canBeTrue(p))) {
					diff.add(p.pddlPrint(false));
				}
			}
		}

		return diff;
	}

	public static List<String> getAddEffects(GroundAction action) {
		List<String> predicates = new ArrayList<String>();

		for(Object o : action.getAddList().sons) {
			predicates.add(((Predicate)o).pddlPrint(false));
		}

		return predicates;
	}

	public static List<String> getPreconditions(GroundAction action) {
		List<String> predicates = new ArrayList<String>();

		for(Object o : action.getPreconditions().getInvolvedPredicates()) {
			predicates.add(((Predicate)o).pddlPrint(false));
		}

		return predicates;
	}

	public static String convertAction(GroundAction action) {
		String ecoString = action.toEcoString();

		String[] split = ecoString.split(" Parameters: ");
		String name = split[0];

		String[] split2 = split[1].split(" ");

		List<String> parameters = new ArrayList<String>();
		for(int i=0; i < split2.length; i++) {
			if(split2[i].trim().startsWith("-")) {
				i++;
				continue;
			}
			if(!split2[i].trim().equals("")) {
				parameters.add(split2[i].trim().replace(" ", ""));
			}
			
		}

		StringBuffer convAction = new StringBuffer();

		convAction.append("(");
		convAction.append(name);

		for(String p : parameters) {
			convAction.append(" " + p);
		}
		convAction.append(")");

		return convAction.toString().replace(" ", "_");
	}

	public static String convertFact(Predicate fact) {
		String conversion = fact.pddlPrint(false).trim().replace(" ", "_");
		
		return conversion;
	}

	public static List<GroundAction> determineSupporters(Predicate p, List<GroundAction> actions) {
		List<GroundAction> supporters = new ArrayList<GroundAction>();

		for(GroundAction action : actions) {
			if(action.getAddList().getInvolvedPredicates().contains(p)) {
				supporters.add(action);
			}
			boolean found = false;
			if(action.cond_effects != null) {
				for(ConditionalEffect cEffect : (Collection<ConditionalEffect>) action.cond_effects.sons) {
					for(Condition c : ((AndCond)cEffect.effect).getTerminalConditions()) {
						if(c instanceof Predicate) {
							if(p.pddlPrint(false).equals(((Predicate)c).pddlPrint(false))) {
								supporters.add(action);
								found = true;
								break;
							}
						}
					}
					if(found) {
						break;
					}
				}
			}
		}

		return supporters;
	}

	public static List<GroundAction> findSupportersForGoal(Predicate goal, List<GroundAction> completeActions, List<Predicate> init, EPddlProblem problem, LandmarkExtraction le) {
		//Set that holds the returned supporters
		Set<GroundAction> supporters = new HashSet<GroundAction>();

		//Get the count of how often each action was already samples for the current goal
		Map<GroundAction, Integer> actionCounts = PDDLUtils.ACTION_COUNTS.get(goal);
		if(actionCounts == null) {
			actionCounts = new HashMap<GroundAction, Integer>();
			PDDLUtils.ACTION_COUNTS.put(goal, actionCounts);
		}
		
		//List of predicates for which supporters have to be found
		LinkedList<Predicate> supToBeFound = new LinkedList<Predicate>();
		supToBeFound.add(goal);

		//Set of predicates for which supporters were already found
		Set<Predicate> supFound = new HashSet<Predicate>();

		//Helper list that hold potential supporters
		List<GroundAction> pSups = new ArrayList<GroundAction>();

		//List that holds all selected supporters
		List<GroundAction> selectedSups = new ArrayList<GroundAction>();
		
		//Indicates whether an action was already selected
		boolean actionSelected;
		
		//Helper variables
		Predicate p;
		Set<Predicate> predsToAdd = new HashSet<Predicate>();
		Set<Predicate> predsToRemove = new HashSet<Predicate>();
		
		//Iterate backwards through the levels of the RPG contained in le
		for(int t = le.levels-1; t >= 0; t--) {

			//While there are still predicates that have to be supported
			while(supToBeFound.size() > 0) {
				
				//Get predicate from queue of predicates that have to be supported
				p = supToBeFound.pollLast();


				pSups.clear();

				//Iterate forward through the RPG until the level of the outer loop is reached
				for(int t2 = 0; t2 <= t; t2++) {
					pSups.addAll(PDDLUtils.determineSupporters(p, (List<GroundAction>)le.action_levels.get(t2)));
					if(pSups.size() > 0) {
						break;
					}
				}

				if(pSups.size() == 0) {
					continue;
				}
				
				//In this case pSups is not empty and, hence, supporters for p were found
				supFound.add(p);

				//Remove all actions from the potential supporter set that are already supporters
				pSups.removeAll(supporters);

				//Initialize action counts for all potential supporters
				Integer count;
				for(GroundAction a : pSups) {
					count = actionCounts.get(a);
					if(count == null) {
						actionCounts.put(a, 0);
					}
				}

				//If there is more than one potential supporter left
				if(pSups.size() > 1) {
					int minCount = Integer.MAX_VALUE;
					for(GroundAction a : pSups) {
						count = actionCounts.get(a);
						if(count < minCount) {
							minCount = count;
						}
					}

					//Remove all potential supporters that occured already more often than the action with the minimal count
					List<GroundAction> actionsToRemove = new ArrayList<GroundAction>();
					for(GroundAction a : pSups) {
						if(actionCounts.get(a) > minCount) {
							actionsToRemove.add(a);
						}
					}

					//If not all actions have the same count, remove all actions with higher counts from potential supporter list
					if(actionsToRemove.size() != pSups.size()) {
						pSups.removeAll(actionsToRemove);
					}
				}

				//Select supporters
				selectedSups.clear();
				if(pSups.size() > 0) {
					actionSelected = false;
					while(!actionSelected) {
						selectedSups.add(PDDLUtils.selectAction(pSups, init, supFound, supToBeFound));
						actionSelected = true;
					}
				}

				//Add selected supporters to the result set
				supporters.addAll(selectedSups);

				//Update queue for predicates that have to be supported with precondtitions of selected supporters
				for(GroundAction a : selectedSups) {
					supFound.addAll(a.getAddList().getInvolvedPredicates());
					if(!(a.getAddList().getInvolvedPredicates().contains(p))) {
						for(ConditionalEffect cEffect : (Collection<ConditionalEffect>)a.cond_effects.sons) {
							if(((AndCond)cEffect.effect).getInvolvedPredicates().contains(p)) {
								supFound.addAll(((AndCond)cEffect.effect).getInvolvedPredicates());
								PDDLUtils.processComplexCond(cEffect.activation_condition, predsToAdd, supFound, supToBeFound, init);
							}
						}
					}
				}
				for(GroundAction a : selectedSups) {
					for(Condition c : a.getPreconditions().getTerminalConditions()) {
						if(c instanceof NotCond) {
							continue;
						}
						else if(c instanceof AndCond) {
							PDDLUtils.processComplexCond(c, predsToAdd, supFound, supToBeFound, init);
						}
						else if(c instanceof Predicate && !supFound.contains(c) && !supToBeFound.contains(c) && !init.contains(c)) {
							// supToBeFound.add((Predicate)c);
							predsToAdd.add((Predicate)c);
						}
					}
					predsToRemove.addAll(a.getAddList().getInvolvedPredicates());
				}
				supToBeFound.removeAll(predsToRemove);
				predsToAdd.removeAll(predsToRemove);
				predsToRemove.clear();
			}
			supToBeFound.addAll(predsToAdd);
			predsToAdd.clear();
		}

		selectedSups.clear();
		for(GroundAction a : supporters) {
			selectedSups.add(a);
		}

		return selectedSups;
	}

	public static GroundAction selectAction(List<GroundAction> actions, List<Predicate> initialPredicates, Set<Predicate> alreadySupportedPredicates, List<Predicate> supportersToBeFound) {
		Random random = new Random();
		return actions.get(random.nextInt(actions.size()));
	}

	public static void processComplexCond(Condition c, Set<Predicate> predsToAdd, Set<Predicate> supFound, LinkedList<Predicate> supToBeFound, List<Predicate> init) {
	    if(c instanceof AndCond) {
		    for(Condition tc : c.getTerminalConditions()) {
			    PDDLUtils.processComplexCond(tc, predsToAdd, supFound, supToBeFound, init);
		    }
	    }
	    else if(c instanceof Predicate && !supFound.contains(c) && !supToBeFound.contains(c) && !init.contains(c)) {
		    predsToAdd.add((Predicate)c);
	    }   
    }

	public static Map<String, List<Pair<GroundAction, Double>>> determineRelevantActionsForGoal(EPddlProblem problem, List<Pair<GroundAction, Double>> actions) {
		Map<String, List<Pair<GroundAction, Double>>> actionsPerPredicate = new HashMap<String, List<Pair<GroundAction, Double>>>();

		//Add goals to the searched predicates
		for(Predicate pred : problem.getGoals().getInvolvedPredicates()) {
			actionsPerPredicate.put(pred.pddlPrint(false), new ArrayList<Pair<GroundAction, Double>>());
		}

		Set<String> previousKeySet;
		List<String> effects;

		//Search for new supporters and/or new predicates that have to be supported.
		//Loop ends when the set of supported predicates does not change after a complete loop.
		do {
			previousKeySet = new HashSet<String>();
			previousKeySet.addAll(actionsPerPredicate.keySet());
			
			//Iterate over all actions and check whether they support any of the searched predicates
			for(Pair<GroundAction, Double> action : actions) {
				effects = PDDLUtils.getAddEffects(action.getLeft());

				//Iterate over all searched predicates
				for(String key : previousKeySet) {

					//If action supports the currently examined predicate, add the preconditions to the searched predicates
					if(effects.contains(key)) {
						if(!actionsPerPredicate.get(key).contains(action)) {
							actionsPerPredicate.get(key).add(action);
							for(String nKey : PDDLUtils.getPreconditions(action.getLeft())) {
								if(actionsPerPredicate.get(nKey) == null) {
									actionsPerPredicate.put(nKey, new ArrayList<Pair<GroundAction, Double>>());
								}
							}
						}
					}					
				}
			}
		}while(!previousKeySet.equals(actionsPerPredicate.keySet()));

		return actionsPerPredicate;
	}

	public static String convertIntToBinary(int number, int stringLength) {
		String result = Integer.toBinaryString(number);
		while(result.length() < stringLength) {
			result = "0" + result;
		}

		return result;
	}

	public static String convertLongToBinary(long number, int stringLength) {
		String result = Long.toBinaryString(number);
		while(result.length() < stringLength) {
			result = "0" + result;
		}

		return result;
	}

	public static List<String> convertActionParameters(GroundAction action) {
		List<String> tmp = new ArrayList<String>();
		for (PDDLObject o : (Collection<PDDLObject>) action.getParameters()) {
                    tmp.add(o.getName());
        }
		return tmp;
	}

	public static void advancePredicateOccurenceCount(GroundAction action, Map<String, Double> predicateOccurences) {
		double occurence;
		for(String p : PDDLUtils.getAddEffects(action)) {
			occurence = predicateOccurences.get(p);
			occurence += 1.0;
			predicateOccurences.put(p, occurence);
		}
	}
	
	public static List<String> getPossiblePredicatesWithCounter(int counter, Map<Integer, List<String>> possiblePredicates) {
		if(possiblePredicates.get(counter) == null) {
			return possiblePredicates.get(PDDLUtils.getMaxKey(possiblePredicates.keySet()));
		}
		return possiblePredicates.get(counter);
	}
	
	public static List<List<String>> getPossiblePredicatesWithCounterV2(int counter, Map<Integer, List<List<String>>> possiblePredicates) {
		if(possiblePredicates.get(counter) == null) {
			return possiblePredicates.get(PDDLUtils.getMaxKey(possiblePredicates.keySet()));
		}
		
		return possiblePredicates.get(counter);
	}
	
	public static int getMaxKey(Set<Integer> keys) {
		int max = Integer.MIN_VALUE;
		
		for(int key : keys) {
			if(key > max) {
				max = key;
			}
		}
		
		return max;
	}
	
	public static Map<Integer, List<String>> getPossiblePredicatesFromObsSequence(File obsFile, PddlDomain domainFile, EPddlProblem problemFile) {
		List<String> observations = new ArrayList<String>();
		
		SimplePlanSamplingStateHandler handler = new SimplePlanSamplingStateHandler(domainFile, problemFile);
		
		try {
			observations = Files.readAllLines(Paths.get(obsFile.getAbsolutePath()));
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		for(String obs : observations) {
			obs = ObsUtils.parseObservationFileLine(obs);
			handler.advanceCurrentRelaxedState(obs);
		}
		
		Map<Integer, List<String>> result = new HashMap<Integer, List<String>>();
		result.put(observations.size(), PDDLUtils.getPossiblePredicates(handler.getCurrentRelaxedState(), problemFile));
		return result;
	}
	
	public static Map<Integer, List<String>> getPossiblePredicatesMapFromObsSequence(File obsFile, PddlDomain domainFile, EPddlProblem problemFile, int aggregationConstant) {
		List<String> observations = new ArrayList<String>();
		
		SimplePlanSamplingStateHandler handler = new SimplePlanSamplingStateHandler(domainFile, problemFile);
		
		try {
			observations = Files.readAllLines(Paths.get(obsFile.getAbsolutePath()));
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		Map<Integer, List<String>> predicatesMap = new HashMap<Integer, List<String>>();
		
		int mapCounter = 1;
		
		String obs;
		for(int i=1; i <= observations.size(); i++) {
			obs = ObsUtils.parseObservationFileLine(observations.get(i-1));
			handler.advanceCurrentRelaxedState(obs);
			
			if(i%aggregationConstant == 0) {
				for(;mapCounter < i; mapCounter++) {
					predicatesMap.put(mapCounter, PDDLUtils.getPossiblePredicates(handler.getCurrentRelaxedState().clone(), handler.getGroundProblem()));
				}
			}
		}	

		return predicatesMap;
	}

	public static Map<Integer, List<String>> getPossiblePredicateMapFromObsSequenceSingleStates(File obsFile, PddlDomain domainFile, EPddlProblem problemFile) {
		List<String> observations = new ArrayList<String>();
		
		SimplePlanSamplingStateHandler handler = new SimplePlanSamplingStateHandler(domainFile, problemFile);
		
		try {
			observations = Files.readAllLines(Paths.get(obsFile.getAbsolutePath()));
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		Map<Integer, List<String>> predicatesMap = new HashMap<Integer, List<String>>();
		
		String obs;
		for(int i=1; i <= observations.size(); i++) {
			obs = ObsUtils.parseObservationFileLine(observations.get(i-1));
			handler.advanceCurrentRelaxedState(obs);
			
			predicatesMap.put(i, PDDLUtils.getPossiblePredicates(handler.getCurrentRelaxedState().clone(), handler.getGroundProblem()));

		}
		
		return predicatesMap;
	}
	
	public static List<String> getPredicates(PDDLState state, EPddlProblem problem) {
		List<String> predicates = new ArrayList<String>();
		
		Predicate p;
		for(Object o : problem.getActualFluents().keySet()) {
			if(o instanceof Predicate) {
				p = (Predicate)o;
				if(state.holds(p)) {
					predicates.add(p.pddlPrint(false));
				}
			}
		}
		
		return predicates;
	}
	
	public static List<String> getPredicates(RelState state, EPddlProblem problem) {
		List<String> predicates = new ArrayList<String>();
		
		Predicate p;
		for(Object o : problem.getActualFluents().keySet()) {
			if(o instanceof Predicate) {
				p = (Predicate)o;
				if(state.canBeTrue(p)) {
					predicates.add(p.pddlPrint(false));
				}
			}
		}
		
		return predicates;
	}
	
	public static List<Double> getIntersection(List<List<Double>> vectors) {
		int size = vectors.get(0).size();
		for(int i=1; i < vectors.size(); i++) {
			if(vectors.get(i).size() != size) {
				System.out.println("Not all vectors of the same size!");
				return null;
			}
		}
		
		double[] intersection = new double[size];
		for(int i=0; i < intersection.length; i++) {
			intersection[i] = 1.0;
		}
		
		for(List<Double> vector : vectors) {
			for(int i=0; i < vector.size(); i++) {
				intersection[i] *= vector.get(i);
			}
		}
		
		List<Double> intersectionList = new ArrayList<Double>();
		for(int i=0; i < intersection.length; i++) {
			intersectionList.add(intersection[i]);
		}
		
		return intersectionList;
	}
	
	public static List<Double> getIntersectionFromMapList(List<Map<Integer, List<Double>>> hypMapList) {
		List<List<Double>> vectors = new ArrayList<List<Double>>();
		for(Map<Integer, List<Double>> map : hypMapList) {
			for(int key : map.keySet()) {
				vectors.add(map.get(key));
			}
		}
		
		return PDDLUtils.getIntersection(vectors);
	}	
}

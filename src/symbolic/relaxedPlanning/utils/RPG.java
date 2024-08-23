package symbolic.relaxedPlanning.utils;

import com.google.common.collect.Sets;
import com.hstairs.ppmajal.conditions.*;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;

import config.GoalRecognitionConfiguration;
import symbolic.relaxedPlanning.bestSupporterFunctions.BestSupporterSelectionFunction;

import com.hstairs.ppmajal.expressions.ExtendedNormExpression;
import com.hstairs.ppmajal.expressions.NumEffect;
import com.hstairs.ppmajal.expressions.NumFluent;
import com.hstairs.ppmajal.expressions.PDDLNumber;

import java.util.*;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.math3.util.CombinatoricsUtils;

public class RPG {
	public int levels;
	public Vector<ArrayList<GroundAction>> action_level;
	public Vector<RelState> rel_state_level;
	public ComplexCondition goal;
	public boolean goal_reached;
	public Map<Predicate, Set<Predicate>> firstAchiever;
	public int goal_reached_at;
	private long cpu_time;
	private long spezzTime;
	private int numberOfActions;
	private Set relevantActions;
	private PDDLState init;
	private RelState relInit;
	private RelState fixPoint;
	public ArrayList<ArrayList<GroundAction>> plan_levels;
	public Map<Integer, ArrayList<GroundAction>> plan_levels_map;
	public List<Map<Integer, List<GroundAction>>> plan_levels_map_list;

	public RPG() {
		super();
		levels = 0;
		action_level = new Vector();
		rel_state_level = new Vector();
		goal_reached = false;
		spezzTime = 0;
		goal_reached_at = 0;
		relevantActions = new HashSet();
	}

	public RPG(RelState init) {
		super();
		levels = 0;
		action_level = new Vector();
		rel_state_level = new Vector();
		goal_reached = false;
		spezzTime = 0;
		relevantActions = new HashSet();
		this.relInit = init;
		goal_reached_at = 0;
	}

	public RPG(PDDLState init) {
		super();
		levels = 0;
		action_level = new Vector();
		rel_state_level = new Vector();
		goal_reached = false;
		spezzTime = 0;
		relevantActions = new HashSet();
		this.init = init;
		goal_reached_at = 0;
	}


	public ArrayList[] computeRelaxedPlan_new(PDDLState initialState, ComplexCondition goal, Set<GroundAction> actions,
			boolean evaluationMode) throws CloneNotSupportedException {

		this.goal = (ComplexCondition) goal.clone();
		RelState current = initialState.relaxState();
		rel_state_level.add(current.clone());

		// actions to be used for planning purposes. This list is a temporary collection
		// to do not compromise the input structure
		ArrayList<GroundAction> acts = new ArrayList<GroundAction>();
		acts.addAll(actions);

		long start = System.currentTimeMillis();

		ArrayList<GroundAction> level = new ArrayList<GroundAction>();
		levels = 0;

		ArrayList previousLevel = new ArrayList();

		while (true) {

			if (current.satisfy(goal)) {
				goal_reached = true;
				this.goal_reached_at = levels;
				break;// goal reached!

			} else {
				level = new ArrayList();
				for (Iterator it = acts.iterator(); it.hasNext();) {
					GroundAction gr = (GroundAction) it.next();

					if (gr.isApplicable(current)) {
						level.add(gr);
						if (gr.getNumericEffects() == null) {
							it.remove();
						}

					}
				}

				if (level.isEmpty() || level.equals(previousLevel)) {
					this.goal_reached = false;
					numberOfActions = Integer.MAX_VALUE;

					if (!evaluationMode) {
						System.out.println("Relaxed plan computation: goal not reached");
					}
					return null;// it means that the goal is unreacheable!
				}

				previousLevel = (ArrayList) level.clone();

				long start2 = System.currentTimeMillis();

				for (Object o : level) {
					GroundAction gr = (GroundAction) o;

					gr.apply(current);

				}
				spezzTime += System.currentTimeMillis() - start2;
				this.action_level.add(level);
				this.rel_state_level.add(current.clone());
				levels++;
			}
		}


		this.setFixPoint(current.clone());
		this.goal_reached = true;
		long start2 = System.currentTimeMillis();
		ArrayList[] ret1 = extractPlan_new();
		cpu_time = System.currentTimeMillis() - start;


		if (ret1 == null) {
			System.out.println("Couldnt extract Plan");
			System.exit(-1);
		}

		return ret1;

	}

	public boolean checkPlan() {

		RelState state = init.relaxState();

		for (int i = 0; i < plan_levels.size(); i++) {

			for (int j = 0; j < plan_levels.get(i).size(); j++) {

				GroundAction g = plan_levels.get(i).get(j);

				if (g.isApplicable(state)) {

					state.apply(g);
					System.out.println("Applying " + g.getName() + g.getParameters());
				}

				else {

					System.out.println(
							g.getName() + g.getParameters() + " is not applicable, searching for supporters...");

					ArrayList<GroundAction> supporters = findSupportersApproach2(action_level.get(i),
							g.getPreconditions(), state);

					System.out.println("Supporters:");
					for (GroundAction s : supporters) {
						System.out.println(s.getName() + s.getParameters());
					}

					System.out.println("- - - - - - - - ");

					if (supporters.isEmpty()) {
						System.out.println("Oh no no no no");
					}

					plan_levels.get(i).addAll(j, supporters);

					return false;

				}
			}
		}

		return true;
	}

	public Map<Integer, ArrayList<GroundAction>> findAllRelevantActions(RelState init,
			List<Predicate> relevantPredicates) {
		this.plan_levels_map = new HashMap<Integer, ArrayList<GroundAction>>();

		ArrayList<GroundAction> plan = new ArrayList<GroundAction>();

		ComplexCondition goalCondition = (ComplexCondition) this.goal.clone();

		List<MutablePair<Integer, Condition>> backchainedConditions = new ArrayList<MutablePair<Integer, Condition>>();

		for (Condition c : (Collection<Condition>) goalCondition.sons) {

			// 0 == AND COND
			if (goalCondition instanceof AndCond) {
				backchainedConditions.add(new MutablePair(0, c));
			}

			// 1 == OR COND
			else if (goalCondition instanceof OrCond) {
				backchainedConditions.add(new MutablePair(1, c));
			} else {
				System.out.println("Weird goal condition: " + goalCondition.getClass());
				System.exit(-1);
			}
		}

		Set<GroundAction> alreadyAppliedActions = new HashSet<GroundAction>();
		for (int t = levels - 1; t >= 0; t--) {

			RelState state = (RelState) rel_state_level.get(t).clone();
			RelState originalState = state.clone();

			Iterator<MutablePair<Integer, Condition>> it = backchainedConditions.listIterator();

			ArrayList<MutablePair<Integer, Condition>> sonsToAdd = new ArrayList<MutablePair<Integer, Condition>>();

			ArrayList<GroundAction> level_list = new ArrayList<GroundAction>();

			ArrayList<GroundAction> usedActions;
			while (it.hasNext()) {
				MutablePair<Integer, Condition> p = it.next();
				Condition c = p.getRight();

				/*
				 * If a condition is already possible in the initial state or has been made
				 * possible through an already applied action, then skip this condition.
				 */
				if ((!(c.can_be_true(originalState)) && c.can_be_true(state)) || ((c.can_be_true(state) && t == 0))
						|| ((c.can_be_true(state) && c.can_be_true(originalState) && t == 0))) {
					it.remove();
					continue;
				}

				if (!(c.can_be_true(state))) {
					usedActions = new ArrayList<GroundAction>();
					for (int t2 = t; t2 >= 0; t2--) {
						usedActions.addAll(this.action_level.get(t2));
					}
					List<GroundAction> acts;
					
					if(GoalRecognitionConfiguration.getRelaxedPlanningHeuristic() == BestSupporterSelectionFunction.GREEDY) {
						acts = findSupportersApproach3((ArrayList<GroundAction>)usedActions, c, state, relevantPredicates, init);
					} 
					else {
						acts = findSupportersApproach4((ArrayList<GroundAction>) usedActions, c, state,
							relevantPredicates, init).get(0);
					}
					// List<GroundAction> acts = findSupportersApproach3((ArrayList<GroundAction>)usedActions, c, state, relevantPredicates, init);

					if (acts.isEmpty()) {
						System.out.println();
						System.out.println("Error! No supporter for " + c + "at level " + t);
						System.out.println(c.getClass());
						System.exit(-1);
					}

					RelState tmpState = originalState.clone();

					for (GroundAction a : acts) {
						if (alreadyAppliedActions.contains(a)) {
							continue;
						}
						alreadyAppliedActions.add(a);
						tmpState.apply(a);
						if (a.getPreconditions() != null) {

							for (Condition preCond : (Collection<Condition>) a.getPreconditions().sons) {

								if (!(preCond instanceof NotCond)) {

									// 0 == AND COND
									if (a.getPreconditions() instanceof AndCond) {

										sonsToAdd.add(new MutablePair(0, preCond));
									}
									// 1 == OR COND
									else if (a.getPreconditions() instanceof OrCond) {

										sonsToAdd.add(new MutablePair(1, preCond));

									} else {
										System.out.println("Weird Precondition: " + a.getPreconditions().getClass());
										System.exit(-1);
									}
								}
							}

						}
						level_list.add(a);
					}
					it.remove();
				}
			}

			// Add all actions that were determined as supporter to the corresponding level
			// of the plan_level_map
			for (GroundAction ga : level_list) {
				for (int t2 = 0; t < this.levels; t2++) {
					if (this.action_level.get(t2).contains(ga)) {
						ArrayList<GroundAction> planL = plan_levels_map.get(t2);
						if (planL == null) {
							planL = new ArrayList<GroundAction>();
							plan_levels_map.put(t2, planL);
						}
						planL.add(ga);
						break;
					}
				}
			}

			plan.addAll(0, level_list);

//			plan_levels.add(0, level_list);

			backchainedConditions.addAll(sonsToAdd);
		}

//		int sum = 0;
//		for(int key : this.plan_levels_map.keySet()) {
//			sum += this.plan_levels_map.get(key).size();
//		}
//		System.out.println(sum);

		System.out.println("Plan levels map finished!");

		return plan_levels_map;
	}

	public List<Map<Integer, List<GroundAction>>> findAllRelevantActionsV2(int noSamples, RelState init,
			List<Predicate> relevantPredicates) {
		this.plan_levels_map_list = new ArrayList<Map<Integer, List<GroundAction>>>();
		Map<Integer, List<GroundAction>> planLevelsMap = new HashMap<Integer, List<GroundAction>>();
		Map<Condition, List<List<GroundAction>>> supporterMap = new HashMap<Condition, List<List<GroundAction>>>();

		ArrayList<GroundAction> plan = new ArrayList<GroundAction>();

		ComplexCondition goalCondition = (ComplexCondition) this.goal.clone();

		Random rand = new Random();
		for (int sample = 0; sample < noSamples; sample++) {
			planLevelsMap = new HashMap<Integer, List<GroundAction>>();

			List<MutablePair<Integer, Condition>> backchainedConditions = new ArrayList<MutablePair<Integer, Condition>>();

			for (Condition c : (Collection<Condition>) goalCondition.sons) {

				// 0 == AND COND
				if (goalCondition instanceof AndCond) {
					backchainedConditions.add(new MutablePair(0, c));
				}

				// 1 == OR COND
				else if (goalCondition instanceof OrCond) {
					backchainedConditions.add(new MutablePair(1, c));
				} else {
					System.out.println("Weird goal condition: " + goalCondition.getClass());
					System.exit(-1);
				}
			}

			Set<GroundAction> alreadyAppliedActions = new HashSet<GroundAction>();
			for (int t = this.levels - 1; t >= 0; t--) {

				RelState state = (RelState) this.rel_state_level.get(t).clone();
				RelState originalState = state.clone();

				Iterator<MutablePair<Integer, Condition>> it = backchainedConditions.listIterator();

				ArrayList<MutablePair<Integer, Condition>> sonsToAdd = new ArrayList<MutablePair<Integer, Condition>>();

				ArrayList<GroundAction> level_list = new ArrayList<GroundAction>();

				ArrayList<GroundAction> usedActions;
				while (it.hasNext()) {
					MutablePair<Integer, Condition> p = it.next();
					Condition c = p.getRight();

					/*
					 * If a condition is already possible in the initial state or has been made
					 * possible through an already applied action, then skip this condition.
					 */
					if ((!(c.can_be_true(originalState)) && c.can_be_true(state)) || ((c.can_be_true(state) && t == 0))
							|| ((c.can_be_true(state) && c.can_be_true(originalState) && t == 0))) {
						it.remove();
						continue;
					}

					if (!(c.can_be_true(state))) {
						usedActions = new ArrayList<GroundAction>();
						for (int t2 = t; t2 >= 0; t2--) {
							usedActions.addAll(this.action_level.get(t2));
						}
						List<GroundAction> acts = findSupportersApproach3((ArrayList<GroundAction>) usedActions, c,
								state, relevantPredicates, init);
//						List<GroundAction> acts = findSupportersApproach2((ArrayList<GroundAction>) usedActions, c, init);

						// Find possible sets of supporters and randomly pick one set for each condition
						List<List<GroundAction>> actsList = supporterMap.get(c);

//						List<GroundAction> acts = findSupportersApproach3(usedActions, c, state, relevantPredicates, init);
						if (actsList == null) {
							actsList = this.findSupportersApproach4(usedActions, c, state, relevantPredicates, init);
							if (actsList.size() > 0) {
								supporterMap.put(c, actsList);
							}
							supporterMap.put(c, actsList);
						}

//						System.out.println(acts.size());
//						System.out.println(actsList.size());
//						List<GroundAction> acts = null;
						if (actsList.size() > 0) {
							acts = actsList.get(rand.nextInt(actsList.size()));
						} else {
							System.out.println();
							System.out.println("Error! No supporter for " + c + "at level " + t);
							System.out.println(c.getClass());
//							System.out.println(p.getLeft());
							System.out.println(acts.size());
//							System.exit(0);
							continue;
						}

						for (GroundAction a : acts) {
							if (alreadyAppliedActions.contains(a)) {
								continue;
							}
							alreadyAppliedActions.add(a);
							if (a.getPreconditions() != null) {

								for (Condition preCond : (Collection<Condition>) a.getPreconditions().sons) {

									if (!(preCond instanceof NotCond)) {

										// 0 == AND COND
										if (a.getPreconditions() instanceof AndCond) {

											sonsToAdd.add(new MutablePair(0, preCond));
										}
										// 1 == OR COND
										else if (a.getPreconditions() instanceof OrCond) {

											sonsToAdd.add(new MutablePair(1, preCond));

										} else {
											System.out
													.println("Weird Precondition: " + a.getPreconditions().getClass());
											System.exit(-1);
										}
									}
								}

							}
							level_list.add(a);
						}
						it.remove();
					}
				}

				// Add all actions that were determined as supporter to the corresponding level
				// of the plan_level_map
				for (GroundAction ga : level_list) {
					for (int t2 = 0; t2 < this.levels; t2++) {
						if (this.action_level.get(t2).contains(ga)) {
							List<GroundAction> planL = planLevelsMap.get(t2);
							if (planL == null) {
								planL = new ArrayList<GroundAction>();
								planLevelsMap.put(t2, planL);
							}
							planL.add(ga);
							break;
						}
					}
				}

				plan.addAll(0, level_list);

//			plan_levels.add(0, level_list);

				backchainedConditions.addAll(sonsToAdd);
			}
			this.plan_levels_map_list.add(planLevelsMap);
		}
//		int sum = 0;
//		for(int key : this.plan_levels_map.keySet()) {
//			sum += this.plan_levels_map.get(key).size();
//		}
//		System.out.println(sum);

		return plan_levels_map_list;
	}

	public boolean getLeafConditions(int parentType, List<MutablePair<Integer, Condition>> cList, Condition condition) {

		if (!(condition instanceof ComplexCondition)) {
			return false;
		}

		int type = condition instanceof AndCond ? 0 : 1;

		for (Condition child : (Collection<Condition>) ((ComplexCondition) condition).sons) {
			if (getLeafConditions(type, cList, child)) {
				cList.add(new MutablePair<Integer, Condition>(parentType, child));
			}
		}

		return true;
	}

	public ArrayList<GroundAction> computePlan() {

		this.plan_levels = new ArrayList<ArrayList<GroundAction>>();

		ArrayList<GroundAction> plan = new ArrayList<GroundAction>();

//		ArrayList[]	p = this.extractPlan_new();

		ComplexCondition goalCondition = (ComplexCondition) this.goal.clone();

		ArrayList<MutablePair<Integer, Condition>> tmp = new ArrayList<MutablePair<Integer, Condition>>();

		for (Condition c : (Collection<Condition>) goalCondition.sons) {

			// 0 == AND COND
			if (goalCondition instanceof AndCond) {

				tmp.add(new MutablePair(0, c));
			}

			// 1 == OR COND
			else if (goalCondition instanceof OrCond) {

				tmp.add(new MutablePair(1, c));

			}

			else {

				System.out.println("Weird goal condition: " + goalCondition.getClass());
				System.exit(-1);
			}
		}

		for (int t = levels - 1; t >= 0; t--) {

			RelState state = (RelState) rel_state_level.get(t).clone();
			RelState originalState = state.clone();

			Iterator<MutablePair<Integer, Condition>> it = tmp.listIterator();

			ArrayList<MutablePair<Integer, Condition>> sonsToAdd = new ArrayList<MutablePair<Integer, Condition>>();

			ArrayList<GroundAction> level_list = new ArrayList<GroundAction>();

			while (it.hasNext()) {

				MutablePair<Integer, Condition> p = it.next();

				Condition c = p.getRight();

				if ((!(c.can_be_true(originalState)) && c.can_be_true(state)) || ((c.can_be_true(state) && t == 0))
						|| ((c.can_be_true(state) && c.can_be_true(originalState) && t == 0))) {
					it.remove();
					continue;
				}

				if (!(c.can_be_true(state))) {
					System.out.println(c.pddlPrint(false));
					ArrayList<GroundAction> acts = findSupportersApproach2(
							(ArrayList<GroundAction>) this.action_level.get(t), c, state);

					if (acts.isEmpty()) {

						if (p.getLeft() == 1) {

							// Condition was part of or cond, may not need to be true
							it.remove();
							continue;
						}

						System.out.println();
						System.out.println("Error! No supporter for " + c + "at level " + t);
						System.out.println(c.getClass());

						System.exit(-1);
					}

					RelState tmpState = originalState.clone();
//					System.out.println(c.can_be_true(tmpState));
					for (Condition b : c.getTerminalConditions()) {
						System.out.println(b.pddlPrint(false) + " " + b.can_be_true(tmpState));
					}
					for (GroundAction a : acts) {
						System.out.println(a.getName() + " " + a.getParameters().pddlPrint());
						tmpState.apply(a);
						if (a.getPreconditions() != null) {

//							sonsToAdd.addAll(a.getPreconditions().sons);

//							sonsToAdd.add(a.getPreconditions());

//							for (Object con : a.getPreconditions().sons) {
//
//								if (!(con instanceof NotCond)) {
//									sonsToAdd.add(con);
//
//								}
//							}

							for (Condition preCond : (Collection<Condition>) a.getPreconditions().sons) {

								if (!(preCond instanceof NotCond)) {

									// 0 == AND COND
									if (a.getPreconditions() instanceof AndCond) {

										sonsToAdd.add(new MutablePair(0, preCond));
									}

									// 1 == OR COND
									else if (a.getPreconditions() instanceof OrCond) {

										sonsToAdd.add(new MutablePair(1, preCond));

									}

									else {

										System.out.println("Weird Precondition: " + a.getPreconditions().getClass());
										System.exit(-1);
									}
								}
							}

						}

						level_list.add(a);

					}
					System.out.println(c.can_be_true(tmpState));
					it.remove();

				}
			}

			plan.addAll(0, level_list);

			plan_levels.add(0, level_list);

			tmp.addAll(sonsToAdd);

		}

//		System.out.println("EARLY PLAN: ");
//
//		for (int i = 0; i < plan_levels.size(); i++) {
//
//			System.out.println("LEVEL " + i);
//
//			for (int j = 0; j < plan_levels.get(i).size(); j++) {
//
//				System.out.println(plan_levels.get(i).get(j).getName() + plan_levels.get(i).get(j).getParameters());
//			}
//
//			System.out.println("-");
//		}
//
//		System.out.println("--------------------");
//
//		while (true) {
//
//			if (checkPlan()) {
//
//				break;
//			}
//
//			else {
//				System.out.println("Plan changed");
//			}
//
//		}

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		return plan;
	}

	public boolean computeRelaxedPlanningGraph(ComplexCondition goal, Set<GroundAction> actions)
			throws CloneNotSupportedException {

		return computeRelaxedPlanningGraph(null, goal, actions);

	}

	public boolean computeRelaxedPlanningGraph(PDDLState initialState, ComplexCondition goal, Set<GroundAction> actions)
			throws CloneNotSupportedException {

		long start = System.currentTimeMillis();

		this.goal = (ComplexCondition) goal.clone();

		RelState current;

		if (relInit != null) {

			current = relInit.clone();

		} else {

			current = initialState.relaxState();

		}

		rel_state_level.add(current.clone());

		ArrayList<GroundAction> acts = new ArrayList<GroundAction>();
		acts.addAll(actions);

		ArrayList<GroundAction> level = new ArrayList<GroundAction>();
		levels = 0;

		ArrayList previousLevel = new ArrayList();

		while (true) {

			if (current.satisfy(goal)) {
				goal_reached = true;
				this.goal_reached_at = levels;
				break;// goal reached!

			} else {

				level = new ArrayList();
				for (Iterator it = acts.iterator(); it.hasNext();) {
					GroundAction gr = (GroundAction) it.next();

					if (gr.isApplicable(current)) {
						level.add(gr);
						if (gr.getNumericEffects() == null) {
							it.remove();
						}

					}
				}

				if (level.isEmpty() || level.equals(previousLevel)) {
					this.goal_reached = false;

					long end = System.currentTimeMillis();

//					System.out.println("RPG TIME: "+(end-start));

					return false;// it means that the goal is unreacheable!
				}

				previousLevel = (ArrayList) level.clone();

				for (Object o : level) {
					GroundAction gr = (GroundAction) o;
					gr.apply(current);

				}
				this.action_level.add(level);
				this.rel_state_level.add(current.clone());
				levels++;
			}
		}

		long end2 = System.currentTimeMillis();

//		System.out.println("RPG TIME: "+(end2-start)+", LEVEL: "+levels);

		return true;

	}

	public MultiValuedMap computeRelaxedPlan(PDDLState s, ComplexCondition goal, Set actions)
			throws CloneNotSupportedException {

		// System.out.println("Find relaxed plan");
		ArrayList ret = new ArrayList();

		this.goal = goal;
		RelState current = s.relaxState();
		rel_state_level.add(current.clone());

		// actions to be used for planning purposes. This list is a temporary collection
		// to do not compromise the input structure
		ArrayList acts = new ArrayList();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		ArrayList level = new ArrayList();
		levels = 0;

		ArrayList previousLevel = new ArrayList();

		while (true) {

			// System.out.println("CURRENT:"+current);
			if (current.satisfy(goal)) {
				goal_reached = true;
				this.goal_reached_at = levels;
//				System.out.println("Goal reached at level " + levels);
				break;// goal reached!
			} else {

				// System.out.println("Do sth");
				level = new ArrayList();
				for (Iterator it = acts.iterator(); it.hasNext();) {
					GroundAction gr = (GroundAction) it.next();
					if (gr.isApplicable(current)) {
						// if (gr.getPreconditions().isSatisfied(current)) {
						level.add(gr);
						if (gr.getNumericEffects() == null) {
							it.remove();
						}

					}
				}
				// System.out.println("Level Size: " + level.size());

				//
				if (level.isEmpty() || level.equals(previousLevel)) {
					this.goal_reached = false;
					numberOfActions = Integer.MAX_VALUE;
					// System.out.println("Relaxed plan computation: goal not reached");
					return null;// it means that the goal is unreacheable!
				}

				previousLevel = (ArrayList) level.clone();

				long start2 = System.currentTimeMillis();

				for (Object o : level) {
					GroundAction gr = (GroundAction) o;
					// System.out.println("Applying effect of:"+gr);
					gr.apply(current);

				}
				spezzTime += System.currentTimeMillis() - start2;
				this.action_level.add(level);
				this.rel_state_level.add(current.clone());
				levels++;
				// System.out.println("Level:"+levels);
			}
		}

		this.setFixPoint(current.clone());
		// System.out.println(current);
		this.goal_reached = true;
//		System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() - start));
		// System.out.println("Spezz Time:" + (spezzTime));
		// System.out.println("NumbersOfLevel" + (levels));
		long start2 = System.currentTimeMillis();
		MultiValuedMap ret1 = extractPlan(goal, levels);
		// System.out.println("estrazione:" + (System.currentTimeMillis() - start2));
		cpu_time = System.currentTimeMillis() - start;
		if (ret1 == null) {
			numberOfActions = Integer.MAX_VALUE;
			return ret1;
		}
		numberOfActions = ret1.size();

		// this considers also the interactions among actions. Inadmissible Heuristic
		// this.goal_reached_at = ret1.size();
		return ret1;

	}

	public int computeUntilFixedPoint(PDDLState s, Set actions) {

		RelState current = s.relaxState();
		ArrayList acts = new ArrayList(100000);
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		HashSet level = new HashSet(10000);
		numberOfActions = 0;
		while (true) {
			boolean newActions = false;
			for (Iterator it = acts.iterator(); it.hasNext();) {
				GroundAction gr = (GroundAction) it.next();
				if (gr.getPreconditions().can_be_true(current)) {
					newActions = true;
					level.add(gr);
					this.numberOfActions++;
					it.remove();
				}
			}
			if (!newActions) {
				break;// it means that the goal is unreacheable!
			}
			long start2 = System.currentTimeMillis();

			for (Object o : level) {
				GroundAction gr = (GroundAction) o;
				// System.out.println(gr.getName());
				gr.apply(current);
			}
			spezzTime += System.currentTimeMillis() - start2;
			levels++;
		}

		fixPoint = current.clone();
		System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() - start));
		System.out.println("NumbersOfLevel" + (levels));

		return numberOfActions;
	}

	public Set computeActionsUntilFixedPoint(PDDLState s, Set actions) {

		RelState current = s.relaxState();
		Set acts = new HashSet();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		HashSet level = new HashSet(10000);
		numberOfActions = 0;
		while (true) {

			boolean newActions = false;
			for (Iterator it = acts.iterator(); it.hasNext();) {
				GroundAction gr = (GroundAction) it.next();
				if (gr.getPreconditions().can_be_true(current)) {
					newActions = true;
					level.add(gr);
					this.numberOfActions++;
					it.remove();
				}
			}
			if (!newActions) {
				break;// it means that the goal is unreacheable!
			}
			long start2 = System.currentTimeMillis();

			for (Object o : level) {
				GroundAction gr = (GroundAction) o;
				// System.out.println(gr.getName());
				gr.apply(current);
			}
			spezzTime += System.currentTimeMillis() - start2;
			levels++;
		}
		fixPoint = current.clone();
		System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() - start));
		System.out.println("Levels Number: " + (levels));
		relevantActions = level;
		return level;
	}

	// The following function computes reacheability for the propositional part of
	// the problem. The numeric part is also considered but there just for the
	// purpose of identifying a
	// the relevant set of actions
//	public Set reacheability(PDDLState s, Set actions) {
//
//		RelState current = s.relaxState();
//		Set acts = new HashSet();
//		acts.addAll(actions);
//		long start = System.currentTimeMillis();
//		LinkedHashSet level = new LinkedHashSet();
//		numberOfActions = 0;
//		levels = 0;
//		while (true) {
//			boolean newActions = false;
//			for (Iterator it = acts.iterator(); it.hasNext();) {
//				GroundAction gr = (GroundAction) it.next();
//				if (gr.getPreconditions().can_be_true(current)) {
//					newActions = true;
//					level.add(gr);
//					this.numberOfActions++;
//					it.remove();
//				}
//			}
//			// storing information about the fact that the goal has been reached. Pay
//			// attention that for the numeric case having a goal that is not reached does
//			// not imply that
//			// the task is not solvable (using this procedure).
//			if (!goal_reached && current.satisfy(goal)) {
//				goal_reached = true;
//				goal_reached_at = levels;
//				// break;
//			}
//			// System.out.println(level.size());
//			this.action_level.add(level.clone());
//			if (!newActions) {
//				break;// fixpoint reached for the propositional part. For the numeric this is not a
//						// fixpoint, and seems hard to define a fixpoint for the numeric case
//			}
//			long start2 = System.currentTimeMillis();
//			for (Object o : level) {
//				GroundAction gr = (GroundAction) o;
//				// System.out.println(gr.getName());
//				gr.apply(current);
//			}
//			spezzTime += System.currentTimeMillis() - start2;
//			levels++;
//			// System.out.println("Level:"+levels);
//		}
//		System.out.println("Total Number of Levels Developped:" + levels);
//		fixPoint = current.clone();
//		FPCTime = System.currentTimeMillis() - start;
//		relevantActions = level;
//		this.cpu_time = System.currentTimeMillis() - start;
//		return level;
//	}

	// The following function computes reacheability for the propositional part of
	// the problem. The numeric part is also considered but there just for the
	// purpose of identifying a
	// the relevant set of actions. As before but it stops when the goal is reached
	// in the relaxed state.
//	public Set reacheabilityTillGoal(PDDLState s, Condition goal, Set actions) {
//
//		RelState current = s.relaxState();
//		Set acts = new HashSet();
//		acts.addAll(actions);
//		long start = System.currentTimeMillis();
//		HashSet level = new HashSet(10000);
//		numberOfActions = 0;
//		levels = 0;
//
//		while (true) {
//			if (current.satisfy(goal)) {
//				goal_reached = true;
//				break;
//			}
//			boolean newActions = false;
//			for (Iterator it = acts.iterator(); it.hasNext();) {
//				GroundAction gr = (GroundAction) it.next();
//
//				if (gr.getPreconditions() == null || gr.getPreconditions().can_be_true(current)) {
//					newActions = true;
//					level.add(gr);
//					this.numberOfActions++;
//					it.remove();
//				}
//			}
//			// System.out.println("Here:"+action_level.size());
//			// System.out.println(level.size());
//			this.action_level.add(level.clone());
//
//			// THIS IS A BUG: IT SHOULD CONSIDER THE FACT THAT THE SATISFACTION OF AT LEAST
//			// A CONDITION IS SATISFIED!
//			if (!newActions) {
//				System.out.println("No new actions applicable and goal is not reacheable...");
//				break;// it means that the goal is unreacheable!
//			}
//			long start2 = System.currentTimeMillis();
//
//			for (Object o : level) {
//				GroundAction gr = (GroundAction) o;
//				// System.out.println(gr.getName());
//				gr.apply(current);
//			}
//			spezzTime += System.currentTimeMillis() - start2;
//			levels++;
//		}
//		fixPoint = current.clone();
//		FPCTime = System.currentTimeMillis() - start;
//		relevantActions = level;
//		this.cpu_time = System.currentTimeMillis() - start;
//		return level;
//	}

	private MultiValuedMap extractPlan(ComplexCondition goal, int levels) {
		ComplexCondition AG[] = new ComplexCondition[levels + 1];
		AG[levels] = goal;
		// MultiValuedMap rel_plan = new MultiValuedMap();
		MultiValuedMap<Integer, GroundAction> rel_plan = new ArrayListValuedHashMap<>();

		for (int t = levels - 1; t >= 0; t--) {
			RelState tem = (RelState) rel_state_level.get(t);
			RelState s = tem.clone();
			AG[t] = new AndCond();
			Map visited = new HashMap();
			// System.out.println("Prima aggiunta proposizioni"+AG[t + 1].isSatisfied(s));
			for (Object o : AG[t + 1].sons) {
				if (o instanceof Predicate) {
					Predicate p = (Predicate) o;
					// System.out.println("Livello in esame:" + t);
					// System.out.println(p.isSatisfied((RelState) this.rel_state_level.get(t )));
					if ((!((p.can_be_true((RelState) rel_state_level.get(t)))))) {
						// if (!((p.isSatisfied(s)))) {
						if (visited.get(p) == null) {
							// System.out.println("Searching for the support for:" + p);
							// GroundAction gr = searchForAndRemove((Collection) this.action_level.get(t),
							// p);
							GroundAction gr = this.searchSupporter((Collection) this.action_level.get(t), p);
							if (gr == null) {
								System.out.println("Error!! No supporter for " + p + " level " + t);
								System.out.println(rel_plan);
								System.out.println("Requisiti livello successivo:" + AG[t + 1]);
								System.exit(-1);
							}
							AG[t].sons.addAll(gr.getPreconditions().sons);
							// System.out.println("AG[t]" + AG[t]);
							// AG[t].sons.addAll(gr.getNumeric().sons);
							// if (gr.getName().equalsIgnoreCase("tp")){
							// System.out.println("prima applicazione tp" +s);
							// }

							for (Object o1 : gr.getAddList().sons) {
								visited.put(o1, true);
							}
							gr.apply(s);
							// if (gr.getName().equalsIgnoreCase("tp-lr")){
							// System.out.println("dopo applicazione tp" +s);
							// }
							// AG[t].sons.remove(o);
							rel_plan.put(t, gr);
						}

					} else {
						AG[t].sons.add(p);
					}

				}
			}
//	            for(Object o:rel_plan){
//	                GroundAction gr = (GroundAction)o;
//	                gr.apply(s);
//	            }

			Iterator it = AG[t + 1].sons.iterator();
			while (it.hasNext()) {
				Object o = it.next();
				if (o instanceof Comparison) {
					Comparison comp = (Comparison) o;
//	                    while(true){
					for (GroundAction gr : (Collection<GroundAction>) this.action_level.get(t)) {
						if (comp.can_be_true(s)) {
							AG[t].sons.add(comp);
							break;
						}
						gr.generateAffectedNumFluents();
//	                            System.out.println("Condizione non soddisfatta:" + comp);
//	                            System.out.println("Piano correntemente trovato:");
//	                            for (int z = 0; z < rel_plan.length; z++) {
//	                               if (rel_plan[z] != null) {
//	                                  System.out.println(rel_plan[z]);
//	                               }
//	                            }
						if (comp.involve(gr.getNumericFluentAffected())) {
							// System.out.println(gr.getNumericFluentAffected());
							gr.apply(s);
							AG[t].sons.addAll(gr.getPreconditions().sons);
							rel_plan.put(t, gr);
						}

					}
					// !comp.isSatisfied(s)
//	                    }
				}
			}

		}

		return rel_plan;

	}

	private ArrayList[] extractPlan_new() {
		ComplexCondition AG;
		AG = (ComplexCondition) this.goal.clone();
//		System.out.println("GOAL:" + AG);
		ArrayList rel_plan[] = new ArrayList[levels];

		LinkedHashSet tmp = (LinkedHashSet) AG.sons;

//		System.out.println(levels);

		for (int t = levels - 1; t >= 0; t--) {

			RelState state = (RelState) rel_state_level.get(t).clone();
			RelState originalState = state.clone();

			Iterator it = tmp.iterator();

			HashSet sonsToAdd = new HashSet();

			while (it.hasNext()) {

				Object o = it.next();

				Condition c = (Condition) o;

//				if (c.toString().equalsIgnoreCase("(>= ((+ 0.0(* -1.0 ((objects_taken))))) ((+ 0.0)))")) {
//
//					System.out.println("(>= ((+ 0.0(* -1.0 ((objects_taken))))) ((+ 0.0)))" + " is " + c.can_be_true(state) + " at level " + t);
//				}
//
//				if (c.toString().equalsIgnoreCase("(made_pizza)")) {
//
//					System.out.println("(made_pizza)" + " is " + c.can_be_true(state) + " at level " + t);
//				}

				if ((!(c.can_be_true(originalState)) && c.can_be_true(state)) || ((c.can_be_true(state) && t == 0))
						|| ((c.can_be_true(state) && c.can_be_true(originalState) && t == 0))) {
					it.remove();
					continue;
				}

//				if(c instanceof Comparison && c.can_be_true(state)) {
//					it.remove();
//					continue;
//				}

				if (!(c.can_be_true(state))) {

					ArrayList<GroundAction> acts = findSupportersApproach2(
							(ArrayList<GroundAction>) this.action_level.get(t), c, state);

//					System.out.println("LEVEL: "+t);
//					for(GroundAction g : acts) {
//						
//						System.out.println(g.getName()+g.getParameters());
//						
//					}

					if (acts.isEmpty()) {

						System.out.println();
						System.out.println("Error! No supporter for " + c + "at level " + t);
						System.out.println(c.getClass());

						Comparison comp = (Comparison) c;

						System.out.println(comp.getLeft());
						System.out.println(comp.getLeft().getClass());

						ExtendedNormExpression ene = (ExtendedNormExpression) comp.getLeft();

						System.out.println(ene.eval(state));

						System.out.println(comp.getRight());
						System.out.println(comp.getComparator());

						System.exit(-1);
					}

					if (rel_plan[t] == null) {
						rel_plan[t] = new ArrayList();
					}

					for (GroundAction a : acts) {

//						this.action_level.get(t).remove(a);
						if (a.getPreconditions() != null) {

//							sonsToAdd.addAll(a.getPreconditions().sons);

//							sonsToAdd.add(a.getPreconditions());

							for (Object con : a.getPreconditions().sons) {

								if (!(con instanceof NotCond)) {
									sonsToAdd.add(con);

//									if(con.toString().equalsIgnoreCase("(>= ((+ 0.0(* -1.0 ((objects_taken))))) ((+ 0.0)))")) {
//										
//										System.out.println("ACTION THAT ADDED WEIRD NUM CON: "+a);
//										for(Object son : a.getPreconditions().sons) {
//											System.out.println(son);
//										}
//									}
								}
							}

//							String test = a.getName()+a.getParameters();
//							
//							
//							if(a.getName().equals("cut")) {
//								
//								System.out.println(test.trim());
//
//								System.out.println("PRECONDS ARE TRUE: "+a.getPreconditions().can_be_true((RelState) rel_state_level.get(t-1)));
//							}
//
////							System.out.println("Action" + a.getName()+a.getParameters()+ " adds "+a.getPreconditions().sons+ " at level "+t);
						}
//						a.apply(state);
						rel_plan[t].add(a);

					}

					it.remove();

				}
			}

			tmp.addAll(sonsToAdd);

//			System.out.println(t);
//
//			System.out.println(rel_plan[t]);

		}

//		System.out.println("PLAN LENGTH: "+rel_plan.length);
		return rel_plan;

	}

	private ArrayList<GroundAction> findSupportersApproach1(ArrayList<GroundAction> actions, Condition con,
			RelState state) {

		ArrayList<GroundAction> ret = new ArrayList<GroundAction>();

//		RelState tmp = emptyRelState.clone();

		Iterator it = actions.iterator();

		while (it.hasNext()) {

			GroundAction gr = (GroundAction) it.next();

			if (gr.getAddList().involve(con)) {
				ret.add(gr);
				gr.apply(state);
				it.remove();
			}

			if (con.can_be_true(state)) {
				break;
			}
		}

		return ret;

	}

	private ArrayList<GroundAction> findNumericSupporters(ArrayList<GroundAction> numActions,
			HashSet<Condition> termConds, RelState state, Condition con) {

		ArrayList<GroundAction> ret = new ArrayList<GroundAction>();

		if (con.can_be_true(state)) {
			return ret;
		}

//		System.out.println("Searching for numeric supporters");

		// Handle numeric actions that may have to be executed multiple times
		for (GroundAction gr : numActions) {
			for (Condition c : termConds) {

				if (c.can_be_true(state)) {
					continue;
				}

				if (c instanceof Comparison) {

					Comparison comp = (Comparison) c;

					int MAX_NO_OF_REPETITIONS = 10;

					int count = 1;

					// Action influences fluents involved in comparison
					if (!Collections.disjoint(comp.getInvolvedFluents(), gr.getNumericFluentAffected())) {

						String comparator = comp.getComparator();

						// CASE: Fluent X Number comparison
						if (comp.getLeft() instanceof NumFluent && comp.getRight() instanceof PDDLNumber) {

							if (comparator.equals(">=") || comparator.equals(">")) {

								for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

									if (ne.getFluentAffected().equals(comp.getLeft())
											&& ne.getOperator().equals("increase")) {

										// check if numeric effect of action is conditionally and does not apply
										RelState tmpState = state.clone();
										PDDLNumber before = tmpState.functionSupValue((NumFluent) comp.getLeft());
										tmpState.apply(gr);
										PDDLNumber after = tmpState.functionSupValue((NumFluent) comp.getLeft());
										if (before.equals(after)) {
											continue;
										}

//										System.out.println("Increasing " + comp.getLeft());
//										System.out.println("Comparison: " + comp);
//
//										System.out.println("Value of " + comp.getLeft() + " before: " + state.functionSupValue((NumFluent) comp.getLeft()));

										while (count < MAX_NO_OF_REPETITIONS) {

											state.apply(gr);
											ret.add(gr);

											if (c.can_be_true(state)) {
												break;
											}

											count++;

										}

//										System.out.println("Value of " + comp.getLeft() + " after: " + state.functionSupValue((NumFluent) comp.getLeft()));

									} else if (ne.getFluentAffected().equals(comp.getLeft())
											&& ne.getOperator().equals("assign")) {

										RelState tmpState = state.clone();
										ne.apply(tmpState);

										if (c.can_be_true(tmpState)) {
											state.apply(gr);
											ret.add(gr);
										}

									}

								}

							} else if (comparator.equals("<=") || comparator.equals("<")) {

								for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

									if (ne.getFluentAffected().equals(comp.getLeft())
											&& ne.getOperator().equals("decrease")) {

										// check if numeric effect of action is conditionally and does not apply
										RelState tmpState = state.clone();
										PDDLNumber before = tmpState.functionSupValue((NumFluent) comp.getLeft());
										tmpState.apply(gr);
										PDDLNumber after = tmpState.functionSupValue((NumFluent) comp.getLeft());
										if (before.equals(after)) {
											continue;
										}

										while (count < MAX_NO_OF_REPETITIONS) {

											state.apply(gr);
											ret.add(gr);

											if (c.can_be_true(state)) {
												break;
											}

											count++;

										}

									} else if (ne.getFluentAffected().equals(comp.getLeft())
											&& ne.getOperator().equals("assign")) {

										RelState tmpState = state.clone();
										ne.apply(tmpState);

										if (c.can_be_true(tmpState)) {
											state.apply(gr);
											ret.add(gr);
										}

									}

								}

							} else if (comparator.equals("=")) {

								NumFluent f = (NumFluent) comp.getLeft();
								PDDLNumber fluentValue = state.functionSupValue(f);

								// fluent is too small
								if (fluentValue.getNumber() < ((PDDLNumber) comp.getRight()).getNumber()) {

									for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

										if (ne.getFluentAffected().equals(comp.getLeft())
												&& ne.getOperator().equals("increase")) {

											// check if numeric effect of action is conditionally and does not apply
											RelState tmpState = state.clone();
											PDDLNumber before = tmpState.functionSupValue((NumFluent) comp.getLeft());
											tmpState.apply(gr);
											PDDLNumber after = tmpState.functionSupValue((NumFluent) comp.getLeft());
											if (before.equals(after)) {
												continue;
											}

											while (count < MAX_NO_OF_REPETITIONS) {

												state.apply(gr);
												ret.add(gr);

												if (c.can_be_true(state)) {
													break;
												}

												count++;

											}

										} else if (ne.getFluentAffected().equals(comp.getLeft())
												&& ne.getOperator().equals("assign")) {

											RelState tmpState = state.clone();
											ne.apply(tmpState);

											if (c.can_be_true(tmpState)) {
												state.apply(gr);
												ret.add(gr);
											}

										}

									}

									// fluent is too big
								} else {

									for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

										if (ne.getFluentAffected().equals(comp.getLeft())
												&& ne.getOperator().equals("decrease")) {

											// check if numeric effect of action is conditionally and does not apply
											RelState tmpState = state.clone();
											PDDLNumber before = tmpState.functionSupValue((NumFluent) comp.getLeft());
											tmpState.apply(gr);
											PDDLNumber after = tmpState.functionSupValue((NumFluent) comp.getLeft());
											if (before.equals(after)) {
												continue;
											}

											while (count < MAX_NO_OF_REPETITIONS) {

												state.apply(gr);
												ret.add(gr);

												if (c.can_be_true(state)) {
													break;
												}

												count++;

											}

										} else if (ne.getFluentAffected().equals(comp.getLeft())
												&& ne.getOperator().equals("assign")) {

											RelState tmpState = state.clone();
											ne.apply(tmpState);

											if (c.can_be_true(tmpState)) {
												state.apply(gr);
												ret.add(gr);
											}
										}
									}

								}

							}

							// CASE: Fluent X Fluent comparison
						} else if (comp.getLeft() instanceof NumFluent && comp.getRight() instanceof NumFluent) {

							System.out.println("Fluent vs Fluent comparison");

							if (comparator.equals(">=") || comparator.equals(">")) {

								for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

									if ((ne.getFluentAffected().equals(comp.getLeft())
											&& ne.getOperator().equals("increase"))
											|| (ne.getFluentAffected().equals(comp.getRight())
													&& ne.getOperator().equals("decrease"))) {

										// check if numeric effect of action is conditionally and does not apply
										RelState tmpState = state.clone();
										PDDLNumber beforeL = tmpState.functionSupValue((NumFluent) comp.getLeft());
										PDDLNumber beforeR = tmpState.functionSupValue((NumFluent) comp.getRight());
										tmpState.apply(gr);
										PDDLNumber afterL = tmpState.functionSupValue((NumFluent) comp.getLeft());
										PDDLNumber afterR = tmpState.functionSupValue((NumFluent) comp.getLeft());

										if (beforeL.equals(afterL) && beforeR.equals(afterR)) {
											continue;
										}

//										System.out.println("Increasing " + comp.getLeft() + " or decreasing " + comp.getRight());
//
//										System.out.println(
//												"Value of " + comp.getLeft() + " before (left): " + state.functionSupValue((NumFluent) comp.getLeft()));
//										System.out.println(
//												"Value of " + comp.getRight() + " before (right): " + state.functionSupValue((NumFluent) comp.getRight()));

										while (count < MAX_NO_OF_REPETITIONS) {

											state.apply(gr);
											ret.add(gr);

											if (c.can_be_true(state)) {
												break;
											}

											count++;

										}

//										System.out.println("Increasing " + comp.getLeft() + " or decreasing " + comp.getRight());
//
//										System.out
//												.println("Value of " + comp.getLeft() + " after (left): " + state.functionSupValue((NumFluent) comp.getLeft()));
//										System.out.println(
//												"Value of " + comp.getRight() + " after (right): " + state.functionSupValue((NumFluent) comp.getRight()));

									} else if ((ne.getFluentAffected().equals(comp.getLeft())
											|| ne.getFluentAffected().equals(comp.getRight()))
											&& ne.getOperator().equals("assign")) {

										RelState tmpState = state.clone();
										ne.apply(tmpState);

										if (c.can_be_true(tmpState)) {
											state.apply(gr);
											ret.add(gr);
										}

									}

								}

							} else if (comparator.equals("<=") || comparator.equals("<")) {

								for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

									if ((ne.getFluentAffected().equals(comp.getLeft())
											&& ne.getOperator().equals("decrease"))
											|| (ne.getFluentAffected().equals(comp.getRight())
													&& ne.getOperator().equals("increase"))) {

										// check if numeric effect of action is conditionally and does not apply
										RelState tmpState = state.clone();
										PDDLNumber beforeL = tmpState.functionSupValue((NumFluent) comp.getLeft());
										PDDLNumber beforeR = tmpState.functionSupValue((NumFluent) comp.getRight());

										tmpState.apply(gr);
										PDDLNumber afterL = tmpState.functionSupValue((NumFluent) comp.getLeft());
										PDDLNumber afterR = tmpState.functionSupValue((NumFluent) comp.getLeft());

										if (beforeL.equals(afterL) && beforeR.equals(afterR)) {
											continue;
										}

//										System.out.println("Decreasing " + comp.getLeft() + " or increasing " + comp.getRight());
//
//										System.out.println(
//												"Value of " + comp.getLeft() + " before (left): " + state.functionSupValue((NumFluent) comp.getLeft()));
//										System.out.println(
//												"Value of " + comp.getRight() + " before (right): " + state.functionSupValue((NumFluent) comp.getRight()));

										while (count < MAX_NO_OF_REPETITIONS) {

											state.apply(gr);
											ret.add(gr);

											if (c.can_be_true(state)) {
												break;
											}

											count++;

										}

//										System.out
//												.println("Value of " + comp.getLeft() + " after (left): " + state.functionSupValue((NumFluent) comp.getLeft()));
//										System.out.println(
//												"Value of " + comp.getRight() + " after (right): " + state.functionSupValue((NumFluent) comp.getRight()));

									} else if ((ne.getFluentAffected().equals(comp.getLeft())
											|| ne.getFluentAffected().equals(comp.getRight()))
											&& ne.getOperator().equals("assign")) {

										RelState tmpState = state.clone();
										ne.apply(tmpState);

										if (c.can_be_true(tmpState)) {
											state.apply(gr);
											ret.add(gr);
										}

									}

								}

							} else if (comparator.equals("=")) {

								NumFluent lf = (NumFluent) comp.getLeft();
								NumFluent rf = (NumFluent) comp.getRight();

								PDDLNumber left = state.functionSupValue(lf);
								PDDLNumber right = state.functionSupValue(rf);

								// left < right
								if (left.getNumber() < right.getNumber()) {

									for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

										if ((ne.getFluentAffected().equals(comp.getLeft())
												&& ne.getOperator().equals("increase"))
												|| (ne.getFluentAffected().equals(comp.getRight())
														&& ne.getOperator().equals("decrease"))) {

											// check if numeric effect of action is conditionally and does not apply
											RelState tmpState = state.clone();
											PDDLNumber beforeL = tmpState.functionSupValue((NumFluent) comp.getLeft());
											PDDLNumber beforeR = tmpState.functionSupValue((NumFluent) comp.getRight());
											tmpState.apply(gr);
											PDDLNumber afterL = tmpState.functionSupValue((NumFluent) comp.getLeft());
											PDDLNumber afterR = tmpState.functionSupValue((NumFluent) comp.getLeft());

											if (beforeL.equals(afterL) && beforeR.equals(afterR)) {
												continue;
											}

											while (count < MAX_NO_OF_REPETITIONS) {

												state.apply(gr);
												ret.add(gr);

												if (c.can_be_true(state)) {
													break;
												}

												count++;

											}

										} else if ((ne.getFluentAffected().equals(comp.getLeft())
												|| ne.getFluentAffected().equals(comp.getRight()))
												&& ne.getOperator().equals("assign")) {

											RelState tmpState = state.clone();
											ne.apply(tmpState);

											if (c.can_be_true(tmpState)) {
												state.apply(gr);
												ret.add(gr);
											}

										}

									}

									// right < left
								} else {

									for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

										if ((ne.getFluentAffected().equals(comp.getLeft())
												&& ne.getOperator().equals("decrease"))
												|| (ne.getFluentAffected().equals(comp.getRight())
														&& ne.getOperator().equals("increase"))) {

											// check if numeric effect of action is conditionally and does not apply
											RelState tmpState = state.clone();
											PDDLNumber beforeL = tmpState.functionSupValue((NumFluent) comp.getLeft());
											PDDLNumber beforeR = tmpState.functionSupValue((NumFluent) comp.getRight());
											tmpState.apply(gr);
											PDDLNumber afterL = tmpState.functionSupValue((NumFluent) comp.getLeft());
											PDDLNumber afterR = tmpState.functionSupValue((NumFluent) comp.getLeft());

											if (beforeL.equals(afterL) && beforeR.equals(afterR)) {
												continue;
											}

											while (count < MAX_NO_OF_REPETITIONS) {

												state.apply(gr);
												ret.add(gr);

												if (c.can_be_true(state)) {
													break;
												}

												count++;

											}

										} else if ((ne.getFluentAffected().equals(comp.getLeft())
												|| ne.getFluentAffected().equals(comp.getRight()))
												&& ne.getOperator().equals("assign")) {

											RelState tmpState = state.clone();
											ne.apply(tmpState);

											if (c.can_be_true(tmpState)) {
												state.apply(gr);
												ret.add(gr);
											}
										}
									}

								}

							}

							// CASE: Complex expression vs number comparison
						} else if (comp.getLeft() instanceof ExtendedNormExpression
								&& comp.getRight() instanceof ExtendedNormExpression
								&& !((ExtendedNormExpression) comp.getLeft()).isNumber()
								&& ((ExtendedNormExpression) comp.getRight()).isNumber()) {

							if (comparator.equals(">=") || comparator.equals(">")) {

								for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

									if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
											.contains(ne.getFluentAffected()) && ne.getOperator().equals("increase")) {

										// check if numeric effect of action is conditionally and does not apply
										ExtendedNormExpression exp = (ExtendedNormExpression) comp.getLeft();
										RelState tmpState = state.clone();
										PDDLNumber before = exp.eval(tmpState).getSup();
										tmpState.apply(gr);
										PDDLNumber after = exp.eval(tmpState).getSup();
										if (before.equals(after)) {
											continue;
										}

										while (count < MAX_NO_OF_REPETITIONS) {

											state.apply(gr);
											ret.add(gr);

											if (c.can_be_true(state)) {
												break;
											}

											count++;

										}

									} else if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
											.contains(ne.getFluentAffected()) && ne.getOperator().equals("assign")) {

										RelState tmpState = state.clone();
										ne.apply(tmpState);

										if (c.can_be_true(tmpState)) {
											state.apply(gr);
											ret.add(gr);
										}

									}

								}

							} else if (comparator.equals("<=") || comparator.equals("<")) {

								for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

									// TODO: "assign" operator case needs to be handled!

									if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
											.contains(ne.getFluentAffected()) && ne.getOperator().equals("decrease")) {

										// check if numeric effect of action is conditionally and does not apply
										ExtendedNormExpression exp = (ExtendedNormExpression) comp.getLeft();
										RelState tmpState = state.clone();
										PDDLNumber before = exp.eval(tmpState).getSup();
										tmpState.apply(gr);
										PDDLNumber after = exp.eval(tmpState).getSup();
										if (before.equals(after)) {
											continue;
										}

										while (count < MAX_NO_OF_REPETITIONS) {

											state.apply(gr);
											ret.add(gr);

											if (c.can_be_true(state)) {
												break;
											}

											count++;

										}

									} else if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
											.contains(ne.getFluentAffected()) && ne.getOperator().equals("assign")) {

										RelState tmpState = state.clone();
										ne.apply(tmpState);

										if (c.can_be_true(tmpState)) {
											state.apply(gr);
											ret.add(gr);
										}

									}

								}

							} else if (comparator.equals("=")) {

								ExtendedNormExpression exp = (ExtendedNormExpression) comp.getLeft();
								PDDLNumber value = exp.eval(state).getSup();

								// fluent is too small
								if (value.getNumber() < ((ExtendedNormExpression) comp.getRight()).getNumber()) {

									for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

										if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
												.contains(ne.getFluentAffected())
												&& ne.getOperator().equals("increase")) {

											// check if numeric effect of action is conditionally and does not apply
											RelState tmpState = state.clone();
											PDDLNumber before = exp.eval(tmpState).getSup();
											tmpState.apply(gr);
											PDDLNumber after = exp.eval(tmpState).getSup();
											if (before.equals(after)) {
												continue;
											}

											while (count < MAX_NO_OF_REPETITIONS) {

												state.apply(gr);
												ret.add(gr);

												if (c.can_be_true(state)) {
													break;
												}

												count++;

											}

										} else if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
												.contains(ne.getFluentAffected())
												&& ne.getOperator().equals("assign")) {

											RelState tmpState = state.clone();
											ne.apply(tmpState);

											if (c.can_be_true(tmpState)) {
												state.apply(gr);
												ret.add(gr);
											}

										}

									}

									// fluent is too big
								} else {

									for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

										if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
												.contains(ne.getFluentAffected())
												&& ne.getOperator().equals("decrease")) {

											// check if numeric effect of action is conditionally and does not apply
											RelState tmpState = state.clone();
											PDDLNumber before = exp.eval(tmpState).getSup();
											tmpState.apply(gr);
											PDDLNumber after = exp.eval(tmpState).getSup();
											if (before.equals(after)) {
												continue;
											}

											while (count < MAX_NO_OF_REPETITIONS) {

												// TODO:
												// ---------------------------------------------------------------------------
												// check if repetition fullfilled condition (auf tmp state ausführen!)

												state.apply(gr);
												ret.add(gr);

												if (c.can_be_true(state)) {
													break;
												}

												count++;

											}

										} else if (((ExtendedNormExpression) comp.getLeft()).getInvolvedNumericFluents()
												.contains(ne.getFluentAffected())
												&& ne.getOperator().equals("assign")) {

											RelState tmpState = state.clone();
											ne.apply(tmpState);

											if (c.can_be_true(tmpState)) {
												state.apply(gr);
												ret.add(gr);
											}
										}
									}
								}
							}
						}

						// Case: Other type of comparison combinations
						else {

							Set<NumFluent> left = comp.getLeft().getInvolvedNumericFluents();
							Set<NumFluent> right = comp.getRight().getInvolvedNumericFluents();

							for (NumEffect ne : gr.getNumericEffectsAsCollection()) {

								if (left.contains(ne.getFluentAffected()) || right.contains(ne.getFluentAffected())) {

									RelState tmpState = state.clone();
									int tmpCount = 0;

									while (count < MAX_NO_OF_REPETITIONS) {

										tmpState.apply(gr);
										tmpCount++;

										if (c.can_be_true(tmpState)) {
											break;
										}

										count++;

									}

									if (c.can_be_true(tmpState)) {

										for (int i = 0; i < tmpCount; i++) {

											ret.add(gr);
											state.apply(gr);
										}

									}

								}

							}

						}

					}
				}

				if (con.can_be_true(state)) {
					break;
				}

			}

			if (con.can_be_true(state)) {
				break;
			}

		}

		return ret;
	}

	private void checkForCondEffectsAfterApplication(RelState state, GroundAction gr) {

		HashSet<Condition> applicableCondEffects = new HashSet<Condition>();

		for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) gr.cond_effects.sons) {

			if (state.satisfy(cEffect.activation_condition)) {

				state.update_values(cEffect.effect.apply(state));

			}
		}
	}

	private ArrayList<GroundAction> findSupportersApproach2(ArrayList<GroundAction> actionss, Condition con,
			RelState state) {

		ArrayList<GroundAction> ret = new ArrayList<GroundAction>();

		RelState tmp = state.clone();

		ArrayList<GroundAction> actionsClone = (ArrayList<GroundAction>) actionss.clone();

		Iterator it = actionsClone.iterator();
		HashSet<Condition> termConds = (HashSet<Condition>) con.getTerminalConditions();

		ArrayList<GroundAction> numActions = new ArrayList<GroundAction>();

		HashSet<GroundAction> alreadyAppliedActions = new HashSet<GroundAction>();

		while (it.hasNext()) {

			GroundAction gr = (GroundAction) it.next();

//			if (gr.getName().equals("make-pizza")) {
//				System.out.println("MAKE PIZZA");
//				System.out.println(gr);
//			}

			if (alreadyAppliedActions.contains(gr)) {
				it.remove();
				continue;
			}

			if (!Collections.disjoint(gr.getAddList().getTerminalConditions(), termConds)) {

				ret.add(gr);
				gr.apply(tmp);

				it.remove();

			} else {

				// Conditional effects

				if (!gr.cond_effects.sons.isEmpty()) {
					for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) gr.cond_effects.sons) {

						if (tmp.satisfy(cEffect.activation_condition)) {
							Set<Condition> terms = Sets
									.newLinkedHashSetWithExpectedSize(((AndCond) cEffect.effect).sons.size());
							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {
								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}

							if (!Collections.disjoint(terms, termConds)) {

//								System.out.println("Adding " + gr.getName() + gr.getParameters() + " as a conditional supporter for " + con);
								ret.add(gr);
								gr.apply(tmp);

								it.remove();

								break;
							}
						}

						else {

							Set<Condition> terms = new HashSet<Condition>();

							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {

								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}

							if (!Collections.disjoint(terms, termConds)) {
//									&& !Collections.disjoint(cEffect.activation_condition.getTerminalConditions(), gr.getAddList().getTerminalConditions())) {

								HashSet<GroundAction> suppsForActivationCond = new HashSet<GroundAction>();

								suppsForActivationCond
										.addAll(findSupportersApproach2((ArrayList<GroundAction>) actionsClone.clone(),
												cEffect.activation_condition, tmp));

//								for (GroundAction g : suppsForActivationCond) {
//									System.out.println(g.getName() + g.getParameters());
//								}

								if (tmp.satisfy(cEffect.activation_condition)) {

									ret.addAll(suppsForActivationCond);
									ret.add(gr);

									gr.apply(tmp);

									alreadyAppliedActions.addAll(suppsForActivationCond);

									it.remove();

									break;

								}

//								ret.add(gr);
//								gr.apply(tmp);
//
//								if (tmp.satisfy(cEffect.activation_condition)) {
//
//									tmp.update_values(cEffect.effect.apply(tmp));
//
//								}
//
//								it.remove();
//
//								break;
							}

						}
					}
				}

				// Numeric effects

				if (gr.getNumericEffects() != null) {

					for (Condition c : termConds) {

						if (c instanceof Comparison) {

							Comparison comp = (Comparison) c;

							if (!Collections.disjoint(comp.getInvolvedFluents(), gr.getNumericFluentAffected())) {

								numActions.add(gr);

							}
						}
					}
				}
			}

			if (con.can_be_true(tmp)) {
//				System.out.println(con +" already true, breaking");
				break;
			}
		}

		// Handle numeric actions
		ArrayList<GroundAction> numSupps = findNumericSupporters(numActions, termConds, tmp, con);

//		if (con.toString().equalsIgnoreCase(
//				"(AND (is-at baking_pan oven)(NOT (is-open oven))(is-turned-on oven)(in cheese baking_pan)(>= ((is-at-num salami_piece baking_pan)) ( 1 ))(in pizza_sauce baking_pan)(is-spread pizza_sauce)(is-unrolled pizza_dough)(NOT (is-open cupboard_br))(NOT (is-open cupboard_bl))(NOT (is-open cupboard_tr))(NOT (is-open cupboard_tm))(NOT (is-open cupboard_tl))(NOT (is-open drawer))(NOT (is-open fridge))(>= ((+ (* 1.0 ((objects_taken)))-0.0)) ((+ 0.0)))(>= ((+ 0.0(* -1.0 ((objects_taken))))) ((+ 0.0))))")) {
//
//			System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAA");
//			System.out.println(numSupps);
//			System.out.println("AAAAAAAAAAAAAAAAAAAAAAAAA");
//
//
//		}

//		System.out.println("Numeric supporters: " + numSupps.size());

		ret.addAll(numSupps);

		if (!con.can_be_true(tmp)) {

//			if (!ret.isEmpty()) {
//				System.out.println("This shouldnt happen :)");
//			}

			return new ArrayList<GroundAction>();

		}

//		System.out.println("SUPPORTERS (UNFILTERED): "+ret.size());

		boolean foundSupporters = false;
		ArrayList<GroundAction> comb = new ArrayList<GroundAction>();

		for (int i = 1; i <= ret.size(); i++) {

			Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(ret.size(), i);

			while (iterator.hasNext()) {

				final int[] combination = iterator.next();

				RelState tmp2 = state.clone();

				comb.clear();

				for (Integer integ : combination) {

					GroundAction gr = ret.get(integ);
					comb.add(gr);
					tmp2.apply(gr);
					checkForCondEffectsAfterApplication(tmp2, gr);
				}

				if (con.can_be_true(tmp2)) {
					foundSupporters = true;
					break;
				}

			}

			if (foundSupporters) {
				break;
			}

		}

//		System.out.println("Supporters for "+con);
		for (GroundAction gr : comb) {
			state.apply(gr);
//			System.out.println(gr.getName()+gr.getParameters());
		}

//		System.out.println("RET");
//		for (GroundAction r : ret) {
//			System.out.println(r.getName() + r.getParameters());
//		}
//		System.out.println("COMB");
//		for (GroundAction c : comb) {
//			System.out.println(c.getName() + c.getParameters());
//		}

//		System.out.println("SUPPORTERS (FILTERED): "+comb.size());

//		To remove or not to remove, this is the question
//		actionss.removeAll(comb);

		return comb;

	}

	private ArrayList<GroundAction> findSupportersApproach3(ArrayList<GroundAction> actionss, Condition con,
			RelState state, List<Predicate> relevantPredicates, RelState init) {

		// List of identified supporters
		Set<GroundAction> ret = new HashSet<GroundAction>();

//		RelState tmp = state.clone();

		// Filter conditions of type Predicate fomr the passed list of terminal
		// conditions of the passed condition "con"
		HashSet<Condition> termConds = (HashSet<Condition>) con.getTerminalConditions();
		List<Predicate> termPreds = new ArrayList<Predicate>();
		List<Condition> termsToRemove = new ArrayList<Condition>();
		for (Condition t : termConds) {
			if (t instanceof PDDLObjectsEquality) {
				termsToRemove.add(t);
			} else if (t instanceof Predicate) {
				termPreds.add((Predicate) t);
			}
		}
		// Remove all terminal conditions that are not of type Predicate
		termConds.removeAll(termsToRemove);

		// Update tmp state to have the same relaxed properties as the passed RelState
		// state variable
		RelState tmp = init.clone();
		for (Predicate p : relevantPredicates) {
			if (!termPreds.contains(p)) {
				if (p.can_be_true(state) && !p.can_be_true(tmp)) {
					tmp.makePositive(p);
				}
				if (p.can_be_false(state) && !p.can_be_false(tmp)) {
					tmp.makeNegative(p);
				}
			}
		}

		HashSet<GroundAction> alreadyAppliedActions = new HashSet<GroundAction>();
		ArrayList<GroundAction> numActions = new ArrayList<GroundAction>();

		// Iterate over the set of actions that was passed in order to identify
		// supporters
		ArrayList<GroundAction> actionsClone = (ArrayList<GroundAction>) actionss.clone();
		Iterator it = actionsClone.iterator();
		while (it.hasNext()) {
			GroundAction gr = (GroundAction) it.next();

			// If the currently inspected action was added to the results already
			if (alreadyAppliedActions.contains(gr)) {
				it.remove();
				continue;
			}

			/*
			 * If the add list of the currently inspected action contains one of the
			 * terminal conditions extracted earlier. This means that the currently
			 * inspected action is a supporter for at least one of these conditions.
			 */
			if (!Collections.disjoint(gr.getAddList().getTerminalConditions(), termConds)) {

				ret.add(gr);
				gr.apply(tmp);

				it.remove();

			} else {
				/*
				 * If the add list of the currently inspected action is disjoint with the list
				 * of terminal facts, there might still be some of the temrinals in a possible
				 * conditional effect. This is checked here.
				 */
				if (!gr.cond_effects.sons.isEmpty()) {

					// For all conditional effects of the inspected action
					for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) gr.cond_effects.sons) {
						/*
						 * If the condition of a conditional effect is satisfied by the current state
						 * then check for supporters in the add list of this conditional effect.
						 */
						if (tmp.satisfy(cEffect.activation_condition)) {
							Set<Condition> terms = Sets
									.newLinkedHashSetWithExpectedSize(((AndCond) cEffect.effect).sons.size());
							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {
								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}
							if (!Collections.disjoint(terms, termConds)) {
								ret.add(gr);
								gr.apply(tmp);
								it.remove();

								break;
							}
						}
						// If the condition of the conditional effect is currently not satisfied.
						else {
							// Extract all terminal conditions of the add list of the currently considered
							// conditional effect.
							Set<Condition> terms = new HashSet<Condition>();
							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {
								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}

							/*
							 * If this conditional might be a supporter for one of the terminal facts in the
							 * termConds list, then check whether we can find supporters for the activation
							 * condition of this conditional effect (as it is not satisfied at the moment).
							 * If yes then consider the action of this conditional effect plus the found set
							 * of supporters of the currently considered list of terminal facts.
							 */
							if (!Collections.disjoint(terms, termConds)) {

								// Search for supporters of the activation condition of the conditional effect.
								List<GroundAction> suppsForActivationCond = findSupportersApproach3(
										(ArrayList<GroundAction>) actionsClone.clone(), cEffect.activation_condition,
										tmp, relevantPredicates, init);

								// If there were found some supporters for the activation condition.
								if (tmp.satisfy(cEffect.activation_condition)) {
									ret.addAll(suppsForActivationCond);
									ret.add(gr);

									gr.apply(tmp);
									alreadyAppliedActions.addAll(suppsForActivationCond);

									it.remove();
									break;
								}
							}

						}
					}
				}

				// Handle numeric effects.
				if (gr.getNumericEffects() != null) {
					for (Condition c : termConds) {
						if (c instanceof Comparison) {
							Comparison comp = (Comparison) c;
							if (!Collections.disjoint(comp.getInvolvedFluents(), gr.getNumericFluentAffected())) {
								numActions.add(gr);
							}
						}
					}
				}
			}
			if (con.can_be_true(tmp)) {
				break;
			}
			// END OF FIRST WHILE LOOP (that iterates over the set of actions)
		}

		// Handle numeric actions
		ArrayList<GroundAction> numSupps = findNumericSupporters(numActions, termConds, tmp, con);
		ret.addAll(numSupps);

		// If no sufficient set of supporters was found to satisfy the examined
		// condition
		if (!con.can_be_true(tmp)) {
			return new ArrayList<GroundAction>();
		}

		boolean foundSupporters = false;
		List<GroundAction> comb = new ArrayList<GroundAction>();
		List<List<GroundAction>> combs = new ArrayList<List<GroundAction>>();
		ArrayList<GroundAction> ret2 = new ArrayList<GroundAction>();
		for (GroundAction act : ret) {
			ret2.add(act);
		}

		for (int i = 1; i <= ret2.size(); i++) {

			Iterator<int[]> iterator = CombinatoricsUtils.combinationsIterator(ret2.size(), i);

			while (iterator.hasNext()) {

				int[] combination = iterator.next();

//				RelState tmp2 = state.clone();
				RelState tmp2 = init.clone();

				for (Predicate p : relevantPredicates) {
					if (!termPreds.contains(p)) {
						if (p.can_be_true(state) && !p.can_be_true(tmp2)) {
							tmp2.makePositive(p);
						}
						if (p.can_be_false(state) && !p.can_be_false(tmp2)) {
							tmp2.makeNegative(p);
						}
					}
				}

				comb.clear();

				for (int index : combination) {

					GroundAction gr = ret2.get(index);
					comb.add(gr);
					tmp2.apply(gr);
					checkForCondEffectsAfterApplication(tmp2, gr);
				}

				if (con.can_be_true(tmp2)) {
					foundSupporters = true;
					combs.add(comb);
					break;
				}

			}

			if (foundSupporters) {
				break;
			}

		}

//		System.out.println(ret2.size() + " " + comb.size());

//		System.out.println("APPLY ACTIONS");
//		System.out.println("Supporters for "+con);
		// for (GroundAction gr : comb) {
		// 	state.apply(gr);
		// }

//		System.out.println("RET");
//		for (GroundAction r : ret) {
//			System.out.println(r.getName() + r.getParameters());
//		}
//		System.out.println("COMB");
//		for (GroundAction c : comb) {
//			System.out.println(c.getName() + c.getParameters());
//		}

//		System.out.println("SUPPORTERS (FILTERED): "+comb.size());

//		To remove or not to remove, this is the question
//		actionss.removeAll(comb);

		return (ArrayList<GroundAction>) comb;
		// ArrayList<GroundAction> retList = new ArrayList(ret);
//		return retList;
	}

	private List<List<GroundAction>> findSupportersApproach4(ArrayList<GroundAction> actionss, Condition con,
		RelState state, List<Predicate> relevantPredicates, RelState init) {
		System.out.println("start" + " " + con.pddlPrint(false));

		// List of identified supporters
		Set<GroundAction> ret = new HashSet<GroundAction>();

		// Filter conditions of type Predicate fomr the passed list of terminal
		// conditions of the passed condition "con"
		HashSet<Condition> termConds = (HashSet<Condition>) con.getTerminalConditions();
		List<Predicate> termPreds = new ArrayList<Predicate>();
		List<Condition> termsToRemove = new ArrayList<Condition>();
		for (Condition t : termConds) {
			if (t instanceof PDDLObjectsEquality) {
				termsToRemove.add(t);
			} else if (t instanceof Predicate) {
				termPreds.add((Predicate) t);
			}
		}
		// Remove all terminal conditions that are not of type Predicate
		termConds.removeAll(termsToRemove);

		// Update tmp state to have the same relaxed properties as the passed RelState
		// state variable
		RelState tmp = init.clone();
		for (Predicate p : relevantPredicates) {
			if (!termPreds.contains(p)) {
				if (p.can_be_true(state) && !p.can_be_true(tmp)) {
					tmp.makePositive(p);
				}
				if (p.can_be_false(state) && !p.can_be_false(tmp)) {
					tmp.makeNegative(p);
				}
			}
		}

		HashSet<GroundAction> alreadyAppliedActions = new HashSet<GroundAction>();
		ArrayList<GroundAction> numActions = new ArrayList<GroundAction>();

		// Iterate over the set of actions that was passed in order to identify
		// supporters
		ArrayList<GroundAction> actionsClone = (ArrayList<GroundAction>) actionss.clone();
		Iterator it = actionsClone.iterator();
		while (it.hasNext()) {
			GroundAction gr = (GroundAction) it.next();

			// If the currently inspected action was added to the results already
			if (alreadyAppliedActions.contains(gr)) {
				it.remove();
				continue;
			}

			/*
			 * If the add list of the currently inspected action contains one of the
			 * terminal conditions extracted earlier. This means that the currently
			 * inspected action is a supporter for at least one of these conditions.
			 */
			if (!Collections.disjoint(gr.getAddList().getTerminalConditions(), termConds)) {

				ret.add(gr);
				gr.apply(tmp);

				it.remove();

			} else {
				/*
				 * If the add list of the currently inspected action is disjoint with the list
				 * of terminal facts, there might still be some of the temrinals in a possible
				 * conditional effect. This is checked here.
				 */
				if (!gr.cond_effects.sons.isEmpty()) {

					// For all conditional effects of the inspected action
					for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) gr.cond_effects.sons) {
						/*
						 * If the condition of a conditional effect is satisfied by the current state
						 * then check for supporters in the add list of this conditional effect.
						 */
						if (tmp.satisfy(cEffect.activation_condition)) {
							Set<Condition> terms = Sets
									.newLinkedHashSetWithExpectedSize(((AndCond) cEffect.effect).sons.size());
							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {
								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}
							if (!Collections.disjoint(terms, termConds)) {
								ret.add(gr);
								gr.apply(tmp);
								it.remove();

								break;
							}
						}
						// If the condition of the conditional effect is currently not satisfied.
						else {
							// Extract all terminal conditions of the add list of the currently considered
							// conditional effect.
							Set<Condition> terms = new HashSet<Condition>();
							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {
								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}

							/*
							 * If this conditional might be a supporter for one of the terminal facts in the
							 * termConds list, then check whether we can find supporters for the activation
							 * condition of this conditional effect (as it is not satisfied at the moment).
							 * If yes then consider the action of this conditional effect plus the found set
							 * of supporters of the currently considered list of terminal facts.
							 */
							if (!Collections.disjoint(terms, termConds)) {

								// Search for supporters of the activation condition of the conditional effect.
								List<GroundAction> suppsForActivationCond = findSupportersApproach3(
										(ArrayList<GroundAction>) actionsClone.clone(), cEffect.activation_condition,
										tmp, relevantPredicates, init);

								// If there were found some supporters for the activation condition.
								if (tmp.satisfy(cEffect.activation_condition)) {
									ret.addAll(suppsForActivationCond);
									ret.add(gr);

									gr.apply(tmp);
									alreadyAppliedActions.addAll(suppsForActivationCond);

									it.remove();
									break;
								}
							}

						}
					}
				}

				// Handle numeric effects.
				if (gr.getNumericEffects() != null) {
					for (Condition c : termConds) {
						if (c instanceof Comparison) {
							Comparison comp = (Comparison) c;
							if (!Collections.disjoint(comp.getInvolvedFluents(), gr.getNumericFluentAffected())) {
								numActions.add(gr);
							}
						}
					}
				}
			}

//			if (con.can_be_true(tmp)) {
//				break;
//			}
			// END OF FIRST WHILE LOOP (that iterates over the set of actions)
		}

		// Handle numeric actions
		ArrayList<GroundAction> numSupps = findNumericSupporters(numActions, termConds, tmp, con);
		ret.addAll(numSupps);

		// If no sufficient set of supporters was found to satisfy the examined
		// condition
		if (!con.can_be_true(tmp)) {
			return new ArrayList<List<GroundAction>>();
		}


		boolean foundSupporters = false;
		
		//Lists that store minimum sets of supporters
		List<GroundAction> comb = new ArrayList<GroundAction>();
		List<List<GroundAction>> combs = new ArrayList<List<GroundAction>>();
		
		//Deep clone ret list
		List<GroundAction> ret2 = new ArrayList<GroundAction>();
		for (GroundAction act : ret) {
			ret2.add(act);
		}

		List<GroundAction> relActions = new ArrayList<GroundAction>();
		relActions.addAll(this.relevantActions);

		//Calculate heuristic scores for all actions that were determined to be possible supporters for the passed condition (cond)
		Map<Integer, List<GroundAction>> actionHeuristicScores = new TreeMap<Integer, List<GroundAction>>();
		int heuristic;
		List<GroundAction> cHScoresList;
		for (GroundAction a : ret2) {
			heuristic = BestSupporterSelectionFunction.calculateHeuristicScore(GoalRecognitionConfiguration.getRelaxedPlanningHeuristic(),
					con, init, relActions, this, a);
			cHScoresList = actionHeuristicScores.get(heuristic);
			if (cHScoresList == null) {
				cHScoresList = new ArrayList<GroundAction>();
				actionHeuristicScores.put(heuristic, cHScoresList);
			}
			cHScoresList.add(a);
		}

		Map<Integer, List<BaseActionCombinationIterator>> iterators = this
				.initializeCombinationIterators(actionHeuristicScores, ret2.size());

		RelState tmp3;
		int minI = Integer.MAX_VALUE;
		int counter;
		List<Integer> keys = new ArrayList<Integer>();
		keys.addAll(actionHeuristicScores.keySet());

		while (!foundSupporters) {
			List<BaseActionCombinationIterator> cIterators = null;
			for (Integer key : iterators.keySet()) {
				cIterators = iterators.get(key);
				if (cIterators.size() > 0) {
					break;
				}
			}

			if (cIterators == null) {
				break;
			}
			
			if(cIterators.size() == 0) {
				combs.add(comb);
				break;
			}
			
			List<BaseActionCombinationIterator> iteratorsToAdd = new ArrayList<BaseActionCombinationIterator>();
			for (BaseActionCombinationIterator iterator : cIterators) {
				comb.clear();
				comb = iterator.getCurrentActionCombination().getActions();
				
				System.out.println("iterating... " + comb.size());
				
				iterator.produceNextActionCombination();
				iteratorsToAdd.add(iterator);
				
				tmp3 = init.clone();
				for (Predicate p : relevantPredicates) {
					if (!termPreds.contains(p)) {
						if (p.can_be_true(state) && !p.can_be_true(tmp3)) {
							tmp3.makePositive(p);
						}
						if (p.can_be_false(state) && !p.can_be_false(tmp3)) {
							tmp3.makeNegative(p);
						}
					}
				}
				for (GroundAction gr : comb) {
					tmp3.apply(gr);
					checkForCondEffectsAfterApplication(tmp3, gr);
				}

				if (con.can_be_true(tmp3)) {
					foundSupporters = true;
					combs.add(comb);
					break;
				}
			}
			if (foundSupporters) {
				System.out.println("FOUND SUPPORTERS");
				break;
			}
			
			cIterators.clear();
			
			List<GroundAction> cItActions;
			for(BaseActionCombinationIterator cIt : iteratorsToAdd) {
				if(cIt.isPossibleActionsEmpty()) {
					continue;
				}
				
				cItActions = cIt.getCurrentActionCombination().getActions();
				int h = BestSupporterSelectionFunction.calculateHeuristicScore(
						GoalRecognitionConfiguration.getRelaxedPlanningHeuristic(), con, init, relActions, this, cItActions);
				List<BaseActionCombinationIterator> itList = iterators.get(h);
				if(itList == null) {
					itList = new ArrayList<BaseActionCombinationIterator>();
					iterators.put(h, itList);
				}
				itList.add(cIt);
			}
		}

		List<List<GroundAction>> retList = new ArrayList<List<GroundAction>>();

		if (combs.size() == 0) {
//			combs.add(comb);
			retList.add(comb);
		} else {

//			retList.add(BestSupporterSelectionFunction.selectBestSupporterSet(BestSupporterSelectionFunction.SIMPLEST_PRECONDITIONS, con, init, relActions, combs));
			retList.add(BestSupporterSelectionFunction.selectBestSupporterSet(GoalRecognitionConfiguration.getRelaxedPlanningHeuristic(), con,
					init, relActions, this, combs));
		}
//		System.out.println("APPLY ACTIONS");
//		System.out.println("Supporters for "+con);
		// for (GroundAction gr : comb) {
		// 	state.apply(gr);
		// }

		System.out.println("finish");

		return retList;
//		return combs;
	}

	private Map<Integer, List<BaseActionCombinationIterator>> initializeCombinationIterators(Map<Integer, List<GroundAction>> actionHeuristicScores, int combinationSize) {
		Map<Integer, List<BaseActionCombinationIterator>> result = new TreeMap<Integer, List<BaseActionCombinationIterator>>();
		
		Set<Integer> keys = actionHeuristicScores.keySet();
		
		List<BaseActionCombinationIterator> iterators;
		List<GroundAction> base;
		for(Integer key : keys) {
			iterators = new ArrayList<BaseActionCombinationIterator>();
			for(GroundAction a : actionHeuristicScores.get(key)) {
				base = new ArrayList<GroundAction>();
				base.add(a);
				iterators.add(new BaseActionCombinationIterator(new ActionCombination(base), combinationSize, actionHeuristicScores));
			}
			result.put(key, iterators);
		}
		
		return result;
	}

	private GroundAction searchForAndRemove(ArrayList get, Condition c) {
		Iterator it = get.iterator();

		if (c instanceof Predicate) {
			Predicate p = (Predicate) c;
			while (it.hasNext()) {
				GroundAction gr = (GroundAction) it.next();
				if (gr.getAddList().sons.contains(p)) {
					it.remove();
					return gr;
				}
				for (Object o : gr.cond_effects.sons) {
					ConditionalEffect e = (ConditionalEffect) o;
					if (((AndCond) e.effect).sons.contains(p)) {
						it.remove();
						return gr;
					}
				}
			}
		} else if (c instanceof AndCond) {

			AndCond a = (AndCond) c;
			Set<Condition> terminals = a.getTerminalConditions();

			Set<Predicate> predicates = new HashSet<Predicate>();
			Set<Predicate> notConds = new HashSet<Predicate>();

			for (Condition cond : terminals) {
				if (cond instanceof Predicate) {
					predicates.add((Predicate) cond);
				} else if (cond instanceof NotCond) {
					notConds.add((Predicate) ((NotCond) cond).getSon());
				} else {
					System.out.println("Geschachtelte Conditions - not supported yet");
				}
			}

			while (it.hasNext()) {
				GroundAction gr = (GroundAction) it.next();
				if (gr.getAddList().sons.containsAll(predicates)
						&& Collections.disjoint(gr.getAddList().sons, notConds)) {
					it.remove();
					return gr;
				}
				for (Object o : gr.cond_effects.sons) {
					ConditionalEffect e = (ConditionalEffect) o;
					if (((AndCond) e.effect).sons.containsAll(predicates)
							&& Collections.disjoint(((AndCond) e.effect).sons, notConds)) {
						it.remove();
						return gr;
					}
				}
			}

		}
		return null;
	}

	private GroundAction bestSupport(HashSet get, Condition conditions, RelState s) {

		float bestDistance = 0;
		GroundAction ret = null;
		//
		AndCond c;
		if (conditions instanceof AndCond) {
			c = (AndCond) conditions;
			for (Object o : c.sons) {
				if (o instanceof Comparison) {
					Comparison comp = (Comparison) o;
					bestDistance += comp.satisfactionDistance(s);
				}

			}
			Iterator it = get.iterator();
			while (it.hasNext()) {
				GroundAction gr = (GroundAction) it.next();
//	                if (gr.getName().contains("tp"))
//	                   System.out.println("tp");
//	                if (gr.getName().contains("comm"))
//	                   System.out.println("comm");
				Float distance2 = new Float(0);
				RelState s1 = s.clone();
				gr.apply(s1);
				for (Object o : c.sons) {
					if (o instanceof Comparison) {
						Comparison comp = (Comparison) o;
						distance2 += comp.satisfactionDistance(s1);
					}

				}

				if (distance2 < bestDistance) {
//	                    System.out.println("Distanza dopo.." + distance2);
					it.remove();
					return gr;

				}

			}
////	          System.out.println("Comparison:" + comp);
//	            System.out.println("Distanza prima.." + distance);
		} else {
			System.out.println("Coniditions not supported");
		}
		return ret;
	}

	/**
	 * @return the cpu_time
	 */
	public long getCpu_time() {
		return cpu_time;
	}

	/**
	 * @return the timeForNumbers
	 */
	public long getTimeForNumbers() {
		return spezzTime;
	}

	/**
	 * @return the numberOfActions
	 */
	public int getNumberOfActions() {
		return numberOfActions;
	}

	private GroundAction searchSupporter(Collection hashSet, Predicate p) {
		Iterator it = hashSet.iterator();
		while (it.hasNext()) {
			GroundAction gr = (GroundAction) it.next();
			if (gr.getAddList().sons.contains(p)) {
				return gr;
			}
			for (Object o : gr.cond_effects.sons) {
				ConditionalEffect e = (ConditionalEffect) o;
				if (((AndCond) e.effect).sons.contains(p)) {
					return gr;
				}
			}
		}
		return null;
	}

	public Set findFirstLevelActions() {

		Set actions = new HashSet();
		if (this.init == null) {
			System.out.println("Initial state unknown");
		}
		// System.out.println("The number of relevant actions is:
		// "+relevantActions.size());
		for (GroundAction gr : (Set<GroundAction>) this.relevantActions) {
			// System.out.println(gr);
			if (gr.isApplicable(init)) {
				actions.add(gr);
			}
		}
		return actions;
	}

	/**
	 * @return the fixPoint
	 */
	public RelState getFixPoint() {
		return fixPoint;
	}

	/**
	 * @param fixPoint the fixPoint to set
	 */
	public void setFixPoint(RelState fixPoint) {
		this.fixPoint = fixPoint;
	}

	public RelState computeStateBound(PDDLState init, ComplexCondition goals, Set actions) {

		this.goal = goals;
		RelState current = init.relaxState();
		rel_state_level.add(current.clone());

		// actions to be used for planning purposes. This list is a temporary collection
		// to do not compromise the input structure
		ArrayList acts = new ArrayList();
		acts.addAll(actions);
		long start = System.currentTimeMillis();
		ArrayList level = new ArrayList();
		while (true) {
			if (current.satisfy(goal)) {
				goal_reached = true;
				break;// goal reached!
			} else {
				level = new ArrayList();
				for (Iterator it = acts.iterator(); it.hasNext();) {
					GroundAction gr = (GroundAction) it.next();
					if (gr.isApplicable(current)) {
						// if (gr.getPreconditions().isSatisfied(current)) {
						level.add(gr);
						it.remove();
					}
				}
				if (level.isEmpty()) {
					this.goal_reached = false;
					numberOfActions = Integer.MAX_VALUE;
					return null;// it means that the goal is unreacheable!
				}
				long start2 = System.currentTimeMillis();

				for (Object o : level) {
					GroundAction gr = (GroundAction) o;
					gr.apply(current);
				}
				spezzTime += System.currentTimeMillis() - start2;
				this.action_level.add(level);
				this.rel_state_level.add(current.clone());
				levels++;
			}
		}

		this.goal_reached = true;
		// System.out.println("Graphplan Building Time:" + (System.currentTimeMillis() -
		// start));
		// long start2 = System.currentTimeMillis();

		cpu_time = System.currentTimeMillis() - start;

		return current;

	}

	private void addPredicatesPrecondition(HashSet lmOfP, Map<Predicate, Set<Predicate>> ret, GroundAction gr) {

		if (gr.getPreconditions() == null) {
			return;
		}

		for (Object o : gr.getPreconditions().sons) {
			if (o instanceof Predicate) {
				lmOfP.addAll(ret.get(o));

			}

		}

	}

	private void intersectPredicatesPrecondition(HashSet lmOfP, Map<Predicate, Set<Predicate>> ret, GroundAction gr) {
		if (gr.getPreconditions() == null) {
			lmOfP.clear();
		} else {
			HashSet preconditions = new HashSet();
			for (Object o : gr.getPreconditions().sons) {
				if (o instanceof Predicate) {
					preconditions.addAll(ret.get(o));
				}
			}

			lmOfP.retainAll(preconditions);

		}
	}

	private void setFA(Predicate p, GroundAction gr, Map<Predicate, Set<Predicate>> FA) {

		HashSet set = new HashSet();
		for (Object o : gr.getPreconditions().sons) {
			if (o instanceof Predicate) {
				set.add(o);
			}
		}

		FA.put(p, set);

	}

}

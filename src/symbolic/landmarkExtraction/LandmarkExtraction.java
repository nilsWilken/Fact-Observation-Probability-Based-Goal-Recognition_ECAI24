package symbolic.landmarkExtraction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.google.common.collect.Sets;
import com.hstairs.ppmajal.conditions.AndCond;
import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.ConditionalEffect;
import com.hstairs.ppmajal.conditions.NotCond;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;

import config.GoalRecognitionConfiguration;
import symbolic.relaxedPlanning.utils.RPG;

public class LandmarkExtraction {
    public PddlDomain domain;
	public EPddlProblem problem;
	public MultiValuedMap<Integer, GroundAction> action_levels = new ArrayListValuedHashMap<>();
	public MultiValuedMap<Integer, Condition> fact_levels = new ArrayListValuedHashMap<>();;
	public int levels;

	public HashSet<Condition> landmarkCandidates = new HashSet<Condition>();
	public HashSet<Condition> landmarks = new HashSet<Condition>();
	public HashSet<Condition> nonInitStateLandmarks = new HashSet<Condition>();

	public MultiValuedMap<Integer, Condition> goals = new ArrayListValuedHashMap<>();

	public boolean PRINT_PROGRESS_BAR = true;
	public boolean USE_ALL_ACTIONS = true;

	public RPG rpg;
	public LGG lgg;
	public String domainFile;
	public String problemFile;
	public ArrayList<GroundAction> relaxedPlan;
	public Map<Integer, ArrayList<GroundAction>> relaxedPlanLevels;
	public List<Map<Integer, List<GroundAction>>> relaxedPlanLevelsList;

	public LandmarkExtraction(String dF, String pF) {

		try {
			this.computeLandmarks(dF, pF, null);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public LandmarkExtraction(String dF, String pF, Collection<GroundAction> actions) {

		try {
			this.computeLandmarks(dF, pF, actions);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public LandmarkExtraction() {
	}

	public LandmarkExtraction(String dF, String pF, boolean calculateLGG) throws Exception {

		domainFile = dF;
		problemFile = pF;

		domain = new PddlDomain(domainFile);
		problem = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);

		domain.substituteEqualityConditions();

		problem.transformGoal();
		problem.groundingActionProcessesConstraints();

		problem.simplifyAndSetupInit(false, false);

	}

	public LGG computeLandmarks(String dF, String pF, Collection<GroundAction> actions) throws Exception {

		domainFile = dF;
		problemFile = pF;

		domain = new PddlDomain(domainFile);
		problem = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);

		domain.substituteEqualityConditions();

		problem.transformGoal();
		problem.groundingActionProcessesConstraints();

		problem.simplifyAndSetupInit(false, false);

		if (actions != null) {
			problem.actions = actions;
		}

		createRPG(false);

		lgg = new LGG();
		lgg.initialize(problem.getGoals());

		System.out.println("Generate landmark candidates for goal: " + this.problem.getGoals());
		generateLandmarkCandidates();
		if(this.landmarkCandidates.size() == 0) {
			System.out.println("NO LANDMARK CANDIDATES GENERATED");
		}
		System.out.println("Generated " + landmarkCandidates.size() + " candidates");

		System.out.println("Start evaluating candidates...");
		evaluateCandidates();

		System.out.println("Evaluated Candidates, found " + landmarks.size() + " landmarks\n");

		lgg.calulateLMSets();

		return lgg;

	}

	public LGG computeLandmarks(PddlDomain d, EPddlProblem p) throws Exception {

		domain = d;
		problem = p;

		createRPG(false);

		lgg = new LGG();
		lgg.initialize(problem.getGoals());

		System.out.println("Generate landmark candidates for goal: " + this.problem.getGoals());
		generateLandmarkCandidates();
		System.out.println("Generated " + landmarkCandidates.size() + " candidates");

		System.out.println("Start evaluating candidates...");
		evaluateCandidates();

		System.out.println("Evaluated Candidates, found " + landmarks.size() + " landmarks\n");

		lgg.calulateLMSets();

		return lgg;

	}

	public void createRPG(PddlDomain d, EPddlProblem p, boolean b) throws CloneNotSupportedException {

		domain = d;
		problem = p;

		createRPG(b);

	}

	public void createRPG(boolean calculateFullPlan) throws CloneNotSupportedException {

		this.rpg = new RPG((PDDLState) this.problem.getInit());

		boolean reachable = rpg.computeRelaxedPlanningGraph(((PDDLState) this.problem.getInit()), this.problem.getGoals(), (Set) this.problem.getActions());

		if (calculateFullPlan) {

			this.relaxedPlan = rpg.computePlan();

		}

		this.levels = rpg.goal_reached_at;

		for (int i = 0; i < levels; i++) {

			ArrayList<GroundAction> actionss = null;

			if (USE_ALL_ACTIONS) {
				
				actionss = (ArrayList<GroundAction>) rpg.action_level.get(i);

			} else {
				
				actionss = (ArrayList<GroundAction>) rpg.plan_levels.get(i);
			}

			for (GroundAction gr : actionss) {
				action_levels.put(i, gr);
			}
		}
		
		Predicate pred;
		for(Object o : this.problem.getActualFluents().keySet()) {
			if(o instanceof Predicate) {
				pred = (Predicate) o;
			}
			else {
				continue;
			}
			for(int i=0; i < levels; i++) {
				if(pred.can_be_true(rpg.rel_state_level.get(i))) {
					fact_levels.put(i, pred);
				}
			}
		}

		for (Condition c : (Collection<Condition>) problem.getGoals().sons) {
			goals.put(getFactLevel(c), c);
		}

	}
	
	public void createRPGWithAllActions(PddlDomain domain, EPddlProblem problem) throws CloneNotSupportedException {
		this.domain = domain;
		this.problem = problem;
		
		this.rpg = new RPG((PDDLState) this.problem.getInit());

		boolean reachable = rpg.computeRelaxedPlanningGraph(((PDDLState) this.problem.getInit()), this.problem.getGoals(), (Set) this.problem.getActions());


		List<Predicate> relevantPredicates = new ArrayList<Predicate>();
		for(Object pred : this.problem.getActualFluents().values()) {
			if(pred instanceof Predicate) {
				relevantPredicates.add((Predicate)pred);
			}
		}

		this.levels = rpg.goal_reached_at;

		for (int i = 0; i < levels; i++) {

			ArrayList<GroundAction> actionss = null;

			if (USE_ALL_ACTIONS) {
				
				actionss = (ArrayList<GroundAction>) rpg.action_level.get(i);

			} else {
				
				actionss = (ArrayList<GroundAction>) rpg.plan_levels.get(i);
			}

			for (GroundAction gr : actionss) {
				action_levels.put(i, gr);
			}
		}

		Iterable<Predicate> init = this.problem.getPredicatesInvolvedInInit();

		fact_levels.putAll(0, init);

		for (int i = 0; i < levels - 1; i++) {

			RelState state = rpg.rel_state_level.get(i);

			for (GroundAction gr : action_levels.get(i)) {

				fact_levels.putAll(i + 1, gr.getAddList().sons);

				// conditional stuff
				if (gr.cond_effects != null) {
					for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) gr.cond_effects.sons) {
						if (state.satisfy(cEffect.activation_condition)) {

							Set<Condition> terms = Sets.newLinkedHashSetWithExpectedSize(((AndCond) cEffect.effect).sons.size());
							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {
								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}

							fact_levels.putAll(i + 1, terms);
						}
					}
				}

			}

		}

		for (int i = 0; i < levels; i++) {

			for (Predicate p : this.problem.getPredicatesInvolvedInInit()) {

				if (!fact_levels.get(i).contains(p)) {
					fact_levels.put(i, p);
				}
			}
		}

		for (Condition c : (Collection<Condition>) problem.getGoals().sons) {

			goals.put(getFactLevel(c), c);
		}
	}
	
	public void createRPGWithAllActionsV2(int samples, PddlDomain domain, EPddlProblem problem) throws CloneNotSupportedException {
		this.domain = domain;
		this.problem = problem;
		
		this.rpg = new RPG((PDDLState) this.problem.getInit());

		boolean reachable = rpg.computeRelaxedPlanningGraph(((PDDLState) this.problem.getInit()), this.problem.getGoals(), (Set) this.problem.getActions());


		List<Predicate> relevantPredicates = new ArrayList<Predicate>();
		for(Object pred : this.problem.getActualFluents().values()) {
			if(pred instanceof Predicate) {
				relevantPredicates.add((Predicate)pred);
			}
		}
		this.relaxedPlanLevelsList = rpg.findAllRelevantActionsV2(samples, ((PDDLState)this.problem.getInit()).relaxState(), relevantPredicates);

		this.levels = rpg.goal_reached_at;

		for (int i = 0; i < levels; i++) {

			ArrayList<GroundAction> actionss = null;

			if (USE_ALL_ACTIONS) {
				
				actionss = (ArrayList<GroundAction>) rpg.action_level.get(i);

			} else {
				
				actionss = (ArrayList<GroundAction>) rpg.plan_levels.get(i);
			}

			for (GroundAction gr : actionss) {
				action_levels.put(i, gr);
			}
		}

		Iterable<Predicate> init = this.problem.getPredicatesInvolvedInInit();

		fact_levels.putAll(0, init);

		for (int i = 0; i < levels - 1; i++) {

			RelState state = rpg.rel_state_level.get(i);

			for (GroundAction gr : action_levels.get(i)) {

				fact_levels.putAll(i + 1, gr.getAddList().sons);

				// conditional stuff
				if (gr.cond_effects != null) {
					for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) gr.cond_effects.sons) {
						if (state.satisfy(cEffect.activation_condition)) {

							Set<Condition> terms = Sets.newLinkedHashSetWithExpectedSize(((AndCond) cEffect.effect).sons.size());
							for (Object c : (Collection<Object>) ((AndCond) cEffect.effect).sons) {
								if (c instanceof Condition) {
									terms.addAll(((Condition) c).getTerminalConditions());
								}
							}

							fact_levels.putAll(i + 1, terms);
						}
					}
				}

			}

		}

		for (int i = 0; i < levels; i++) {

			for (Predicate p : this.problem.getPredicatesInvolvedInInit()) {

				if (!fact_levels.get(i).contains(p)) {
					fact_levels.put(i, p);
				}
			}
		}

		for (Condition c : (Collection<Condition>) problem.getGoals().sons) {

			goals.put(getFactLevel(c), c);
		}
	}

	public void generateLandmarkCandidates() {

		MultiValuedMap<Integer, Condition> C = new ArrayListValuedHashMap<>();

		for (Condition c : goals.values()) {
			C.put(this.getFactLevel(c), c);
		}

		MultiValuedMap<Integer, Condition> C_dash;

		int lmSetID = 1;

		while (C.size() > 0) {

			C_dash = new ArrayListValuedHashMap<>();

			for (Entry<Integer, Condition> entry : C.entries()) {

				if (entry.getKey() > 0) {

					MultiValuedMap<Integer, GroundAction> A = new ArrayListValuedHashMap<>();

					// let A be the set of all actions a such that L_dash is element of add(a), and
					// level(a) = level(L_dash) - 1

// ------------------------------------------------ NEW APPROACH					
					for (Entry<Integer, GroundAction> entryA : action_levels.entries()) {

						if (entryA.getKey() == (entry.getKey() - 1)) {
							
							/*
							 * IF searched condition (entry) is part of the add list of the currently checked action
							 * THEN add action to action set A
							 */
							if( entryA.getValue().getAddList().getInvolvedPredicates().contains(entry.getValue())) {
								A.put(entryA.getKey(), entryA.getValue());
							}
							/*
							 * Check conditional effects of checked action
							 */
							else if (entryA.getValue().cond_effects != null) {
								for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) entryA.getValue().cond_effects.sons) {
									if (entry.getKey() != 999 && rpg.rel_state_level.get((entry.getKey() - 1)).satisfy(cEffect.activation_condition)) {
										if (((Condition) cEffect.effect).getInvolvedPredicates().contains(entry.getValue())) {
											A.put(entryA.getKey(), entryA.getValue());
										}
									}
								}
							}
							
						}
					}
// -------------------------------------------------
					
					// for all facts L such that for all a element A : L is element of pre(a)
					if (A.size() != 0) {
						MultiValuedMap<Integer, Condition> temp = new ArrayListValuedHashMap<>();
						ArrayList<Condition> temp2 = new ArrayList<Condition>();
						boolean newList = true;

						//For all previously found actions that achieve some facts in the checked condition
						for (Entry<Integer, GroundAction> a : A.entries()) {
							if (a.getValue().getPreconditions() == null) {
								temp2 = new ArrayList<Condition>();
								break;
							}
							
							//If this is the first run of the for loop
							if (newList) {			
								for (Condition terminalCondition : a.getValue().getPreconditions().getTerminalConditions()) {
									if (terminalCondition instanceof Predicate) {
										temp2.add(terminalCondition);
									}
								}
								newList = false;
							} else {
								ArrayList<Condition> temp3 = new ArrayList<Condition>();
								
								for (Condition terminalCondition : a.getValue().getPreconditions().getTerminalConditions()) {
									if (terminalCondition instanceof Predicate) {
										temp3.add(terminalCondition);
									}

								}
								//Iteratively calculate the intersection of the terminal conditions of the peconditions of all actions in A
								temp2.retainAll(temp3);

								//If the intersection is empty then not landmark candidates can be generated!
								if (temp2.isEmpty()) {
									break;
								}
							}
						}

						for (Condition c : temp2) {
							if (!lgg.containsNode(c)) {
								temp.put(this.getFactLevel(c), c);
								lgg.addNode(c);
							}

							lgg.getNodeFromCond(c).addLmSetID(lmSetID);
							lgg.addEdge(c, entry.getValue(), lmSetID);
						}

						C_dash.putAll(temp);
						landmarkCandidates.addAll(temp.values());
					}
				}
				lmSetID++;
			}
			C = C_dash;
		}
	}

	public void evaluateCandidates() throws CloneNotSupportedException {

		long start = System.currentTimeMillis();

		Set<Predicate> initialFacts = (Set<Predicate>) problem.getPredicatesInvolvedInInit();

		double count = 1;
		int progress = 0;
		int noOfCandidates = landmarkCandidates.size();

		for (Condition c : landmarkCandidates) {
			count++;

			//Print progress bar
			if (PRINT_PROGRESS_BAR && (count / noOfCandidates) * 100 > progress) {
				System.out.print("|");
				progress++;
			}

			if (c instanceof Predicate || c instanceof NotCond) {
				if (initialFacts.contains(c)) {
					if(GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
						landmarks.add(c);
					}
					continue;
				}
			} else {
				if (initialFacts.containsAll(c.getTerminalConditions())) {
					if(GoalRecognitionConfiguration.USE_INITIAL_STATE_LANDMARKS) {
						landmarks.add(c);
					}
					continue;
				}
			}

			HashSet<GroundAction> actionss = new HashSet<GroundAction>();
			for (GroundAction g : ((HashSet<GroundAction>) problem.getActions())) {
				actionss.add((GroundAction) g.clone());
			}

			HashSet<GroundAction> actionsToBeRemoved = new HashSet<GroundAction>();

			HashSet<GroundAction> changedActions = new HashSet<GroundAction>();

			for (GroundAction ga : actionss) {
				if (ga.getAddList().involve(c)) {
					changedActions.add(ga);
					actionsToBeRemoved.add(ga);
					continue;
				}

				// conditional stuff
				List<ConditionalEffect> condEffectsToBeRemoved = new ArrayList<ConditionalEffect>();
				if (!ga.cond_effects.sons.isEmpty()) {
					for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) ga.cond_effects.sons) {
						if (((AndCond) cEffect.effect).sons.contains(c)) {
							condEffectsToBeRemoved.add(cEffect);
						}

					}
					ga.cond_effects.sons.removeAll(condEffectsToBeRemoved);
				}
			}

			if (changedActions.isEmpty()) {
				continue;
			}


			actionss.removeAll(actionsToBeRemoved);

			RPG rpg = new RPG((PDDLState) problem.getInit());

			boolean reachable = rpg.computeRelaxedPlanningGraph(((PDDLState) problem.getInit()), problem.getGoals(), actionss);

			if (!reachable) {
				landmarks.add(c);
			}
		}

		nonInitStateLandmarks = (HashSet<Condition>) landmarks.clone();

		nonInitStateLandmarks.removeAll(initialFacts);

		HashSet<Condition> notLMs = (HashSet<Condition>) landmarkCandidates.clone();
		notLMs.removeAll(landmarks);

		for (Condition c : notLMs) {

			lgg.removeNode(c);
		}

		long end = System.currentTimeMillis();
	}

	public int getFactLevel(Condition c) {
		for (int i = 0; i <= levels; i++) {
			if (c.can_be_true(rpg.rel_state_level.get(i))) {
				return i; // + 1;
			}
		}
		return 999;
	}
    
}

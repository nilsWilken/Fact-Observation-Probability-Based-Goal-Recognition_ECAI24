package symbolic.relaxedPlanning.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import com.hstairs.ppmajal.conditions.PDDLObject;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.ActionSchema;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;

import symbolic.planning.stateHandler.obsConversion.GMPDDLStateHandler;

public class ProbabilityCalculator {

	public static PddlDomain domain;
	public static EPddlProblem problem;
	public static ArrayList<String> goals;
	public static MultiValuedMap<String, ArrayList<GroundAction>> observations;
	public static HashMap<String, ArrayList<Map.Entry<String, Double>>> actionProbabilities;
	public static HashMap<String, ArrayList<Map.Entry<String, Double>>> objectProbabilities;
	public static HashMap<String, HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>> actionFluentProbabilities;
	public static HashMap<String, HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>> objectFluentProbabilities;
	public static HashMap<String, ArrayList<Map.Entry<String, Double>>> softLMs;

	public static HashMap<String, Double> actions = new HashMap<String, Double>();
	public static HashMap<String, Double> objects = new HashMap<String, Double>();
	public static HashMap<Predicate, HashMap<String, Double>> actionsPerFluent = new HashMap<Predicate, HashMap<String, Double>>();
	public static HashMap<Predicate, HashMap<String, Double>> objectsPerFluent = new HashMap<Predicate, HashMap<String, Double>>();
	public static HashSet<Predicate> boolFluents = new HashSet<Predicate>();

	private static DecimalFormat df = new DecimalFormat("0.00");

	public static void main(String[] args) throws Exception {

		String domainFile = "./resources/kitchen_evaluation/FIdomain.pddl";
		String problemFile = "./resources/kitchen_evaluation/FItemplate.pddl";

		String obsPath = "./resources/kitchen_evaluation/observations/";
		String goalsFile = "./resources/kitchen_evaluation/goals.txt";

		init(domainFile, problemFile, obsPath, goalsFile);

		calculateSoftLandmarks();
		// true: doesn't print initial state fluents and fluents with prob 0%
		printSoftLMs(true);

	}

	public static void printActionAndObjectProbs() {

		System.out.println("ACTION PROBABILITIES:\n");
		for (Entry<String, ArrayList<Entry<String, Double>>> e : actionProbabilities.entrySet()) {

			System.out.println("Probabilities for Goal: " + e.getKey() + ":");
			for (Entry<String, Double> ee : e.getValue()) {
				System.out.println("- " + ee.getKey() + ": " + df.format(ee.getValue() * 100) + "%");
			}

			System.out.println("\n");

		}

		System.out.println("OBJECT PROBABILITIES:\n");
		for (Entry<String, ArrayList<Entry<String, Double>>> e : objectProbabilities.entrySet()) {

			System.out.println("Probabilities for Goal: " + e.getKey() + ":");
			for (Entry<String, Double> ee : e.getValue()) {
				System.out.println("- " + ee.getKey() + ": " + df.format(ee.getValue() * 100) + "%");
			}

			System.out.println("\n");

		}

	}

	public static void printFluentProbs(int firstXfluents) {

		System.out.println("ACTION PROBABILITIES PER FLUENT:\n");
		for (Entry<String, HashMap<Predicate, ArrayList<Entry<String, Double>>>> e : actionFluentProbabilities
				.entrySet()) {

			System.out.println("Probabilities for Goal: " + e.getKey());

			int i = 0;

			for (Entry<Predicate, ArrayList<Entry<String, Double>>> ee : e.getValue().entrySet()) {

				if (ee.getKey().isGrounded()) {

					if (i > firstXfluents) {
						break;
					}

					System.out.println("  Probabilities for fluent: " + ee.getKey());

					for (Entry<String, Double> action : ee.getValue()) {

						System.out
								.println("     -" + action.getKey() + ": " + df.format(action.getValue() * 100) + "%");

					}

					i++;
				}
			}
		}

		System.out.println("\nOBJECT PROBABILITIES PER FLUENT:\n");
		for (Entry<String, HashMap<Predicate, ArrayList<Entry<String, Double>>>> e : objectFluentProbabilities
				.entrySet()) {

			System.out.println("Probabilities for Goal: " + e.getKey() + "\n");

			int i = 0;

			for (Entry<Predicate, ArrayList<Entry<String, Double>>> ee : e.getValue().entrySet()) {

				if (ee.getKey().isGrounded()) {

					if (i > firstXfluents) {
						break;
					}

					System.out.println("  Probabilities for fluent: " + ee.getKey());

					for (Entry<String, Double> object : ee.getValue()) {

						System.out
								.println("     -" + object.getKey() + ": " + df.format(object.getValue() * 100) + "%");

					}

					System.out.println();

					i++;
				}
			}
		}
	}

	public static void printSoftLMs(boolean ignoreInitiallyTrueFluentsAnd0Percent) {

		HashSet<String> initialFluents = new HashSet<String>();
		for (Predicate p : problem.getPredicatesInvolvedInInit()) {
			initialFluents.add(p.toString());
		}
		
		int trueLMs = 0;

		System.out.println("SOFT LANDMARKS:\n");
		for (Entry<String, ArrayList<Entry<String, Double>>> e : softLMs.entrySet()) {

			System.out.println("Probabilities for Goal: " + e.getKey() + ":");
			for (Entry<String, Double> ee : e.getValue()) {

				if (ignoreInitiallyTrueFluentsAnd0Percent && (initialFluents.contains(ee.getKey()) || ee.getValue() == 0.0)) {
					continue;
				}
				
				if(ee.getValue() == 1.0) {
					trueLMs++;
				}
				System.out.println(" - " + ee.getKey() + ": " + df.format(ee.getValue() * 100) + "%");
			}

			System.out.println("\nTrue Landmarks: "+trueLMs);
			System.out.println("\n");

		}
	}

	public static void calculateActionAndObjectProbabilities() {

		HashMap<String, ArrayList<Map.Entry<String, Double>>> actionProbs = new HashMap<String, ArrayList<Map.Entry<String, Double>>>();
		HashMap<String, ArrayList<Map.Entry<String, Double>>> objectProbs = new HashMap<String, ArrayList<Map.Entry<String, Double>>>();

		for (String goal : observations.keySet()) {

			int totalActionObs = 0;
			int totalObjectInteractions = 0;

			ArrayList<ArrayList<GroundAction>> obs = new ArrayList(observations.get(goal));

			for (ArrayList<GroundAction> sequence : obs) {
				for (GroundAction ga : sequence) {

					String actionName = ga.getName();
					double occurences = actions.get(actionName);
					actions.put(actionName, (occurences + 1));
					totalActionObs++;

					for (PDDLObject o : (ArrayList<PDDLObject>) ga.getParameters()) {

						String objectName = o.getName();
						double object_occurences = objects.get(objectName);
						objects.put(objectName, (object_occurences + 1));
						totalObjectInteractions++;
					}

				}
			}

			for (String key : actions.keySet()) {

				actions.put(key, (actions.get(key) / totalActionObs));

			}

			for (String key : objects.keySet()) {

				objects.put(key, (objects.get(key) / totalObjectInteractions));

			}

			ArrayList<Map.Entry<String, Double>> sortedActions = sortByValue(actions);

			actionProbs.put(goal, sortedActions);

			ArrayList<Map.Entry<String, Double>> sortedObjects = sortByValue(objects);

			objectProbs.put(goal, sortedObjects);

			resetActionsAndObjects();

		}

		actionProbabilities = actionProbs;
		objectProbabilities = objectProbs;

	}

	public static void calculateSoftLandmarks() {

		softLMs = new HashMap<String, ArrayList<Map.Entry<String, Double>>>();

		HashMap<String, Double> fluentProbs = new HashMap<String, Double>();

		for (String goal : observations.keySet()) {

			for (Predicate p : boolFluents) {
				fluentProbs.put(p.toString(), 0.0);
			}

			ArrayList<ArrayList<GroundAction>> obs = new ArrayList(observations.get(goal));

			int obsSize = obs.size();

			for (ArrayList<GroundAction> sequence : obs) {

				PDDLState state = (PDDLState) problem.getInit().clone();

				HashSet<Predicate> tmpFluents = (HashSet<Predicate>) boolFluents.clone();

				for (GroundAction ga : sequence) {

					HashSet<Predicate> fluentsToDelete = new HashSet<Predicate>();

					for (Predicate p : tmpFluents) {

						if (state.holds(p)) {

							double occurences = fluentProbs.get(p.toString());
							fluentProbs.put(p.toString(), (occurences + 1));
							fluentsToDelete.add(p);

						}
					}

					state.apply(ga, state.clone());
					tmpFluents.removeAll(fluentsToDelete);

				}

				for (Predicate p : tmpFluents) {

					if (state.holds(p)) {

						double occurences = fluentProbs.get(p.toString());
						fluentProbs.put(p.toString(), (occurences + 1));

					}
				}
			}

			for (String key : fluentProbs.keySet()) {

				fluentProbs.put(key, (fluentProbs.get(key) / obsSize));

			}

			ArrayList<Map.Entry<String, Double>> sortedFluents = sortByValue(fluentProbs);

			softLMs.put(goal, sortedFluents);
		}

	}

	public static void calculateProbabilitiesPerFluent() {

		HashMap<String, HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>> actionProbs = new HashMap<String, HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>>();
		HashMap<String, HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>> objectProbs = new HashMap<String, HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>>();

		for (String goal : observations.keySet()) {

			int totalActionObs = 0;
			int totalObjectInteractions = 0;

			ArrayList<ArrayList<GroundAction>> obs = new ArrayList(observations.get(goal));

			for (ArrayList<GroundAction> sequence : obs) {
				
				PDDLState state = (PDDLState) problem.getInit().clone();
				
				for (GroundAction ga : sequence) {
					for (Predicate p : boolFluents) {

						if (state.holds(p)) {

							double occurences = actionsPerFluent.get(p).get(ga.getName());
							actionsPerFluent.get(p).put(ga.getName(), (occurences + 1));

							for (PDDLObject o : (ArrayList<PDDLObject>) ga.getParameters()) {

								String objectName = o.getName();
								double object_occurences = objectsPerFluent.get(p).get(objectName);
								objectsPerFluent.get(p).put(objectName, (object_occurences + 1));

							}

						}

					}

					state.apply(ga, state.clone());

					totalActionObs++;
					totalObjectInteractions += ga.getParameters().size();

				}
				
				for (Predicate p : boolFluents) {

					if (state.holds(p)) {

						double occurences = actionsPerFluent.get(p).get(sequence.get(sequence.size()-1).getName());
						actionsPerFluent.get(p).put(sequence.get(sequence.size()-1).getName(), (occurences + 1));

						for (PDDLObject o : (ArrayList<PDDLObject>) sequence.get(sequence.size()-1).getParameters()) {

							String objectName = o.getName();
							double object_occurences = objectsPerFluent.get(p).get(objectName);
							objectsPerFluent.get(p).put(objectName, (object_occurences + 1));

						}

					}

				}
			}
			
			

			for (HashMap<String, Double> map : actionsPerFluent.values()) {
				for (String key : map.keySet()) {

					map.put(key, (map.get(key) / totalActionObs));
				}

			}

			for (HashMap<String, Double> map : objectsPerFluent.values()) {
				for (String key : map.keySet()) {

					map.put(key, (map.get(key) / totalObjectInteractions));
				}

			}

			HashMap<Predicate, ArrayList<Map.Entry<String, Double>>> orderedActionsMap = new HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>();
			HashMap<Predicate, ArrayList<Map.Entry<String, Double>>> orderedObjectsMap = new HashMap<Predicate, ArrayList<Map.Entry<String, Double>>>();

			for (Predicate p : boolFluents) {

				ArrayList<Map.Entry<String, Double>> sortedActionsPerFluent = sortByValue(actionsPerFluent.get(p));
				ArrayList<Map.Entry<String, Double>> sortedObjectsPerFluent = sortByValue(objectsPerFluent.get(p));

				orderedActionsMap.put(p, sortedActionsPerFluent);
				orderedObjectsMap.put(p, sortedObjectsPerFluent);

			}

			actionProbs.put(goal, orderedActionsMap);
			objectProbs.put(goal, orderedObjectsMap);

			resetActionFluentsAndObjectFluents();

		}

		actionFluentProbabilities = actionProbs;
		objectFluentProbabilities = objectProbs;

	}

	public static void resetActionsAndObjects() {

		for (ActionSchema as : domain.getActionsSchema()) {

			actions.put(as.getName(), 0.0);

		}

		for (PDDLObject o : domain.getConstants()) {

			objects.put(o.getName(), 0.0);

		}

	}

	public static void resetActionFluentsAndObjectFluents() {

		for (Predicate p : boolFluents) {

			actionsPerFluent.put(p, (HashMap<String, Double>) actions.clone());
			objectsPerFluent.put(p, (HashMap<String, Double>) objects.clone());

		}
	}

	public static ArrayList<Map.Entry<String, Double>> sortByValue(HashMap<String, Double> hm) {

		HashMap<String, Double> clone = (HashMap<String, Double>) hm.clone();

		ArrayList<Map.Entry<String, Double>> list = new ArrayList<Map.Entry<String, Double>>(clone.entrySet());

		Collections.sort(list, (i2, i1) -> i1.getValue().compareTo(i2.getValue()));

		return list;

	}

	public static void init(String domainFile, String problemFile, String obsPath, String goalsFile) throws Exception {

		domain = new PddlDomain(domainFile);
		problem = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);

		domain.substituteEqualityConditions();

		problem.transformGoal();
		problem.groundingActionProcessesConstraints();

		problem.simplifyAndSetupInit(false, false);

		goals = readGoals(goalsFile);

		observations = readObservations(domainFile, problemFile, obsPath, goals);

		for (ActionSchema as : domain.getActionsSchema()) {

			actions.put(as.getName(), 0.0);

		}

		for (PDDLObject o : domain.getConstants()) {

			objects.put(o.getName(), 0.0);

		}

		for (Object o : problem.getActualFluents().keySet()) {

			if (o instanceof Predicate) {
				boolFluents.add((Predicate) o);
			}
		}

		for (Predicate p : boolFluents) {

//			System.out.println(p);

			actionsPerFluent.put(p, (HashMap<String, Double>) actions.clone());
			objectsPerFluent.put(p, (HashMap<String, Double>) objects.clone());

		}

//		System.out.println("FLUENTS: " + boolFluents.size());
//		System.out.println("GROUNDED FLUENTS: " + grounded);
//		System.out.println("NOT GROUNDED: " + (boolFluents.size() - grounded));
//		System.out.println(problem.getActualFluents());

	}

	public static MultiValuedMap<String, ArrayList<GroundAction>> readObservations(String domain, String problem,
			String obsPath, ArrayList<String> goals) {

		GMPDDLStateHandler stateHandler = new GMPDDLStateHandler(domain, problem);

		String[] obsFiles;

		File f = new File(obsPath);

		obsFiles = f.list();

		MultiValuedMap<String, ArrayList<GroundAction>> observations = new ArrayListValuedHashMap<>();

		for (String obsFile : obsFiles) {

			String goal = "";

			for (String g : goals) {

				if (g.contains(obsFile.substring(0, obsFile.indexOf("_")))) {

					goal = g;
				}

			}

			observations.put(goal, (ArrayList) stateHandler.retrieveActionListFromObsFile(obsPath + obsFile));
		}

		return observations;
	}

	public static ArrayList<String> readGoals(String filepath) {

		ArrayList<String> goals = new ArrayList<String>();

		BufferedReader reader;
		try {
			reader = new BufferedReader(new FileReader(filepath));
			String line = reader.readLine();
			while (line != null) {
				goals.add(line);
				line = reader.readLine();
			}
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return goals;

	}

}

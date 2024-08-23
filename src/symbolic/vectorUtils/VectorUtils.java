package symbolic.vectorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.problem.GroundAction;

public class VectorUtils {
    public static List<Double> convertRelaxedStateToVector(List<String> pPredicates, List<Predicate> allPredicates) {
		List<Double> vector = new ArrayList<Double>();
		
		for(Predicate p : allPredicates) {
			if(pPredicates.contains(p.pddlPrint(false))) {
				vector.add(1.0);
			}
			else {
				vector.add(0.0);
			}
		}
		return vector;
	}

	public static List<Double> convertRelaxedStateToVector(Map<String, Double> pPredicates, List<Predicate> allPredicates) {
		List<Double> vector = new ArrayList<Double>();

		String predicateName;
		for(Predicate p : allPredicates) {
			predicateName = p.pddlPrint(false);

			if(pPredicates.keySet().contains(predicateName)) {
				vector.add(pPredicates.get(predicateName));
			}
			else {
				vector.add(0.0);
			}
		}

		return vector;
	}

    public static List<List<Double>> convertRelaxedStatesToVector(List<List<String>> pPredicatesList, List<Predicate> allPredicates) {
		List<List<Double>> result = new ArrayList<List<Double>>();
		
		for(List<String> pPredicates : pPredicatesList) {
			result.add(VectorUtils.convertRelaxedStateToVector(pPredicates, allPredicates));
		}
		
		
		return result;
	}

	public static List<Double> convertRelaxedStateAndObsToVector(Map<String, Double> pPredicates, Map<GroundAction, Double> observedActions, List<Predicate> allPredicates, List<GroundAction> allActions) {
		List<Double> vector = new ArrayList<Double>();

		String predicateName;
		for(Predicate p : allPredicates) {
			predicateName = p.pddlPrint(false);

			if(pPredicates.keySet().contains(predicateName)) {
				vector.add(pPredicates.get(predicateName));
			}
			else {
				vector.add(0.0);
			}
		}

		boolean found;
		for(GroundAction a : allActions) {
			found = false;
			for(GroundAction obsA : observedActions.keySet()) {
				if(obsA.getName().equals(a.getName()) && PDDLUtils.convertActionParameters(a).equals(PDDLUtils.convertActionParameters(obsA))) {
					vector.add(observedActions.get(obsA));
					found = true;
					break;
				}
			}
			if(!found) {
				vector.add(0.0);
			}
		}

		return vector;
	}

    public static List<Map<Integer, List<Double>>> removeIntersectionElementsFromVectors(List<Map<Integer, List<Double>>> vectorMaps, List<Double> intersectionVector) {
		List<Map<Integer, List<Double>>> removedVectorMaps = new ArrayList<Map<Integer, List<Double>>>();
		
		Map<Integer, List<Double>> removedVectorMap;
		for(Map<Integer, List<Double>> map : vectorMaps) {
			removedVectorMap = new HashMap<Integer, List<Double>>();
			for(int key : map.keySet()) {
				removedVectorMap.put(key, VectorUtils.removeIntersectionElementsFromVector(map.get(key), intersectionVector));
			}
			removedVectorMaps.add(removedVectorMap);
		}
		
		return removedVectorMaps;
	}
	
	public static List<Double> removeIntersectionElementsFromVector(List<Double> vector, List<Double> intersectionVector) {
		List<Double> convertedVector = new ArrayList<Double>();
		for(int i=0; i < vector.size(); i++) {
			if(intersectionVector.get(i) == 1.0) {
				convertedVector.add(0.0);
			} else {
				convertedVector.add(vector.get(i));
			}
		}
		
		return convertedVector;
	}

    public static List<Double> divideVector(List<Double> vector, double nominator) {
		List<Double> result = new ArrayList<Double>();

		for(double d : vector) {
			result.add(d/nominator);
		}

		return result;
	}

    public static double calculateL2Norm(List<Double> vec) {
		double norm = 0.0;

		for(double ele : vec) {
			norm += Math.pow(ele, 2);
		}

		return Math.sqrt(norm);
	}
}

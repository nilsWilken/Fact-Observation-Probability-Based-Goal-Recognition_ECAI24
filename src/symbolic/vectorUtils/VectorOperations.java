package symbolic.vectorUtils;

import java.util.ArrayList;
import java.util.List;

import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

public class VectorOperations {
	
	/**
	 * Convert the nominal attributes of WEKA instances to numeric attributes.
	 * @param instances Instance of the WEKA 'Instances' class.
	 * @return Instance of the WEKA 'Instances' class that contains the converted instances.
	 */
	public static Instances convertNominalAttributesToNumericAttributes(Instances instances) {
		ArrayList<Attribute> atts = new ArrayList<Attribute>();
				
		Attribute att;
		for(int i=0; i < instances.numAttributes(); i++) {
			att = instances.attribute(i);
			atts.add(new Attribute(att.name()));
		}
		
		Instances result = new Instances("Numeric Training Data", atts, instances.numInstances());
		
		DenseInstance instNew;
		for(Instance inst : instances) {
			instNew = new DenseInstance(inst.numAttributes());
			
			for(int i=0; i < inst.numAttributes(); i++)  {
				instNew.setValue(inst.attribute(i), inst.value(inst.attribute(i)));
			}
			result.add(instNew);
		}
		
		return result;
	}

	public static double calculateEuclideanNorm(List<Double> v) {
		double sum = 0.0;

		for(double value : v) {
			sum += Math.pow(value, 2);
		}

		return Math.sqrt(sum);
	}

	public static double calculateNorm(List<Double> v, int l) {
		double sum = 0.0;

		for(double value : v) {
			sum += Math.pow(Math.abs(value), l);
		}

		return Math.sqrt(sum);
	}
	
	public static double calculateP1Norm(List<Double> v) {
		double sum = 0;
		
		for(double value : v) {
			sum += value;
		}
		
		return sum;
	}
	
	public static List<Double> calculateDirection(Instance inst1, Instance inst2) {
		List<Double> result = new ArrayList<Double>();
		
		for(int i=0; i < inst1.numAttributes(); i++) {
			result.add(inst2.value(inst2.attribute(i)) - inst1.value(inst1.attribute(i)));
		}
		
		return result;
	}
	
	public static List<Double> calculateDirection(List<Double> v1, List<Double> v2) {
		List<Double> result = new ArrayList<Double>();
		
		for(int i=0; i < v1.size(); i++) {
			result.add(v2.get(i) - v1.get(i));
		}
		
		return result;
	}
	
	public static List<Double> convertInstanceToVector(Instance inst) {
		List<Double> result = new ArrayList<Double>();
		
		for(int i=0; i < inst.numAttributes(); i++) {
			result.add(inst.value(inst.attribute(i)));
		}
		
		return result;
	}
	
	public static List<Double> convertArrayToVector(double[] array) {
		List<Double> result = new ArrayList<Double>();
		
		for(double value : array) {
			result.add(value);
		}
		
		return result;
	}
	
	public static List<Double> convertArrayToTruncatedVector(double[] array, int rank) {
		List<Double> result = new ArrayList<Double>();
		
		double scalingConstant = 1;
		
		for(int i=0; i < rank; i++) {
			result.add(array[i]*scalingConstant);
		}
		
		return result;
	}
	
	public static List<Double> convertVectorToTruncatedVector(List<Double> vec, int rank) {
		List<Double> result = new ArrayList<Double>();
		
		for(int i=0; i < rank; i++) {
			result.add(vec.get(i));
		}
		
		return result;
	}
	
	public static List<List<Double>> convertToMatchingObservedFactsVectors(List<Double> observedV, List<Double> goalStateV) {
		List<Double> convertedObservedV = new ArrayList<Double>();
		List<Double> convertedGoalStateV = new ArrayList<Double>();
		
		for(int i=0; i < observedV.size(); i++) {
			if(observedV.get(i).doubleValue() == 1.0) {
				convertedObservedV.add(observedV.get(i));
				convertedGoalStateV.add(goalStateV.get(i));
			}
		}
		
		List<List<Double>> result = new ArrayList<List<Double>>();
		result.add(convertedObservedV);
		result.add(convertedGoalStateV);
	
		return result; 
	}

	public static List<Double> elementWiseMultiplication(List<Double> v1, List<Double> v2) {
		List<Double> result = new ArrayList<Double>();

		for(int i=0; i < v1.size(); i++) {
			result.add(v1.get(i)*v2.get(i));
		}

		return result;
	}

	public static List<Double> elementWiseMultiplicationCurrent(List<Double> current, List<Double> hyp) {
		List<Double> result = new ArrayList<Double>();

		for(int i=0; i < current.size(); i++) {
			if(hyp.get(i) > 0) {
				result.add(hyp.get(i)*current.get(i));
			}
			else {
				result.add(current.get(i));
			}
		}

		return result;
	}

}

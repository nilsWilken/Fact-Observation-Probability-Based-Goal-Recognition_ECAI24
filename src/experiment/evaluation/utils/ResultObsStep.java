package experiment.evaluation.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import config.GoalRecognitionConfiguration;
import eu.amidst.core.distribution.UnivariateDistribution;
import utils.database.ColumnType;
import utils.database.DatabaseRecord;

public class ResultObsStep implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 6664147561768103612L;
	private HashMap<String, Double> goalProbs;
	
	public ResultObsStep() {
		this.goalProbs = new HashMap<String, Double>();
	}
	
	public ResultObsStep(HashMap<String, Double> goalProbs) {
		this.goalProbs = goalProbs;
	}
	
	public ResultObsStep(double[] goalProbs) {
		this.goalProbs = new HashMap<String, Double>();
		for(int i=0; i < goalProbs.length; i++) {
			this.goalProbs.put(GoalRecognitionConfiguration.generateGenericHypName(i), goalProbs[i]);
		}
	}
	
	public void addGoalProbability(String hyp, double prob) {
		this.goalProbs.put(hyp, prob);
	}
	
	public double getGoalProbability(String hyp) {
		return this.goalProbs.get(hyp);
	}
	
	public List<String> getHypWithMaxProbability() {
		List<String> maxHyps = new ArrayList<String>();
		double maxProb = this.getMaxProbability();
				
		for(String hyp : this.goalProbs.keySet()) {
			if(this.goalProbs.get(hyp) == maxProb) {
				maxHyps.add(hyp);
			}
		}
		return maxHyps;
	}
	
	public List<String> getHypWithMaxProbability(double threshold) {
		List<String> maxHyps = new ArrayList<String>();
		double maxProb = this.getMaxProbability();
		
		for(String hyp : this.goalProbs.keySet()) {
			if(this.goalProbs.get(hyp) > (maxProb - threshold)) {
				maxHyps.add(hyp);
			}
		}
		return maxHyps;
	}
	
	public double getMaxProbability() {
		double max = (double)Integer.MIN_VALUE;
		for (double prob : this.goalProbs.values()) {
			if(prob > max) {
				max = prob;
			}
		}
		return max;
	}
	
	public double[] getMaxProbabilities(int number) {
		double[] maxProbs = new double[number];
		
		for(double prob : this.goalProbs.values()) {
			if(prob > maxProbs[0]) {
				for(int i = maxProbs.length-1; i > 0; i--) {
					maxProbs[i] = maxProbs[i-1];
				}
				maxProbs[0] = prob;
			}
		}
		
		return maxProbs;
	}
	
	public Set<String> getSetOfHypotheses() {
		return this.goalProbs.keySet();
	}
	
	public void normalizeGoalProbabilites() {
		double totalSum = 0.0;
		
		for(String key : this.goalProbs.keySet()) {
			totalSum += this.goalProbs.get(key);
		}
		double prob;
		if (totalSum != 0.0) {
			for (String key : this.goalProbs.keySet()) {
				prob = this.goalProbs.get(key) / totalSum;
				this.goalProbs.put(key, prob);
			}
		}
	}
	
	public List<DatabaseRecord> convertToDatabaseRecord(int obs) {
		List<DatabaseRecord> result = new ArrayList<DatabaseRecord>();
		
		ColumnType[] schema = new ColumnType[] {ColumnType.OBSERVATION_STEP, ColumnType.HYPOTHESIS, ColumnType.GOAL_PROBABILITY};
		Object[] values;
		for(String hyp : this.goalProbs.keySet()) {
			values = new Object[3];
			values[0] = obs;
			values[1] = hyp;
			values[2] = this.goalProbs.get(hyp);
			
			result.add(new DatabaseRecord(schema, values));
		}
		
		return result;
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		for(String hyp : this.goalProbs.keySet()) {
			result.append(hyp + ": " + this.goalProbs.get(hyp) + "\n");
		}
		
		return result.toString();
	}

}

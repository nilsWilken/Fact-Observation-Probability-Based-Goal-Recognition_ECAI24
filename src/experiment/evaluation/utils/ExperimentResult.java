package experiment.evaluation.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import config.EvaluationConfiguration;
import utils.database.ColumnType;
import utils.database.DatabaseRecord;
import utils.database.EvaluationTable;
import utils.database.SQLiteDBHandler;

public class ExperimentResult implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8460364566478390986L;
	
	private Map<Integer, ResultObsStep> observationSteps;
	private Set<String> setOfHypotheses;
	private String trueHyp;
	private Map<String, Long> computationTimes;
	private Map<String, Integer> planLengths;
	private Long totalComputationTime = 0L;
	private String expName;
	
	public ExperimentResult(String name) {
		this.expName = name;
		this.observationSteps = new HashMap<Integer, ResultObsStep>();
		this.computationTimes = new HashMap<String, Long>();
		this.planLengths = new HashMap<String, Integer>();
	}
	
	public String getExperimentName() {
		return this.expName;
	}
	
	public void addResultStep(int obs, ResultObsStep step) {
		if(this.setOfHypotheses == null || (this.setOfHypotheses.containsAll(step.getSetOfHypotheses()) && this.setOfHypotheses.size() == step.getSetOfHypotheses().size())) {
			this.observationSteps.put(obs, step);
		}
		else {
		}
	}
	
	public ResultObsStep getResultStep(int obs) {
		return this.observationSteps.get(obs);
	}
	
	public Map<Integer, ResultObsStep> getAllResultSteps() {
		return this.observationSteps;
	}
	
	public int numberOfResultSteps() {
		return this.observationSteps.values().size();
	}
	
	public void setTrueHyp(String trueHyp) {
		this.trueHyp = trueHyp;
	}
	
	public List<String> getHypsWithMaxProbabilityForObsStep(int step) {
		return this.observationSteps.get(step).getHypWithMaxProbability();
	}
	
	public List<String> getHypsWithMaxProbabilityForObsStep(int step, double threshold) {
		return this.observationSteps.get(step).getHypWithMaxProbability(threshold);
	}
	
	public double getMaxProbabilityForObsStep(int step) {
		return this.observationSteps.get(step).getMaxProbability();
	}
	
	public Set<String> getSetOfHypotheses() {
		return this.setOfHypotheses;
	}
	
	public void setSetOfHypotheses(Set<String> hypotheses) {
		this.setOfHypotheses = hypotheses;
	}
	
	public String getTrueHyp() {
		return this.trueHyp;
	}
	
	public void addComputationTimeInMillis(String timeKey, long computationTimeInMillis) {
		this.computationTimes.put(timeKey, computationTimeInMillis);
	}
	
	public long getComputationTimeInMillis(String timeKey) {
		return this.computationTimes.get(timeKey);
	}
	
	public Map<String, Long> getAllComputationTimes() {
		return this.computationTimes;
	}
	
	public void setTotalComputationTimeInMillis(long computationTimeInMillis) {
		this.totalComputationTime = computationTimeInMillis;
	}
	
	public long getTotalComputationTimeInMillis() {
		return this.totalComputationTime;
	}
	
	public void addPlanLength(String planLengthKey, int planLength) {
		this.planLengths.put(planLengthKey, planLength);
	}
	
	public int getPlanLength(String planLengthKey) {
		return this.planLengths.get(planLengthKey);
	}
	
	public Map<String, Integer> getAllPlanLengths() {
		return this.planLengths;
	}
	
	public boolean isTrueHypPredictedForObsStep(int step) {
		List<String> maxHyps;
		if(EvaluationConfiguration.USE_GOAL_SCORE_THRESHOLD) {
			maxHyps = this.getHypsWithMaxProbabilityForObsStep(step, EvaluationConfiguration.GOAL_SCORE_THRESHOLD);
		}
		else {
			maxHyps = this.getHypsWithMaxProbabilityForObsStep(step);		
		}
		return this.checkMaxHyps(maxHyps);
	}

	private boolean checkMaxHyps(List<String> maxHyps) {
		for(String hyp : maxHyps) {
			if(hyp.equals(this.trueHyp)) {
				return true;
			}
		}
		return false;
	}
	
	public int getSpreadForObsStep(int step) {
		if(EvaluationConfiguration.USE_GOAL_SCORE_THRESHOLD) {
			return this.getSpreadForObsStep(step, EvaluationConfiguration.GOAL_SCORE_THRESHOLD);
		}
		return this.getHypsWithMaxProbabilityForObsStep(step).size();
	}

	public int getSpreadForObsStep(int step, double threshold) {
		return this.getHypsWithMaxProbabilityForObsStep(step, threshold).size();
	}
	
	public void setExperimentName(String name) {
		this.expName = name;
	}
	
	public double getGoalProbabilityForObsStep(int obsStep, String goal) {
		return this.observationSteps.get(obsStep).getGoalProbability(goal);
	}
	
	public void writeResultToDatabase(String summaryName, String tableName, SQLiteDBHandler database) {
		database.createRecordTable(tableName, EvaluationTable.EXPERIMENT_RESULT);
		
		for(int obs : this.observationSteps.keySet()) {
			for(DatabaseRecord record : this.observationSteps.get(obs).convertToDatabaseRecord(obs)) {
				database.insertRecord(tableName, record);
			}
		}
		
		//Insert computation times
		database.createRecordTable(tableName, EvaluationTable.EXPERIMENT_COMPUTATION_TIMES);
		for(DatabaseRecord record : this.convertComputationTimeToDatabaseRecords()) {
			database.insertRecord(tableName, record);
		}
		
		//Insert plan lengths
		List<DatabaseRecord> planLengthRecords = this.convertPlanLengthsToDatabaseRecords();
		if(planLengthRecords.size() > 0) {
			database.createRecordTable(tableName, EvaluationTable.PLAN_LENGTHS);
			for(DatabaseRecord record : planLengthRecords) {
				database.insertRecord(tableName, record);
			}
		}		
		
		//Insert experiment name into summary table
		ColumnType[] summarySchema = new ColumnType[] {ColumnType.EXPERIMENT_NAME, ColumnType.COMPUTATION_TIME, ColumnType.TRUE_HYPOTHESIS};
		Object[] values = new Object[] {tableName, this.totalComputationTime, this.trueHyp};
		database.insertRecord(summaryName, new DatabaseRecord(summarySchema, values));
		
	}
	
	@Override
	public String toString() {
		StringBuffer result = new StringBuffer();
		
		for(int key : this.observationSteps.keySet()) {
			result.append("\n" + key + "\n" + this.observationSteps.get(key).toString());
		}
		
		return result.toString();
	}
	
	private List<DatabaseRecord> convertComputationTimeToDatabaseRecords() {
		List<DatabaseRecord> records = new ArrayList<DatabaseRecord>();
		
		if(this.computationTimes == null) {
			return records;
		}
		
		ColumnType[] schema = EvaluationTable.getTableSchema(EvaluationTable.EXPERIMENT_COMPUTATION_TIMES);
		Object[] values;
		
		for(String timeKey : this.computationTimes.keySet()) {
			values = new Object[2];
			values[0] = timeKey;
			values[1] = this.computationTimes.get(timeKey);
			
			records.add(new DatabaseRecord(schema, values));
		}
		
		return records;
	}
	
	private List<DatabaseRecord> convertPlanLengthsToDatabaseRecords() {
		List<DatabaseRecord> records = new ArrayList<DatabaseRecord>();
		
		if(this.planLengths == null || this.planLengths.keySet().size() == 0) {
			return records;
		}
		
		ColumnType[] schema = EvaluationTable.getTableSchema(EvaluationTable.PLAN_LENGTHS);
		Object[] values;
		
		for(String planLengthKey : this.planLengths.keySet()) {
			values = new Object[2];
			values[0] = planLengthKey;
			values[1] = this.planLengths.get(planLengthKey);
			
			records.add(new DatabaseRecord(schema, values));
		}
		
		return records;
	}
}

package utils.database;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import config.GoalRecognitionConfiguration;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.ResultObsStep;
import experiment.management.ExperimentType;

public class ExperimentResultDatabase {
	private SQLiteDBHandler database;
	private ExperimentType expType;
	private String summaryName;
	
	private List<String> expNames;
	private Map<String, String> trueHyps;
	private Map<String, Long> totalComputationTimes;
	
	public ExperimentResultDatabase(String dbFilePath, ExperimentType expType) {
		this.database = SQLiteDBHandler.getInstance(dbFilePath);
		this.expType = expType;
		
		this.summaryName = ExperimentType.getDatabaseSummaryName(this.expType);
		
		this.expNames = new ArrayList<String>();
		this.trueHyps = new HashMap<String, String>();
		this.totalComputationTimes = new HashMap<String, Long>();
	}
	
	public ExperimentResultDatabase(String dbFilePath) {
		this.database = SQLiteDBHandler.getInstance(dbFilePath);
		
		this.expType = this.determineExperimentType();
		
		this.summaryName = ExperimentType.getDatabaseSummaryName(this.expType);
		
		this.initializeSummaryData();
	}
	
	public ExperimentType getExperimentType() {
		return this.expType;
	}
	
	public List<ExperimentResult> readExperimentResultsListFromDatabase() {
		//Read results for all extracted experiment names
		List<ExperimentResult> results = new ArrayList<ExperimentResult>();
		for(String expName : this.expNames) {
			results.add(this.readExperimentResultFromDatabase(expName));
		}
		
		return results;
	}
	
	public Map<String, ExperimentResult> readExperimentResultMapFromDatabase() {
		//Read results for all extracted experiment names
		Map<String, ExperimentResult> results = new HashMap<String, ExperimentResult>();
		ExperimentResult cResult;
		for(String expName : this.expNames) {
			cResult = this.readExperimentResultFromDatabase(expName);
			results.put(expName, cResult);
		}
		
		return results;
	}
	
	public ExperimentResult readExperimentResultFromDatabase(String expName) {		
		List<DatabaseRecord> resultEntries = this.database.getResultTable(expName, EvaluationTable.EXPERIMENT_RESULT);
		List<DatabaseRecord> computationTimes = this.database.getResultTable(expName, EvaluationTable.EXPERIMENT_COMPUTATION_TIMES);
		List<DatabaseRecord> planLengths = this.database.getResultTable(expName, EvaluationTable.PLAN_LENGTHS);
		
		if(resultEntries == null) {
			System.out.println("Database entries for " + expName + " not found!");
		}
		
		ExperimentResult result = new ExperimentResult(expName);
		
		//Read result steps
		Map<Integer, ResultObsStep> resultSteps = new HashMap<Integer, ResultObsStep>();
		int obs;
		ResultObsStep cStep;
		Set<String> setOfHypotheses = new HashSet<String>();
		for(DatabaseRecord record : resultEntries) {
			obs = record.getInt(ColumnType.OBSERVATION_STEP);
			if(resultSteps.get(obs) == null) {
				resultSteps.put(obs, new ResultObsStep());
			}
			cStep = resultSteps.get(obs);
			cStep.addGoalProbability(record.getString(ColumnType.HYPOTHESIS), record.getDouble(ColumnType.GOAL_PROBABILITY));
			setOfHypotheses.add(record.getString(ColumnType.HYPOTHESIS));
		}
		
		result.setSetOfHypotheses(setOfHypotheses);
		
		//Add result steps to ExperimentResult
		for(int key : resultSteps.keySet()) {
			result.addResultStep(key, resultSteps.get(key));
		}
		
		
		//Read and add computation times
		if(computationTimes != null) {
			for(DatabaseRecord record : computationTimes) {
				result.addComputationTimeInMillis(record.getString(ColumnType.COMPUTATION_TIME_KEY), record.getLong(ColumnType.COMPUTATION_TIME));
			}
		}
		
		//Read and add plan lengths
		if(planLengths != null) {
			for(DatabaseRecord record : planLengths) {
				result.addPlanLength(record.getString(ColumnType.PLAN_LENGTH_KEY), record.getInt(ColumnType.PLAN_LENGTH));
			}
		}
		
		result.setTrueHyp(this.trueHyps.get(expName));
		result.setTotalComputationTimeInMillis(this.totalComputationTimes.get(expName));

		if(result.getTrueHyp() == null) {
			String[] split = result.getExperimentName().split("_");

			result.setTrueHyp(result.getExperimentName().split("_")[0]);
		}
		
		return result;
	}
	
	public List<DatabaseRecord> getSummaryTable() {
		System.out.println("SUMMARY NAME: " + this.summaryName);
		return this.database.getResultTable(this.summaryName, EvaluationTable.SUMMARY);
	}
	
	public void createSummaryTable() {
		this.database.createRecordTable(this.summaryName, EvaluationTable.SUMMARY);
		this.database.commit();
	}
	
	public void commit() {
		this.database.commit();
	}
	
	public void writeExperimentResultToDatabase(ExperimentResult result) {
		result.writeResultToDatabase(this.summaryName, result.getExperimentName(), this.database);
		
		this.expNames.add(result.getExperimentName());
		this.trueHyps.put(result.getExperimentName(), result.getTrueHyp());
		this.totalComputationTimes.put(result.getExperimentName(), result.getTotalComputationTimeInMillis());
	}
	
	private ExperimentType determineExperimentType() {
		List<String> tableNames = this.database.getTableCatalog();
		
		for(String name : tableNames) {
		    System.out.println(name);
			if(name.contains(EvaluationTable.getTableName(EvaluationTable.SUMMARY))) {
				return ExperimentType.parseFromSummaryName(name);
			}
		}
		return null;
	}
	
	private void initializeSummaryData() {
		List<DatabaseRecord> expNameRecords = this.getSummaryTable();
				
		this.expNames = new ArrayList<String>();
		this.trueHyps = new HashMap<String, String>();
		this.totalComputationTimes = new HashMap<String, Long>();
		
		//Read all experiment names from database
		String name;
		for(DatabaseRecord expNameRecord : expNameRecords) {
			name = expNameRecord.getString(ColumnType.EXPERIMENT_NAME);
			this.expNames.add(name);
			
			this.trueHyps.put(name, expNameRecord.getString(ColumnType.TRUE_HYPOTHESIS));
			this.totalComputationTimes.put(name, expNameRecord.getLong(ColumnType.COMPUTATION_TIME));
		}
	}

}

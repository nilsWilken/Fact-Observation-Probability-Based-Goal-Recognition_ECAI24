package experiment.evaluation.wrapper;

import java.util.Map;

import experiment.evaluation.utils.ExperimentResult;
import utils.database.SQLiteDBHandler;

public class PRAPResultsWrapper {

	private Map<String, ExperimentResult> PRAPResults;
	
	public PRAPResultsWrapper(Map<String, ExperimentResult> PRAPResults) {
		this.PRAPResults = PRAPResults;
	}
	
	public Map<String, ExperimentResult> getResults() {
		return this.PRAPResults;
	}
	
	public void writeResultsToDatabase(SQLiteDBHandler database) {
		
	}
	
	public static PRAPResultsWrapper readResultsFromDatabase(SQLiteDBHandler database) {
		return null;
	}
	
}

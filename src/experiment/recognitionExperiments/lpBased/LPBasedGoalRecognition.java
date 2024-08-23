package experiment.recognitionExperiments.lpBased;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import utils.DirectoryManager;
import utils.database.ExperimentResultDatabase;

public class LPBasedGoalRecognition {	
	private DirectoryManager dirManager;
	private ExecutorService executionService;
	
	public LPBasedGoalRecognition(String rootDirPath) {
		this.dirManager = DirectoryManager.getInstance(new File(rootDirPath));

		this.executionService = Executors.newSingleThreadExecutor();
	}
	
	public void runExperiment() {
		LPBasedGoalRecognitionExperiment lpExp;
		Map<String, LPBasedGoalRecognitionExperiment> experiments = new HashMap<String, LPBasedGoalRecognitionExperiment>();
		
		//Initialize experiment threads
		for(String expName : this.dirManager.getExperimentSetupDirectoryNames()) {
			lpExp = new LPBasedGoalRecognitionExperiment(expName, this.dirManager.getRootDirectory().getAbsolutePath());
		
			if(lpExp.checkObservationFile()) {
				experiments.put(expName, lpExp);
			}
		}
		
		//Create summary table in results database
		ExperimentResultDatabase db = this.dirManager.getPlanningResultsDBHandler();
		db.createSummaryTable();
		
		//Start experiment threads
		LPBasedGoalRecognitionExperiment exp;
		for(String key : experiments.keySet()) {
			exp = experiments.get(key);
				
			exp = this.executeExperiment(exp);
				
			db.writeExperimentResultToDatabase(exp.generateExperimentResult());
		}
		db.commit();
		this.executionService.shutdown();
	}

	private LPBasedGoalRecognitionExperiment executeExperiment(LPBasedGoalRecognitionExperiment exp) {
		List<LPBasedGoalRecognitionExperiment> exps = new ArrayList<LPBasedGoalRecognitionExperiment>();
		exps.add(exp);

		try {
			return this.executionService.invokeAny(exps);
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	public static void main(String[] args) {
		LPBasedGoalRecognition lpRecognition = new LPBasedGoalRecognition(args[0]);
		lpRecognition.runExperiment();
	}

}


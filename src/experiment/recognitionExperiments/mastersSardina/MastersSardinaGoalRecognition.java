package experiment.recognitionExperiments.mastersSardina;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import config.GoalRecognitionConfiguration;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.wrapper.PRAPResultsWrapper;
import experiment.management.EvaluationExperimentManager;
import utils.DirectoryManager;
import utils.JSONUtils;
import utils.database.ExperimentResultDatabase;

public class MastersSardinaGoalRecognition {	
	private DirectoryManager dirManager;
	private Map<String, ExperimentResult> PRAPResults;
	
	public MastersSardinaGoalRecognition(String rootDir) {
		this.dirManager = DirectoryManager.getInstance(new File(rootDir));
	}
	
	public Map<String, ExperimentResult> getPRAPResults() {
		return this.PRAPResults;
	}
	
	public void runExperiment() {
		//Map that stores the goal recognition results
		this.PRAPResults = new HashMap<String, ExperimentResult>();

		MastersSardinaExperiment prapExp = null;
		List<MastersSardinaExperiment> experiments = new ArrayList<MastersSardinaExperiment>();
		
		//Iterate through all experiment setups in a given goal recognition evaluation setup and create MastersSardinaExperiment instances
		for (String expName : this.dirManager.getExperimentSetupDirectoryNames()) {

			//If the system is configured to run in a docker container
			if (EvaluationExperimentManager.getInstance(null, null).runInDocker()) {
				try {
					prapExp = new MastersSardinaExperiment(expName, this.dirManager.getRootDirectory().getCanonicalPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				prapExp = new MastersSardinaExperiment(expName, this.dirManager.getRootDirectory().getAbsolutePath());
			}
			
			//If an observation file exists --> Create the temporary pddl problem files.
			if(prapExp.checkObservationFile()) {
				prapExp.generateProblemFiles();

				experiments.add(prapExp);
			}
		}

		//SQLite Database that stores the goal recognition results
		ExperimentResultDatabase db = this.dirManager.getPlanningResultsDBHandler();
		
		//Try to execute all experiments via the ExecutorService framework
		try {
			ExecutorService executorService = Executors.newFixedThreadPool(GoalRecognitionConfiguration.MAX_PLANNING_THREADS);
			List<Future<MastersSardinaExperiment>> results = executorService.invokeAll(experiments);

			db.createSummaryTable();
		
			ExperimentResult cResult;
			MastersSardinaExperiment exp;
			for(int i=0; i < results.size(); i++) {
				exp = results.get(i).get();
				cResult = exp.generateExperimentResult();
				this.PRAPResults.put(exp.getExperimentName(), cResult);
				db.writeExperimentResultToDatabase(cResult);
			}

			executorService.shutdown();
			db.commit();
		}catch(InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}

		JSONUtils.writeObjectToJSON(new PRAPResultsWrapper(this.PRAPResults), this.dirManager.getSerializedPlanningResultsFile());
	}
}

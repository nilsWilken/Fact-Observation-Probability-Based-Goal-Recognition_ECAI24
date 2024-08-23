package experiment.recognitionExperiments.factObservationBased;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;

import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.wrapper.PRAPResultsWrapper;
import symbolic.planning.planningUtils.PDDLInitialStateWrapper;
import utils.DirectoryManager;
import utils.JSONUtils;
import utils.database.ExperimentResultDatabase;

import com.hstairs.ppmajal.conditions.Predicate;

public class FactObservationBasedGoalRecognition {	
	private DirectoryManager dirManager;
	private Map<String, ExperimentResult> simResults;

	//Instantiate execution service that will asynchronously execute all previously created experiment instances
	private ExecutorService executionService = Executors.newSingleThreadExecutor();
	
	public FactObservationBasedGoalRecognition(String rootDir) {
		this.dirManager = DirectoryManager.getInstance(new File(rootDir));
	}

	
	public void runExperiment() {
		//Initialize map for experiment results
		this.simResults = new HashMap<String, ExperimentResult>();
		
		//Generate experiment instances
		FactObservationBasedGoalRecognitionExperiment cosExperiment;
		List<FactObservationBasedGoalRecognitionExperiment> experiments = new ArrayList<FactObservationBasedGoalRecognitionExperiment>();

		List<PDDLInitialStateWrapper> distinctInitialStates = this.computeDistinctInitialStates();
		
		//Generate experiment instance for every experiment setup directory in the general setup directory
		for(String expName : this.dirManager.getExperimentSetupDirectoryNames()) {
			//Create experiment instance for current setup directory
			cosExperiment = new FactObservationBasedGoalRecognitionExperiment(expName, this.dirManager.getRootDirectory().getAbsolutePath());
			cosExperiment.generateProblemFiles();

			if(cosExperiment.checkObservationFile()) {
				experiments.add(cosExperiment);
			}
		}

		
		
		Map<Integer, List<Map<Integer, Map<String, Double>>>> hypPredicatesMapsPerInitialState = new HashMap<Integer, List<Map<Integer, Map<String, Double>>>>();
		Map<Integer, Map<Integer, Set<Predicate>>> allPredicatesPerInitialState = new HashMap<Integer, Map<Integer, Set<Predicate>>>();
		
		//Instantiate handler for the specified result databse
		ExperimentResultDatabase db = this.dirManager.getPlanningResultsDBHandler();
		//Execute all experiments (this call is blocking)

		//Create summary table in database
		db.createSummaryTable();

		PDDLInitialStateWrapper initWrapper;
		List<Map<Integer, Map<String, Double>>> hypPredicatesMaps;
		Map<Integer, Set<Predicate>> allPredicates;
		for(FactObservationBasedGoalRecognitionExperiment exp : experiments) {
			initWrapper = new PDDLInitialStateWrapper(this.dirManager.getTemplateFile(exp.getExperimentName()));

			hypPredicatesMaps = hypPredicatesMapsPerInitialState.get(distinctInitialStates.indexOf(initWrapper));
			allPredicates = allPredicatesPerInitialState.get(distinctInitialStates.indexOf(initWrapper));

			if(hypPredicatesMaps != null) {
				exp.setHypPredicatesMaps(hypPredicatesMaps);
			}
			if(allPredicates != null) {
				exp.setAllPredicatesPerHyp(allPredicates);
			}

			cosExperiment = this.executeExperiment(exp);

			if(hypPredicatesMaps == null) {
				hypPredicatesMapsPerInitialState.put(distinctInitialStates.indexOf(initWrapper), cosExperiment.getHypPredicatesMaps());
			}
			if(allPredicates == null) {
				allPredicatesPerInitialState.put(distinctInitialStates.indexOf(initWrapper), cosExperiment.getAllPredicates());
			}

			this.simResults.put(cosExperiment.getExperimentName(), cosExperiment.generateExperimentResult());
			db.writeExperimentResultToDatabase(this.simResults.get(cosExperiment.getExperimentName()));
		}
		
		db.commit();

		
		//Write experiment results to a JSON file
		JSONUtils.writeObjectToJSON(new PRAPResultsWrapper(this.simResults), this.dirManager.getSerializedAveragePRAPPerformanceFile());

		executionService.shutdown();
	}

	private List<PDDLInitialStateWrapper> computeDistinctInitialStates() {
		List<PDDLInitialStateWrapper> initialStates = new ArrayList<PDDLInitialStateWrapper>();
		for(String expName : this.dirManager.getExperimentSetupDirectoryNames()) {
			PDDLInitialStateWrapper initWrapper = new PDDLInitialStateWrapper(this.dirManager.getTemplateFile(expName));
			
			if(!initialStates.contains(initWrapper)) {
				initialStates.add(initWrapper);
			}
		}		
		return initialStates;
	}

	private FactObservationBasedGoalRecognitionExperiment executeExperiment(FactObservationBasedGoalRecognitionExperiment exp) {
		List<FactObservationBasedGoalRecognitionExperiment> exps = new ArrayList<FactObservationBasedGoalRecognitionExperiment>();
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
    
}

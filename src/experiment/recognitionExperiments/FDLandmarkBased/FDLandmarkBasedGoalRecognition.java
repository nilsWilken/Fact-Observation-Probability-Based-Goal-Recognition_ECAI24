package experiment.recognitionExperiments.FDLandmarkBased;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.hstairs.ppmajal.conditions.ComplexCondition;
import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.problem.GroundAction;

import symbolic.planning.planningUtils.PDDLInitialStateWrapper;
import symbolic.planning.stateHandler.obsConversion.GMPDDLStateHandler;
import symbolic.relaxedPlanning.utils.FilterGroundActions;
import utils.DirectoryManager;
import utils.database.ExperimentResultDatabase;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

public class FDLandmarkBasedGoalRecognition {	
	private DirectoryManager dirManager;
	private ExecutorService executionService;
	
	public FDLandmarkBasedGoalRecognition(String rootDirPath) {
		this.dirManager = DirectoryManager.getInstance(new File(rootDirPath));

		this.executionService = Executors.newSingleThreadExecutor();
	}
	
	public void runExperiment() {
		this.runLiftedLandmarkBasedExperiments();
	}
	
	private void runLiftedLandmarkBasedExperiments() {		
		FDLandmarkBasedExperiment liftedLandmarkExp;
		Map<String, FDLandmarkBasedExperiment> experiments = new HashMap<String, FDLandmarkBasedExperiment>();
		
		//Initialize experiment threads
		for(String expName : this.dirManager.getExperimentSetupDirectoryNames()) {
			liftedLandmarkExp = new FDLandmarkBasedExperiment(expName, this.dirManager.getRootDirectory().getAbsolutePath());
		
			if(liftedLandmarkExp.checkObservationFile()) {
				experiments.put(expName, liftedLandmarkExp);
				liftedLandmarkExp.generateProblemFiles();
			}
		}
		
		//Create summary table in results database
		ExperimentResultDatabase db = this.dirManager.getPlanningResultsDBHandler();
		db.createSummaryTable();
		
		List<PDDLInitialStateWrapper> distinctInitialStates = this.computeDistinctInitialStates();
		Map<Integer, Map<ComplexCondition, Map<Integer, String[]>>> liftedLandmarksPerInitialState = new HashMap<Integer, Map<ComplexCondition, Map<Integer, String[]>>>();
		Map<Integer, Map<ComplexCondition, Map<Condition, List<Integer>>>> liftedLandmarkMappingsPerInitialState = new HashMap<Integer, Map<ComplexCondition, Map<Condition, List<Integer>>>>();
		Map<Integer, Map<ComplexCondition, Graph<String, DefaultEdge>>> landmarkGraphPerInitialState = new HashMap<Integer, Map<ComplexCondition, Graph<String, DefaultEdge>>>();

		//Start experiment threads
		FDLandmarkBasedExperiment exp;
		PDDLInitialStateWrapper initWrapper;
		Map<ComplexCondition, Map<Integer, String[]>> liftedLandmarksPerGoal;
		Map<ComplexCondition, Map<Condition, List<Integer>>> liftedLandmarkMappingsPerGoal;
		Map<ComplexCondition, Graph<String, DefaultEdge>> landmarkGraphPerGoal;
		for(String key : experiments.keySet()) {
			exp = experiments.get(key);
			experiments.put(key, null);
				
			initWrapper = new PDDLInitialStateWrapper(this.dirManager.getTemplateFile(exp.getExperimentName()));
			liftedLandmarksPerGoal = liftedLandmarksPerInitialState.get(distinctInitialStates.indexOf(initWrapper));
			liftedLandmarkMappingsPerGoal = liftedLandmarkMappingsPerInitialState.get(distinctInitialStates.indexOf(initWrapper));
			landmarkGraphPerGoal = landmarkGraphPerInitialState.get(distinctInitialStates.indexOf(initWrapper));
			if(liftedLandmarksPerGoal != null) {
				exp.setLiftedLandmarksPerGoal(liftedLandmarksPerGoal);
			}
			if(liftedLandmarkMappingsPerGoal != null) {
				exp.setLiftedLandmarkMappingsPerGoal(liftedLandmarkMappingsPerGoal);
			}
			if(landmarkGraphPerGoal != null) {
				exp.setLandmarkGraphPerGoal(landmarkGraphPerGoal);
			}
			
			exp = this.executeExperiment(exp);
				
			if(liftedLandmarksPerGoal == null) {
				liftedLandmarksPerInitialState.put(distinctInitialStates.indexOf(initWrapper), exp.getLiftedLandmarksPerGoal());
			}
			if(liftedLandmarkMappingsPerGoal == null) {
				liftedLandmarkMappingsPerInitialState.put(distinctInitialStates.indexOf(initWrapper), exp.getLiftedLandmarkMappingsPerGoal());
			}
			if(landmarkGraphPerGoal == null) {
				landmarkGraphPerInitialState.put(distinctInitialStates.indexOf(initWrapper), exp.getLandmarkGraphPerGoal());
			}
				
			db.writeExperimentResultToDatabase(exp.generateExperimentResult());
		}
		db.commit();
		this.executionService.shutdown();
	}
	
	private Map<String, Set<GroundAction>> initializeObservedActionsPerGoal(String expName, List<String> trainingSetNames) {
		Map<String, Set<GroundAction>> observedActionsPerGoal = new HashMap<String, Set<GroundAction>>();
		GMPDDLStateHandler handler = new GMPDDLStateHandler(this.dirManager.getDomainFile().getAbsolutePath(), this.dirManager.getInitialProblemFile(expName, 0).getAbsolutePath());
		
		Set<GroundAction> observedActions;
		//For each defined goal
		for(String goal : this.dirManager.getPlanningHyps()) {
			observedActions = new HashSet<GroundAction>();
			//For each training example given when this experiment was initialized
			for(String trainingExp : trainingSetNames) {
				if(trainingExp.contains(this.dirManager.getTextGoalFromPlanningGoal(goal))) {
					observedActions.addAll(handler.retrieveActionListFromObsFile(this.dirManager.getObsFile(trainingExp).getAbsolutePath()));
				}
			}
			observedActionsPerGoal.put(goal, observedActions);
		}
		return observedActionsPerGoal;
	}
	
	private Map<String, Collection<GroundAction>> filterActions(Map<String, Set<GroundAction>> observedActionsPerGoal, String expName) {
		Map<String, Collection<GroundAction>> filteredActionMap = new HashMap<String, Collection<GroundAction>>();
		
		List<String> planningHyps = this.dirManager.getPlanningHyps();
		for(int hypCounter = 0; hypCounter < planningHyps.size(); hypCounter++) {
			
			Collection<GroundAction> filteredActions = FilterGroundActions.filterTouchedObjects(this.dirManager.getDomainFile().getAbsolutePath(), this.dirManager.getInitialProblemFile(expName, hypCounter).getAbsolutePath(), observedActionsPerGoal.get(planningHyps.get(hypCounter)), this.dirManager.getTextGoalFromPlanningGoal(planningHyps.get(hypCounter)));
			filteredActionMap.put(this.dirManager.getTextGoalFromPlanningGoal(planningHyps.get(hypCounter)), filteredActions);
		}
		
		return filteredActionMap;
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

	private FDLandmarkBasedExperiment executeExperiment(FDLandmarkBasedExperiment exp) {
		List<FDLandmarkBasedExperiment> exps = new ArrayList<FDLandmarkBasedExperiment>();
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

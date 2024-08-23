package experiment.recognitionExperiments.mastersSardina;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import config.GoalRecognitionConfiguration;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.ResultObsStep;
import experiment.recognitionExperiments.abstractExperiment.AbstractEvaluationPRAPGoalRecognitionExperiment;
import experiment.recognitionExperiments.abstractExperiment.AbstractPRAPGoalRecognitionExperiment;
import symbolic.planning.stateHandler.obsConversion.GMPDDLStateHandler;

public class MastersSardinaExperiment extends AbstractEvaluationPRAPGoalRecognitionExperiment implements Callable<MastersSardinaExperiment> {
	
	// Stores inital plan costs key= hypothesis counter; value = plan costs
	private Map<Integer, Integer> initialPlanCosts;

	// Stores plan costs first key = obs counter; second key = hypothesis counter;
	// value = plan costs
	private Map<Integer, Map<Integer, Integer>> planCosts;

	private Map<Integer, Map<Integer, Double>> goalProbabilities;
	
	private Map<Integer, Long> initialComputationTimes;
	private Map<Integer, Map<Integer, Long>> computationTimes;
	
	private boolean runOnlyOnLastObservation = false;

	
	public MastersSardinaExperiment(String name, String rootDir) {
		super(name, rootDir);
		this.initialPlanCosts = new HashMap<Integer, Integer>();
		this.planCosts = new HashMap<Integer, Map<Integer, Integer>>();
		this.goalProbabilities = new HashMap<Integer, Map<Integer, Double>>();
		
		this.initialComputationTimes = new HashMap<Integer, Long>();
		this.computationTimes = new HashMap<Integer, Map<Integer, Long>>();
	}
	
	@Override
	public MastersSardinaExperiment call() {
		this.runExperiment();
		return this;
	}
	
	public void setRunOnlyOnLastObservation(boolean bla) {
		this.runOnlyOnLastObservation = bla;
	}
	
	private void runExperiment() {
		//GENERATE PROBLEM FILES
		if(runOnlyOnLastObservation) {
			this.generateProblemFiles();
		}
		
		//CHECK WHETHER PROBLEMS WERE GENERATED
		if(this.dirManager.getNumberOfProblemDirs(this.name) > 0) {
			this.numberObs = this.dirManager.getProblemDir(this.name, 0).list().length - 1;
		}
				
		//ACTUALLY SOLVE PLANNING PROBLEMS
		Map<Integer, Integer> cPlanCosts;
		Map<Integer, Long> cComputationTimes;
		for(int hypCounter=0; hypCounter < this.dirManager.getNumberOfProblemDirs(this.name); hypCounter++) {
			
			//Solve the unmodified planning problem from the given initial state to one of the goals
			this.runPlannerForInitialProblem(hypCounter);
			this.initialComputationTimes.put(hypCounter, this.planner.getRuntime());
			this.initialPlanCosts.put(hypCounter, this.planner.getPlanLength());
									
			int obsCounter = 1;
			if(this.runOnlyOnLastObservation) {
				obsCounter = this.numberObs;
			}
			
			//Iterate through all observations and solve the correpsonding planning problems
			for(; obsCounter <= this.numberObs; obsCounter++) {
				cPlanCosts = this.planCosts.get(obsCounter);
				if(cPlanCosts == null) {
					cPlanCosts = new HashMap<Integer, Integer>();
					this.planCosts.put(obsCounter, cPlanCosts);
				}
				
				cComputationTimes = this.computationTimes.get(obsCounter);
				if(cComputationTimes == null) {
					cComputationTimes = new HashMap<Integer, Long>();
					this.computationTimes.put(obsCounter, cComputationTimes);
				}
				
				this.runPlannerForObsProblem(obsCounter, hypCounter);
				cComputationTimes.put(hypCounter, this.planner.getRuntime());
				if(this.planner.getPlanLength() > -1)  {
					cPlanCosts.put(hypCounter, (this.planner.getPlanLength() + obsCounter));
				} else {
					cPlanCosts.put(hypCounter, Integer.MAX_VALUE);
				}
			}
		}	
	}
	
	@Override
	//Generates the temporary pddl problem files required by the approach
	public void generateProblemFiles() {
		System.out.println("Create problem files for " + this.name);
		this.hypMap = new HashMap<Integer, String>();

		GMPDDLStateHandler stateHandler;
		File outputDir;
		String initialProblemFilePath = "";
		
		List<String> outputFilePaths = new ArrayList<String>();
		List<String> outputFileNames = new ArrayList<String>();
		List<String> initialProblemFiles = new ArrayList<String>();
		boolean generateProblems = false;

		int hypCounter = 0;
		
		for (String line : this.dirManager.getPlanningHyps()) {
			outputDir = this.dirManager.generateProblemDir(this.name, hypCounter);
			
			outputFilePaths.add(outputDir.getAbsolutePath());
			outputFileNames.add(this.name);

			if(outputDir.listFiles().length == 0) {
				generateProblems = true;
				initialProblemFilePath = this.problemTemplate.generateInitialProblemFile(line, outputDir.getAbsolutePath(),
						this.name, hypCounter);
				initialProblemFiles.add(initialProblemFilePath);
			}

			this.dirManager.addProblemDir(this.name, hypCounter, outputDir);
			this.hypMap.put(hypCounter, line);
			hypCounter++;
		}
		
		if(generateProblems) {
			stateHandler = new GMPDDLStateHandler(this.dirManager.getDomainFile().getAbsolutePath(),
					initialProblemFilePath);
			stateHandler.processObsFile(outputFileNames, hypCounter, initialProblemFiles, outputFilePaths,
					this.dirManager.getObsFile(this.name).getAbsolutePath());
		}
		
		this.numberObs = hypCounter;
	}
	
	@Override
	//Creates ExperimentResult object for the results of this experiment
	public ExperimentResult generateExperimentResult() {
		this.calculateGoalProbabilities();
		
		ExperimentResult expResult = new ExperimentResult(this.name);
		
		//Add goal probabilities to result
		ResultObsStep cStep;
		for(int obs : this.goalProbabilities.keySet()) {
			cStep = new ResultObsStep();
			for(int hyp : this.hypMap.keySet()) {
				cStep.addGoalProbability(this.dirManager.getTextGoalFromPlanningGoal(this.hypMap.get(hyp)), this.goalProbabilities.get(obs).get(hyp));
			}
			expResult.addResultStep(obs, cStep);
		}

		expResult.setTrueHyp(this.dirManager.getTrueTextGoalForExperiment(this.name));
		
		//Add computation times to result
		for(int hyp : this.initialComputationTimes.keySet()) {
			expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyInitial(hyp), this.initialComputationTimes.get(hyp));
			for(int obs : this.computationTimes.keySet()) {
				expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyGMMS(hyp, obs), this.computationTimes.get(obs).get(hyp));
			}
		}
		
		//Add plan costs to result
		for(int hyp : this.initialPlanCosts.keySet()) {
			expResult.addPlanLength(AbstractPRAPGoalRecognitionExperiment.generateGMMSInitialPlanLengthKey(hyp), this.initialPlanCosts.get(hyp));
			for(int obs : this.planCosts.keySet()) {
				expResult.addPlanLength(AbstractPRAPGoalRecognitionExperiment.generateGMMSPlanLengthKey(hyp, obs), this.planCosts.get(obs).get(hyp));
			}
		}
		return expResult;
	}
	
	@Override
	public void generateGoalProbabilityReports() {
		this.calculateGoalProbabilities();
		try {
			BufferedWriter out;
			File outFile;
			for (int obs : this.goalProbabilities.keySet()) {
				outFile = Paths.get(this.dirManager.getPlanningObsResultsDir(obs, this.name).getAbsolutePath(),
				GoalRecognitionConfiguration.generateProbabilityReportFileName(obs)).toFile();
				out = new BufferedWriter(new FileWriter(outFile));
				out.write(this.generateGoalProbabilityReport(obs));
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String generateGoalProbabilityReport(int obsCounter) {
		StringBuffer probReport = new StringBuffer();

		for (int hyp : this.initialPlanCosts.keySet()) {
			probReport.append("Hypothesis: " + this.hypMap.get(hyp) + "\n");

			probReport.append("\tPlan Cost Not O: " + this.initialPlanCosts.get(hyp) + "\n");
			probReport.append("\tPlan Cost O: " + this.planCosts.get(obsCounter).get(hyp) + "\n");

			probReport.append("\tGoal Probability: " + this.goalProbabilities.get(obsCounter).get(hyp));
			probReport.append("\n\n");
		}

		return probReport.toString();
	}

	private void calculateGoalProbabilities() {
		this.goalProbabilities = new HashMap<Integer, Map<Integer, Double>>();

		HashMap<Integer, Double> cRatios;

		for (int obs : this.planCosts.keySet()) {
			cRatios = new HashMap<Integer, Double>();
			for (int hyp : this.planCosts.get(obs).keySet()) {
				cRatios.put(hyp, GoalRecognitionConfiguration.calculateLikelihoodRatio(this.planCosts.get(obs).get(hyp),
						this.initialPlanCosts.get(hyp)));
			}
			this.goalProbabilities.put(obs, GoalRecognitionConfiguration.calculateProbabilitiesFromLikelihoods(cRatios));
		}
	}
	
	public int getNumberOfObs() {
		return this.numberObs;
	}

}

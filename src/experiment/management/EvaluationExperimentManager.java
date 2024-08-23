package experiment.management;

import java.io.File;
import java.io.IOException;

import config.ConfigurationManager;
import config.GoalRecognitionConfiguration;
import experiment.recognitionExperiments.FDLandmarkBased.FDLandmarkBasedGoalRecognition;
import experiment.recognitionExperiments.factObservationBased.FactObservationBasedGoalRecognition;
import experiment.recognitionExperiments.lpBased.LPBasedGoalRecognition;
import experiment.recognitionExperiments.mastersSardina.MastersSardinaGoalRecognition;
import experiment.recognitionExperiments.relaxedPlan.RelaxedPlanBasedGoalRecognition;
import utils.DirectoryManager;

public class EvaluationExperimentManager {

	private DirectoryManager dirManager;
	private static EvaluationExperimentManager instance;
	private boolean runInDocker = true;
	private String rootPath;

	public static EvaluationExperimentManager getInstance(String rootDir, String configurationFile) {
		if (instance == null) {
			EvaluationExperimentManager.instance = new EvaluationExperimentManager(rootDir, configurationFile);
		}
		return EvaluationExperimentManager.instance;
	}

	private EvaluationExperimentManager(String rootDir, String configurationFile) {
		try {
			ConfigurationManager.initializeGoalRecognitionConfig(new File(configurationFile));
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.dirManager = DirectoryManager.getInstance(new File(rootDir));

		if (runInDocker) {
			try {
				this.rootPath = this.dirManager.getRootDirectory().getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			this.rootPath = this.dirManager.getRootDirectory().getAbsolutePath();
		}

	}

	public void runExperiment() {
		ExperimentType expType = ExperimentType.parseFromString(GoalRecognitionConfiguration.EXPERIMENT_TYPE);

		switch (expType) {
		case MASTERS_SARDINA:
			this.runMastersSardinaExperiment();
			break;
		case LIFTED_LANDMARK_BASED:
			this.runLiftedLandmarkBasedExperiment();
			break;
		case RELAXED_PLAN_RAMIREZ_GEFFNER:
			this.runRelaxedPlanRamirezGeffnerExperiment();
			break;
		case LP_BASED:
			this.runLPBasedExperiment();
			break;
		case FACT_OBSERVATION_PROBABILITY:
			this.runFactObservationProbabilityBasedExperiment();
			break;
		default:
			break;
		}
	}

	private void runMastersSardinaExperiment() {
		MastersSardinaGoalRecognition goalMirroringRecognition = new MastersSardinaGoalRecognition(this.rootPath);
		goalMirroringRecognition.runExperiment();
	}

	private void runLiftedLandmarkBasedExperiment() {
		FDLandmarkBasedGoalRecognition liftedLandmarkGoalRecognition = new FDLandmarkBasedGoalRecognition(this.rootPath);
		liftedLandmarkGoalRecognition.runExperiment();
	}
	
	public boolean runInDocker() {
		return this.runInDocker;
	}

	private void runRelaxedPlanRamirezGeffnerExperiment() {
		System.out.println("RUN RELAXED PLAN RAMIREZ GEFFNER EXPERIMENT");
		RelaxedPlanBasedGoalRecognition relPlanGoalRecognition = new RelaxedPlanBasedGoalRecognition(this.dirManager.getRootDirectory().getAbsolutePath());
		relPlanGoalRecognition.runExperiment();
	}

	private void runLPBasedExperiment() {
		LPBasedGoalRecognition lpBasedRecognition = new LPBasedGoalRecognition(this.dirManager.getRootDirectory().getAbsolutePath());
		lpBasedRecognition.runExperiment();
	}

	private void runFactObservationProbabilityBasedExperiment() {
		FactObservationBasedGoalRecognition fpvRecognition = new FactObservationBasedGoalRecognition(this.dirManager.getRootDirectory().getAbsolutePath());
		fpvRecognition.runExperiment();
	}
	
	public static void main(String[] args) {
		EvaluationExperimentManager manager = EvaluationExperimentManager.getInstance(args[0], args[1]);
		manager.runExperiment();
	}

}

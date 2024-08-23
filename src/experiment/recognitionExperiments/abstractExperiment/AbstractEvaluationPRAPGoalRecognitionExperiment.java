package experiment.recognitionExperiments.abstractExperiment;

import experiment.evaluation.utils.ExperimentResult;
import utils.database.ExperimentResultDatabase;

public abstract class AbstractEvaluationPRAPGoalRecognitionExperiment extends AbstractPRAPGoalRecognitionExperiment {

	public AbstractEvaluationPRAPGoalRecognitionExperiment(String name, String rootDir) {
		super(name, rootDir);
	}
	
	public abstract void generateProblemFiles();
	public abstract void generateGoalProbabilityReports();
	public abstract ExperimentResult generateExperimentResult();
	
	public void writeExperimentResultToDatabase(String summaryName, ExperimentResultDatabase database) {
		database.writeExperimentResultToDatabase(this.generateExperimentResult());
	}

}

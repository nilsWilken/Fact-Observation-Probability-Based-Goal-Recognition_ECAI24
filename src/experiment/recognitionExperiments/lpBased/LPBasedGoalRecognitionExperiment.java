package experiment.recognitionExperiments.lpBased;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.ResultObsStep;
import experiment.recognitionExperiments.abstractExperiment.AbstractEvaluationPRAPGoalRecognitionExperiment;
import experiment.recognitionExperiments.abstractExperiment.AbstractPRAPGoalRecognitionExperiment;
import utils.DirectoryManager;
import utils.externalProcessExecution.externalExecutionWrapper.LPTestInstance;


public class LPBasedGoalRecognitionExperiment extends AbstractEvaluationPRAPGoalRecognitionExperiment implements Callable<LPBasedGoalRecognitionExperiment> {

	private Map<Integer, Map<Integer, Double>> goalScores;
	private Map<Integer, Long> computationTimes;
	private String archiveFilePath;
	private List<String> observationsFileList;
	private String observationsFile;
	private String domainFile;
	private String hypsFile;
	private String realHypFile;
	private String templateFile;
	private List<String> hypotheses;

	
	
	public LPBasedGoalRecognitionExperiment(String name, String rootDir) {
		super(name, rootDir);
		
		this.goalScores = new HashMap<Integer, Map<Integer, Double>>();
		this.computationTimes = new HashMap<Integer, Long>();

		//This implementation creates a new .bz2 archive for each experiment and saves it temporarily under this path
		this.archiveFilePath = Paths.get(this.dirManager.getArchiveDir().getAbsolutePath(), this.generateArchiveName()).toString();

		//Parse all relevant experiment files
		this.parseHypsFile();
		this.parseDomainFile();
		this.parseObservationsFile();
		this.parseRealHypFile();
		this.parseTemplateFile();
	}

	@Override
	public LPBasedGoalRecognitionExperiment call() {
		this.runExperiment();
		return this;
	}
	
	private void runExperiment() {
		//Generate temporary pddl problem files from experiment setups
		this.generateProblemFiles();

		//Perform online goal recognition
		StringBuffer observationString = new StringBuffer();
		for(int i=0; i < this.observationsFileList.size(); i++) {
			observationString.append(this.observationsFileList.get(i));

			this.observationsFile = observationString.toString();
			this.generateBZ2Archive();

			observationString.append("\n");

			//Uses external original implementation to generate the resutls (has to be installed properly to be used)
			LPTestInstance execution = new LPTestInstance();
			execution.runLPTestInstance(new File(this.archiveFilePath), "delta-f2", new File("tmp.log2"));

			this.goalScores.put((i+1), execution.getScores());
			this.computationTimes.put((i+1), execution.getRuntime());
		}
	}
	
	@Override
	public void generateProblemFiles() {
		this.hypMap = new HashMap<Integer, String>();
		
		int hypCounter = 0;
		
		for(String hyp : this.dirManager.getPlanningHyps()) {
			this.hypMap.put(hypCounter, hyp);
			hypCounter++;
		}
	}

	//Has to convert hyps file so that hyps are separated by commata. Rest should work as it is.
	private void generateBZ2Archive() {
		List<String> fileNames = new ArrayList<String>();
		fileNames.add("domain.pddl");
		fileNames.add("hyps.dat");
		fileNames.add("obs.dat");
		fileNames.add("real_hyp.dat");
		fileNames.add("template.pddl");

		List<String> fileContents = new ArrayList<String>();
		fileContents.add(this.domainFile);
		fileContents.add(this.hypsFile);
		fileContents.add(this.observationsFile);
		fileContents.add(this.realHypFile);
		fileContents.add(this.templateFile);

		DirectoryManager.writeStringsToTarBZ2(archiveFilePath, fileNames, fileContents);
	}

	private void parseHypsFile() {
		//Convert planning hypotheses to format that is used by original code
		this.hypotheses = new ArrayList<String>();
		for(String hyp : this.dirManager.getPlanningHyps()) {
			hypotheses.add(this.convertHyp(hyp));
		}

		this.hypsFile = this.aggregateHyps(hypotheses);
	}

	private void parseDomainFile() {
		this.domainFile = DirectoryManager.aggregateStringList(DirectoryManager.readFileAsStrings(this.dirManager.getDomainFile()));
	}

	private void parseObservationsFile() {
		this.observationsFileList = DirectoryManager.readFileAsStrings(this.dirManager.getObsFile(this.name));
	}

	private void parseTemplateFile() {
		this.templateFile = DirectoryManager.aggregateStringList(DirectoryManager.readFileAsStrings(this.dirManager.getTemplateFile(this.name)));
	}

	private void parseRealHypFile() {
		this.realHypFile = this.convertHyp(this.dirManager.getTrueGoalForExperiment(this.name));
	}
	

	@Override
	public void generateGoalProbabilityReports() {
			
	}

	@Override
	public ExperimentResult generateExperimentResult() {
		ExperimentResult expResult = new ExperimentResult(this.name);
		expResult.setTrueHyp(this.dirManager.getTrueTextGoalForExperiment(this.name));
		
		ResultObsStep cStep;
		for(int obs : this.goalScores.keySet()) {
			cStep = new ResultObsStep();
			for(int hyp : this.hypMap.keySet()) {
				if(this.goalScores.get(obs).get(hyp) == null) {
					this.goalScores.get(obs).put(hyp, 0.0);
				}
				cStep.addGoalProbability(this.dirManager.getTextGoalFromPlanningGoal(this.hypMap.get(hyp)), this.goalScores.get(obs).get(hyp));
			}
			expResult.addResultStep(obs, cStep);
		}

		for(int obs : this.computationTimes.keySet()) {
			for(int hyp : this.hypMap.keySet()) {
				expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyLPBased(hyp, obs), (this.computationTimes.get(obs)/this.hypMap.keySet().size()));
			}
		}
		return expResult;
	}

	private String convertHyp(String hyp) {
		return hyp.replace(") (", "), (");
	}

	private String aggregateHyps(List<String> hyps) {
		StringBuffer buff = new StringBuffer();
		for(int i=0; i < hyps.size(); i++) {
			buff.append(hyps.get(i));
			if(i+1 < hyps.size()) {
				buff.append("\n");
			}
		}

		return buff.toString();
	}

	private String generateArchiveName() {
		return this.name + ".tar.bz2";
	}
}

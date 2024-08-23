package experiment.recognitionExperiments.relaxedPlan;

import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.utils.ResultObsStep;
import experiment.recognitionExperiments.abstractExperiment.AbstractEvaluationPRAPGoalRecognitionExperiment;
import experiment.recognitionExperiments.abstractExperiment.AbstractPRAPGoalRecognitionExperiment;
import symbolic.relaxedPlanning.utils.RPG;
import utils.ObsUtils;
import utils.externalProcessExecution.externalExecutionWrapper.PR2Plan;
import utils.externalProcessExecution.externalExecutionWrapper.SuboptimalPR;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.Callable;

public class RelaxedPlanBasedExperiment extends AbstractEvaluationPRAPGoalRecognitionExperiment implements Callable<RelaxedPlanBasedExperiment> {

    private Map<Integer, Map<Integer, Integer>> goalScores;

    //First key is observation count, second key is hypothesis id.
    private Map<Integer, Map<Integer, Long>> relaxedPlanComputationTimes;

    //First key is hypothesis id, second key is observation count.
    private Map<Integer, Map<Integer, Long>> translationComputationTimes;
    private long totalComputationTime;
    private int obsCount;

    public RelaxedPlanBasedExperiment(String name, String rootDir) {
        super(name, rootDir);

        this.translationComputationTimes = new HashMap<Integer, Map<Integer, Long>>();
        this.relaxedPlanComputationTimes = new HashMap<Integer, Map<Integer, Long>>();
        this.goalScores = new HashMap<Integer, Map<Integer, Integer>>();

    }

    @Override
    public RelaxedPlanBasedExperiment call() {
        this.runExperiment();
        return this;
    }

    private void runExperiment() {
        long totalStartTime = System.currentTimeMillis();

        this.generateProblemAndDomainFiles();

        SuboptimalPR subPR;
        File dFile;
        File pFile;
        File logFile = Paths.get(System.getProperty("user.dir"), "subPR.log").toFile();
        File solnTargetFile;
        Map<Integer, Integer> cScores;
        Map<Integer, Long> compTimes;
        for(int obsCounter = 1; obsCounter <= this.obsCount; obsCounter++) {
            cScores = new HashMap<Integer, Integer>();
            compTimes = new HashMap<Integer, Long>();
            for(int hypCounter = 0; hypCounter < this.hypMap.keySet().size(); hypCounter++) {
                dFile = this.dirManager.getRG09DomainFileForObs(this.name, hypCounter, obsCounter);
                pFile = this.dirManager.getRG09ProblemFileForObs(this.name, hypCounter, obsCounter);
                solnTargetFile = this.dirManager.getRG09RelaxedPlanFile(this.name, hypCounter, obsCounter);

                subPR = new SuboptimalPR();
                subPR.runSubPR(dFile, pFile, logFile, solnTargetFile);
                cScores.put(hypCounter, RelaxedPlanBasedExperiment.readRelaxedPlanOverlapFromFile(solnTargetFile.getAbsolutePath()));
                compTimes.put(hypCounter, subPR.getRuntime());
            }
            this.goalScores.put(obsCounter, cScores);
            this.relaxedPlanComputationTimes.put(obsCounter, compTimes);
        }

        this.totalComputationTime = System.currentTimeMillis() - totalStartTime;
    }

    @Override
    public void generateProblemFiles() {
        this.hypMap = new HashMap<Integer, String>();

        File outputDir;
        String initialProblemFilePath = "";

        List<String> initialProblemFiles = new ArrayList<String>();

        int hypCounter = 0;

        for (String hyp : this.dirManager.getPlanningHyps()) {
            outputDir = this.dirManager.generateProblemDir(this.name, hypCounter);

            if (outputDir.listFiles().length == 0) {
                initialProblemFilePath = this.problemTemplate.generateInitialProblemFile(hyp, outputDir.getAbsolutePath(), this.name, hypCounter);
                initialProblemFiles.add(initialProblemFilePath);
            }

            this.dirManager.addProblemDir(this.name, hypCounter, outputDir);
            this.hypMap.put(hypCounter, hyp);
            hypCounter++;
        }
    }

    public void generateProblemAndDomainFiles() {
		this.hypMap = new HashMap<Integer, String>();

		List<String> parsedObservations = new ArrayList<String>();
        List<String> rawObservations = new ArrayList<String>();
		try {
			rawObservations = Files.readAllLines(Paths.get(this.dirManager.getObsFile(this.name).getAbsolutePath()));
            this.obsCount = rawObservations.size();

			for (String rawObs : rawObservations) {
				parsedObservations.add(ObsUtils.parseObservationFileLine(rawObs));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

        //Generate domain directorys
        File domainDir = this.dirManager.generateDomainDir(this.name);
		this.dirManager.addDomainDir(this.name, domainDir);

        File domain = this.dirManager.getGroundedDomainFile();

        //1. Create different obs files for each possible length of the observation sequence
        StringBuffer fileContent = new StringBuffer();
		for (int i = 0; i < rawObservations.size(); i++) {
			fileContent.append(rawObservations.get(i) + "\n");
            try {
                Files.writeString(Paths.get(this.dirManager.getRG09ObsFile(this.name, i+1).getAbsolutePath()), fileContent.toString(), StandardOpenOption.CREATE);
            } catch (IOException e) {
                e.printStackTrace();
            }			
		}	
		

		int hypCounter = 0;
		File problemDir;
        PR2Plan pr2plan;
        Map<Integer, Long> computationTimes;
		for (String line : this.dirManager.getPlanningHyps()) {
			//Create problem directory for each possible hypothesis
            problemDir = this.dirManager.generateProblemDir(this.name, hypCounter);
			this.dirManager.addProblemDir(this.name, hypCounter, problemDir);

            //Check whether problem files were already generated before
			// if (problemDir.listFiles().length == 0) {

                //Create initial problem file (without observations compiled to it)
				this.problemTemplate.generateInitialProblemFile(line, problemDir.getAbsolutePath(), this.name, hypCounter);

                //Call pr2plan for each possible combination of domain file, initial problem file, and observation file (i)
                File dFile;
                File pFile;
                File obsFile;
                File logFile = Paths.get(System.getProperty("user.dir"), "pr2plan.log").toFile();
                File dTargetFile;
                File pTargetFile;
                computationTimes = new HashMap<Integer, Long>();
				for (int i = 1; i <= rawObservations.size(); i++) {
                    pr2plan = new PR2Plan();
                    dFile = domain;
                    pFile = this.dirManager.getInitialProblemFile(this.name, hypCounter);
                    obsFile = this.dirManager.getRG09ObsFile(this.name, i);
                    dTargetFile = this.dirManager.getRG09DomainFileForObs(this.name, hypCounter, i);
                    pTargetFile = this.dirManager.getRG09ProblemFileForObs(this.name, hypCounter, i);
                    pr2plan.runPPR2Plan(dFile, pFile, obsFile, logFile, dTargetFile, pTargetFile);
                    computationTimes.put(i, pr2plan.getRuntime());
				}
			// }
            
            this.translationComputationTimes.put(hypCounter, computationTimes);
			this.hypMap.put(hypCounter, line);
			hypCounter++;
		}
		this.numberObs = parsedObservations.size();
	}

    @Override
    public void generateGoalProbabilityReports() {
        // TODO Auto-generated method stub

    }

    @Override
    public ExperimentResult generateExperimentResult() {

        ExperimentResult expResult = new ExperimentResult(this.name);
        expResult.setTrueHyp(this.dirManager.getTrueTextGoalForExperiment(this.name));

        ResultObsStep cStep;
        for (int obs : this.goalScores.keySet()) {
            cStep = new ResultObsStep();
            for (int hyp : this.hypMap.keySet()) {
                cStep.addGoalProbability(this.dirManager.getTextGoalFromPlanningGoal(this.hypMap.get(hyp)), this.goalScores.get(obs).get(hyp));
            }
            expResult.addResultStep(obs, cStep);

            for (int hyp : this.hypMap.keySet()) {
                if (this.relaxedPlanComputationTimes.get(obs) != null) {
                    expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyRelaxedPlanComputation(hyp, obs), this.relaxedPlanComputationTimes.get(obs).get(hyp));
                }
                expResult.addComputationTimeInMillis(AbstractPRAPGoalRecognitionExperiment.generateComputationTimesKeyRG09Translation(hyp, obs), this.translationComputationTimes.get(hyp).get(obs));
            }
        }

        expResult.setTotalComputationTimeInMillis(this.totalComputationTime);
        return expResult;
    }

    private ArrayList<GroundAction> computeRelaxedPlan(String domainFile, String problemFile) throws Exception {

        PddlDomain domain = new PddlDomain(domainFile);
        EPddlProblem problem = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);

        domain.substituteEqualityConditions();

        problem.transformGoal();
        problem.groundingActionProcessesConstraints();

        problem.simplifyAndSetupInit(false, false);

        RPG rpg = new RPG((PDDLState) problem.getInit());

        rpg.computeRelaxedPlanningGraph(((PDDLState) problem.getInit()), problem.getGoals(), (Set) problem.getActions());


        List<Predicate> relevantPredicates = new ArrayList<>();
        for (Object pred : problem.getActualFluents().values()) {
            if (pred instanceof Predicate) {
                relevantPredicates.add((Predicate) pred);
            }
        }

        Map<Integer, ArrayList<GroundAction>> relaxedPlanLevels = rpg.findAllRelevantActions(((PDDLState) problem.getInit()).relaxState(), relevantPredicates);

        ArrayList<GroundAction> relaxedPlan = new ArrayList<>();

        for (ArrayList<GroundAction> actions : relaxedPlanLevels.values()) {
            relaxedPlan.addAll(actions);
        }

        return relaxedPlan;

    }

    public static int readRelaxedPlanOverlapFromFile(String filePath) {

        int overlap = 0;

        try {
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.matches(".*Obs]")) {
                    overlap++;
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return overlap;
    }

}



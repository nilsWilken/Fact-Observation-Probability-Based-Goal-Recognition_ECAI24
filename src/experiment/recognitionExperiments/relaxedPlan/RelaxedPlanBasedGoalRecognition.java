package experiment.recognitionExperiments.relaxedPlan;
import config.Configuration;
import config.ConfigurationManager;
import experiment.evaluation.utils.ExperimentResult;
import experiment.evaluation.wrapper.PRAPResultsWrapper;
import utils.DirectoryManager;
import utils.JSONUtils;
import utils.database.ExperimentResultDatabase;
import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RelaxedPlanBasedGoalRecognition {
    private DirectoryManager dirManager;
    private ExecutorService executionService;
    private Map<String, ExperimentResult> simResults;

    public RelaxedPlanBasedGoalRecognition(String rootDirPath) {
        this.dirManager = DirectoryManager.getInstance(new File(rootDirPath));
        this.executionService = Executors.newSingleThreadExecutor();
        this.simResults = new HashMap<String, ExperimentResult>();
    }

    public void runExperiment() {

        RelaxedPlanBasedExperiment rpExp;
        Map<String, RelaxedPlanBasedExperiment> experiments = new HashMap<>();

        //Initialize experiment threads
        for (String expName : this.dirManager.getExperimentSetupDirectoryNames()) {
            rpExp = new RelaxedPlanBasedExperiment(expName, this.dirManager.getRootDirectory().getAbsolutePath());

            if(rpExp.checkObservationFile()) {
                experiments.put(expName, rpExp);
            }
            //TODO: necessary? already generating problem files in experiment class
            //landmarkExp.generateProblemFiles();
        }

        //Create summary table in results database
        ExperimentResultDatabase db = this.dirManager.getPlanningResultsDBHandler();
        db.createSummaryTable();

        //Start experiment threads
        ExperimentResult cResult;
        for (RelaxedPlanBasedExperiment exp : experiments.values()) {
            exp = this.executeExperiment(exp);
            cResult = exp.generateExperimentResult();
            this.simResults.put(exp.getExperimentName(), cResult);
            db.writeExperimentResultToDatabase(cResult);
        }

        JSONUtils.writeObjectToJSON(new PRAPResultsWrapper(this.simResults), this.dirManager.getSerializedAveragePRAPPerformanceFile());
        
        db.commit();
        this.executionService.shutdown();
    }

    private RelaxedPlanBasedExperiment executeExperiment(RelaxedPlanBasedExperiment exp) {
        List<RelaxedPlanBasedExperiment> exps = new ArrayList<>();
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



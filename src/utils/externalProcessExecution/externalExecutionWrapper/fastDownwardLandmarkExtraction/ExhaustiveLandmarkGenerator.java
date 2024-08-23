package utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import config.GoalRecognitionConfiguration;
import utils.externalProcessExecution.ExternalProcessExecutionHandler;
import utils.externalProcessExecution.ProcessExecutionHandler;

public class ExhaustiveLandmarkGenerator implements ILandmarkGenerator {
    private long runTime;
    private Set<String[]> landmarks;

    public void runLandmarkGenerator(File dFile, File pFile, File logFile) {
        try {
            ProcessExecutionHandler execution = ExternalProcessExecutionHandler.startProcessExecution(this.generateExecutionCommand(dFile, pFile), logFile, false);

            execution.join();

            this.landmarks = FastDownwardLMExtractionUtils.generateLandmarkSet(execution.getLog());
        }catch(InterruptedException e) {
            e.printStackTrace();
        }

    }

    public List<String> generateExecutionCommand(File dFile, File pFile) {
        List<String> command = new ArrayList<String>();

        GoalRecognitionConfiguration.USE_BASH_C = false;
        GoalRecognitionConfiguration.USE_EXTERNAL_EXECUTION_SCRIPT = false;

        command.add(GoalRecognitionConfiguration.MODDED_FD_PATH);

        command.add(dFile.getAbsolutePath());

        command.add(pFile.getAbsolutePath());

        command.add("--landmarks");

        command.add("lm=lm_exhaust(reasonable_orders=true, only_causal_landmarks=false, disjunctive_landmarks=true, conjunctive_landmarks=true, no_orders=false)");

        command.add("--search");

        command.add("astar(lmcut())");

        command.add("--heuristic");

        command.add("hlm=lmcount(lm)");

        return command;
    }

    public Map<Integer, String[]> getLandmarks() {
        Map<Integer, String[]> result = new HashMap<Integer, String[]>();
        int count = 0;
        for(String[] lm : this.landmarks) {
            result.put(count, lm);
            count++;
        }
        return result;
    }

    public long getRuntime() {
        return this.runTime;
    }
    
}

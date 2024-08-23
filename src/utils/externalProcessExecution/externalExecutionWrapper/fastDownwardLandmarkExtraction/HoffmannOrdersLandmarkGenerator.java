package utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import config.GoalRecognitionConfiguration;
import experiment.recognitionExperiments.FDLandmarkBased.LandmarkBasedGoalRecognitionUtils;
import utils.externalProcessExecution.ExternalProcessExecutionHandler;
import utils.externalProcessExecution.ProcessExecutionHandler;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import org.apache.commons.lang3.tuple.Pair;


public class HoffmannOrdersLandmarkGenerator implements ILandmarkGenerator{
    private long runTime;
    private Pair<Map<String, String[]>, Graph> landmarks;
    private Graph<String, DefaultEdge> landmarkGraph;

    public void runLandmarkGenerator(File dFile, File pFile, File logFile) {
        try {
            ProcessExecutionHandler execution = ExternalProcessExecutionHandler.startProcessExecution(this.generateExecutionCommand(dFile, pFile), logFile, false);

            execution.join();

            this.landmarks = FastDownwardLMExtractionUtils.generateLandmarkSetNewVersion(execution.getLog());
            this.landmarkGraph = this.landmarks.getRight();
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

        command.add("--search");

        switch(GoalRecognitionConfiguration.LANDMARK_EXTRACTION_TYPE) {
            case HOFFMANN_ORDERS_EXHAUSTIVE:
                command.add("let(lm, lm_reasonable_orders_hps(lm_exhaust(only_causal_landmarks=false, verbosity=debug), verbosity=debug), astar(landmark_sum(lm)))");
                break;
            case HOFFMANN_ORDERS_HM:
                command.add("let(lm, lm_reasonable_orders_hps(lm_hm(conjunctive_landmarks=true, verbosity=debug, use_orders=true), verbosity=debug), astar(landmark_sum(lm)))");
                break;
            case HOFFMANN_ORDERS_RHW:
                command.add("let(lm, lm_reasonable_orders_hps(lm_rhw(disjunctive_landmarks=true, verbosity=debug, use_orders=true, only_causal_landmarks=false), verbosity=debug), astar(landmark_sum(lm)))");
                break;
            case HOFFMANN_ORDERS_ZG:
                command.add("let(lm, lm_reasonable_orders_hps(lm_zg(verbosity=debug, use_orders=true), verbosity=debug), astar(landmark_sum(lm)))");
                break;
            default:
                break;
        }

        return command;
    }

    public Map<Integer, String[]> getLandmarks() {
        Map<Integer, String[]> result = new HashMap<Integer, String[]>();
        Map<String, String[]> landmarkMap = this.landmarks.getLeft();
        for(String id : landmarkMap.keySet()) {
            result.put(LandmarkBasedGoalRecognitionUtils.getCountFromID(id), landmarkMap.get(id));
        }
        return result;
    }

    public long getRuntime() {
        return this.runTime;
    }

    public Graph<String, DefaultEdge> getLandmarkGraph() {
        return this.landmarkGraph;
    }
    
}

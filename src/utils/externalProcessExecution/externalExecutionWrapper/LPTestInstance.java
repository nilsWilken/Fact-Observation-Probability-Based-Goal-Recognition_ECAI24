package utils.externalProcessExecution.externalExecutionWrapper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.externalProcessExecution.ExternalProcessExecutionHandler;
import utils.externalProcessExecution.ProcessExecutionHandler;
import config.GoalRecognitionConfiguration;

public class LPTestInstance {
    private long runTime;
    private double accuracy;
    private double spread;
    private double agreement;
    private Map<Integer, Double> scores;

    public void runLPTestInstance(File archiveFile, String heuristic, File logFile) {
        long startTime = System.currentTimeMillis();
        try {
            ProcessExecutionHandler execution = ExternalProcessExecutionHandler.startProcessExecution(this.generateExecutionCommand(archiveFile, heuristic), logFile, true);

            execution.join();

            this.processLog(execution.getLog());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        this.runTime = System.currentTimeMillis() - startTime;
    }

    //Creates execution command that is used to call the external LP implementation
    public List<String> generateExecutionCommand(File archiveFile, String heuristic) {
		StringBuffer exeCommand = new StringBuffer();
        List<String> command = new ArrayList<String>();

        GoalRecognitionConfiguration.USE_BASH_C = true;
		
        //Start with path to executable, which has to be specified in the general configuration
		exeCommand.append(GoalRecognitionConfiguration.LP_TEST_INSTANCE_PATH + " ");
		
        //Problem File
        exeCommand.append("-r " + heuristic + " ");

		//Experiment file
		exeCommand.append("-e " + archiveFile.getAbsolutePath() + " ");


        command.add(exeCommand.toString());
				
		return command;
	}

    public long getRuntime() {
        return this.runTime;
    }

    public double getAccuracy() {
        return this.accuracy;
    }

    public double getSpread() {
        return this.spread;
    }

    public double getAgreement() {
        return this.agreement;
    }

    public Map<Integer, Double> getScores() {
        return this.scores;
    }

    //Parses results from the log of the external execution
    private void processLog(List<String> log) {
        System.out.println(log.size());
        this.scores = new HashMap<Integer, Double>();
        for(String line : log) {
            if(line.startsWith("Agreement: ")) {
                this.agreement = Double.parseDouble(line.split(": ")[1]);
            }
            else if(line.startsWith("Accuracy: ")) {
                this.accuracy = Double.parseDouble(line.split(": ")[1]);
            }
            else if(line.startsWith("Spread: ")) {
                this.spread = Double.parseDouble(line.split(": ")[1]);
            }
            else if(line.startsWith("> ")) {
                String[] split = line.split("> ")[1].split(":");
                int hyp = Integer.parseInt(split[0]);
                boolean included = Boolean.parseBoolean(split[1]);
                if(included) {
                    this.scores.put(hyp, 1.0);
                }
                else {
                    this.scores.put(hyp, 0.0);
                }
            }
        }
    }
    
}

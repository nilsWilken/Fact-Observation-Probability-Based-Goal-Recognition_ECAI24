package utils.externalProcessExecution.externalExecutionWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import config.GoalRecognitionConfiguration;
import utils.externalProcessExecution.ExternalProcessExecutionHandler;
import utils.externalProcessExecution.ProcessExecutionHandler;

public class PR2Plan {
    private long runTime;

    public void runPPR2Plan(File dFile, File pFile, File obsFile, File logFile, File dTargetFile, File pTargetFile) {
                try {
                    Thread execution = ExternalProcessExecutionHandler.scheduleCommandForExecution(this.generateExecutionCommand(dFile, pFile, obsFile), logFile, true);
                    while(execution.getState() == Thread.State.NEW) {
                        Thread.sleep(100);
                    }
                    execution.join();
                    this.runTime = ((ProcessExecutionHandler)execution).getRuntime();
                    
                    File cDir = new File(System.getProperty("user.dir"));
                    File domain = Paths.get(cDir.getAbsolutePath(), "pr-domain.pddl").toFile();
                    File problem = Paths.get(cDir.getAbsolutePath(), "pr-problem.pddl").toFile();

                    try {
                        Files.move(Paths.get(domain.getAbsolutePath()), Paths.get(dTargetFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                        Files.move(Paths.get(problem.getAbsolutePath()), Paths.get(pTargetFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
    }

    public List<String> generateExecutionCommand(File dFile, File pFile, File obsFile) {
		StringBuffer exeCommand = new StringBuffer();
        List<String> command = new ArrayList<String>();

        GoalRecognitionConfiguration.USE_BASH_C = true;
		
		exeCommand.append(GoalRecognitionConfiguration.PR2PLAN_LOCATION + " ");
		
		//Domain File
		exeCommand.append("-d " + dFile.getAbsolutePath() + " ");
		
		//Problem File
		exeCommand.append("-i " + pFile.getAbsolutePath() + " ");
		
		//Observation File
		exeCommand.append("-o " + obsFile.getAbsolutePath());

        command.add(exeCommand.toString());
				
		return command;
	}

    public long getRuntime() {
        return this.runTime;
    }
    
}
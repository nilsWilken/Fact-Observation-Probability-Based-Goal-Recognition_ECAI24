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

public class SuboptimalPR {
    private long runTime;

    public void runSubPR(File dFile, File pFile, File logFile, File solnTargetFile) {
        try {
            Thread execution = ExternalProcessExecutionHandler.scheduleCommandForExecution(this.generateExecutionCommand(dFile, pFile), logFile, true);
            while(execution.getState() == Thread.State.NEW) {
                Thread.sleep(100);
            }
            execution.join();
            this.runTime = ((ProcessExecutionHandler)execution).getRuntime();

            File solnFile = Paths.get(pFile.getAbsolutePath().replace(".pddl", ".soln")).toFile();

            try {
                Files.move(Paths.get(solnFile.getAbsolutePath()), Paths.get(solnTargetFile.getAbsolutePath()), StandardCopyOption.REPLACE_EXISTING);
            }catch(IOException e) {
                e.printStackTrace();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public List<String> generateExecutionCommand(File dFile, File pFile) {
		StringBuffer exeCommand = new StringBuffer();
        List<String> command = new ArrayList<String>();

        GoalRecognitionConfiguration.USE_BASH_C = true;
        
        exeCommand.append(GoalRecognitionConfiguration.SUBOPT_PR_LOCATION + " ");
		
		//Domain File
		exeCommand.append("-d " + dFile.getAbsolutePath() + " ");
		
		//Problem File
		exeCommand.append("-i " + pFile.getAbsolutePath() + " -I");

        command.add(exeCommand.toString());
				
		return command;
	}

    public long getRuntime() {
        return this.runTime;
    }
}

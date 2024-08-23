package utils.externalProcessExecution;

import java.io.File;
import java.util.List;

import config.GoalRecognitionConfiguration;

public class ExternalProcessExecutionHandler {
	
	private static ExternalProcessExecutionQueue queue = new ExternalProcessExecutionQueue(GoalRecognitionConfiguration.MAX_PLANNING_THREADS);
	
	
	public synchronized static ProcessExecutionHandler scheduleCommandForExecution(List<String> command, File logFile, boolean hardDestroy) {
		ProcessExecutionHandler handler = new ProcessExecutionHandler(command, logFile, hardDestroy);
		
		if(!ExternalProcessExecutionHandler.queue.isAlive()) {
			ExternalProcessExecutionHandler.queue.setDaemon(true);
			ExternalProcessExecutionHandler.queue.start();
		}
		
		ExternalProcessExecutionHandler.queue.addProcessExecution(handler);
		
		return handler;
	}

	public static ProcessExecutionHandler startProcessExecution(List<String> command, File logFile, boolean hardDestroy) {
		try {
            ProcessExecutionHandler execution = ExternalProcessExecutionHandler.scheduleCommandForExecution(command, logFile, false);

            while(execution.getState() == Thread.State.NEW) {
                Thread.sleep(100);
            }
			return execution;
        }catch(InterruptedException e) {
            e.printStackTrace();
        }
		return null;
	}
	
	public static void requestExecutionQueueStop() {
		ExternalProcessExecutionHandler.queue.requestStop();
	}
}

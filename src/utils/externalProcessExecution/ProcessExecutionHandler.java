package utils.externalProcessExecution;

import java.io.File;
import java.util.List;

import config.GoalRecognitionConfiguration;

public class ProcessExecutionHandler extends Thread {

	private List<String> command;
	private File logFile;
	private boolean hardDestroy;
	private volatile long runTime;
	private volatile List<String> log;
	private volatile boolean timedOut;
	private volatile boolean startFailed;
	
	public ProcessExecutionHandler(List<String> command, File logFile, boolean hardDestroy) {
		this.command = command;
		this.logFile = logFile;
		this.hardDestroy = hardDestroy;
	}
	
	@Override
	public void run() {
		ProcessExecutor pExecutor = new ProcessExecutor(command, logFile);
		long startTimeGlobal = System.currentTimeMillis();
		pExecutor.start();
		
		if(this.hardDestroy) {
			//Wait until the process finishes or the time limit is reached
			try {
				long remaining = GoalRecognitionConfiguration.MAX_TIME_MILLISECONDS - (System.currentTimeMillis() - startTimeGlobal);
				if(remaining > 0) {
					pExecutor.join(remaining);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			if(pExecutor.isAlive()) {
				this.timedOut = true;
				pExecutor.destroyProcess();
				pExecutor.interrupt();
			
				try {
					pExecutor.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			this.runTime = pExecutor.getRuntime();
			this.log = pExecutor.getLog();
		}
		else {
			try {
				pExecutor.join();
				this.runTime = pExecutor.getRuntime();
				this.log = pExecutor.getLog();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		this.startFailed = pExecutor.hasProcessStartFailed();
		if(this.runTime == 0) {
			this.runTime = System.currentTimeMillis() - startTimeGlobal;
		}
	}
	
	public long getRuntime() {
		return this.runTime;
	}

	public List<String> getLog() {
		return this.log;
	}
	
	public boolean isTimedOut() {
		return this.timedOut;
	}
	
	public boolean hasStartFailed() {
		return this.startFailed;
	}
}

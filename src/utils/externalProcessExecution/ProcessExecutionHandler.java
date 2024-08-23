package utils.externalProcessExecution;

import java.io.File;
import java.util.List;

import config.GoalRecognitionConfiguration;

public class ProcessExecutionHandler extends Thread {

	private List<String> command;
	private File logFile;
	private boolean hardDestroy;
	private long runTime;
	private List<String> log;
	
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
			long startTime = System.currentTimeMillis();
			while((System.currentTimeMillis() - startTime) < GoalRecognitionConfiguration.MAX_TIME_MILLISECONDS && pExecutor.isAlive()) {
				// try {
				// 	Thread.sleep(100);
				// 	System.out.println(System.currentTimeMillis() - startTime);
				// } catch (InterruptedException e) {
				// 	e.printStackTrace();
				// }
			}

			
			if(pExecutor.isAlive()) {
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
}

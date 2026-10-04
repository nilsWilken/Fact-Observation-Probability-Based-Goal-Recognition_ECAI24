package utils.externalProcessExecution;

import utils.StreamGobbler;
import utils.externalProcessExecution.externalScriptWrapper.ExternalScriptType;
import utils.externalProcessExecution.externalScriptWrapper.IExternalScript;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import config.GoalRecognitionConfiguration;

public class ProcessExecutor extends Thread {
	
	private List<String> command;
	private File logFile;
	private volatile Process process;
	private volatile boolean processDestroyed;
	private volatile boolean processStartFailed;
	private volatile long runTime;
	private List<String> log;
	
	public ProcessExecutor(List<String> command, File logFile) {
		this.command = command;
		this.logFile = logFile;
		this.processDestroyed = false;
		this.processStartFailed = false;
		this.log = new ArrayList<String>();
	}
	
	public void run() {
		long startTime = System.currentTimeMillis();

		IExternalScript commandScript = ExternalScriptType.getExternalScriptWrapper(ExternalScriptType.EXTERNAL_COMMAND_EXECUTION);

		List<String> extendedCommand = new ArrayList<String>();

		if(GoalRecognitionConfiguration.USE_EXTERNAL_EXECUTION_SCRIPT) {
			extendedCommand.add(commandScript.getLocation().getAbsolutePath());
		}
		else if(GoalRecognitionConfiguration.USE_BASH_C){
			extendedCommand.add("bash");
        	extendedCommand.add("-c");
		}
		extendedCommand.addAll(this.command);

		System.out.println(extendedCommand);


		try {
			ProcessBuilder builder = new ProcessBuilder();
			builder.command(extendedCommand);
			builder.directory(new File(System.getProperty("user.dir")));
			
			this.process = builder.start();
		}catch(IOException e) {
			this.processStartFailed = true;
			System.err.println("Could not start external process " + extendedCommand + ": " + e.getMessage());
			e.printStackTrace();
			this.runTime = System.currentTimeMillis() - startTime;
			return;
		}

		StreamGobbler errorReader = new StreamGobbler(this.process.getErrorStream());
		errorReader.start();

		//Only this thread writes and closes the log. After a forced destroy the pipe reaches EOF and the loop ends.
		try(BufferedReader procIn = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
			BufferedWriter procLogOut = new BufferedWriter(new FileWriter(this.logFile))) {
			String line;
			while((line = procIn.readLine()) != null) {
				procLogOut.write(line);
				procLogOut.newLine();
				this.log.add(line);
			}
		}catch(IOException e) {
			if(!this.processDestroyed) {
				e.printStackTrace();
			}
		}

		errorReader.interrupt();
		this.runTime = System.currentTimeMillis() - startTime;
	}
	
	
	public void destroyProcess() {
		Logger logger = Logger.getLogger("ProcessExecutor");
		logger.setLevel(Level.ALL);
		
		this.processDestroyed = true;
		
		logger.info("Try to destroy process....");
		Process p = this.process;
		if(p != null) {
			p.destroyForcibly();
		}
		logger.info("Successfully destroyed process!");
	}
	
	public boolean isProcessDestroyed() {
		return this.processDestroyed;
	}
	
	public boolean hasProcessStartFailed() {
		return this.processStartFailed;
	}
	
	public long getRuntime() {
		return this.runTime;
	}

	public List<String> getLog() {
		return this.log;
	}
}

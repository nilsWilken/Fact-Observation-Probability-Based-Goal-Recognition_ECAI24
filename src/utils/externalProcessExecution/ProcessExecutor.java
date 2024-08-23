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
	private BufferedReader procIn;
	private BufferedWriter procLogOut;
	private String line;
	private Process process;
	private boolean processDestroyed;
	private long runTime;
	private List<String> log;
	
	public ProcessExecutor(List<String> command, File logFile) {
		this.command = command;
		this.logFile = logFile;
		this.processDestroyed = false;
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

			this.procIn = new BufferedReader(new InputStreamReader(this.process.getInputStream()));
			this.procLogOut = new BufferedWriter(new FileWriter(this.logFile));

			StreamGobbler errorReader = new StreamGobbler(process.getErrorStream());
			errorReader.start();

			while(!this.processDestroyed && (this.line=this.procIn.readLine()) != null) {
				this.procLogOut.write(line);
				this.procLogOut.newLine();
				this.log.add(line);
			}

			errorReader.interrupt();

			if(!this.processDestroyed) {
				this.procLogOut.flush();
				this.procLogOut.close();
				this.procIn.close();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		this.runTime = System.currentTimeMillis() - startTime;
	}
	
	
	public void destroyProcess() {
		Logger logger = Logger.getLogger("ProcessExecutor");
		logger.setLevel(Level.ALL);
		
		this.processDestroyed = true;
		
		logger.info("Try to destroy process....");
		this.process.destroyForcibly();
		
		try {
			if (this.line != null) {
				this.procLogOut.write(line);
				this.procLogOut.flush();
				this.log.add(line);
			}

			this.procLogOut.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		logger.info("Successfully destroyed process!");
	}
	
	public long getRuntime() {
		return this.runTime;
	}

	public List<String> getLog() {
		return this.log;
	}
}

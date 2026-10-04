package symbolic.planning.planners.classicPlanners;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import config.GoalRecognitionConfiguration;
import utils.externalProcessExecution.ExternalProcessExecutionHandler;
import utils.externalProcessExecution.ProcessExecutionHandler;
import utils.externalProcessExecution.ProcessStartFailedException;

public class MetricFFPlanner implements IClassicalPlanner {
	
	//Metric-FF output lines that indicate a successful run
	private static final String PLAN_FOUND_MARKER = "ff: found legal plan as follows";
	private static final String EMPTY_PLAN_MARKER = "ff: goal can be simplified to TRUE";
	
	private List<String> plan = new ArrayList<String>();
	private double planCost = IClassicalPlanner.FAILED_PLAN_LENGTH;
	private long runTime;

	@Override
	public List<String> getPlan() {
		return this.plan;
	}

	@Override
	public double getPlanCost() {
		return this.planCost;
	}

	@Override
	public int getPlanLength() {
		if(this.planCost == IClassicalPlanner.FAILED_PLAN_LENGTH) {
			return IClassicalPlanner.FAILED_PLAN_LENGTH;
		}
		return this.plan.size();
	}

	@Override
	public boolean parsePlanFromLogFile(File logFile) {
		this.markAsFailed();
		try {
			List<String> lines = Files.readAllLines(Paths.get(logFile.getAbsolutePath()), StandardCharsets.UTF_8);
			
			boolean planFound = false;
			boolean emptyPlanSolves = false;
			for(String line : lines) {
				if(line.contains(PLAN_FOUND_MARKER)) {
					planFound = true;
				}
				else if(line.contains(EMPTY_PLAN_MARKER)) {
					emptyPlanSolves = true;
				}
				//Plan steps (and the optional plan cost) are only printed after the success marker
				else if(planFound) {
					this.parseLine(line);
				}
			}
			
			if(planFound && !this.plan.isEmpty()) {
				if(this.planCost == IClassicalPlanner.FAILED_PLAN_LENGTH) {
					this.planCost = this.plan.size();
				}
			}
			else if(emptyPlanSolves && !planFound) {
				this.planCost = 0;
			}
			//Timeout, unsolvable problem, or unexpected output
			else {
				this.markAsFailed();
			}
			return true;
		}catch(IOException e) {
			e.printStackTrace();
			this.markAsFailed();
		}
		return false;
	}
	
	private void markAsFailed() {
		this.plan = new ArrayList<String>();
		this.planCost = IClassicalPlanner.FAILED_PLAN_LENGTH;
	}

	@Override
	public boolean parsePlanFromPlanFile(File planFile) {
		System.out.println("MetricFF does not support saving the plan in a dedicated file!");
		return false;
	}

	@Override
	public void writePlanToFile(File file) {
		try {
			StringBuffer plan = new StringBuffer();
			
			for(String action : this.plan) {
				plan.append(action + "\n");
			}
			Files.writeString(Paths.get(file.getAbsolutePath()), plan.toString(), new OpenOption[0]);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<String> generateExecutionCommand(File dFile, File pFile, File planFile) {
		StringBuffer exeCommand = new StringBuffer();
		List<String> command = new ArrayList<String>();

		GoalRecognitionConfiguration.USE_BASH_C = true;
		
		exeCommand.append(GoalRecognitionConfiguration.METRICFF_PLANNER_LOCATION + " ");
		
		//Domain File
		exeCommand.append("-o ");
		exeCommand.append(dFile.getAbsolutePath() + " ");
		
		//Problem File
		exeCommand.append("-f ");
		exeCommand.append(pFile.getAbsolutePath() + " ");
		
		//Strategy
		exeCommand.append("-s ");
		exeCommand.append(GoalRecognitionConfiguration.METRICFF_PLANNER_STRATEGY);
				
		command.add(exeCommand.toString());

		return command;
	}

	@Override
	public void runPlanner(File dFile, File pFile, File planFile, File logFile) {
		this.markAsFailed();
		this.runTime = 0;
		
		//Remove results of earlier runs so that they can never be parsed as results of this run
		try {
			Files.deleteIfExists(logFile.toPath());
			Files.deleteIfExists(planFile.toPath());
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ProcessExecutionHandler execution;
		try {
			execution = ExternalProcessExecutionHandler.scheduleCommandForExecution(this.generateExecutionCommand(dFile, pFile, planFile), logFile, true);
			while(execution.getState() == Thread.State.NEW) {
				Thread.sleep(100);
			}
			execution.join();
			this.runTime = execution.getRuntime();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		
		if(execution.hasStartFailed()) {
			throw new ProcessStartFailedException("Metric-FF could not be started for problem " + pFile.getAbsolutePath()
				+ ". Aborting the run (was the JDK upgraded while this JVM was running?).");
		}
		if(execution.isTimedOut()) {
			System.out.println("Metric-FF timed out after " + this.runTime + " ms on " + pFile.getAbsolutePath());
			return;
		}
		
		this.parsePlanFromLogFile(logFile);
		
		//Only successful runs get a plan file: missing plan file = failure, empty plan file = empty plan
		if(this.getPlanLength() != IClassicalPlanner.FAILED_PLAN_LENGTH) {
			this.writePlanToFile(planFile);
		}
	}
	
	public long getRuntime() {
		return this.runTime;
	}
	
	private void parseLine(String line) {
		StringBuffer action = new StringBuffer();
		
		//Remove unnecessary whitespaces
		line = line.trim();
		
		//Check whether the currently parsed line contains an action
		if(line.matches("(step)?\\s*[0-9]+: .*")) {
			String[] split = line.split(" ");
			
			//Filter for the first action line in the log file (which starts with "step")
			if(split[0].equals("step")) {
				split = line.split("step");
				split = split[1].trim().split(" ");
			}
			
			int i=1;
			if(split[1].contains("__")) {
				i=0;
				split = split[1].split("__");
			}
			
			//Generate action string
			for(;i < split.length; i++) {
				action.append(split[i]);
				if((i+1) < split.length) {
					action.append(" ");
				}
			}
			
			this.plan.add("(" + action.toString() + ")");
		}
		//Parse plan cost
		else if(line.matches("plan cost: [0-9]+\\.[0-9]+")) {
			String[] split = line.split(":");
			this.planCost = Double.parseDouble(split[1].trim());
		}
	}

}

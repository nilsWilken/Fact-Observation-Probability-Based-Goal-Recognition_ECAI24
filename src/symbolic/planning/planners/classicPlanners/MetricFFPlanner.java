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

public class MetricFFPlanner implements IClassicalPlanner {
	
	private List<String> plan;
	private double planCost = -1;
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
		if(this.planCost == -1) {
			return -1;
		}
		return this.plan.size();
	}

	@Override
	public boolean parsePlanFromLogFile(File logFile) {
		try {
			this.plan = new ArrayList<String>();
			
			List<String> lines = Files.readAllLines(Paths.get(logFile.getAbsolutePath()), StandardCharsets.UTF_8);
			
			for(String line : lines) {
				this.parseLine(line);
			}
			if(lines.size() > 10 && this.planCost == -1) {
				this.planCost = this.plan.size();
			}
						
			return true;
		}catch(IOException e) {
			e.printStackTrace();
		}
		return false;
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
		this.planCost = -1;
		try {
			Thread execution = ExternalProcessExecutionHandler.scheduleCommandForExecution(this.generateExecutionCommand(dFile, pFile, planFile), logFile, true);
			while(execution.getState() == Thread.State.NEW) {
				Thread.sleep(100);
			}
			execution.join();
			this.runTime = ((ProcessExecutionHandler)execution).getRuntime();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.parsePlanFromLogFile(logFile);
		this.writePlanToFile(planFile);
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

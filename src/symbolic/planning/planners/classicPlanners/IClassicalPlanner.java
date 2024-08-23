package symbolic.planning.planners.classicPlanners;

import java.io.File;
import java.util.List;

public interface IClassicalPlanner {
	
	public abstract List<String> getPlan();
	public abstract double getPlanCost();
	public abstract int getPlanLength();
	
	public abstract boolean parsePlanFromLogFile(File logFile);
	public abstract boolean parsePlanFromPlanFile(File planFile);
	
	public abstract void writePlanToFile(File file);
	
	public abstract List<String> generateExecutionCommand(File dFile, File pFile, File planFile);
	
	public void runPlanner(File dFile, File pFile, File planFile, File logFile);
	
	public long getRuntime();

}

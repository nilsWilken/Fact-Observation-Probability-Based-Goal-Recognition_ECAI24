package symbolic.planning.planners.classicPlanners;

import java.io.File;
import java.util.List;

public interface IClassicalPlanner {

	/** Plan length reported for every run that did not produce a valid plan (timeout, unsolvable, crash). */
	public static final int FAILED_PLAN_LENGTH = -1;

	public abstract List<String> getPlan();
	public abstract double getPlanCost();

	/**
	 * @return number of actions of the found plan (0 only if the goal already holds),
	 *         or FAILED_PLAN_LENGTH if no valid plan was found
	 */
	public abstract int getPlanLength();
	
	public abstract boolean parsePlanFromLogFile(File logFile);
	public abstract boolean parsePlanFromPlanFile(File planFile);
	
	public abstract void writePlanToFile(File file);
	
	public abstract List<String> generateExecutionCommand(File dFile, File pFile, File planFile);
	
	public void runPlanner(File dFile, File pFile, File planFile, File logFile);
	
	public long getRuntime();

}

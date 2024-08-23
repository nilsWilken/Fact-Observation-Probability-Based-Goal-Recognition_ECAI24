package symbolic.planning.stateHandler.planSampling;

import java.io.File;
import java.util.List;

import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;

public interface IPlanSamplingStateHandler {
	
	public List<GroundAction> computeApplicableActions();
	public GroundAction sampleNextAction();
	
	public void advanceCurrentState(GroundAction action);
	public void advanceCurrentState(String action);

	public void writeCurrentStateToPDDLProblemFile(String newProblemFile);
	
	public boolean isGoalFullfilled();
	
	public void setGoal(String goal);
	
	public void initializeSamplingModelFromFile(File samplingModelFile);
	
	public void resetCurrentState();
	public void setCurrentState(PDDLState state);
	public void revertCurrentStateByOneStep();
}

package symbolic.planning.stateHandler.planSampling;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;

import config.GoalRecognitionConfiguration;
import symbolic.planning.stateHandler.AbstractStateHandler;

public class SimplePlanSamplingStateHandler extends AbstractStateHandler implements IPlanSamplingStateHandler {

	private PDDLState currentState;
	private RelState currentRelaxedState;
	private List<PDDLState> previousStates;
	private String goal;
	
	public SimplePlanSamplingStateHandler(String domainFile, String problemFile) {
		super(domainFile, problemFile);
				
		this.currentState = (PDDLState)this.ground.getInit().clone();
		this.currentRelaxedState = this.currentState.relaxState();
		this.previousStates = new ArrayList<PDDLState>();
	}
	
	public SimplePlanSamplingStateHandler(PddlDomain domain, EPddlProblem problem) {
		super(domain, problem);
		
		this.currentState = (PDDLState)this.ground.getInit().clone();
		this.currentRelaxedState = this.currentState.relaxState();
		this.previousStates = new ArrayList<PDDLState>();
	}

	@Override
	public List<GroundAction> computeApplicableActions() {
		List<GroundAction> applicableActions = new ArrayList<GroundAction>();
		
		GroundAction action;
		for(Object a : this.ground.getActions()) {
			action = (GroundAction)a;
			if(this.currentState.satisfy(action.getPreconditions())) {
				applicableActions.add(action);
			}
		}
		
		return applicableActions;
	}
	
	@Override
	public void initializeSamplingModelFromFile(File samplingModelFile) {
	}

	@Override
	public GroundAction sampleNextAction() {
		return null;
	}
	
	@Override
	public void advanceCurrentState(GroundAction action) {
		if(this.previousStates.size() > 0 && this.previousStates.get(this.previousStates.size()-1) != this.currentState) {
			this.previousStates.add(this.currentState.clone());
		}
		this.advanceStateWithAction(action, this.currentState);
	}
	
	@Override
	public void advanceCurrentState(String action) {
		if(this.previousStates.size() > 0 && this.previousStates.get(this.previousStates.size()-1) != this.currentState) {
			this.previousStates.add(this.currentState.clone());
		}
		this.advanceStateWithActionObs(action, this.currentState);
	}
	
	public void advanceCurrentRelaxedState(String action) {
		this.advanceRelaxedStateWithActionObsWithoutPreconditions(action, currentRelaxedState);
	}

	@Override
	public void writeCurrentStateToPDDLProblemFile(String newProblemFile) {
		this.writePDDLStateToFile(this.currentState, newProblemFile);
	}
	
	@Override
	public boolean isGoalFullfilled() {
		return this.ground.goalSatisfied(this.currentState);
	}
	
	public void setGoal(String goal) {
		this.goal = goal;
	}
	
	public void resetCurrentState() {
		this.currentState = (PDDLState)this.ground.getInit().clone();
		this.previousStates = new ArrayList<PDDLState>();
	}
	
	public PDDLState getCurrentState() {
		return this.currentState;
	}
	
	public RelState getCurrentRelaxedState() {
		return this.currentRelaxedState;
	}
	
	public void setCurrentState(PDDLState state) {
		this.currentState = state;
	}
	
	public void revertCurrentStateByOneStep() {
		if(this.previousStates.size() > 0) {
			this.currentState = this.previousStates.get(this.previousStates.size()-1).clone();
		}
		else {
			this.currentState = (PDDLState)this.ground.getInit().clone();
		}
	}	
}

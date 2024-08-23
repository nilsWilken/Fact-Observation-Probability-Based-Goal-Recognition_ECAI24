package symbolic.planning.stateHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.PDDLObject;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.expressions.NumFluent;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;
import com.hstairs.ppmajal.problem.RelState;
import com.hstairs.ppmajal.problem.State;

import symbolic.planning.planningUtils.PDDLProblem;

public abstract class AbstractStateHandler {
	
	protected EPddlProblem ground;
	protected PDDLProblem problem;
	
	public AbstractStateHandler(String domainFile, String problemFile) {
		this.problem = new PDDLProblem(new File(problemFile));
		
		PddlDomain domain = new PddlDomain(domainFile);
		domain.substituteEqualityConditions();
		
		this.ground = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);

		try {
			this.ground.transformGoal();
			this.ground.groundingActionProcessesConstraints();
			this.ground.simplifyAndSetupInit(false, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public AbstractStateHandler(PddlDomain domain, EPddlProblem problem) {
		this.ground = problem;
	}
	
	protected void writePDDLStateToFile(PDDLState state, String filePath) {
		List<String> props = this.convertStateToPropStringsWithParameters(state);
		Map<String, String> funcValues = this.convertStateToFuncValues(state);
		
		StringBuffer stateString = new StringBuffer();
		for(String prop : props) {
			stateString.append("\t\t" + prop.trim() + "\n");
		}
		for(String key : funcValues.keySet()) {
			stateString.append("\t\t(= (" + key + ") " + funcValues.get(key) + ")\n");
		}
		this.problem.generateProblemFileWithNewInitState(stateString.toString(), filePath);
	}
	
	protected void writePDDLStateToFile(PDDLState state, String initProblemFile, String outputFile) {
		List<String> props = this.convertStateToPropStringsWithParameters(state);
		Map<String, String> funcValues = this.convertStateToFuncValues(state);
		
		StringBuffer stateString = new StringBuffer();
		for(String prop : props) {
			stateString.append("\t\t" + prop.trim() + "\n");
		}
		for(String key : funcValues.keySet()) {
			stateString.append("\t\t(= (" + key + ") " + funcValues.get(key) + ")\n");
		}
		
		PDDLProblem prob = new PDDLProblem(new File(initProblemFile));
		prob.generateProblemFileWithNewInitState(stateString.toString(), outputFile);
	}
	
	protected void advanceStateWithActionObs(String obs, State state) {
		List<GroundAction> actions = (List<GroundAction>)this.retrieveActionsFromString(obs);

		boolean applied = false;
		for(GroundAction a : actions) {
			if (a != null && state.satisfy(a.getPreconditions())) {
				applied = true;
				state.apply(a, state);
				break;
			}
		}
		if(!applied) {
			System.out.println("Preconditions for " + obs + " not satisfied!");
			for(GroundAction a : actions) {
				for(Object o : a.getPreconditions().sons) {
					Condition c = (Condition)o;
					if(!c.isSatisfied(state)) {
						System.out.println(c + " is not satisfied!");
					}
				}
			}
			System.exit(0);
		}
	}
	
	protected void advanceRelaxedStateWithActionObs(String obs, RelState state) {
		List<GroundAction> actions = (List<GroundAction>)this.retrieveActionsFromString(obs);

		boolean applied = false;
		for(GroundAction a : actions) {
			if (a != null && state.satisfy(a.getPreconditions())) {
				applied = true;
				state.apply(a);
				break;
			}
		}
		if(!applied) {
			System.out.println("Preconditions for " + obs + " not satisfied!");
			for(GroundAction a : actions) {
				for(Object o : a.getPreconditions().sons) {
					Condition c = (Condition)o;
					if(!c.isSatisfied(state)) {
						System.out.println(c + " is not satisfied!");
					}
				}
			}
		}
	}

	protected void advanceRelaxedStateWithActionObsWithoutPreconditions(String obs, RelState state) {
		List<GroundAction> actions = (List<GroundAction>)this.retrieveActionsFromString(obs);

		boolean applied = false;
		for(GroundAction a : actions) {
			if (a != null && state.satisfy(a.getPreconditions())) {
				applied = true;
				state.apply(a);
				break;
			}
		}
		if(!applied) {
			for(GroundAction a : actions) {
				for(Object o : a.getPreconditions().sons) {
					Condition c = (Condition)o;
					if(!c.isSatisfied(state) && c instanceof Predicate) {
						// System.out.println(c + " is not satisfied!");
						state.makePositive((Predicate)c);
					}
				}
				state.apply(a);
			}
		}
	}
	
	public List<GroundAction> retrieveActionsFromString(String obs) {
		Iterator it = this.ground.getActions().iterator();
		GroundAction a;
		List<GroundAction> result = new ArrayList<GroundAction>();
		
		boolean found = false;
		while(it.hasNext()) {
			a = (GroundAction)it.next();
			if (AbstractStateHandler.convertActionToActionString(a).toLowerCase().equals(obs.toLowerCase())) {
				result.add(a);
			}
		}
		if(result.size() == 0) {
			System.out.println(obs + " not found!");
		}
		return result;
	}
	
	protected void advanceStateWithAction(GroundAction action, State state) {
		if(state.satisfy(action.getPreconditions())) {
			state.apply(action, state);
		}
		else {
			System.out.println("Action " + action.getName() + " is not applicable!");
		}
	}

	protected Map<String, String> convertStateToFuncValues(State state) {
		Map<String, String> result = new HashMap<String, String>();
		Iterator it = this.ground.getNumFluents().iterator();
		NumFluent f;		
				
		StringBuffer key;
		while(it.hasNext()) {
			f =(NumFluent)it.next();
			
			key = new StringBuffer();
			key.append(f.getName());
			
			for(Object o : f.getTerms()) {
				if(o instanceof PDDLObject) {
					key.append(" " + ((PDDLObject)o).getName());
				}
			}
			
			result.put(key.toString(), "" + ((PDDLState)state).fluentValue(f));
		}
		
		
		return result;
	}
	
	protected List<String> convertStateToPropStrings(PDDLState state) {
		List<String> result = new ArrayList<String>();
		
		Iterator pNext = state.getConnectedProblem().getBooleanFluents().iterator();
		Predicate p;
		
		while(pNext.hasNext()) {
			p = (Predicate) pNext.next();
			if(((PDDLState)state).holds(p)) {
				result.add(p.getPredicateName());
			}
		}
		
		return result;
	}
	
	protected List<String> convertStateToPropStringsWithParameters(PDDLState state) {
		List<String> result = new ArrayList<String>();
		
		Iterator pNext = state.getConnectedProblem().getBooleanFluents().iterator();
		Predicate p;
		
		while(pNext.hasNext()) {
			p = (Predicate) pNext.next();
			if(((PDDLState)state).holds(p)) {
				result.add(p.pddlPrint(false));
			}
		}
		
		return result;
	}
	
	public static String convertActionToActionString(GroundAction a) {
		StringBuffer result = new StringBuffer();
		result.append(a.getName());
		
		Iterator it = a.getParameters().iterator();
		PDDLObject o;
		
		while(it.hasNext()) {
			o = (PDDLObject)it.next();
			result.append("__" + o.getName());
		}
		
		return result.toString();
	}
	
	public EPddlProblem getGroundProblem() {
		return this.ground;
	}

}

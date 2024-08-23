package symbolic.relaxedPlanning.bestSupporterFunctions;

import java.util.List;

import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.RelState;

public abstract class AbstractBestSupporterSelectionFunction {
	
	protected PddlDomain domain;
	protected EPddlProblem problem;
	
	public AbstractBestSupporterSelectionFunction(PddlDomain domain, EPddlProblem problem) {
		this.domain = domain;
		this.problem = problem;
	}
	
	
	public abstract GroundAction selectBestSupporter(RelState state, Condition supportedFact, List<GroundAction> supporters);

}

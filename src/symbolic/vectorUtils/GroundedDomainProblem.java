package symbolic.vectorUtils;

import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;

public class GroundedDomainProblem {
	
	private EPddlProblem problem;
	private PddlDomain domain;
	
	public GroundedDomainProblem(String domainFile, String problemFile) {
		this.domain = new PddlDomain(domainFile);
		this.problem = new EPddlProblem(problemFile, this.domain.getConstants(), this.domain.types, this.domain);

		domain.substituteEqualityConditions();

		try {
			problem.transformGoal();
			problem.groundingActionProcessesConstraints();

			problem.simplifyAndSetupInit(false, false);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public EPddlProblem getGroundedProblem() {
		return this.problem;
	}
	
	public PddlDomain getGroundedDomain() {
		return this.domain;
	}
}

package utils;

import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;

public class DomainAnalysis {
	
	private PddlDomain domain;
	private EPddlProblem problem;
	
	public DomainAnalysis(String domain, String problem) {
		//Initialize PDDLDomain and problem file according to currently considered hypothesis
		this.domain = new PddlDomain(domain);
		this.problem = new EPddlProblem(
				problem,
				this.domain.getConstants(), this.domain.types, this.domain);

		this.domain.substituteEqualityConditions();

		try {
			this.problem.transformGoal();
			this.problem.groundingActionProcessesConstraints();

			this.problem.simplifyAndSetupInit(false, false);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	public int getNumberOfActions() {
		return this.problem.getActions().size();
	}
	
	public int getNumberOfFluents() {
		return this.problem.getActualFluents().size();
	}
	
	public int getNumberOfPredicates() {
		int count = 0;
		
		for(Object key : this.problem.getActualFluents().keySet()) {
			if(key instanceof Predicate) {
				count++;
			}
		}
		
		return count;
	}
	
	public static void main(String[] args) {
		String domainPath = "";
		String problemPath = "";
		
		DomainAnalysis analysis = new DomainAnalysis(domainPath, problemPath);
		System.out.println("Actions: " + analysis.getNumberOfActions());
		System.out.println("Predicates: " + analysis.getNumberOfPredicates());
		System.out.println("Fluents: " + analysis.getNumberOfFluents());
	}

}

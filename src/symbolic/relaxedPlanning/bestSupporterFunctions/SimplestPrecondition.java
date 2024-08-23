package symbolic.relaxedPlanning.bestSupporterFunctions;

import java.util.List;

import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.RelState;

public class SimplestPrecondition {


	public static GroundAction selectBestSupporter(RelState state, Condition supportedFact, List<GroundAction> actions, List<GroundAction> supporters) {
		GroundAction simplestPrec = null;
				
		for(GroundAction supporter : supporters) {
			if(simplestPrec == null || simplestPrec.getPreconditions().getTerminalConditions().size() > supporter.getPreconditions().getTerminalConditions().size()) {
				simplestPrec = supporter;
			}
		}
		
		return simplestPrec;
	}
	
	public static List<GroundAction> selectBestSupporterSet(RelState state, Condition supportedFact, List<GroundAction> actions, List<List<GroundAction>> supporterSets) {
		int index = -1;
		int minScore = Integer.MAX_VALUE;
		int score;
		
		for(int i=0; i < supporterSets.size(); i++) {
			score = SimplestPrecondition.computeSupporterSetScore(supporterSets.get(i));
			if(score < minScore) {
				minScore = score;
				index = i;
			}
		}
		
		return supporterSets.get(index);
	}
	
	public static int computeSupporterSetScore(List<GroundAction> supporterSet) {
		int score = 0;
		
		for(GroundAction action : supporterSet) {
			score += SimplestPrecondition.computeSupporterScore(action);
		}
		
		return score;
	}
	
	public static int computeSupporterScore(GroundAction supporter) {
		return supporter.getPreconditions().getTerminalConditions().size();
	}

}

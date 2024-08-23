package symbolic.relaxedPlanning.bestSupporterFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.RelState;

import symbolic.relaxedPlanning.utils.RPG;

public class HAdd {

	public static GroundAction selectBestSupporter(RelState state, Condition supportedFact, List<GroundAction> actions, List<GroundAction> supporters) {
		return null;
	}
	
	public static List<GroundAction> selectBestSupporterSet(RelState state, Condition supportedFact, List<GroundAction> actions, RPG rpg, List<List<GroundAction>> supporterSets) {
		int minHAdd = Integer.MAX_VALUE;
		int minSupporterSetIndex = -1;
		
		int hAdd;
		for(List<GroundAction> supporterSet : supporterSets) {
			hAdd = HAdd.calculateHAdd(supporterSet, rpg);
			if(hAdd < minHAdd) {
				minHAdd = hAdd;
				minSupporterSetIndex = supporterSets.indexOf(supporterSet);
			}
		}
		
		return supporterSets.get(minSupporterSetIndex);
	}
	
	public static int calculateHAdd(List<GroundAction> actions, RPG rpg) {
		int hAdd = 0;
		for(GroundAction action : actions) {
			hAdd += HAdd.calculateHAdd(action, rpg);
		}
		
		return hAdd;
	}
	
	public static int calculateHAdd(GroundAction action, RPG rpg) {
		int hAdd = 0;
		
		//Find maximum RPG level of all facts that are part of the preconditions of the examined action
		int rpgLevel;
		int maxRPGLevel = 0;
		for(Condition terminal : action.getPreconditions().getTerminalConditions()) {
			if(terminal instanceof Predicate) {
				rpgLevel = HAdd.calculateRelPlanLevel((Predicate)terminal, rpg);
				if(rpgLevel > maxRPGLevel) {
					maxRPGLevel = rpgLevel;
				}
			}
		}
		
		//Calculate the HAdd scores for all facts that are part of actions that have a level <= the previously determined maximum level in the RPG.
		Map<Predicate, Integer> HAddScores = HAdd.calculateHAddScores(maxRPGLevel, rpg);
		
		//Calculate HAdd score for the examined action on the basis of the previously calculated fact scores.
		for(Condition terminal : action.getPreconditions().getTerminalConditions()) {
			if(terminal instanceof Predicate) {
				if(HAddScores.get((Predicate)terminal) != null) {
					hAdd += HAddScores.get((Predicate)terminal);
				}
			}
		}
		
		return hAdd;
	}
	
	public static int calculateRelPlanLevel(Predicate p, RPG rpg) {
		int planLevel = -1;
		for(int i=0; i < rpg.levels; i++) {
			if(rpg.rel_state_level.get(i).canBeTrue(p)) {
				planLevel = i;
				break;
			}
		}
		
		return planLevel;
	}
	
	public static Map<Predicate, Integer> calculateHAddScores(int maxLevel, RPG rpg) {
		Map<Predicate, Integer> HAddScores = new HashMap<Predicate, Integer>();
		
		int HAddScore;
		for(int i=0; i <= maxLevel; i++)  {
			for(GroundAction a : rpg.action_level.get(i)) {
				HAddScore = 0;
				//Loop through all actions that are part of the ith level of the RPG.
				for(Condition terminal : a.getPreconditions().getTerminalConditions()) {
					if(terminal instanceof Predicate) {
						//If we see a fact for the first time
						if(i == 0 || HAddScores.get((Predicate)terminal) == null) {
							HAddScores.put((Predicate)terminal, 0);
						}
						//If we already have determined a score for the currently checked fact (terminal)
						else {
							HAddScore += HAddScores.get((Predicate)terminal);
						}
					}
				}
				//Add action costs to the HAdd score.
				HAddScore++;
				
				//Calculate HAdd scores for all facts that are part of the effects of the currently examined action.
				for(Condition terminal : a.getAddList().getTerminalConditions()) {
					if(terminal instanceof Predicate) {
						if(HAddScores.get((Predicate)terminal) == null || HAddScore < HAddScores.get((Predicate)terminal)) {
							HAddScores.put((Predicate)terminal, HAddScore);
						}
					}
				}
			}
		}
		
		return HAddScores;
	}
	

}

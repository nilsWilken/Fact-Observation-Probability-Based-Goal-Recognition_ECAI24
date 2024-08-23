package symbolic.relaxedPlanning.bestSupporterFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.RelState;

import symbolic.relaxedPlanning.utils.RPG;

public class HMax {

	public static GroundAction selectBestSupporter(RelState state, Condition supportedFact, List<GroundAction> actions, List<GroundAction> supporters) {
		return null;
	}
	
	public static List<GroundAction> selectBestSupporterSet(RelState state, Condition supportedFact, List<GroundAction> actions, RPG rpg, List<List<GroundAction>> supporterSets) {
		int minHMax = Integer.MAX_VALUE;
		int minSupporterSetIndex = -1;
		
		int hMax;
		for(List<GroundAction> supporterSet : supporterSets) {
			hMax = HMax.calculateHMax(supporterSet, rpg);
			if(hMax < minHMax) {
				minHMax = hMax;
				minSupporterSetIndex = supporterSets.indexOf(supporterSet);
			}
		}
		
		return supporterSets.get(minSupporterSetIndex);
	}
	
	public static int calculateHMax(List<GroundAction> actions, RPG rpg) {
		int hMax = 0;
		for(GroundAction action : actions) {
			hMax += HMax.calculateHMax(action, rpg);
		}
		return hMax;
	}
	
	public static int calculateHMax(GroundAction action, RPG rpg) {
		int hMax = -1;
		
		int rpgLevel;
		int maxRPGLevel = 0;
		for(Condition c : action.getPreconditions().getTerminalConditions()) {
			if(c instanceof Predicate) {
				rpgLevel = HAdd.calculateRelPlanLevel((Predicate)c, rpg);
				if(rpgLevel > maxRPGLevel) {
					maxRPGLevel = rpgLevel;
				}
			}
		}
		Map<Predicate, Integer> HMaxScores = HMax.calculateHMaxScores(maxRPGLevel, rpg);
		
		for(Condition terminal : action.getPreconditions().getTerminalConditions()) {
			if(terminal instanceof Predicate) {
				if(HMaxScores.get((Predicate)terminal) != null && HMaxScores.get((Predicate)terminal) > hMax) {
					hMax = HMaxScores.get((Predicate)terminal);
				}
			}
		}
		
		return hMax;
	}
	
	public static Map<Predicate, Integer> calculateHMaxScores(int maxLevel, RPG rpg) {
		Map<Predicate, Integer> HMaxScores = new HashMap<Predicate, Integer>();
		
		int HMaxScore;
		for(int i=0; i <= maxLevel; i++) {
			for(GroundAction a : rpg.action_level.get(i)) {
				HMaxScore = 0;
				for(Condition terminal : a.getPreconditions().getTerminalConditions()) {
					if(terminal instanceof Predicate) {
						if(i == 0 || HMaxScores.get((Predicate)terminal) == null) {
							HMaxScores.put((Predicate)terminal, 0);
						}
						else {
							if(HMaxScores.get((Predicate)terminal) > HMaxScore) {
								HMaxScore = HMaxScores.get((Predicate)terminal);
							}
						}
					}
				}
				HMaxScore++;
				for(Condition terminal : a.getAddList().getTerminalConditions()) {
					if(terminal instanceof Predicate) {
						if(HMaxScores.get((Predicate)terminal) == null || HMaxScore < HMaxScores.get((Predicate)terminal)) {
							HMaxScores.put((Predicate)terminal, HMaxScore);
						}
					}
				}
			}
		}
		
		return HMaxScores;
	}
}

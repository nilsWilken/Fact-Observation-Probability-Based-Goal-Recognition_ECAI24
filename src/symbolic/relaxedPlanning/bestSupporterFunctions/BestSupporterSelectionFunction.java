package symbolic.relaxedPlanning.bestSupporterFunctions;

import java.util.List;

import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.RelState;

import symbolic.relaxedPlanning.utils.RPG;

public enum BestSupporterSelectionFunction {
	
	SIMPLEST_PRECONDITIONS,
	H_ADD,
	H_MAX,
	GREEDY;
	
	public static BestSupporterSelectionFunction parseFromString(String function) {
		switch(function.toLowerCase()) {
			case "simplest_preconditions":
				return SIMPLEST_PRECONDITIONS;
			case "h_max":
				return H_MAX;
			case "h_add":
				return H_ADD;
			case "greedy":
				return GREEDY;
			default:
				return H_MAX;
		}
	}

	public static GroundAction selectBestSupporter(BestSupporterSelectionFunction selectorType, Condition supportedCondition, RelState initialState, List<GroundAction> actions, List<GroundAction> supporters) {
		switch(selectorType) {
		case H_ADD:
			return HAdd.selectBestSupporter(initialState, supportedCondition, actions, supporters);
		case H_MAX:
			return HMax.selectBestSupporter(initialState, supportedCondition, actions, supporters);
		case SIMPLEST_PRECONDITIONS:
			return SimplestPrecondition.selectBestSupporter(initialState, supportedCondition, actions, supporters);
		default:
			return null;
		
		}
	}
	
	public static List<GroundAction> selectBestSupporterSet(BestSupporterSelectionFunction selectorType, Condition supportedCondition, RelState initialState, List<GroundAction> actions, List<List<GroundAction>> supporterSets) {
		switch(selectorType) {
		case SIMPLEST_PRECONDITIONS:
			return SimplestPrecondition.selectBestSupporterSet(initialState, supportedCondition, actions, supporterSets);
		default:
			return null;
		
		}
	}
	
	public static List<GroundAction> selectBestSupporterSet(BestSupporterSelectionFunction selectorType, Condition supportedCondition, RelState initialState, List<GroundAction> actions, RPG rpg, List<List<GroundAction>> supporterSets) {
		switch(selectorType) {
		case H_ADD:
			return HAdd.selectBestSupporterSet(initialState, supportedCondition, actions, rpg, supporterSets);
		case H_MAX:
			return HMax.selectBestSupporterSet(initialState, supportedCondition, actions, rpg, supporterSets);
		default:
			return null;
		}
	}
	
	public static int calculateHeuristicScore(BestSupporterSelectionFunction selectorType, Condition supportedCondition, RelState initialState, List<GroundAction> action, RPG rpg, GroundAction supporterAction) {
		switch(selectorType) {
		case H_ADD:
			return HAdd.calculateHAdd(supporterAction, rpg);
		case H_MAX:
			return HMax.calculateHMax(supporterAction, rpg);
		case SIMPLEST_PRECONDITIONS:
			return SimplestPrecondition.computeSupporterScore(supporterAction);
		default:
			return -1;
		}
	}
	
	public static int calculateHeuristicScore(BestSupporterSelectionFunction selectorType, Condition supportedCondition, RelState initialState, List<GroundAction> action, RPG rpg, List<GroundAction> supporterActions) {
		switch(selectorType) {
		case H_ADD:
			return HAdd.calculateHAdd(supporterActions, rpg);
		case H_MAX:
			return HMax.calculateHMax(supporterActions, rpg);
		case SIMPLEST_PRECONDITIONS:
			return SimplestPrecondition.computeSupporterSetScore(supporterActions);
		default:
			return -1;
		}
	}

}

package symbolic.relaxedPlanning.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hstairs.ppmajal.problem.GroundAction;

public class BaseActionCombinationIterator {
	
	private ActionCombination base;
	private ActionCombination cActionCombination;
	private int combinationSize;
	private Map<Integer, List<GroundAction>> actionHeuristicScores;
	private int cHeuristicScoreIndex;
	private int cListIndex;
	private List<Integer> heuristicScores;
	private boolean possibleActionsEmpty;
	
	
	public BaseActionCombinationIterator(ActionCombination base, int combinationSize, Map<Integer, List<GroundAction>> actionHeuristicScores) {
		this.base = base;
		this.cActionCombination = new ActionCombination(this.base.getActions());
		this.combinationSize = combinationSize;
		this.actionHeuristicScores = actionHeuristicScores;
		
		this.possibleActionsEmpty = false;
		
		this.heuristicScores = new ArrayList<Integer>();
		for(int key : this.actionHeuristicScores.keySet()) {
			this.heuristicScores.add(key);
		}
	}
	
	public void produceNextActionCombination() {
		System.out.println(this + " BaseActions: " + this.base.getActions().size());
		if(this.base.getActions().size() == 0) {
			System.exit(0);
		}
		List<GroundAction> cActions = this.base.getActions();
		List<GroundAction> possibleActions;
		
		if(this.cHeuristicScoreIndex < this.heuristicScores.size()) {
			possibleActions = this.actionHeuristicScores.get(this.heuristicScores.get(this.cHeuristicScoreIndex));
		}
		else {
			possibleActions = new ArrayList<GroundAction>();
		}
		
		if(possibleActions.size() == this.cListIndex) {
			this.cHeuristicScoreIndex += 1;
			this.cListIndex = 0;
		}
		
		if(this.cHeuristicScoreIndex < this.heuristicScores.size()) {
			possibleActions = this.actionHeuristicScores.get(this.heuristicScores.get(this.cHeuristicScoreIndex));
		}
		
		if(possibleActions.size() == 0) {
			this.possibleActionsEmpty = true;
		}
		
		System.out.println("PossibleActionsSize: " + possibleActions.size());
		
		while(this.cListIndex < possibleActions.size()) {
			System.out.println(this.cHeuristicScoreIndex + " " + this.combinationSize);
			if(!cActions.contains(possibleActions.get(this.cListIndex))) {
				cActions.add(possibleActions.get(this.cListIndex));
			}
			
			this.cListIndex++;
			if(cActions.size() == this.combinationSize) {
				break;
			}
			
			if(this.cListIndex == possibleActions.size()) {
				this.cHeuristicScoreIndex += 1;
				this.cListIndex = 0;
				if(this.cHeuristicScoreIndex >= this.heuristicScores.size()) {
					break;
				}
				possibleActions = this.actionHeuristicScores.get(this.heuristicScores.get(this.cHeuristicScoreIndex));
				if(possibleActions == null) {
					break;
				}
			}
			
			if(cActions.size() == this.combinationSize) {
				this.cActionCombination = new ActionCombination(cActions);
			}
			else {
				this.cActionCombination = null;
			}
		}
		
		System.out.println("CActionsSize: " + cActions.size());
		this.cActionCombination = new ActionCombination(cActions);		
	}
	
	public ActionCombination getCurrentActionCombination() {
		return this.cActionCombination;
	}
	
	public boolean isPossibleActionsEmpty() {
		return this.possibleActionsEmpty;
	}

}

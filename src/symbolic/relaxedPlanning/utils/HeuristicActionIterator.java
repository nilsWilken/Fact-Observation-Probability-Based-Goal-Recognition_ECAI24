package symbolic.relaxedPlanning.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.hstairs.ppmajal.problem.GroundAction;

public class HeuristicActionIterator implements Iterator<ActionCombination> {

	private Map<Integer, List<GroundAction>> actionsWithHeuristicScores;
	private int maxReturnSetSize;
	private Map<Integer, List<ActionCombination>> combQueue;
	
	
	public HeuristicActionIterator(Map<Integer, List<GroundAction>> actionsWithHeuristicScores, int maxReturnSetSize) {
		this.actionsWithHeuristicScores = actionsWithHeuristicScores;
		this.maxReturnSetSize = maxReturnSetSize;		
		
		this.initiCombQueue();
	}
	
	@Override
	public boolean hasNext() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ActionCombination next() {
		// TODO Auto-generated method stub
		return null;
	}
	
	private void initiCombQueue() {
		this.combQueue = new TreeMap<Integer, List<ActionCombination>>();
		
		List<Integer> keys = new ArrayList<Integer>();
		for(int key : this.actionsWithHeuristicScores.keySet()) {
			keys.add(key);
		}
		
		ActionCombination cComb;
		List<ActionCombination> combs;
		List<GroundAction> actionsI;
		List<GroundAction> actionsJ;
		int hScore;
		for(int i=0; i < keys.size()-1; i++) {
			for(int j=i+1; j < keys.size(); j++) {
				hScore = 0;
				hScore += keys.get(i);
				hScore += keys.get(j);
				
				combs = this.combQueue.get(hScore) ;
				if(combs == null) {
					combs = new ArrayList<ActionCombination>();
					this.combQueue.put(hScore, combs);
				}
				
				actionsI = this.actionsWithHeuristicScores.get(keys.get(i));
				actionsJ = this.actionsWithHeuristicScores.get(keys.get(j));
				
				for(GroundAction aI : actionsI) {
					for(GroundAction aJ : actionsJ) {
						List<GroundAction> combActions = new ArrayList<GroundAction>();
						combActions.add(aI);
						combActions.add(aJ);
						cComb = new ActionCombination(combActions);
						combs.add(cComb);
					}
				}
			}
		}
		
	}

}

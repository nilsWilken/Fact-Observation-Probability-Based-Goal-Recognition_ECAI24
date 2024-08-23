package symbolic.relaxedPlanning.utils;

import java.util.ArrayList;
import java.util.List;

import com.hstairs.ppmajal.problem.GroundAction;

public class ActionCombination {
	
	private List<GroundAction> actions;
	
	public ActionCombination(List<GroundAction> actions) {
		this.actions = actions;
	}

	public List<GroundAction> getActions() {
		List<GroundAction> ret = new ArrayList<GroundAction>();
		for(GroundAction a : this.actions)  {
			ret.add(a);
		}
		return ret;
	}
	
	@Override
	public boolean equals(Object o2) {
		if(!(o2 instanceof ActionCombination)) {
			return false;
		}
		
		List<GroundAction> actions = ((ActionCombination)o2).getActions();
		if(actions.size() != this.actions.size()) {
			return false;
		}
		
		for(int i=0; i < this.actions.size(); i++) {
			if(!actions.contains(this.actions.get(i))) {
				return false;
			}
		}
		
		return true;
	}

}

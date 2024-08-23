package symbolic.planning.planningUtils;

public class ConditionalEffectWrapper {
	
	private String precondition;
	private String effect;
	
	public ConditionalEffectWrapper(String precondition, String effect) {
		this.precondition = precondition;
		this.effect = effect;
	}
	
	public String getPrecondition() {
		return this.precondition;
	}
	
	public String getEffect() {
		return this.effect;
	}
	
	public String getCondEffectString() {
		StringBuffer condEffect = new StringBuffer();
		condEffect.append("(when " + this.precondition + " " + this.effect + ")");
		return condEffect.toString();
	}

}

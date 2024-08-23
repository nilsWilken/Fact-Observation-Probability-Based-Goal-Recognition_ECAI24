package symbolic.planning.planningUtils;

import java.util.ArrayList;
import java.util.List;

public class GroundedTextActionWrapper {
	
	private String actionString;
	private String header;
	private String parameters;
	private String preconditions;
	private String effect;
	
	public GroundedTextActionWrapper(String actionGroupString) {
		this.actionString = actionGroupString;
		this.parseActionElementsFromString();
	}
	
	public String getParameters() {
		return this.parameters;
	}
	
	public String getPreconditions() {
		return this.preconditions;
	}
	
	public String getEffect() {
		return this.effect;
	}
	
	public String getHeader() {
		return this.header;
	}
	
	public String getName() {
		return this.header.split(":action")[1].trim().toLowerCase();
	}
	
	protected String getActionString() {
		return this.actionString;
	}
	
	public void adjustName(String adjustedName) {
		this.header = "(:action " + adjustedName;
	}
	
	public String toString() {
		StringBuffer buff = new StringBuffer();
		if(this.header != null) {
			buff.append(this.header + "\n");
		}
		
		if(this.parameters != null) {
			buff.append(this.parameters + "\n");
		}
		
		if(this.preconditions != null) {
			buff.append(this.preconditions + "\n");
		}
		
		if(this.effect != null) {
			buff.append(this.effect + ")\n");
		}
		
		return buff.toString();
	}
	
	public void addEffect(String additionalEffect) {
		List<String> additionalEffects = new ArrayList<String>();
		additionalEffects.add(additionalEffect);
		this.addEffects(additionalEffects);
	}
	
	public void addEffects(List<String> additionalEffects) {
		String existingEffect = this.effect.split(":effect")[1];
		
		StringBuffer adjustedEffect = new StringBuffer();
		adjustedEffect.append(":effect (and ");
		
		for(String additionalEffect : additionalEffects) {
			adjustedEffect.append(additionalEffect + " ");
		}
		
		adjustedEffect.append(existingEffect.replaceFirst("\\(and", ""));
		
		this.effect = adjustedEffect.toString();
	}
	
	public void addConditionalEffect(ConditionalEffectWrapper additionalConditionalEffect) {
		List<ConditionalEffectWrapper> additionalEffects = new ArrayList<ConditionalEffectWrapper>();
		additionalEffects.add(additionalConditionalEffect);
		this.addConditionalEffects(additionalEffects);
	}
	
	public void addConditionalEffects(List<ConditionalEffectWrapper> additionalConditionalEffects) {
		String existingEffect = this.effect.split(":effect")[1];
		
		StringBuffer adjustedEffect = new StringBuffer();
		adjustedEffect.append(":effect (and ");
		
		for(ConditionalEffectWrapper condEff : additionalConditionalEffects) {
			adjustedEffect.append(condEff.getCondEffectString() + " ");
		}
		adjustedEffect.append(existingEffect.replaceFirst("\\(and", ""));
		
		this.effect = adjustedEffect.toString();
	}
	
	public void addPrecondition(String additionalPrecondition) {
		List<String> additionalPreconditions = new ArrayList<String>();
		additionalPreconditions.add(additionalPrecondition);
		this.addPreconditions(additionalPreconditions);
	}
	
	public void addPreconditions(List<String> additionalPreconditions) {
		String existingPreconditions = this.preconditions.split(":precondition")[1];
		
		StringBuffer adjustedPreconditions = new StringBuffer();
		adjustedPreconditions.append(":precondition (and ");
		
		for(String additionalPrecondition : additionalPreconditions) {
			adjustedPreconditions.append(additionalPrecondition + " ");
		}
		adjustedPreconditions.append(existingPreconditions + ")");
		
		this.preconditions = adjustedPreconditions.toString();
	}
	
	private void parseActionElementsFromString() {
		this.parseHeaderFromString();
		this.parseParametersFromString();
		this.parsePreconditionsFromString();
		this.parseEffectFromString();
	}
	
	private void parseHeaderFromString() {
		int end = this.actionString.indexOf(":parameters");
		this.header = this.actionString.substring(0, end).trim();
	}
	
	private void parseParametersFromString() {
		int start = this.actionString.indexOf(":parameters");
		this.parameters = this.parseGroupFromStartIndex(start);
	}
	
	private void parsePreconditionsFromString() {
		int start = this.actionString.indexOf(":precondition");
		this.preconditions = this.parseGroupFromStartIndex(start);
	}
	
	private void parseEffectFromString() {
		int start = this.actionString.indexOf(":effect");
		this.effect = this.parseGroupFromStartIndex(start);
	}
	
	private String parseGroupFromStartIndex(int start) {
		int openBracketCounter = -1;
		StringBuffer group = new StringBuffer();
		char c;
		for(int i=start; i < this.actionString.length(); i++) {
			c = this.actionString.charAt(i);
			
			group.append(c);
			if(c == '(') {
				if(openBracketCounter == -1) {
					openBracketCounter = 0;
				}
				openBracketCounter++;
			} else if(c == ')') {
				openBracketCounter--;
			}
			if(openBracketCounter == 0) {
				break;
			}
		}
		
		return group.toString();
	}

}

package symbolic.planning.planningUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroundedDomainWrapper {
		
	private String domainString = "";
	private String domainName;
	private String requirements;
	private String types;
	private String predicates;
	private String constants;
	private String functions;
	private Map<String, GroundedTextActionWrapper> actions;
	private List<String> addedPredicates;
	
	public GroundedDomainWrapper(File groundedDomainFile) {
		System.out.println(groundedDomainFile.getAbsolutePath());
		
		this.actions = new HashMap<String, GroundedTextActionWrapper>();
		this.addedPredicates = new ArrayList<String>();
		
		this.parseGroundedDomainFile(groundedDomainFile);
	}
	
	public Map<String, GroundedTextActionWrapper> getActions() {
		return this.actions;
	}
	
	public String getDomainName() {
		return this.domainName;
	}
	
	public String getRequirements() {
		return this.requirements;
	}
	
	public String getTypes() {
		return this.types;
	}
	
	public String getPredicates() {
		return this.predicates;
	}
	
	public String getConstants() {
		return this.constants;
	}
	
	public String getFunctions() {
		return this.functions;
	}
	
	public void copyAction(String copyActionName, String newActionName) {
		GroundedTextActionWrapper action = this.getActionByName(copyActionName);
		GroundedTextActionWrapper copy = new GroundedTextActionWrapper(action.getActionString());
		
		copy.adjustName(newActionName);
		
		this.actions.put(copy.getName(), copy);
	}
	
	public void adjustActionName(String currentName, String newName) {
		this.getActionByName(currentName).adjustName(newName);
	}
	
	public void addActionEffect(String actionName, String additionalEffect) {
		this.getActionByName(actionName).addEffect(additionalEffect);
		this.checkNewPredicate(additionalEffect);
	}
	
	public void addActionEffects(String actionName, List<String> additionalEffects) {
		this.getActionByName(actionName).addEffects(additionalEffects);
		this.checkNewPredicates(additionalEffects);
	}
	
	public void addActionConditionalEffect(String actionName, ConditionalEffectWrapper additionalEffect) {
		System.out.println(actionName);
		this.getActionByName(actionName).addConditionalEffect(additionalEffect);
		this.checkNewPredicate(additionalEffect.getEffect());
	}
	
	public void addActionConditionalEffects(String actionName, List<ConditionalEffectWrapper> additionalEffects) {
		this.getActionByName(actionName).addConditionalEffects(additionalEffects);
		List<String> newPredicates = new ArrayList<String>();
		
		for(ConditionalEffectWrapper condEff : additionalEffects) {
			newPredicates.add(condEff.getEffect());
		}
		
		this.checkNewPredicates(newPredicates);
	}
	
	public void addActionPrecondition(String actionName, String additionalPrecondition) {
		this.getActionByName(actionName).addPrecondition(additionalPrecondition);
		this.checkNewPredicate(additionalPrecondition);
	}
	
	public void addActionPreconditions(String actionName, List<String> additionalPreconditions) {
		this.getActionByName(actionName).addPreconditions(additionalPreconditions);
		this.checkNewPredicates(additionalPreconditions);
	}
	
	public String toString() {
		StringBuffer buff = new StringBuffer();
		
		if(this.domainName != null) {
			buff.append("(define " + this.domainName + "\n");
		}
		
		if(this.requirements != null) {
			buff.append(this.requirements + "\n");
		}
		
		if(this.types != null) {
			buff.append(this.types + "\n");
		}
		
		if(this.constants != null) {
			buff.append(this.constants + "\n");
		}
		
		if(this.predicates != null) {
			buff.append(this.predicates + "\n");
		}
		
		if(this.functions != null) {
			buff.append(this.functions + "\n");
		}
		
		if(this.actions.size() > 0) {
			for(GroundedTextActionWrapper a : this.actions.values()) {
				buff.append(a.toString() + "\n");
			}
		}
		buff.append(")");
		
		return buff.toString();
	}
	
	public void writeToFile(File file) {
		try {
			Files.writeString(Paths.get(file.getAbsolutePath()), this.toString(), new OpenOption[0]);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private GroundedTextActionWrapper getActionByName(String actionName) {
		return this.actions.get(actionName.toLowerCase());
	}
	
	private void checkNewPredicate(String additionalPredicate) {
		if(!this.addedPredicates.contains(additionalPredicate)) {
			this.addPredicate(additionalPredicate);
		}
	}
	
	private void checkNewPredicates(List<String> additionalPredicates) {
		for(String newPredicate : additionalPredicates) {
			if(!this.addedPredicates.contains(newPredicate)) {
				this.addPredicate(newPredicate);
			}
		}
	}
	
	private void addPredicate(String additionalPredicate) {
		String existingPredicates = this.predicates.split(":predicates")[1];
		StringBuffer adjustedPredicates = new StringBuffer();
		
		adjustedPredicates.append("(:predicates ");
		adjustedPredicates.append(additionalPredicate);
		adjustedPredicates.append(existingPredicates);
		
		this.predicates = adjustedPredicates.toString();
		this.addedPredicates.add(additionalPredicate);
	}
	
	
	private void parseGroundedDomainFile(File groundedDomainFile) {
		StringBuffer buff = new StringBuffer();
		
		try {
			for(String line : Files.readAllLines(Paths.get(groundedDomainFile.getAbsolutePath()))) {
				buff.append(line + "\n");
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
		this.domainString = buff.toString();
	
		this.parseElementsFromDomainString();
	}
	
	private void parseElementsFromDomainString() {
		StringBuffer buff = new StringBuffer();
		
		char c;
		boolean firstBracketPassed = false;
		boolean currentlyInGroup = false;
		int openBrackCounter = 0;
		for(int i=0; i < this.domainString.length(); i++) {
			c = this.domainString.charAt(i);
			if(!firstBracketPassed && !(c == '(')) {
				continue;
			}
			else if(!firstBracketPassed && c == '(') {
				firstBracketPassed = true;
				continue;
			}
			
			if(c == '(' && !currentlyInGroup) {
				buff = new StringBuffer();
				buff.append(c);
				openBrackCounter = 1;
				currentlyInGroup = true;
			} else if(c == '(' && currentlyInGroup) {
				buff.append(c);
				openBrackCounter++;
			} else if(c == ')') {
				openBrackCounter--;
				buff.append(c);
				if(openBrackCounter == 0) {
					currentlyInGroup = false;
					this.parseCurrentGroupString(buff.toString());
				}
			} else {
				buff.append(c);
			}
				
			
		}
	}
	
	private void parseCurrentGroupString(String groupString) {
		if(groupString.startsWith("(domain")) {
			this.domainName = groupString;
		} else if(groupString.contains(":requirements")) {
			this.requirements = groupString;
		} else if(groupString.contains(":predicates")) {
			this.predicates = groupString;
		} else if(groupString.contains(":constants")) {
			this.constants = groupString;
		} else if(groupString.contains(":functions")) {
			this.functions = groupString;
		} else if(groupString.contains("types")) {
			this.types = groupString;
		}
		else if(groupString.contains(":action")) {
			GroundedTextActionWrapper a = new GroundedTextActionWrapper(groupString);
			this.actions.put(a.getName(), a);
		}
	}

}

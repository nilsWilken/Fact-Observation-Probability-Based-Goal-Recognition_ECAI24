package symbolic.planning.planningUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PDDLInitialStateWrapper {
	
	private List<String> fluents;
	
	public PDDLInitialStateWrapper(File templateFile) {
		this.fluents = new ArrayList<String>();
		this.parseFluentsFromTemplateFile(templateFile);
	}
	
	public List<String> getFluents() {
		return this.fluents;
	}
	
	public boolean equals(Object initState2) {
		List<String> fluents2 = ((PDDLInitialStateWrapper)initState2).getFluents();
		
		if(fluents2.size() != this.fluents.size()) {
			return false;
		}
		for(String fluent : this.fluents) {
			if(!fluents2.contains(fluent)) {
				return false;
			}
		}
		
		return true;
	}
	
	public String toString() {
		StringBuffer out = new StringBuffer();
		
		out.append("\n(:init");
		
		for(String fluent : this.fluents) {
			out.append("\n" + fluent);
		}
		
		out.append("\n)");
		
		return out.toString();
	}
	
	private void parseFluentsFromTemplateFile(File templateFile) {
		String templateString = this.parseProblemTemplateFile(templateFile);
		
		int startIndex = templateString.indexOf("(:init");
		int cIndex = startIndex;
		int openCounter = 0;
		int closeCounter = 0;
		
		StringBuffer cFluent = new StringBuffer();
		do {
			if(templateString.charAt(cIndex) == '(') {
				openCounter++;
			}
			else if(templateString.charAt(cIndex) == ')') {
				closeCounter++;
			}
			
			if((openCounter - closeCounter) > 1) {
				cFluent.append(templateString.charAt(cIndex));
			}
			else {
				if(templateString.charAt(cIndex) == ')' && cFluent.length() > 0) {
					cFluent.append(")");
					this.fluents.add(cFluent.toString());
					cFluent = new StringBuffer();
				}
			}
			cIndex++;
		}while(openCounter > closeCounter);
	}
	
	private String parseProblemTemplateFile(File templateFile) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(templateFile));
			String line;
			StringBuffer problemString = new StringBuffer();
			
			while((line=in.readLine()) != null) {
				problemString.append(line + "\n");
			}
			
			in.close();
			
			return problemString.toString();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return "";
	}

}

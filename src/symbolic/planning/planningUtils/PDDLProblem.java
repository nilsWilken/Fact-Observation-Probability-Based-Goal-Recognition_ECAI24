package symbolic.planning.planningUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class PDDLProblem {
	
private StringBuffer problemString = new StringBuffer();
	
	public PDDLProblem(File problemFile) {
		this.parseProblemFile(problemFile);
	}
	
	public void generateProblemFileWithNewInitState(String initString, String outputPath) {
		String nProblemString = this.problemString.toString();
		
		int startIndex = nProblemString.indexOf("(:init");
		int cIndex = startIndex;
		int openCounter = 0;
		int closeCounter = 0;
		do {
			if(nProblemString.charAt(cIndex) == '(') {
				openCounter++;
			}
			else if(nProblemString.charAt(cIndex) == ')') {
				closeCounter++;
			}
			cIndex++;
		}while(openCounter > closeCounter);
		
		nProblemString = nProblemString.replace(nProblemString.substring(startIndex, cIndex), "(:init\n" + initString + ")");
		
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(outputPath)));
			out.write(nProblemString);
			out.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private void parseProblemFile(File templateFile) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(templateFile));
			String line;
			
			while((line=in.readLine()) != null) {
				this.problemString.append(line + "\n");
			}
			
			in.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

}

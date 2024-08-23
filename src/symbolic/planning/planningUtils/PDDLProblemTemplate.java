package symbolic.planning.planningUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import config.GoalRecognitionConfiguration;

public class PDDLProblemTemplate {
	
	private String problemTemplateString = "";
	
	public PDDLProblemTemplate(File templateFile) {
		this.parseTemplateFile(templateFile);
	}
	
	
	public String generateInitialProblemFile(String goalString, String outDirPath, String expName, int hypCounter) {
		File outDir = new File(outDirPath);
		try {
			if(!outDir.exists()) {
				outDir.mkdir();
			}
			BufferedWriter out = new BufferedWriter(new FileWriter(new File(Paths.get(outDirPath, GoalRecognitionConfiguration.generateInitialProblemFileName(expName, hypCounter)).toString())));
			out.write(this.problemTemplateString.replace("<HYPOTHESIS>", goalString));
			out.close();
			return Paths.get(outDirPath, GoalRecognitionConfiguration.generateInitialProblemFileName(expName, hypCounter)).toString();
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public File generateProblemFile(String goalString, File outFile) {
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outFile));
			out.write(this.problemTemplateString.replace("<HYPOTHESIS>", goalString));
			out.close();
			return outFile;
		}catch(IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void parseTemplateFile(File templateFile) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(templateFile));
			String line;
			
			while((line=in.readLine()) != null) {
				this.problemTemplateString += line + "\n";
			}
			
			in.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

}

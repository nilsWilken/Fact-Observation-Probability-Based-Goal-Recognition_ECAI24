package symbolic.planning.stateHandler.obsConversion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.PDDLState;

import config.GoalRecognitionConfiguration;
import symbolic.planning.stateHandler.AbstractStateHandler;
import utils.ObsUtils;

public class GMPDDLStateHandler extends AbstractStateHandler implements IObsConversionStateHandler {
	
	public GMPDDLStateHandler(String dFilePath, String pFilePath) {
		super(dFilePath, pFilePath);
	}
	
	public void processObsFile(String outputFileName, int hypCounter, String outputFilePath, String obsFilePath) {
		int counter = 0;
		PDDLState init = (PDDLState)this.ground.getInit().clone();
			
		for(String line : this.parseObsFile(obsFilePath)) {
			if(!line.equals("")) {
				this.advanceStateWithActionObs(line, init);
				this.writePDDLStateToFile(init, Paths.get(outputFilePath, GoalRecognitionConfiguration.generateObsProblemFileName(outputFileName, hypCounter, ++counter)).toString());
			}

		}
		System.out.println(counter + " lines processed!");
	}
	
	public EPddlProblem getProblem() {
		return super.ground;
	}
	
	public void processObsFile(List<String> outputFileNames, int hypCounter, List<String> initialProblemFiles, List<String> outputFilePaths, String obsFilePath) {
		int counter = 0;
		PDDLState init = (PDDLState)this.ground.getInit().clone();
		
		for(String line : this.parseObsFile(obsFilePath)) {
			if(!line.equals("")) {
				this.advanceStateWithActionObs(line, init);
				counter++;
				for(int i=0; i < outputFilePaths.size(); i++) {
					this.writePDDLStateToFile(init, initialProblemFiles.get(i), Paths.get(outputFilePaths.get(i), GoalRecognitionConfiguration.generateObsProblemFileName(outputFileNames.get(i), i, counter)).toString());
				}
			}
		}
	}
	
	public List<GroundAction> retrieveActionListFromObsFile(String obsFilePath) {
		List<GroundAction> observedActions = new ArrayList<GroundAction>();
		
		for(String line : this.parseObsFile(obsFilePath)) {
			observedActions.addAll(this.retrieveActionsFromString(line));
		}
		
		return observedActions;
	}
	
	private List<String> parseObsFile(String obsFilePath) {
		try {
			List<String> rawLines = Files.readAllLines(Paths.get(obsFilePath));
			
			List<String> parsedLines = new ArrayList<String>();
			
			for(String rawLine : rawLines) {
				parsedLines.add(ObsUtils.parseObservationFileLine(rawLine).strip());
			}
			
			return parsedLines;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
}

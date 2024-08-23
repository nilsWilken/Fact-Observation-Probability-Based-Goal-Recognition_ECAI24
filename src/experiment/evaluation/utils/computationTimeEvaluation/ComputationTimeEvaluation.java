package experiment.evaluation.utils.computationTimeEvaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import config.ComputationTimeEvaluationConfiguration;
import config.ConfigurationManager;
import experiment.evaluation.utils.computationTimeEvaluation.computation.ComputationTimeComputeUtils;
import experiment.evaluation.utils.computationTimeEvaluation.print.ComputationTimePrinter;
import experiment.management.ExperimentType;
import utils.database.ExperimentResultDatabase;

public class ComputationTimeEvaluation {
	
	public static void main(String[] args) {
		if(args.length > 0) {
			try {
				System.out.println("Initialize configs");
				ConfigurationManager.initializeComputationTimeConfiguration(new File(args[0]));
			} catch(IOException e) {
				e.printStackTrace();
			}
			String pathToDBDirectory = ComputationTimeEvaluationConfiguration.RESULT_DIRECTORY_PATH;
			for(File f : new File(pathToDBDirectory).listFiles()) {
				if(!f.isDirectory()) {
					continue;
				}
				ComputationTimeEvaluation.computeAndPrintAverageComputationTimesForDir(f);

				System.out.println("\n\n\n");
			}
		}
		System.out.println("No proper evaluation path was given!");
	}

	public static void computeAndPrintAverageComputationTimesForDir(File dir) {
		ExperimentType expType = null;
		List<List<Map>> averageComputationTimes = new ArrayList<List<Map>>();
		for(File f : dir.listFiles()) {

			//Make sure to only consider database files
			if(!f.getName().endsWith(".db")) {
				continue;
			}

			ExperimentResultDatabase db = new ExperimentResultDatabase(f.getAbsolutePath());
			
			//If current experiment is the first one evaluated
			if(expType == null) {
				expType = db.getExperimentType();
			}

			//Compute computation time statistics
			averageComputationTimes.add(ComputationTimeComputeUtils.computeAverageComputationTimes(db));
		}

		//Print computation time statistics and current domain name
		System.out.println(dir.getName().toUpperCase());
		ComputationTimePrinter.printAverageComputationTimes(averageComputationTimes, expType);
	}
}

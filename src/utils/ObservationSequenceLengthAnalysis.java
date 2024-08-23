package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ObservationSequenceLengthAnalysis {
    private static Map<String, Map<Integer, Integer>> extractObservationSequenceLengths(DirectoryManager dirManager) {
        Map<String, Map<Integer, Integer>> obsLengths = new TreeMap<String, Map<Integer, Integer>>();

        Map<Integer, Integer> cObsLengths;
        String trueHyp;
        for(String expName : dirManager.getExperimentSetupDirectoryNames()) {
            trueHyp = dirManager.getTrueTextGoalForExperiment(expName);
            cObsLengths = obsLengths.get(trueHyp);

            if(cObsLengths == null) {
                cObsLengths = new TreeMap<Integer, Integer>();
                obsLengths.put(trueHyp, cObsLengths);
            }
            
            try {
                List<String> obsLines = Files.readAllLines(Paths.get(dirManager.getObsFile(expName).getAbsolutePath()));
                cObsLengths.put(Integer.parseInt(expName.split("_")[1]), obsLines.size());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }   

        return obsLengths;
    }

    private static void printObservationSequenceLengths(Map<String, Map<Integer, Integer>> obsLengths) {
        for(String trueHyp : obsLengths.keySet()) {
            System.out.println(trueHyp);
            for(int expNumber : obsLengths.get(trueHyp).keySet()) {
                System.out.println("\t" + expNumber + " " + obsLengths.get(trueHyp).get(expNumber));
            }
        }
    }

}
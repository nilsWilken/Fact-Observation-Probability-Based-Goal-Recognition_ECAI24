package experiment.evaluation.utils.computationTimeEvaluation.print;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class FactObservationProbabilityComputationTimePrinter {
    public static void printAverageComputationTimes(
            List<List<Map>> averageComputationTimes) {
        double factProbabilityCompTime = 0.0;
        Map<Double, Double> heuristicCompTime = new TreeMap<Double, Double>();
        double tmpFPVTime;

        for (List<Map> list : averageComputationTimes) {
            factProbabilityCompTime += (double) list.get(0).get("total");

            for (double key : ((Map<Double, Double>) list.get(1)).keySet()) {
                if (heuristicCompTime.get(key) == null) {
                    heuristicCompTime.put(key, 0.0);
                }

                tmpFPVTime = heuristicCompTime.get(key);
                tmpFPVTime += (double) list.get(1).get(key);
                heuristicCompTime.put(key, tmpFPVTime);
            }
        }

        factProbabilityCompTime = factProbabilityCompTime / (double) averageComputationTimes.size();
        System.out.println("Average fact probability computation time: " + factProbabilityCompTime);

        double totalAverage = 0.0;
        System.out.println("\nAverage heuristic computation time: ");
        for (double key : heuristicCompTime.keySet()) {
            tmpFPVTime = heuristicCompTime.get(key);
            tmpFPVTime = tmpFPVTime / (double) averageComputationTimes.size();
            totalAverage += tmpFPVTime;
            heuristicCompTime.put(key, tmpFPVTime);
            System.out.println(key + ": " + tmpFPVTime);
        }
        totalAverage = totalAverage / (double) heuristicCompTime.keySet().size();
        System.out.println("\nTotal average: " + totalAverage);
    }
}

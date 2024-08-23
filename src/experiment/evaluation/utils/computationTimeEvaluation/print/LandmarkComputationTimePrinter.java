package experiment.evaluation.utils.computationTimeEvaluation.print;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import experiment.evaluation.utils.computationTimeEvaluation.computation.ComputationTimeComputeUtils;

public class LandmarkComputationTimePrinter {
    public static void printAverageComputationTimes(List<List<Map>> averageComputationTimes) {
        double totalAverageExtractionTime = 0.0;
        Map<Double, Double> totalAverageHeuristicTimes = new TreeMap<Double, Double>();
        double tmpTime;
        for (List<Map> list : averageComputationTimes) {
            totalAverageExtractionTime += (double) list.get(0).get("total");
            for (double key : ((Map<Double, Double>) list.get(1)).keySet()) {
                if (totalAverageHeuristicTimes.get(key) == null) {
                    totalAverageHeuristicTimes.put(key, 0.0);
                }
                tmpTime = totalAverageHeuristicTimes.get(key);
                tmpTime += (double) list.get(1).get(key);
                totalAverageHeuristicTimes.put(key, tmpTime);
            }
        }

        totalAverageExtractionTime = totalAverageExtractionTime / (double) averageComputationTimes.size();
        System.out.println("Average extraction time: " + totalAverageExtractionTime);

        double totalAverage = 0.0;
        for (double key : totalAverageHeuristicTimes.keySet()) {
            tmpTime = totalAverageHeuristicTimes.get(key);
            tmpTime = tmpTime / (double) averageComputationTimes.size();
            totalAverage += tmpTime;
            totalAverageHeuristicTimes.put(key, tmpTime);
            System.out.println(key + ": " + tmpTime);
        }
        totalAverage = totalAverage / (double) totalAverageHeuristicTimes.keySet().size();
        System.out.println("\nTotal average: " + totalAverage);
    }

    public static void printLandmarkComputationTimes(Map<Integer, Double> averageLCompTimes, double totalLAverage,
            Map<Double, Double> averageHComputationTimes) {
        System.out.println("Landmark extraction time statistics: ");
        for (int hyp : averageLCompTimes.keySet()) {
            System.out.println("hyp: " + hyp + " " + ComputationTimeComputeUtils.round(averageLCompTimes.get(hyp), 2));
        }
        System.out.println("Total: " + ComputationTimeComputeUtils.round(totalLAverage, 2));

        System.out.println("\n\nHeuristic computation time statistics:");
        for (double perc : ComputationTimeComputeUtils.percKeysToList(averageHComputationTimes.keySet())) {
            System.out
                    .println(perc + ": " + ComputationTimeComputeUtils.round((averageHComputationTimes.get(perc)), 2));
        }
    }
}

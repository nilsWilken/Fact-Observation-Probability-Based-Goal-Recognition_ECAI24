package experiment.evaluation.utils.computationTimeEvaluation.print;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class VectorSimilarityComputationTimePrinter {
    public static void printAverageComputationTimes(List<List<Map>> averageComputationTimes) {
        double representationCompTime = 0.0;
        double representationCompTimeSum = 0.0;
        double similarityCompTimeSum = 0.0;
        Map<Double, Double> simCompTime = new TreeMap<Double, Double>();
        double tmpVSTime;

        for (List<Map> list : averageComputationTimes) {
            representationCompTime += (double) list.get(0).get("total");

            for (double key : ((Map<Double, Double>) list.get(1)).keySet()) {
                if (simCompTime.get(key) == null) {
                    simCompTime.put(key, 0.0);
                }

                tmpVSTime = simCompTime.get(key);
                tmpVSTime += (double) list.get(1).get(key);
                simCompTime.put(key, tmpVSTime);
            }
        }

        representationCompTime = representationCompTime / (double) averageComputationTimes.size();
        System.out.println("Average representation computation time: " + representationCompTime);
        representationCompTimeSum += representationCompTime;

        double totalVSAverage = 0.0;
        System.out.println("\nAverage similarity computation time: ");
        for (double key : simCompTime.keySet()) {
            tmpVSTime = simCompTime.get(key);
            tmpVSTime = tmpVSTime / (double) averageComputationTimes.size();
            totalVSAverage += tmpVSTime;
            simCompTime.put(key, tmpVSTime);
            System.out.println(key + ": " + tmpVSTime);
        }
        totalVSAverage = totalVSAverage / (double) simCompTime.keySet().size();
        System.out.println("\nTotal average: " + totalVSAverage);
        similarityCompTimeSum += totalVSAverage;
    }
}

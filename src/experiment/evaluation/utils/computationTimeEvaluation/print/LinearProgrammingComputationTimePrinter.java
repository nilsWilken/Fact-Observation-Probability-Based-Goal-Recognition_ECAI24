package experiment.evaluation.utils.computationTimeEvaluation.print;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class LinearProgrammingComputationTimePrinter {
    public static void printAverageComputationTimes(List<List<Map>> averageComputationTimes) {
        Map<Double, Double> totalAverageLPTimes = new TreeMap<Double, Double>();
        double tmpLPTime;
        for (List<Map> list : averageComputationTimes) {
            for (double key : ((Map<Double, Double>) list.get(0)).keySet()) {
                if (totalAverageLPTimes.get(key) == null) {
                    totalAverageLPTimes.put(key, 0.0);
                }
                tmpLPTime = totalAverageLPTimes.get(key);
                tmpLPTime += (double) list.get(0).get(key);
                totalAverageLPTimes.put(key, tmpLPTime);
            }
        }

        double totalLPAverage = 0.0;
        for (double key : totalAverageLPTimes.keySet()) {
            tmpLPTime = totalAverageLPTimes.get(key);
            tmpLPTime = tmpLPTime / (double) averageComputationTimes.size();
            totalLPAverage += tmpLPTime;
            totalAverageLPTimes.put(key, tmpLPTime);
            System.out.println(key + ": " + tmpLPTime);
        }
        totalLPAverage = totalLPAverage / (double) totalAverageLPTimes.keySet().size();
        System.out.println("\nTotal average: " + totalLPAverage);
    }
}

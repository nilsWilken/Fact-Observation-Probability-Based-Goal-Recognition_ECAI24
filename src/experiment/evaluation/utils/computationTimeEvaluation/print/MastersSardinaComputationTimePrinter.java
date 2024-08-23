package experiment.evaluation.utils.computationTimeEvaluation.print;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MastersSardinaComputationTimePrinter {
    public static void printAverageComputationTimes(List<List<Map>> averageComputationTimes) {
        double totalAverageInitialPlanCompTime = 0.0;
        Map<Double, Double> totalAveragePlanningTime = new TreeMap<Double, Double>();
        double tmpMSTime;

        for (List<Map> list : averageComputationTimes) {
            totalAverageInitialPlanCompTime += (double) list.get(0).get("total");
            for (double key : ((Map<Double, Double>) list.get(1)).keySet()) {
                if (totalAveragePlanningTime.get(key) == null) {
                    totalAveragePlanningTime.put(key, 0.0);
                }
                tmpMSTime = totalAveragePlanningTime.get(key);
                tmpMSTime += (double) list.get(1).get(key);
                totalAveragePlanningTime.put(key, tmpMSTime);
            }
        }

        totalAverageInitialPlanCompTime = totalAverageInitialPlanCompTime / (double) averageComputationTimes.size();
        System.out.println("Average initial planning time: " + totalAverageInitialPlanCompTime);

        double totalMSAverage = 0.0;
        for (double key : totalAveragePlanningTime.keySet()) {
            tmpMSTime = totalAveragePlanningTime.get(key);
            tmpMSTime = tmpMSTime / (double) averageComputationTimes.size();
            totalMSAverage += tmpMSTime;
            totalAveragePlanningTime.put(key, tmpMSTime);
            System.out.println(key + ": " + tmpMSTime);
        }
        totalMSAverage = totalMSAverage / (double) totalAveragePlanningTime.keySet().size();
        System.out.println("\nTotal average: " + totalMSAverage);
    }
}

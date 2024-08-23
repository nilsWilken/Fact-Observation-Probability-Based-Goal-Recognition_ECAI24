package experiment.evaluation.utils.computationTimeEvaluation.print;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import experiment.evaluation.utils.computationTimeEvaluation.computation.ComputationTimeComputeUtils;

public class RamirezGeffnerRelaxedComputationTimePrinter {
    public static void printAverageComputationTimes(List<List<Map>> averageComputationTimes) {
        Map<Double, Double> totalAverageTranslationTime = new TreeMap<Double, Double>();
        Map<Double, Double> totalAverageRelaxedPlanningTime = new TreeMap<Double, Double>();
        double tmpRGTime;
        for (List<Map> list : averageComputationTimes) {
            for (double key : ((Map<Double, Double>) list.get(0)).keySet()) {
                if (totalAverageTranslationTime.get(key) == null) {
                    totalAverageTranslationTime.put(key, 0.0);
                    totalAverageRelaxedPlanningTime.put(key, 0.0);
                }
                tmpRGTime = totalAverageTranslationTime.get(key);
                tmpRGTime += (double) list.get(0).get(key);
                totalAverageTranslationTime.put(key, tmpRGTime);

                tmpRGTime = totalAverageRelaxedPlanningTime.get(key);
                tmpRGTime += (double) list.get(1).get(key);
                totalAverageRelaxedPlanningTime.put(key, tmpRGTime);
            }
        }

        System.out.println("Average translation times: ");
        for (double key : totalAverageTranslationTime.keySet()) {
            tmpRGTime = totalAverageTranslationTime.get(key);
            tmpRGTime = tmpRGTime / (double) averageComputationTimes.size();
            totalAverageTranslationTime.put(key, tmpRGTime);
            System.out.println(key + ": " + tmpRGTime);
        }

        System.out.println("\nAverage relaxed planning time: ");
        for (double key : totalAverageRelaxedPlanningTime.keySet()) {
            tmpRGTime = totalAverageRelaxedPlanningTime.get(key);
            tmpRGTime = tmpRGTime / (double) averageComputationTimes.size();
            totalAverageRelaxedPlanningTime.put(key, tmpRGTime);
            System.out.println(key + ": " + tmpRGTime);
        }

        double totalRGAverage = 0.0;
        System.out.println("\nAverage total time: ");
        for (double key : totalAverageRelaxedPlanningTime.keySet()) {
            tmpRGTime = totalAverageTranslationTime.get(key) + totalAverageRelaxedPlanningTime.get(key);
            totalRGAverage += tmpRGTime;
            System.out.println(key + ": " + tmpRGTime);
        }
        totalRGAverage = totalRGAverage / (double) totalAverageRelaxedPlanningTime.keySet().size();
        System.out.println("\nTotal average: " + totalRGAverage);
    }

    public static void printRG09ComputationTimeStatistics(Map<Double, Double> averageTranslationCompTimes,
            Map<Double, Double> averageRelaxedPlanComputationTimes) {
        System.out.println("Translation time statistics: ");
        for (double obsPerc : averageTranslationCompTimes.keySet()) {
            System.out.println(
                    obsPerc + " " + ComputationTimeComputeUtils.round(averageTranslationCompTimes.get(obsPerc), 2));
        }

        System.out.println("\n\nRelaxed plan computation time statistics: ");
        for (double perc : averageRelaxedPlanComputationTimes.keySet()) {
            System.out.println(
                    perc + ": " + ComputationTimeComputeUtils.round((averageRelaxedPlanComputationTimes.get(perc)), 2));
        }
    }
}

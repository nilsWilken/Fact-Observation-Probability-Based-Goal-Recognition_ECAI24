package symbolic.vectorUtils.similarityMeasures;

import java.util.List;

public class KullbackLeiblerDivergence {
    
    protected static double calculateKullbackLeiblerDivergence(List<Double> v1, List<Double> v2) {
        double divergence = 0.0;

        for(int i = 0; i < v1.size(); i++) {
            if(v2.get(i) > 0) {
                divergence += v1.get(i) * Math.log(v1.get(i)/v2.get(i));
            }
        }

        return divergence;
    }
}

package symbolic.vectorUtils.distanceMeasures;

import java.util.List;

public class HammingDistance {
	
	public static double calculateHammingDistance(List<Double> v1, List<Double> v2) {
		double distance = 0;
		
		for(int i=0; i < v1.size(); i++) {
			if(v1.get(i).doubleValue() != v2.get(i).doubleValue()) {
				distance++;
			}
		}
		return distance;
	}

}

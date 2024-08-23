package utils;

public class ObsUtils {
	
	public static String parseObservationFileLine(String line) {
		line = line.replace("(", "");
		line = line.replace(")", "");
		
		String[] split = line.split(" ");
		
		StringBuffer result = new StringBuffer();
		for(int i = 0; i < split.length; i++) {
			if(i > 0) {
				result.append("__");
			}
			result.append(split[i]);
		}
		
		return result.toString();
	}

}

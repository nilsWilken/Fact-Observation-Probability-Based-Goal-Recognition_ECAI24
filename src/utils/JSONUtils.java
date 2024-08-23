package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSONUtils {
	
	public static void writeObjectToJSON(Object object, File outputFile) {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			java.nio.file.Files.writeString(Paths.get(outputFile.getAbsolutePath()), gson.toJson(object), new OpenOption[0]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Object readObjectFromJSON(Class c, File inputFile) {
		try {
			List<String> lines = java.nio.file.Files.readAllLines(Paths.get(inputFile.getAbsolutePath()));
			StringBuffer buff = new StringBuffer();
			for(String line : lines) {
				buff.append(line);
			}
			
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			return gson.fromJson(buff.toString(), c);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

}

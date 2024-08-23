package config;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ConfigurationManager {
	public static void generateConfigJson(Configuration config) {
		Object ob = null;
		Class c = Configuration.getConfigClass(config);
		
		try {
			ob = c.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e1) {
			e1.printStackTrace();
		}

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);

		Gson gson = gsonBuilder.setPrettyPrinting().create();

		String path = c.getName().replace(".", "/") + ".json";

		try (FileWriter writer = new FileWriter(path)) {
			gson.toJson(ob, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void generateConfigJson(Configuration config, String configFilePath) {
		Object ob = null;
		Class c = Configuration.getConfigClass(config);
		
		try {
			ob = c.getDeclaredConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e1) {
			e1.printStackTrace();
		}

		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);

		Gson gson = gsonBuilder.setPrettyPrinting().create();

		try (FileWriter writer = new FileWriter(configFilePath)) {
			gson.toJson(ob, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void initializeGoalRecognitionConfig(File configFile) throws IOException {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);
		
		Gson gson = gsonBuilder.create();
		
		Reader reader = new FileReader(configFile.getAbsolutePath());
		gson.fromJson(reader, GoalRecognitionConfiguration.class);
		reader.close();
	}

	public static void initializeEvaluationConfiguration(File configFile) throws IOException {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);
		
		Gson gson = gsonBuilder.create();
		
		Reader reader;
		
		reader = new FileReader(configFile.getAbsolutePath());

		gson.fromJson(reader, EvaluationConfiguration.class);
		reader.close();
	}

	public static void initializeComputationTimeConfiguration(File configFile) throws IOException {
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.excludeFieldsWithModifiers(java.lang.reflect.Modifier.TRANSIENT);
		
		Gson gson = gsonBuilder.create();
		
		Reader reader;
		
		reader = new FileReader(configFile.getAbsolutePath());

		gson.fromJson(reader, ComputationTimeEvaluationConfiguration.class);
		reader.close();
	}
	
	public static String getConfigurationClassPath(File configDir, Configuration config) {
		return Paths.get(configDir.getAbsolutePath(), Configuration.getConfigClass(config).getName().replace(".", "/") + ".json").toString();
	}

	public static String getResultCalculationConfigurationClassPath(File configDir, Configuration config) {
		return Paths.get(configDir.getAbsolutePath(), Configuration.getConfigClass(config).getSimpleName() + ".json").toString();
	}
	
	public static boolean isInitialized(Configuration config) {
		Class c = Configuration.getConfigClass(config);
		Field[] fields = c.getDeclaredFields();

		boolean initialized = false;

		for (Field field : fields) {
			try {
				Object attribute_value = field.get(c);
				if (attribute_value != null) {
					initialized = true;
					break;
				}
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		return initialized;
	}

}

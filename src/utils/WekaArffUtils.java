package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ArffLoader;
import weka.core.converters.ArffSaver;

public class WekaArffUtils {
	
	private static final String TMP_INSTANCES_FILE_NAME = "tmp_arff_instances.arff";
	
	public static void writeArffFile(Instances data, File targetFile) {
		ArffSaver s = new ArffSaver();
		
		s.setInstances(data);
		try {
			s.setFile(targetFile);
			s.writeBatch();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static Instances loadInstancesFromFile(File instancesFile) {
		ArffLoader loader = new ArffLoader();
		try {
			loader.setFile(instancesFile);
			return loader.getDataSet();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static File generateFileWithHiddenCategoricalAttribute(File dataFile, String attributeName, List<String> attributeValues) {
		File tmpArffFile = Paths.get(dataFile.getParentFile().getAbsolutePath(), WekaArffUtils.TMP_INSTANCES_FILE_NAME).toFile();
		
		Attribute newAtt = new Attribute(attributeName, attributeValues);
		Instances data = WekaArffUtils.loadInstancesFromFile(dataFile);
		
		data.insertAttributeAt(newAtt, 0);
		WekaArffUtils.writeArffFile(data, tmpArffFile);
		
		return tmpArffFile;
	}
	
	public static File generateFileWithSequenceAndTimeIDs(double sequenceIDCounter, File dataFile) {
		double timeCounter = 0;
		Instances tmpInstances = WekaArffUtils.loadInstancesFromFile(dataFile);
		Attribute sequenceID = new Attribute("SEQUENCE_ID");
		Attribute timeID = new Attribute("TIME_ID");
		
		tmpInstances.insertAttributeAt(timeID, 0);
		tmpInstances.insertAttributeAt(sequenceID, 0);
		
		for(Instance inst : tmpInstances) {	
			inst.setValue(0, sequenceIDCounter);
			inst.setValue(1, timeCounter++);
		}
		
		File arffTmpFile = Paths.get(dataFile.getParentFile().getAbsolutePath(), WekaArffUtils.TMP_INSTANCES_FILE_NAME).toFile();
		WekaArffUtils.writeArffFile(tmpInstances, arffTmpFile);
		
		return arffTmpFile;
	}

}

package utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;

import config.GoalRecognitionConfiguration;
import experiment.management.ExperimentType;
import utils.database.ExperimentResultDatabase;

public class DirectoryManager {

	private File resultDir;
	private File dataDir;
	private File rootDir;
	private File actionSamplingModel;
	private File archiveDir;

	private File netFile;
	private File pddlToNetSerializationFile;

	private Map<String, HashMap<Integer, File>> problemDirs;
	private Map<String, File> domainDirs;
	
	private Map<String, String> planningGoalTextGoalMapping;
	
	private Map<String, String> samplingTextGoalPDDLGoalMapping;
	private List<String> planningGoals;
	
	private ExperimentType expType;
	private ExperimentResultDatabase PRAPDBHandler;
	private ExperimentResultDatabase BNDBHandler;

	private String START_DATE;

	private static Map<String, DirectoryManager> instances = new HashMap<String, DirectoryManager>();

	public static synchronized DirectoryManager getInstance(File rootDir) {
		if(DirectoryManager.instances.get(rootDir.getAbsolutePath()) == null) {
			DirectoryManager.instances.put(rootDir.getAbsolutePath(), new DirectoryManager(rootDir));
		}

		return DirectoryManager.instances.get(rootDir.getAbsolutePath());
	}
	
	private DirectoryManager(File rootDir) {
		this.rootDir = rootDir;

		this.START_DATE = DirectoryManager.getCurrentDate();

		try {
			this.resultDir = Paths.get(this.rootDir.getAbsolutePath(), GoalRecognitionConfiguration.RESULTS_DIR_NAME).toFile();
		}catch(NullPointerException e) {
			this.resultDir = null;
		}

		this.problemDirs = new HashMap<String, HashMap<Integer, File>>();
		this.domainDirs = new HashMap<String, File>();
		
		this.expType = ExperimentType.parseFromString(GoalRecognitionConfiguration.EXPERIMENT_TYPE);

	}

	// -------------------------------------------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------COMMON STUFF--------------------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------------------------------------------------------------
	
	public File getRootDirectory() {
		return this.rootDir;
	}
	
	public static File getConfigDirectory(String rootDir) {
		return Paths.get(rootDir, "config").toFile();
	}

	public File getPlanningCommonFilesDir() {
		return Paths
				.get(this.getPlanningSetupDir().getAbsolutePath(), GoalRecognitionConfiguration.PLANNING_COMMON_FILES_DIR_NAME)
				.toFile();
	}
	
	public File getResultsDir() {
		if (!this.resultDir.exists()) {
			this.resultDir.mkdir();
		}
		return this.resultDir;
	}

	private File generateInitialResultsDir(File prefixDir, String expName) {
		File expDir = Paths.get(prefixDir.getAbsolutePath(), expName).toFile();
		
		if(!expDir.exists()) {
			expDir.mkdir();
		}
		
		File initialResultsDir = Paths
				.get(expDir.getAbsolutePath(), GoalRecognitionConfiguration.generateInitialResultDirName())
				.toFile();

		if (!initialResultsDir.exists()) {
			initialResultsDir.mkdir();
		}

		return initialResultsDir;
	}

	private File generateObsResultsDir(File prefixDir, int obsCounter, String expName) {
		File expResultsDir = Paths.get(prefixDir.getAbsolutePath(), expName).toFile();
		
		if(!expResultsDir.exists()) {
			expResultsDir.mkdir();
		}
		
		File obsResultsDir = Paths
				.get(expResultsDir.getAbsolutePath(), GoalRecognitionConfiguration.generateObsResultDirName(obsCounter))
				.toFile();

		if (!obsResultsDir.exists()) {
			obsResultsDir.mkdir();
		}

		return obsResultsDir;
	}

	private File generateObsHypPlanningResultsDir(File prefixDir, int hypCounter, String expName) {
		File expDir = Paths.get(prefixDir.getAbsolutePath(), expName).toFile();
		
		if(!expDir.exists()) {
			expDir.mkdir();
		}
		
		File hypsResultsDir = Paths
				.get(expDir.getAbsolutePath(), GoalRecognitionConfiguration.generateHypResultDirName(hypCounter))
				.toFile();

		if (!hypsResultsDir.exists()) {
			hypsResultsDir.mkdir();
		}

		return hypsResultsDir;
	}
	
	public static String getHypName(int hypCounter) {
		return "hyp" + hypCounter;
	}

	public List<String> getExperimentSetupDirectoryNames() {
		List<String> result = new ArrayList<String>();

		for (String fName : this.getPlanningSetupDir().list()) {
			if (!fName.equals(GoalRecognitionConfiguration.PLANNING_COMMON_FILES_DIR_NAME)) {
				result.add(fName);
			}
		}

		return result;
	}
	
	public File getSerializedAveragePerformanceFile() {
		return Paths.get(this.getResultsDir().getAbsolutePath(), GoalRecognitionConfiguration.SERIALIZED_AVERAGE_PERFORMANCE_FILE_NAME.replace(".json", "_" + this.START_DATE + ".json")).toFile();
	}
	
	
	

	// -------------------------------------------------------------------------------------------------------------------------------------------------------------
	// -----------------------------------------------PLANNING RELATED STUFF----------------------------------------------------------------------------------------
	// -------------------------------------------------------------------------------------------------------------------------------------------------------------

	
	public ExperimentResultDatabase getPlanningResultsDBHandler() {
		if(this.PRAPDBHandler == null) {
			this.PRAPDBHandler = new ExperimentResultDatabase(this.getPlanningResultsDBPath(), this.expType);
		}
		return this.PRAPDBHandler;
	}
	
	public void addProblemDir(String expName, int key, File problemDir) {
		HashMap<Integer, File> expProblemDirs = this.problemDirs.get(expName);
		if (expProblemDirs == null) {
			expProblemDirs = new HashMap<Integer, File>();
		}

		expProblemDirs.put(key, problemDir);
		if (!problemDir.exists()) {
			problemDir.mkdir();
		}

		this.problemDirs.put(expName, expProblemDirs);
	}

	public File getProblemDir(String expName, int hyp) {
		return this.problemDirs.get(expName).get(hyp);
	}

	public File getArchiveDir() {
		return this.archiveDir;
	}
	
	public void addDomainDir(String expName, File domainDir) {
		if(!domainDir.exists()) {
			domainDir.mkdir();
		}
		
		this.domainDirs.put(expName, domainDir);
	}
	
	public File getDomainDir(String expName) {
		return this.domainDirs.get(expName);
	}

	public File getPlanningSetupDir() {
		return Paths.get(this.rootDir.getAbsolutePath(), GoalRecognitionConfiguration.PLANNING_SETUP_DIR_NAME).toFile();
	}

	public File getDomainFile() {
		return Paths.get(this.getPlanningCommonFilesDir().getAbsolutePath(), GoalRecognitionConfiguration.DOMAIN_FILE_NAME)
				.toFile();
	}

	public File getGroundedDomainFile() {
		return Paths.get(this.getPlanningCommonFilesDir().getAbsolutePath(), GoalRecognitionConfiguration.GROUNDED_DOMAIN_FILE_NAME)
				.toFile();
	}


	public File getObsFile(String expName) {
		File obsFile = Paths.get(this.getPlanningSetupDir().getAbsolutePath(), expName, GoalRecognitionConfiguration.OBS_FILE_NAME)
		.toFile();

		if(!obsFile.exists()) {
			File nObsFile;

			int index = 0;
			do{
				nObsFile = new File(obsFile.getAbsolutePath().replace(".dat", "") + "_" + index++ + ".dat");
			}while(!nObsFile.exists() && index < 5);
			obsFile = nObsFile;
		}

		return obsFile;
	}
	
	public Path getObsFileAsPath(String expName) {
		return Paths.get(this.getObsFile(expName).getAbsolutePath());
	}

	public File getTemplateFile(String expName) {
		return Paths.get(this.getPlanningSetupDir().getAbsolutePath(), expName, GoalRecognitionConfiguration.TEMPLATE_FILE_NAME)
				.toFile();
	}
	
	public Path getTemplateFileAsPath(String expName) {
		return Paths.get(this.getTemplateFile(expName).getAbsolutePath());
	}
	
	public File getTrueHypFile(String expName) {
		return Paths.get(this.getPlanningSetupDir().getAbsolutePath(), expName, GoalRecognitionConfiguration.TRUE_HYP_FILE_NAME).toFile();
	}
	
	public Path getTrueHypFileAsPath(String expName) {
		return Paths.get(this.getTrueHypFile(expName).getAbsolutePath());
	}
	
	public List<String> getPlanningHyps() {
		if(this.planningGoals == null) {
			this.parsePlanningHypsFile();
		}
		return this.planningGoals;
	}
	
	public List<String> getTextHyps() {
		if(this.planningGoalTextGoalMapping == null) {
			this.parsePlanningHypsFile();
		}

		List<String> result = new ArrayList<String>();
		for(String hyp : this.planningGoalTextGoalMapping.values()) {
			 result.add(hyp);
		}
		
		return result;
	}
	
	public String getTextGoalFromPlanningGoal(String planningGoal) {
		if(this.planningGoalTextGoalMapping == null) {
			this.parsePlanningHypsFile();
		}
		
		return this.planningGoalTextGoalMapping.get(planningGoal);
	}
	
	public String getTrueGoalForExperiment(String expName) {
		try {
			List<String> lines = Files.readAllLines(Paths.get(this.getTrueHypFile(expName).getAbsolutePath()));
			return lines.get(0).trim();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	public String getTrueTextGoalForExperiment(String expName) {
		return this.getTextGoalFromPlanningGoal(this.getTrueGoalForExperiment(expName));
	}
	
	private void parsePlanningHypsFile() {
		this.planningGoalTextGoalMapping = new HashMap<String, String>();
		this.planningGoals = new ArrayList<String>();
		
		try {
			List<String> hypLines = Files.readAllLines(Paths.get(this.getPlanningCommonFilesDir().getAbsolutePath(), GoalRecognitionConfiguration.ALL_HYPS_FILE_NAME));
			
			for(String hypLine : hypLines) {
				String[] hypSplit = hypLine.split(";");
				this.planningGoalTextGoalMapping.put(hypSplit[0].strip(), hypSplit[1].strip());
				this.planningGoals.add(hypSplit[0].strip());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	
	public File getPlanningExpDir(String expName) {
		return Paths.get(this.getPlanningSetupDir().getAbsolutePath(), expName).toFile();
	}

	public File generateProblemDir(String expName, int hypCounter) {
		File problemDir = Paths.get(this.getPlanningExpDir(expName).getAbsolutePath(),
		GoalRecognitionConfiguration.generateHypSetupDirName(hypCounter)).toFile();

		if (!problemDir.exists()) {
			problemDir.mkdir();
		}

		return problemDir;
	}
	
	public File generateDomainDir(String expName) {
		File domainDir = Paths.get(this.getPlanningExpDir(expName).getAbsolutePath(), GoalRecognitionConfiguration.generateGroundedDomainsDirName()).toFile();
		
		if(!domainDir.exists()) {
			domainDir.mkdir();
		}
		
		return domainDir;
	}

	public File generateRG09ObsDir(String expName, int obsCounter) {
		File obsDir = Paths.get(this.getPlanningExpDir(expName).getAbsolutePath(), "obs_" + obsCounter).toFile();

		if(!obsDir.exists()) {
			obsDir.mkdir();
		}

		return obsDir;
	}

	public File getRG09ObsFile(String expName, int obsCounter) {
		return Paths.get(this.generateRG09ObsDir(expName, obsCounter).getAbsolutePath(), "obs_" + obsCounter  +".dat").toFile();
	}

	public File generateRG09ProblemDir(String expName, int hypCounter, int obsCounter) {
		File problemDir = Paths.get(this.generateRG09ObsDir(expName, obsCounter).getAbsolutePath(), GoalRecognitionConfiguration.generateHypSetupDirName(hypCounter)).toFile();

		if(!problemDir.exists()) {
			problemDir.mkdir();
		}

		return problemDir;
	}

	public File getRG09ProblemFileForObs(String expName, int hypCounter, int obsCounter) {
		File problemDir = this.generateRG09ProblemDir(expName, hypCounter, obsCounter);
		File problemFile = Paths.get(problemDir.getAbsolutePath(), GoalRecognitionConfiguration.generateObsProblemFileName(expName, hypCounter, obsCounter)).toFile();

		return problemFile;
	}

	public File getRG09DomainFileForObs(String expName, int hypCounter, int obsCounter) {
		File domainDir = this.generateRG09DomainDir(expName, hypCounter, obsCounter);
		File domainFile = Paths.get(domainDir.getAbsolutePath(), GoalRecognitionConfiguration.generateObsDomainFileName(expName, hypCounter, obsCounter)).toFile();

		return domainFile;
	}

	public File generateRG09DomainDir(String expName, int hypCounter, int obsCounter) {
		File domainDir = Paths.get(this.generateRG09ObsDir(expName, obsCounter).getAbsolutePath(), GoalRecognitionConfiguration.generateHypDomainDirName(hypCounter)).toFile();

		if(!domainDir.exists()) {
			domainDir.mkdir();
		}

		return domainDir;
	}

	public File generateRG09RelaxedPlanDir(String expName, int obsCounter) {
		File planDir = Paths.get(this.generateRG09ObsDir(expName, obsCounter).getAbsolutePath(), "relaxedPlans").toFile();

		if(!planDir.exists()) {
			planDir.mkdir();
		}

		return planDir;
	}

	public File getRG09RelaxedPlanFile(String expName, int hypCounter, int obsCounter) {
		File planDir = this.generateRG09RelaxedPlanDir(expName, obsCounter);
		File planFile = Paths.get(planDir.getAbsolutePath(), "relPlan_hyp" + hypCounter + ".soln").toFile();

		return planFile;
	}

	public File getInitialProblemFile(String experimentName, int hypCounter) {
//		File problemDir = this.problemDirs.get(experimentName).get(hypCounter);
		File problemDir = this.generateProblemDir(experimentName, hypCounter);
		return Paths.get(problemDir.getAbsolutePath(),
			GoalRecognitionConfiguration.generateInitialProblemFileName(experimentName, hypCounter)).toFile();
	}

	public File getProblemFileDir(String experimentName, int hypCounter) {
		File problemDir = this.generateProblemDir(experimentName, hypCounter);
		return problemDir;
	}

	public File getProblemFileForObs(String experimentName, int hypCounter, int obsCounter) {
		File problemDir = this.problemDirs.get(experimentName).get(hypCounter);
		File problemFile = Paths.get(problemDir.getAbsolutePath(),
			GoalRecognitionConfiguration.generateObsProblemFileName(experimentName, hypCounter, obsCounter)).toFile();

		return problemFile;
	}
	
	public File getGoalProblemFileForObs(String experimentName, int hypCounter, int obsCounter) {
		File problemDir = this.problemDirs.get(experimentName).get(hypCounter);
		File problemFile = Paths.get(problemDir.getAbsolutePath(), GoalRecognitionConfiguration.generateObsGoalProblemFileName(experimentName, hypCounter, obsCounter)).toFile();
		
		return problemFile;
	}
	
	public File getNotGoalProblemFileForObs(String experimentName, int hypCounter, int obsCounter) {
		File problemDir = this.problemDirs.get(experimentName).get(hypCounter);
		File problemFile = Paths.get(problemDir.getAbsolutePath(), GoalRecognitionConfiguration.generateObsNotGoalProblemFileName(experimentName, hypCounter, obsCounter)).toFile();
		
		return problemFile;
	}
	
	public File getDomainFileForObs(String experimentName, int obsCounter) {
		File domainDir = this.domainDirs.get(experimentName);
		File domainFile = Paths.get(domainDir.getAbsolutePath(), GoalRecognitionConfiguration.generateObsDomainFileName(experimentName, obsCounter)).toFile();
		
		return domainFile;
	}
	
	public File getPlanningResultsDir() {
		File planningResultsDir = Paths
				.get(this.getResultsDir().getAbsolutePath(), GoalRecognitionConfiguration.PLANNING_RESULTS_DIR_NAME).toFile();

		if (!planningResultsDir.exists()) {
			planningResultsDir.mkdir();
		}

		return planningResultsDir;
	}
	
	public File getInitialPlanningResultsDir(String expName) {
		return this.generateInitialResultsDir(this.getPlanningResultsDir(), expName);
	}
	
	public File getPlanningObsResultsDir(int obsCounter, String expName) {
		return this.generateObsResultsDir(this.getPlanningResultsDir(), obsCounter, expName);
	}
	
	public File getPlanningObsHypResultsDir(int obsCounter, int hypCounter, String expName) {
		return this.generateObsHypPlanningResultsDir(this.getPlanningObsResultsDir(obsCounter, expName), hypCounter,
				expName);
	}
	
	public File getInitialPlanningLogFile(int hypCounter, String expName) {
		return Paths.get(this.getInitialPlanningResultsDir(expName).getAbsolutePath(),
			GoalRecognitionConfiguration.generateHypLogFileName(hypCounter)).toFile();
	}

	public File getPlanningObsHypLogFile(int hypCounter, int obsCounter, String expName) {
		return Paths.get(this.getPlanningObsHypResultsDir(obsCounter, hypCounter, expName).getAbsolutePath(),
			GoalRecognitionConfiguration.generateHypLogFileName(hypCounter)).toFile();
	}
	
	public int getNumberOfProblemDirs(String expName) {
		return this.problemDirs.get(expName).size();
	}
	
	public boolean isDomainDirExisting(String expName) {
		return this.domainDirs.get(expName) != null;
	}
	
	public File getInitialPlanFile(int hypCounter, String expName) {
		return Paths.get(this.getInitialPlanningResultsDir(expName).getAbsolutePath(),
			GoalRecognitionConfiguration.generateHypPlanFileName(hypCounter)).toFile();
	}

	public File getObsHypPlanFile(int obsCounter, int hypCounter, String expName) {
		return Paths.get(this.getPlanningObsHypResultsDir(obsCounter, hypCounter, expName).getAbsolutePath(),
			GoalRecognitionConfiguration.generateHypPlanFileName(hypCounter)).toFile();
	}
	
	public File getSerializedPlanningResultsFile() {
		return Paths.get(this.getPlanningResultsDir().getAbsolutePath(), GoalRecognitionConfiguration.SERIALIZED_PLANNING_RESULTS_FILE_NAME.replace(".json", "_" + this.START_DATE + ".json")).toFile();
	}
	
	public File getSerializedAveragePRAPPerformanceFile() {
		return Paths.get(this.getPlanningResultsDir().getAbsolutePath(), GoalRecognitionConfiguration.SERIALIZED_AVERAGE_PRAP_PERFORMANCE_FILE_NAME.replace(".json", "_" + this.START_DATE + ".json")).toFile();
	}
	
	public static void deleteDirectoryRecursively(File directory) {
		for(File f : directory.listFiles()) {
			if(f.isDirectory()) {
				DirectoryManager.deleteDirectoryRecursively(f);
			}
			else {
				f.delete();
			}
		}
		directory.delete();
	}

	private String getPlanningResultsDBPath() {
		File db = Paths.get(this.getPlanningResultsDir().getAbsolutePath(), "evaluationResults_" + this.START_DATE + ".db").toFile();
		return db.getAbsolutePath();
	}

	public static String getCurrentDate() {
		LocalDateTime date = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd_MM_uuuu_HH_mm_ss_AAA");

		return date.format(formatter);
	}

	public static void writeFilesToBZ2(String archivePath, List<File> files) {
		try {
        	OutputStream fout = Files.newOutputStream(Paths.get(archivePath));
        	BufferedOutputStream out = new BufferedOutputStream(fout);
        	BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(out);
			TarArchiveOutputStream tOut = new TarArchiveOutputStream(bzOut);
        
			TarArchiveEntry entry;
			for(File f : files) {
				entry = new TarArchiveEntry(f, f.getName());
				tOut.putArchiveEntry(entry);

				Files.copy(Paths.get(f.getAbsolutePath()), tOut);

				tOut.closeArchiveEntry();
			}

			tOut.finish();

			tOut.close();
        	bzOut.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeStringsToTarBZ2(String archivePath, List<String> fileNames, List<String> fileContents) {
		try {
        	OutputStream fout = Files.newOutputStream(Paths.get(archivePath));
        	BufferedOutputStream out = new BufferedOutputStream(fout);
        	BZip2CompressorOutputStream bzOut = new BZip2CompressorOutputStream(out);
			TarArchiveOutputStream tOut = new TarArchiveOutputStream(bzOut);
        
			TarArchiveEntry entry;
			File tmp;
			for(int i=0; i < fileNames.size(); i++) {
				tmp = new File(fileNames.get(i));
				Files.writeString(Paths.get(tmp.getAbsolutePath()), fileContents.get(i), new OpenOption[0]);

				entry = new TarArchiveEntry(tmp, fileNames.get(i));
				tOut.putArchiveEntry(entry);

				Files.copy(Paths.get(tmp.getAbsolutePath()), tOut);

				tOut.closeArchiveEntry();
				tmp.delete();
			}

			tOut.finish();

			tOut.close();
        	bzOut.close();
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	public static void writeStringsToBZ2(String archivePath, List<String> fileNames, List<String> fileContents) {
		try {
        	OutputStream fout = Files.newOutputStream(Paths.get(archivePath));
			CompressorOutputStream bzOut = new CompressorStreamFactory().createCompressorOutputStream("bzip2", fout);

			File tmp;
			for(int i=0; i < fileNames.size(); i++) {
				tmp = new File(fileNames.get(i));
				Files.writeString(Paths.get(tmp.getAbsolutePath()), fileContents.get(i), new OpenOption[0]);

				Files.copy(Paths.get(tmp.getAbsolutePath()), bzOut);

				tmp.delete();
			}

        	bzOut.close();
		}catch(IOException e) {
			e.printStackTrace();
		} catch (CompressorException e) {
			e.printStackTrace();
		}
	}

	public static List<String> readFileAsStrings(File f) {
		try {
			return Files.readAllLines(Paths.get(f.getAbsolutePath()));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new ArrayList<String>();
	}

	public static String aggregateStringList(List<String> lines) {
		StringBuffer buff = new StringBuffer();
		for(int i=0; i < lines.size(); i++) {
			buff.append(lines.get(i));

			if(i+1 < lines.size()) {
				buff.append("\n");	
			}
		}

		return buff.toString();
	}
}

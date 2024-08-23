package utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction;

import java.io.File;
import java.util.Map;

public interface ILandmarkGenerator {
    public Map<Integer, String[]> getLandmarks();
    public void runLandmarkGenerator(File dFile, File pFile, File logFile);
    public long getRuntime();
    
}

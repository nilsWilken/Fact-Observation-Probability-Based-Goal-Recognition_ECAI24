package experiment.evaluation.utils.texGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableGenerator {
    private static int APPROACH_NAME_ASCII = 65;

    public static void main(String[] args) {
        int indexMinus = 0;

        String fileNamePostFix = "";

        try {
            Integer.parseInt(args[args.length-1]);
            fileNamePostFix = args[args.length-1];
            indexMinus = 2;
        } catch(NumberFormatException e) {
            indexMinus = 1;
        }

        String writeToDir = args[args.length-indexMinus];

        Map<String, List<File>> sameParentDirsMap = new HashMap<String, List<File>>();
        Map<File, String> nameMap = new HashMap<File, String>();
        String path;
        File parentDir;
        List<File> sameParentDirs;
        for(int i=0; i < args.length-indexMinus; i++) {
            path = args[i];
            if(path.contains("standardDeviationPerformance")) {

            }
            parentDir = new File(path);
            nameMap.put(parentDir, "" + ((char)TableGenerator.APPROACH_NAME_ASCII++));
            if(parentDir.isDirectory()) {
                for(File subDir : parentDir.listFiles()) {
                    if(subDir.isDirectory()) {
                        sameParentDirs = sameParentDirsMap.get(subDir.getName());
                        if(sameParentDirs == null) {
                            sameParentDirs = new ArrayList<File>();
                        }
                        sameParentDirs.add(parentDir);
                        sameParentDirsMap.put(subDir.getName(), sameParentDirs);
                    }
                }
            }
        }

        TexTable table = new TexTable("evaluationTable");
        String coordinates;
        String performanceDeviation = "";
        String spread;
        String spreadDeviation = "";
        for(String name : sameParentDirsMap.keySet()) {
            sameParentDirs = sameParentDirsMap.get(name);
            for(File dir : sameParentDirs) {
                coordinates = TableGenerator.parseCoordinatesFromFile(Paths.get(dir.getAbsolutePath(), name, "averageResults_table" + fileNamePostFix + ".tex"));
                if(Paths.get(dir.getAbsolutePath(), name, "averageResultsDeviation_table.tex").toFile().exists()) {
                    performanceDeviation = TableGenerator.parseCoordinatesFromFile(Paths.get(dir.getAbsolutePath(), name, "averageResultsDeviation_table.tex"));
                }
                
                spread = TableGenerator.parseAverageSpreadFromFile(Paths.get(dir.getAbsolutePath(), name, "averageSpreads" + fileNamePostFix + ".txt"));
                if(Paths.get(dir.getAbsolutePath(), name, "averageSpreadsDeviation.txt").toFile().exists()) {
                    spreadDeviation = TableGenerator.parseAverageSpreadFromFile(Paths.get(dir.getAbsolutePath(), name, "averageSpreadsDeviation.txt"));
                }
                
                if(!name.equals("average")) {
                    table.addCoordinateBlock(coordinates, performanceDeviation, spread, spreadDeviation, name, TableGenerator.parseNameFromDirectory(dir), nameMap.get(dir));
                }
                else {
                    table.addAverageCoordinateString(nameMap.get(dir), coordinates);
                    table.addAverageDeviationCoordinateString(nameMap.get(dir), performanceDeviation);
                    table.addAverageSpreadString(nameMap.get(dir), spread);
                    table.addAverageSpreadDeviationString(nameMap.get(dir), spreadDeviation);
                }
                performanceDeviation = "";
                spreadDeviation = "";
            }
        }

        table.addAverageCoordinateBlock();

        table.writeTableToFile(Paths.get(writeToDir, "evaluationTable" + fileNamePostFix + ".tex").toFile());        
    }

    private static String parseCoordinatesFromFile(Path coordinatesFilePath) {
        try {
            List<String> lines = Files.readAllLines(coordinatesFilePath);
            return lines.get(0).trim();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String parseAverageSpreadFromFile(Path spreadFilePath) {
        try {
            List<String> lines = Files.readAllLines(spreadFilePath);
            return lines.get(lines.size()-1).split(":")[1].trim();
        }catch(IOException e) {
            e.printStackTrace();
        }

        return "";
    }

    private static String parseNameFromDirectory(File directory) {
        if(directory.getAbsolutePath().contains("standardDeviationPerformance")) {
            return directory.getParentFile().getName();
        }
        else {
            return directory.getName();
        }
    }
    
}

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
import java.util.TreeMap;

public class FigureGenerator {

    private static String[] colors = {"cyan", "red", "orange", "black", "green", "yellow", "magenta", "gray", "brown", "lime", "olive", "pink", "purple", "teal", "violet"};
    private static String MARK = "none";
    private static String LINE_TYPE = "dashed";

    
    public static void main(String[] args) {
        if(args.length-1 > FigureGenerator.colors.length) {
            System.out.println("Not enough color predefined for the number of approaches selected for comparison!");
        }

        String writeToDir = args[args.length-1];

        Map<String, List<File>> sameParentDirsMap = new HashMap<String, List<File>>();
        Map<String, TexFigure> figureMap = new HashMap<String, TexFigure>();
        Map<File, String> colorMap = new HashMap<File, String>();
        Map<Integer, File> dirMap = new HashMap<Integer, File>();
        String path;
        File parentDir;
        List<File> sameParentDirs;
        for(int i=0; i < args.length-1; i++) {
            path = args[i];
            parentDir = new File(path);
            colorMap.put(parentDir, colors[i]);
            dirMap.put(i, parentDir);
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

        TexFigure tmp;
        String coordinates;
        for(String name : sameParentDirsMap.keySet()) {
            sameParentDirs = sameParentDirsMap.get(name);
            tmp = new TexFigure(name);
            for(File dir : sameParentDirs) {
                coordinates = FigureGenerator.parseCoordinatesFromFile(Paths.get(dir.getAbsolutePath(), name, "averageResults.tex"));
                // System.out.println(coordinates);
                tmp.addCoordinateBlock(coordinates, colorMap.get(dir), FigureGenerator.MARK, FigureGenerator.LINE_TYPE, dir.getName());
            }
            figureMap.put(name, tmp);
        }


        List<Map<Double, Double>> averageCoordinates = new ArrayList<Map<Double, Double>>();
        Double value;
        for(String name : figureMap.keySet()) {
            // System.out.println(figureMap.get(name).generateFigureString());
            tmp = figureMap.get(name);
            tmp.writeFigureToFile(Paths.get(writeToDir, name + ".tex").toFile());

            List<Map<Double, Double>> tmpCoords = tmp.getCoordinates();
            Map<Double, Double> tmpCoordMap;
            for(int i=0; i < tmpCoords.size(); i++) {
                if(i >= averageCoordinates.size()) {
                    tmpCoordMap = new TreeMap<Double, Double>();
                    averageCoordinates.add(tmpCoordMap);
                }
                else {
                    tmpCoordMap = averageCoordinates.get(i);
                }
                for(double index : tmpCoords.get(i).keySet()) {
                    value = tmpCoordMap.get(index);
                    if(value == null) {
                        value = 0.0;
                    }
                    value += tmpCoords.get(i).get(index);
                    tmpCoordMap.put(index, value);
                }
            }
        }
        for(Map<Double, Double> averageCoords : averageCoordinates) {
            for(double index : averageCoords.keySet()) {
                value = averageCoords.get(index);
                value /= ((double)figureMap.keySet().size());
                averageCoords.put(index, value);
            }
        }

        TexFigure average = new TexFigure("average");
        for(int i=0; i < averageCoordinates.size(); i++) {
            average.addCoordinateBlock(FigureGenerator.convertCoordinatesToString(averageCoordinates.get(i)), colorMap.get(dirMap.get(i)), FigureGenerator.MARK, FigureGenerator.LINE_TYPE, dirMap.get(i).getName());
        }
        average.writeFigureToFile(Paths.get(writeToDir, "average" + ".tex").toFile());
    }

    private static String convertCoordinatesToString(Map<Double, Double> coordinates) {
        StringBuffer coordinateString = new StringBuffer();
        coordinateString.append("coordinates { ");
        
        for(double index : coordinates.keySet()) {
            coordinateString.append("(" + index + "," + coordinates.get(index) + ")");
        }

        coordinateString.append("};");

        return coordinateString.toString();
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
}

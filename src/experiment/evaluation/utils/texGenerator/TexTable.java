package experiment.evaluation.utils.texGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import utils.GeneralUtils;

public class TexTable {
    private static String LATEX_PLUS_MINUS_SIGN = "$\\pm$";

    private static DecimalFormat PERFORMANCE_FORMAT = new DecimalFormat(".##", new DecimalFormatSymbols(Locale.ENGLISH));

    private Map<String, List<String>> coordinateBlocks;
    private Map<String, String> averageCoordinateStrings;
    private Map<String, String> averageDeviationCoordinateStrings;
    private Map<String, String> averageSpreadStrings;
    private Map<String, String> averageSpreadDeviationStrings;
    private Map<String, Map<Integer, Double>> performances;
    private Map<String, Double> spreads;
    private List<String> approachNames;
    private String name;

    public TexTable(String name) {
        this.name = name;

        this.coordinateBlocks = new TreeMap<String, List<String>>();
        this.approachNames = new ArrayList<String>();

        this.performances = new TreeMap<String, Map<Integer, Double>>();
        this.spreads = new TreeMap<String, Double>();

        this.averageCoordinateStrings = new HashMap<String, String>();
        this.averageDeviationCoordinateStrings = new HashMap<String, String>();
        this.averageSpreadStrings = new HashMap<String, String>();
        this.averageSpreadDeviationStrings = new HashMap<String, String>();
    }

    public void addAverageCoordinateString(String approach, String coordinateString) {
        this.averageCoordinateStrings.put(approach, coordinateString);
    }

    public void addAverageDeviationCoordinateString(String approach, String deviationString) {
        this.averageDeviationCoordinateStrings.put(approach, deviationString);
    }

    public void addAverageSpreadString(String approach, String spreadString) {
        this.averageSpreadStrings.put(approach, spreadString);
    }

    public void addAverageSpreadDeviationString(String approach, String spreadDeviationString) {
        this.averageSpreadDeviationStrings.put(approach, spreadDeviationString);
    }

    private String generateHeader() {
        StringBuffer header = new StringBuffer();

        header.append("\\begin{table}\n");
        header.append(this.generateCaption() + "\n");
        header.append("\\label{tab:" + this.generateLabel() + "}\n");
        header.append("\\resizebox*{\\columnwidth}{!}{\n");
        header.append("\\begin{tabular}{ll|llllllllll|l}\n");
        header.append("\\multicolumn{1}{c}{}\t\t & \\multicolumn{1}{c|}{}\t\t & \\multicolumn{10}{c|}{\\textbf{Accuracy (for different $\\lambda$)}}\t\t & \\textbf{S} \\\\\n");
        header.append("\t\t\t & \t\t\t & .1 \t\t & .2 \t\t & .3 \t\t & .4 \t\t & .5 \t\t & .6 \t\t & .7 \t\t & .8 \t\t & .9 \t\t & 1 \t\t & \\\\ \\hline \n");

        return header.toString();
    }

    private String generateCaption() {
        StringBuffer caption = new StringBuffer();
        
        caption.append("\\caption{Comparison of evaluation results among different approaches (");
        
        for(int i=0; i < this.approachNames.size(); i++) {
            caption.append(TexFigure.getTransformedApproachName(this.approachNames.get(i)));
            if(i+1 < this.approachNames.size()) {
                caption.append(", ");
            }
        }

        caption.append(") when applied to different benchmark domains. The accuracy is reported for different fractions $\\lambda$ of the observation sequences that were used for recognition. Column \\textbf{S} reports the average spread (size of the set of goals that are recognized as the true goal) for each domain.}\n");
        

        return caption.toString();
    }

    private String generateLabel() {
        return this.name;
    }

    private String generateFooter() {
        StringBuffer footer = new StringBuffer();

        footer.append("\\end{tabular}\n");
        footer.append("}\n");
        footer.append("\\end{table}");
        return footer.toString();
    }

    public void addCoordinateBlock(String coordinateString, String deviationString, String spreadString, String spreadDeviationString, String domainName, String approachName, String approachChar) {
        StringBuffer coordinateBlock = new StringBuffer();
        List<String> coordinateStrings = this.coordinateBlocks.get(domainName);

        if(coordinateStrings == null) {
            coordinateStrings = new ArrayList<String>();
            this.coordinateBlocks.put(domainName, coordinateStrings);
        }

        double spread = Double.parseDouble(spreadString);
        spread = GeneralUtils.roundDouble(spread, 1);

        if(deviationString.length() > 0) {
            coordinateBlock.append("\t\t & " + approachChar + " " + TexTable.updateCoordinateString(coordinateString, deviationString) + " " + spread + TexTable.LATEX_PLUS_MINUS_SIGN + " " + spreadDeviationString + "\\\\");
        }
        else {
            coordinateBlock.append("\t\t & " + approachChar + " " + coordinateString + " " + spread + "\\\\");
        }
        
        coordinateStrings.add(coordinateBlock.toString());
        
        if(!this.approachNames.contains(approachName)) {
            this.approachNames.add(approachName);
        }

        if(!domainName.equals("average")) {
            Map<Integer, Double> averagePerformance = this.performances.get(approachChar);
            if(averagePerformance == null) {
                averagePerformance = new TreeMap<Integer, Double>();
                this.performances.put(approachChar, averagePerformance);
            }

            Double perf;
            String[] split = coordinateString.split("&");
            String tr;
            for(int i=0; i < split.length; i++) {
                perf = averagePerformance.get(i);
                if(perf == null) {
                    perf = 0.0;
                }
                tr = split[i].trim();
                if(tr.length() > 0) {
                    perf += Double.parseDouble(tr);
                    averagePerformance.put(i, perf);
                }
            }

            Double averageSpread = this.spreads.get(approachChar);
            if(averageSpread == null) {
                averageSpread = spread;
            }
            else {
                averageSpread += spread;
            }
            this.spreads.put(approachChar, averageSpread);
        }
    }

    public void addAverageCoordinateBlock() {
        int size = this.coordinateBlocks.keySet().size();
        int nameIndex = 0;

        String coordinateString;
        String spreadString;
        String domainName = "average";
        Map<Integer, Double> averagePerformances;

        for(String approach : this.performances.keySet()) {
            if(this.averageCoordinateStrings.get(approach) == null) {
                averagePerformances = this.performances.get(approach);

                for(int key : averagePerformances.keySet()) {
                    averagePerformances.put(key, averagePerformances.get(key)/(double)size);
                }
                coordinateString = this.generateCoordinateString(averagePerformances);

                spreadString = (this.spreads.get(approach)/(double)size) + "";

                this.addCoordinateBlock("&" + coordinateString, "", spreadString, "", domainName, this.approachNames.get(nameIndex), approach);
            }
            else {
                this.addCoordinateBlock(this.averageCoordinateStrings.get(approach), this.averageDeviationCoordinateStrings.get(approach), this.averageSpreadStrings.get(approach), this.averageSpreadDeviationStrings.get(approach), domainName, this.approachNames.get(nameIndex), approach);
            }

            nameIndex++;
        }
    }

    public String generateCoordinateString(Map<Integer, Double> performances) {
        StringBuffer result = new StringBuffer();

        for(int key : performances.keySet()) {
            result.append(performances.get(key) + " & ");
        }

        return result.toString();
    }

    public String generateTableString() {
        StringBuffer table = new StringBuffer();
        table.append(this.generateHeader());

        for(String domainName : this.coordinateBlocks.keySet()) {
            table.append(this.generateCoordinateBlockHeader(domainName));
            table.append(this.getTransformedCoordinateBlocks(domainName));
        }

        table.append(this.generateFooter());

        return table.toString();
    }

    public void writeTableToFile(File writeTo) {
        if(writeTo.exists()) {
            writeTo.delete();
        }

        try {
            Files.writeString(Paths.get(writeTo.getAbsolutePath()), this.generateTableString(), new OpenOption[0]);
        }catch(IOException e) {
            e.printStackTrace();
        }
    }

    private String generateCoordinateBlockHeader(String domainName) {
        StringBuffer blockHeader = new StringBuffer();

        blockHeader.append("\\multirow{" + this.coordinateBlocks.get(domainName).size() + "}{*}{\\rotatebox[origin=c]{90}{" + domainName + "}}\n");

        return blockHeader.toString();
    }

    private String getTransformedCoordinateBlocks(String domainName) {
        List<String> blockLines = this.coordinateBlocks.get(domainName);

        Map<Integer, Double> maxes = new HashMap<Integer, Double>();
        Double max;
        double parsedEle;
        double min = Double.MAX_VALUE;
        for(String line : blockLines) {
            String[] split = line.split("&");
            for(int i=0; i < split.length; i++) {
                max = maxes.get(i);
                if(max == null) {
                    max = -1.0;
                }
                try {
                    if(!split[i].trim().endsWith("\\")) {
                        parsedEle = Double.parseDouble(split[i].replace(TexTable.LATEX_PLUS_MINUS_SIGN, "").trim().split(" ")[0].trim());
                        if(parsedEle > max) {
                            max = parsedEle;
                        }
                    }
                    else {
                         parsedEle = Double.parseDouble(split[i].replace(TexTable.LATEX_PLUS_MINUS_SIGN, "").replace("\\", "").trim().split(" ")[0].trim());
                        if(parsedEle < min) {
                            min = parsedEle;
                        }
                    }
                }catch(NumberFormatException e) {
                    
                }
                
                maxes.put(i, max);
            }
        }

        Map<Integer, String> maxStrings = new HashMap<Integer, String>();
        for(int key : maxes.keySet()) {
            max = maxes.get(key);
            if(max < 1.0) {
                // maxStrings.put(key, (""+max).replace("0", ""));
                maxStrings.put(key, TexTable.formatPerformanceMetric(max));
            }
            else {
                maxStrings.put(key, (""+maxes.get(key)));
            }
            
        }

        StringBuffer transformedBlock = new StringBuffer();
        for(String line : blockLines) {
            String[] split = line.split("&");
            String[] split2;
            String ele;
            for(int i=0; i < split.length; i++) {
                ele = split[i].trim();
                try {
                    split2 = ele.replace(TexTable.LATEX_PLUS_MINUS_SIGN, "").split(" ");
                    parsedEle = Double.parseDouble(split2[0].trim());
                    if(parsedEle == maxes.get(i)) {
                        if(split2.length == 2) {
                            transformedBlock.append("\\textbf{" + TexTable.formatPerformanceMetric(parsedEle) + TexTable.LATEX_PLUS_MINUS_SIGN + " " + TexTable.formatPerformanceMetric(Double.parseDouble(split2[1].trim())) + "} & ");
                        }
                        else {
                            transformedBlock.append("\\textbf{" + TexTable.formatPerformanceMetric(parsedEle) + "} & ");
                        }
                    }
                    else {
                        if(parsedEle < 1.0) {
                            // transformedBlock.append((""+parsedEle).replace("0", "") + " & ");
                            if(split2.length == 2) {
                                transformedBlock.append(TexTable.formatPerformanceMetric(parsedEle) + TexTable.LATEX_PLUS_MINUS_SIGN + " " + TexTable.formatPerformanceMetric(Double.parseDouble(split2[1].trim())) + " & ");
                            }
                            else {
                                transformedBlock.append(TexTable.formatPerformanceMetric(parsedEle) + " & ");
                            }
                            
                        }
                        else {
                            if(split2.length == 2) {
                                transformedBlock.append(parsedEle + TexTable.LATEX_PLUS_MINUS_SIGN + " " + TexTable.formatPerformanceMetric(Double.parseDouble(split2[1].trim())) + " & ");
                            }
                            else {
                                transformedBlock.append(parsedEle + " & ");
                            }
                        }
                    }
                }catch(NumberFormatException e){
                    if(!ele.contains("\\")) {
                        transformedBlock.append(ele);
                        transformedBlock.append(" & ");
                    }
                    else {
                        split2 = split[i].replace(TexTable.LATEX_PLUS_MINUS_SIGN, "").replace("\\", "").trim().split(" ");
                        if(split2[0].trim().length() > 0 && Double.parseDouble(split2[0].trim()) == min) {
                            if(split2.length == 2) {
                                transformedBlock.append("\\textbf{" + TexTable.formatPerformanceMetric(Double.parseDouble(split2[0].trim())) + TexTable.LATEX_PLUS_MINUS_SIGN + " " + TexTable.formatPerformanceMetric(Double.parseDouble(split2[1].trim())) + "} \\\\");
                            }
                            else {
                                transformedBlock.append("\\textbf{" + TexTable.formatPerformanceMetric(Double.parseDouble(split2[0].trim())) + "} \\\\");
                            }
                        }
                        else {
                            if(split2.length == 2) {
                                transformedBlock.append(TexTable.formatPerformanceMetric(Double.parseDouble(split2[0].trim())) + TexTable.LATEX_PLUS_MINUS_SIGN + " " + TexTable.formatPerformanceMetric(Double.parseDouble(split2[1].trim())) + " \\\\");
                            }
                            else {
                                transformedBlock.append(TexTable.formatPerformanceMetric(Double.parseDouble(split2[0].trim())) + " \\\\");
                            }
                        }
                    }
                }
            }
            transformedBlock.append("\n");
        }
        transformedBlock.append("\\hline\n");

        return transformedBlock.toString();
    }

    private static String updateCoordinateString(String coordinates, String deviations) {
        String[] coordinatesSplit = coordinates.split("&");
        String[] deviationSplit = deviations.split("&");

        StringBuffer updatedCoordinates = new StringBuffer();

        String coordinate;
        String deviation;
        for(int i=0; i < coordinatesSplit.length; i++) {
            coordinate = coordinatesSplit[i].trim();
            deviation = deviationSplit[i].trim();

            if(coordinate.length() > 0) {
                updatedCoordinates.append("&" + coordinate + TexTable.LATEX_PLUS_MINUS_SIGN + " " + deviation + " ");
            }
        }

        updatedCoordinates.append("&");

        return updatedCoordinates.toString();
    }

    private static String formatPerformanceMetric(double performanceValue) {
        if((performanceValue > 0.0 && performanceValue < 1.0) || performanceValue > 1.0) {
            String formatted = TexTable.PERFORMANCE_FORMAT.format(performanceValue);
            String tmp = "0" + formatted;
            if(Double.parseDouble(tmp) == 0.0) {
                return "0";
            }
            return formatted;
        }
        else if(performanceValue == 0.0) {
            return "0";
        }
        else {
            return "1";
        }
    }

}

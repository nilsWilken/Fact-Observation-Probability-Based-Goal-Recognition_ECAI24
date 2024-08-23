package experiment.evaluation.utils.texGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TexFigure {
    private List<String> coordinateBlocks;
    private List<Map<Double, Double>> coordinates;
    private List<String> approachNames;
    private String name;

    public TexFigure(String name) {
        this.coordinateBlocks = new ArrayList<String>();
        this.coordinates = new ArrayList<Map<Double, Double>>();
        this.approachNames = new ArrayList<String>();
        this.name = name;
    }

    private String generateHeader() {
        StringBuffer header = new StringBuffer();

        header.append("\\begin{figure}[h!]\n");
        header.append("\t\\centering\n");
        header.append("\t\t\\subfloat{\n");
        header.append("\t\t\t\\begin{tikzpicture}\n");
        header.append("\t\t\t\\pgfplotsset{every x tick label/.append style={font=\\tiny}}\n");
        header.append("\t\t\t\\pgfplotsset{every y tick label/.append style={font=\\tiny}}\n");
        header.append("\t\t\t\t\\begin{axis}[\n");
        header.append("\t\t\t\t\twidth=1\\linewidth,\n");
        header.append("\t\t\t\t\theight=5cm,\n");
        header.append("\t\t\t\t\txlabel={\\scriptsize{Part of observation sequence}},\n");
        header.append("\t\t\t\t\tylabel={\\scriptsize{Accuracy}},\n");
        header.append("\t\t\t\t\txlabel near ticks,\n");
        header.append("\t\t\t\t\tylabel near ticks,\n");
        header.append("\t\t\t\t\txmin=1, xmax=10,\n");
        header.append("\t\t\t\t\tymin=0, ymax=1.1,\n");
        header.append("\t\t\t\t\txtick={0,1,2,3,4,5,6,7,8,9,10, 11},\n");
        header.append("\t\t\t\t\txticklabels={0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 0.99},\n");
        header.append("\t\t\t\t\tytick={0.2,0.4,0.6,0.8,1},\n");
        header.append("\t\t\t\t\tlegend pos=north west,\n");
        header.append("\t\t\t\t\tymajorgrids=false,\n");
        header.append("\t\t\t\t\txmajorgrids=false,\n");
        header.append("\t\t\t\t\tmajor grid style={line width=.1pt,draw=gray!50},\n");
        header.append("\t\t\t\t\tx axis line style={draw=black!60},\n");
        header.append("\t\t\t\t\ttick style={draw=black!60},\n");
        header.append("\t\t\t\t\tlegend columns=3,\n");
        header.append("\t\t\t\t\tlegend style={draw=none},\n");
        header.append("\t\t\t\t\tlegend entries={" + this.generateLegendString() + "},\n");
        header.append("\t\t\t\t\tlegend to name={" + this.generateLegendName() + "}\n");
        header.append("\t\t\t\t]\n");

        return header.toString();
    }

    private String generateLegendName() {
        return this.name + "_performance";
    }

    public String generateFooter() {
        StringBuffer footer = new StringBuffer();

        footer.append("\t\t\t\t\\end{axis}\n");
        footer.append("\t\t\t\\end{tikzpicture}\n");
        footer.append("\t\t\t}\n");
        footer.append("\t\t\t\\newline\n");
        footer.append("\t\t\t\\vspace{-0.2cm}\n");
        footer.append("\t\\ref{" + this.generateLegendName() + "}\n");
        footer.append("\t" + this.generateCaption() + "\n");
        footer.append("\n\t\\label{fig:" + this.generateLegendName() + "}\n");
        footer.append("\\end{figure}");

        return footer.toString();
    }

    public void addCoordinateBlock(String coordinateString, String color, String mark, String lineType, String approachName) {
        StringBuffer coordinateBlock = new StringBuffer();

        coordinateBlock.append("\t\t\t\t\\addplot[\n");
        coordinateBlock.append("\t\t\t\t\tcolor=" + color + ",\n");
        coordinateBlock.append("\t\t\t\t\t\tmark=" + mark + ",\n");
        coordinateBlock.append("\t\t\t\t\t\t" + lineType + ",\n");
        coordinateBlock.append("\t\t\t\t\t\t]\n");
        coordinateBlock.append("\t\t\t\t\t\t" + coordinateString + "\n");

        this.coordinateBlocks.add(coordinateBlock.toString());
        this.coordinates.add(this.parseCoordinateString(coordinateString));
        this.approachNames.add(approachName);
    }

    public String generateFigureString() {
        StringBuffer figure = new StringBuffer();
        figure.append(this.generateHeader());

        for(String coordinates : this.coordinateBlocks) {
            figure.append("\n" + coordinates);
        }

        figure.append("\n" + this.generateFooter());
    
        return figure.toString();
    }

    public void writeFigureToFile(File writeTo) {
        if(writeTo.exists()) {
            writeTo.delete();
        }

        try {
            Files.writeString(Paths.get(writeTo.getAbsolutePath()), this.generateFigureString(), new OpenOption[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String generateLegendString() {
        StringBuffer legend = new StringBuffer();

        for(int i=0; i < this.approachNames.size(); i++) {
            legend.append("\\footnotesize{" + TexFigure.getTransformedApproachName(this.approachNames.get(i)) + "}");
            if(i+1 < this.approachNames.size()) {
                legend.append(",");
            }
        }

        return legend.toString();
    }

    public static String getTransformedApproachName(String name) {
        return name.replace("_", "-");
    }

    public List<Map<Double, Double>> getCoordinates() {
        return this.coordinates;
    }

    private Map<Double, Double> parseCoordinateString(String coordinateString) {
        Map<Double, Double> coordinates = new HashMap<Double, Double>();

        String coords = coordinateString.replace("coordinates {", "");
        coords = coords.replace("};", "");

        String[] split = coords.split("\\(");
        String ele;
        for(int i=1; i < split.length; i++) {
            ele = split[i];
            ele = ele.replace(")", "");
            String[] split2 = ele.split(",");
            double index = Double.parseDouble(split2[0]);
            double value = Double.parseDouble(split2[1]);
            coordinates.put(index, value);
        }

        return coordinates;
    }

    private String generateCaption() {
        StringBuffer caption = new StringBuffer();

        StringBuffer captionApproaches = new StringBuffer();
        for(int i=0; i < this.approachNames.size(); i++) {
            captionApproaches.append(TexFigure.getTransformedApproachName(this.approachNames.get(i)));
            if(i+2 == this.approachNames.size()) {
                captionApproaches.append(", and ");
            }
            else if(i+1 < this.approachNames.size()) {
                captionApproaches.append(", ");
            }
        }
        
        caption.append("\\caption{");
        caption.append("Mean accuracy of the " + captionApproaches.toString() + " approaches on the \\emph{" + this.name + "} domain.");
        caption.append("}");
        
        return caption.toString();
    }
    
}

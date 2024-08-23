package utils.externalProcessExecution.externalExecutionWrapper.fastDownwardLandmarkExtraction;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Map;
import java.util.HashMap;

import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import experiment.recognitionExperiments.FDLandmarkBased.LandmarkBasedGoalRecognitionUtils;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

public class FastDownwardLMExtractionUtils {

    private static final Pattern ATOM_PATTERN = Pattern.compile("(Atom[^\\(]*\\([^\\)]*\\))");
    public static final String FORMULA_LANDMARK_SEPARATOR = ";";

    public static Pair<Map<String, String[]>, Graph> generateLandmarkSetNewVersion(List<String> lines) {
        Map<String, String[]> landmarks = new TreeMap<String, String[]>();
        List<String> edgeLines = new ArrayList<String>();

        boolean inGraph = false;
        List<String[]> formulaAtoms;
        int graphCounter = 0;

        int idCounter = 0;

        Map<String, String> idMapping = new HashMap<String, String>();

        for(String line : lines) {
            if(line.contains("Dumping landmark graph")) {
                graphCounter++;
                if(graphCounter == 2) {
                    inGraph = true;
                    continue;
                }
            }
            if(line.contains("Landmark graph end.")) {
                inGraph = false;
                continue;
            }
            if(inGraph) {
                line = line.trim();
                
                if(line.startsWith("lm") && !(line.contains("->") || line.contains("<-"))) {
                    String[] split = line.split(" \\[label=");

                    String id = split[0];
                    String newId = LandmarkBasedGoalRecognitionUtils.getStringLMID(idCounter);

                    idMapping.put(id, newId);
                    id = newId;
                    
                    String label = parseNodeLabel(split[1]);

                    if(label.contains("&")) {
                        formulaAtoms = FastDownwardLMExtractionUtils.parseFormulaAtoms(line);
                        landmarks.put(id, FastDownwardLMExtractionUtils.convertLandmarkList(formulaAtoms, "conj"));
                        idCounter++;
                    }
                    else if(label.contains("|")) {
                        formulaAtoms = FastDownwardLMExtractionUtils.parseFormulaAtoms(line);
                        landmarks.put(id, FastDownwardLMExtractionUtils.convertLandmarkList(formulaAtoms, "disj"));
                        idCounter++;
                    }
                    else if (label.contains("NegatedAtom")) {
                
                    }
                    else if (label.contains("Atom")){
                        landmarks.put(id, FastDownwardLMExtractionUtils.parseAtom(line));
                        idCounter++;
                    }
                }
                else if(line.startsWith("lm") && (line.contains("->") || line.contains("<-"))) {
                    edgeLines.add(line);
                }
            }
        }

        return new ImmutablePair<Map<String, String[]>, Graph> (landmarks, parseEdgeLines(landmarks.keySet(), edgeLines, idMapping));
    }

    private static Graph parseEdgeLines(Set<String> landmarkIDs, List<String> edgeLines, Map<String, String> idMapping) {
        Graph<String, DefaultEdge> landmarkGraph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);

        for(String landmarkID : landmarkIDs) {
            landmarkGraph.addVertex(landmarkID);
        }

        String[] parsedEdge;
        for(String edgeLine : edgeLines) {
            parsedEdge = parseEdgeLine(edgeLine);
            String v1 = idMapping.get(parsedEdge[0]);
            String v2 = idMapping.get(parsedEdge[1]);
            if(landmarkGraph.containsVertex(v1) && landmarkGraph.containsVertex(v2)) {
                landmarkGraph.addEdge(v1, v2);
            }
        }

        return landmarkGraph;
    }

    private static String[] parseEdgeLine(String line) {
        String[] parsedEdge = new String[2];

        line = line.trim();
        
        String[] split = line.split(" ");

        if(split[1].equals("->")) {
            parsedEdge[0] = split[0];
            parsedEdge[1] = split[2];
        }
        else if(split[1].equals("<-")) {
            parsedEdge[0] = split[2];
            parsedEdge[1] = split[0];
        }

        return parsedEdge;
    }

    private static String parseNodeLabel(String node) {
        String label = "";

        String[] split = node.split("\"");
        label = split[1];

        return label;
    }

    public static Set<String[]> generateLandmarkSet(List<String> lines) {
		Set<String[]> landmarks = new HashSet<String[]>();

        boolean inGraph = false;
        List<String[]> formulaAtoms;
        for(String line : lines) {
            if(line.startsWith("Landmark graph:")) {
                inGraph = true;
                continue;
            }
            if(line.startsWith("Landmark graph end.")) {
                inGraph = false;
                continue;
            }
            if(inGraph) {
                if(line.startsWith("LM")) {
                    String[] split = line.split(" ");
                    if(split[2].equals("conj")) {
                        formulaAtoms = FastDownwardLMExtractionUtils.parseFormulaAtoms(line);
                        landmarks.add(FastDownwardLMExtractionUtils.convertLandmarkList(formulaAtoms, "conj"));
                    }
                    else if(split[2].equals("disj")) {
                        formulaAtoms = FastDownwardLMExtractionUtils.parseFormulaAtoms(line);
                        landmarks.add(FastDownwardLMExtractionUtils.convertLandmarkList(formulaAtoms, "disj"));
                    }
                    else if (split[2].equals("Atom")){
                        landmarks.add(FastDownwardLMExtractionUtils.parseAtom(line));
                    }
                    else if (split[2].equals("Negated")) {
                
                    }
                }
            }
        }
        return landmarks;
	}

    private static String[] convertLandmarkList(List<String[]> formulaAtoms, String type) {
        List<String> landmark = new ArrayList<String>();
        landmark.add(type);
        landmark.add(FastDownwardLMExtractionUtils.FORMULA_LANDMARK_SEPARATOR);
        for(String[] atom : formulaAtoms) {
            for(String ele : atom) {
                landmark.add(ele);
            }
            landmark.add(FastDownwardLMExtractionUtils.FORMULA_LANDMARK_SEPARATOR);
        }

        String[] result = new String[landmark.size()];
        for(int i=0; i < landmark.size(); i++) {
            result[i] = landmark.get(i);
        }

        return result;
    }

    private static List<String[]> parseFormulaAtoms(String formula) {
        List<String[]> atoms = new ArrayList<String[]>();
        
        Matcher atomMatcher = FastDownwardLMExtractionUtils.ATOM_PATTERN.matcher(formula);
        while(atomMatcher.find()) {
            atoms.add(FastDownwardLMExtractionUtils.parseAtom(atomMatcher.group(1)));
        }

        return atoms;
    }

    private static String[] parseAtom(String atom) {
        Matcher atomMatcher = FastDownwardLMExtractionUtils.ATOM_PATTERN.matcher(atom);
        atomMatcher.find();

        atom = atomMatcher.group(1);

        Pattern pPattern = Pattern.compile("Atom ([^\\(]*\\([^\\)]*\\))");
        Matcher pMatcher = pPattern.matcher(atom);

        pMatcher.find();
        
        String lm = pMatcher.group(1);
        lm = lm.replace("(", " ");
        lm = lm.replace(")", "");
        lm = lm.replace(",", "");

        String[] tmp = lm.split(" ");
        for(int i=0; i < tmp.length; i++) {
            tmp[i] = tmp[i].trim();
        }

        return tmp;
    }
}

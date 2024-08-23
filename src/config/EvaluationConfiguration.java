package config;

import experiment.evaluation.resultCalculation.PerformanceMeasure;
import experiment.evaluation.resultCalculation.ResultCalculationType;

public class EvaluationConfiguration {
    public static ResultCalculationType CALCULATION_TYPE = ResultCalculationType.SYMBOLIC;
    public static PerformanceMeasure PERFORMANCE_MEASURE = PerformanceMeasure.ACCURACY;

    public static boolean USE_GOAL_SCORE_THRESHOLD = false;
    public static double GOAL_SCORE_THRESHOLD = 0.04;

    public static boolean WRITE_RESULTS_TO_LATEX_FIGURE = true;

    public static String DB_PATH_PREFIX = "";

    public static String SYMBOLIC_DB_DIRECTORY_PATH = "";

    public static String SYMBOLIC_DB_PATH = "";

    public static String OUT_FILE_PATH = "";

    public static String OUT_FILE_PATH_SUFFIX = "";

    public static String LATEX_OUT_FILE_PATH = "";

    public static String LATEX_OUT_FILE_PATH_SUFFIX = "";

    public static Double[] OBS_PERCENTAGES = {0.05, 0.1, 0.15, 0.2, 0.25, 0.3, 0.35, 0.4, 0.45, 0.5, 0.6, 0.65, 0.7, 0.75, 0.8, 0.85, 0.9, 0.95, 0.99};

    public static ResultCalculationType getCalculationType() {
        return CALCULATION_TYPE;
    }

    public static void setCalculationType(ResultCalculationType calculationType) {
        CALCULATION_TYPE = calculationType;
    }

    public static PerformanceMeasure getPerformanceMeasure() {
        return PERFORMANCE_MEASURE;
    }

    public static void setPerformanceMeasure(PerformanceMeasure performanceMeasure) {
        PERFORMANCE_MEASURE = performanceMeasure;
    }

    public static boolean getWriteResultsToLatexFigure() {
        return WRITE_RESULTS_TO_LATEX_FIGURE;
    }

    public static void setWriteResultsToLatexFigure(boolean writeResultsToLatexFigure) {
        WRITE_RESULTS_TO_LATEX_FIGURE = writeResultsToLatexFigure;
    }

    public static String getDbPathPrefix() {
        return DB_PATH_PREFIX;
    }

    public static void setDbPathPrefix(String dbPathPrefix) {
        DB_PATH_PREFIX = dbPathPrefix;
    }

    public static String getOutFilePath() {
        return OUT_FILE_PATH;
    }

    public static void setOutFilePath(String outFilePath) {
        OUT_FILE_PATH = outFilePath;
    }

    public static String getOutFilePathSuffix() {
        return OUT_FILE_PATH_SUFFIX;
    }

    public static void setOutFilePathSuffix(String outFilePathSuffix) {
        OUT_FILE_PATH_SUFFIX = outFilePathSuffix;
    }

    public static String getLatexOutFilePath() {
        return LATEX_OUT_FILE_PATH;
    }

    public static void setLatexOutFilePath(String latexOutFilePath) {
        LATEX_OUT_FILE_PATH = latexOutFilePath;
    }

    public static String getLatexOutFilePathSuffix() {
        return LATEX_OUT_FILE_PATH_SUFFIX;
    }

    public static void setLatexOutFilePathSuffix(String latexOutFilePathSuffix) {
        LATEX_OUT_FILE_PATH_SUFFIX = latexOutFilePathSuffix;
    }

    public static Double[] getObsPercentages() {
        return OBS_PERCENTAGES;
    }

    public static void setObsPercentages(Double[] obsPercentages) {
        OBS_PERCENTAGES = obsPercentages;
    }

    public static String getSymbolicDbPath() {
        return SYMBOLIC_DB_PATH;
    }

    public static void setSymbolicDbPath(String symbolicDbPath) {
        SYMBOLIC_DB_PATH = symbolicDbPath;
    }
}

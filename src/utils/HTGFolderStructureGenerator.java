package utils;

import com.google.common.collect.Lists;
import com.hstairs.ppmajal.conditions.*;
import com.hstairs.ppmajal.domain.ActionSchema;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.domain.Type;
import com.hstairs.ppmajal.domain.Variable;
import com.hstairs.ppmajal.problem.*;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HTGFolderStructureGenerator {


    public static PddlDomain domain;
    public static EPddlProblem problem;

    public static int id = 0;

    public static String powerliftedPath = "";
    public static HashMap<Type, ArrayList<PDDLObject>> typeObjectMap = null;

    public static final AtomicBoolean foundPlan = new AtomicBoolean();
    public static final AtomicBoolean keepSearching = new AtomicBoolean();


    public static boolean hasSubdirectories(String folderPath) {

        File[] directories = new File(folderPath).listFiles(File::isDirectory);

        return (directories.length == 0) ? false : true;

    }

    public static void setPowerliftedPath(String plp) {
        powerliftedPath = plp;
    }

    //add typing (if missing) + replace 'at' with 'AT' :))
    public static void changeFilesCozOfFunnyEnricoBugs(String domainFile, String problemFile) {

        try {

            //domain

            File file = new File(domainFile);

            String content = FileUtils.readFileToString(file, "UTF-8");

            Pattern pattern = Pattern.compile("\\(:requirements.*\\)");

            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String match = matcher.group();

                if (!match.contains(":typing")) {

                    StringBuffer typing = new StringBuffer();
                    typing.append(match, 0, match.length() - 1);
                    typing.append(" :typing)\n(:types object)");
                    content = content.replaceAll(pattern.toString(), typing.toString());

                }
            }

            content = content.replaceAll("at ", "AT ");

            FileUtils.writeStringToFile(file, content, "UTF-8");

            //problem

            file = new File(problemFile);

            content = FileUtils.readFileToString(file, "UTF-8");

            pattern = Pattern.compile("\\(:objects(\\s.*?)*\\)");

            matcher = pattern.matcher(content);

            if (matcher.find()) {

                String match = matcher.group();

                if (!match.contains(" - ")) {
                    StringBuffer typing = new StringBuffer();
                    String substr = match.substring(0, match.length() - 1).trim();
                    typing.append(substr);
                    typing.append(" - object\n)");
                    content = content.replaceAll(pattern.toString(), typing.toString());
                }
            }


            content = content.replaceAll("at ", "AT ");

            FileUtils.writeStringToFile(file, content, "UTF-8");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void initializeDomain(String domainFile, String problemFile) throws Exception {


        System.out.println(problemFile);

        changeFilesCozOfFunnyEnricoBugs(domainFile, problemFile);


        domain = new PddlDomain(domainFile);
        problem = new EPddlProblem(problemFile, domain.getConstants(), domain.types, domain);

        problem.simplifyAndSetupInit(false, false);
    }

    public static int getRandomNumber(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    public static HashMap<Type, ArrayList<PDDLObject>> createTypeObjectMap() {

        HashMap<Type, ArrayList<PDDLObject>> typeObjectMap = new HashMap<>();

        for (PDDLObject obj : problem.getObjects()) {
            if (typeObjectMap.keySet().contains(obj.getType())) {
                typeObjectMap.get(obj.getType()).add(obj);
            } else {
                ArrayList<PDDLObject> objects = new ArrayList<>();
                objects.add(obj);
                typeObjectMap.put(obj.getType(), objects);
            }
        }

        return typeObjectMap;
    }

    public static ArrayList<GroundAction> groundActions(HashMap<Type, ArrayList<PDDLObject>> objects) {

        ArrayList<GroundAction> groundedActions = new ArrayList<>();

        for (ActionSchema as : domain.getActionsSchema()) {

            if (objects.isEmpty()) {
                GroundAction fake = as.fakeGround(problem);
                groundedActions.add(fake);
                continue;
            }

            ArrayList parameters = new ArrayList();

            for (Variable v : (ArrayList<Variable>) as.getPar()) {
                parameters.add(objects.get(v.getType()));
            }

            for (List<PDDLObject> list : (List<List>) Lists.cartesianProduct(parameters)) {

                HashMap<Variable, PDDLObject> substitution = new HashMap<>();
                HashSet<PDDLObject> objs = new HashSet<>();

                for (int i = 0; i < as.getPar().size(); i++) {
                    substitution.put((Variable) as.getPar().get(i), list.get(i));
                    objs.add(list.get(i));
                }

                groundedActions.add(as.ground((Map) substitution, new PDDLObjects(objs), problem));

            }
        }

        return groundedActions;

    }

    public static ArrayList<GroundAction> findSupporters(Condition con, ArrayList<GroundAction> actions) {
        ArrayList<GroundAction> supporters = new ArrayList<>();

        for (GroundAction ga : actions) {
            if (ga.getAddList().getTerminalConditions().contains(con)) {
                supporters.add(ga);
            }
        }

        return supporters;
    }

    public static ArrayList<ArrayList<Predicate>> createRandomGoals(int numberOfGoals, int[] goalSize) {
        id = 555;

        HashMap<Type, ArrayList<PDDLObject>> objects = new HashMap<>();

        ArrayList<ArrayList<Predicate>> goals = new ArrayList<>();

        typeObjectMap = createTypeObjectMap();

        for (int i = 0; i < numberOfGoals; i++) {

            ArrayList<Predicate> predicates = new ArrayList<>();
            predicates.addAll(domain.getPredicates());

            RelState goalState = ((PDDLState) problem.getInit()).relaxState();

            ArrayList<Predicate> goalConds = new ArrayList<>();

            while (goalConds.size() < goalSize[i]) {

                HashMap<Type, ArrayList<PDDLObject>> tmpObjects = new HashMap<>(objects);

                Predicate p = predicates.get(getRandomNumber(0, predicates.size()));

                if (p.getTerms().size() == 0) {
                    predicates.remove(p);
                }

                HashMap<Variable, PDDLObject> substitution = new HashMap<>();

                for (Variable v : (ArrayList<Variable>) p.getTerms()) {

                    PDDLObject o = typeObjectMap.get(v.getType()).get(getRandomNumber(0, typeObjectMap.get(v.getType()).size()));
                    substitution.put(v, o);

                    if (tmpObjects.keySet().contains(o.getType())) {
                        tmpObjects.get(o.getType()).add(o);
                    } else {
                        ArrayList<PDDLObject> list = new ArrayList<>();
                        list.add(o);
                        tmpObjects.put(o.getType(), list);
                    }

                }

                Predicate groundedPredicate = (Predicate) p.ground(substitution, id++);


                //skip if chosen random predicate is already true in initial state
                if (problem.getInitBoolFluentValue(groundedPredicate) == true) {

                    continue;
                }


                objects = tmpObjects;
                goalConds.add(groundedPredicate);

            }

            goals.add(goalConds);
        }

        return goals;
    }

    public static boolean generateObservations(String domainPath, String problemFolderPath) throws IOException {

        File templateFile = new File(problemFolderPath + "/template.pddl");
        File realHypFile = new File(problemFolderPath + "/real_hyp.dat");

        String template = FileUtils.readFileToString(templateFile, "UTF-8");
        String realHyp = FileUtils.readFileToString(realHypFile, "UTF-8");

        System.out.println("Generating Observations for Goal: "+realHyp);

        String problem = template.replaceFirst("<HYPOTHESIS>", realHyp);

        FileUtils.writeStringToFile(templateFile, problem, "UTF-8");

        foundPlan.set(false);
        keepSearching.set(true);

        try {

            StringBuilder obs = new StringBuilder();

            Thread powerlifted = new Thread() {

                public void run() {

                    ProcessBuilder processBuilder = new ProcessBuilder();

                    processBuilder.command("cmd.exe", "/c", "python", powerliftedPath, "-d", domainPath, "-i", (problemFolderPath + "/template.pddl"), "-s", "alt-bfws1", "-e", "ff", "-g", "yannakakis");

                    Process process = null;
                    try {
                        process = processBuilder.start();


                        BufferedReader reader = new BufferedReader(
                                new InputStreamReader(process.getInputStream()));

                        boolean readPlan = false;

                        String line;

                        while (keepSearching.get() && !foundPlan.get()) {
                            if (reader.ready()) {
                                if ((line = reader.readLine()) != null) {

                                    if (line.equalsIgnoreCase("PLAN START")) {
                                        readPlan = true;
                                    } else if (line.equalsIgnoreCase("PLAN END")) {
                                        readPlan = false;
                                        foundPlan.set(true);
                                    } else if (readPlan == true) {
                                        obs.append(line + "\n");
                                    }

                                }
                            }
                        }

                        if (!keepSearching.get() || foundPlan.get()) {
                            process.descendants().forEach(processHandle -> processHandle.destroyForcibly());
                            process.destroyForcibly();
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };

            powerlifted.start();

            long start = System.currentTimeMillis();
            long end = start + 10000;


            while (!foundPlan.get() && (System.currentTimeMillis() < end)) {
                //wait
            }

            if (foundPlan.get()) {
                System.out.println("Observations successfully generated!");
                File obsFile = new File(problemFolderPath + "/obs.dat");
                FileUtils.writeStringToFile(obsFile, obs.toString(), "UTF-8");
                FileUtils.writeStringToFile(templateFile, template, "UTF-8");
                return true;
            } else {
                keepSearching.set(false);
                System.out.println("--");
                System.out.println(obs.toString());
                System.out.println("Error generating Observations!");
                FileUtils.writeStringToFile(templateFile, template, "UTF-8");
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        FileUtils.writeStringToFile(templateFile, template, "UTF-8");
        return false;
    }

    public static void generateFolderStructure(String htgSourceFolder, String destinationFolder, String configFolder, int numGoals, int[] goalSize) throws Exception {
        File[] files = new File(htgSourceFolder).listFiles();

        int i = 1;
        for (File f : files) {

            if (f.getName().equals("domain.pddl")) {
                continue;
            }

            initializeDomain(htgSourceFolder + "/domain.pddl", f.getAbsolutePath());

            String template = FileUtils.readFileToString(f, "UTF-8");

            template = template.replaceFirst("\\(:goal(\\s.*)*", "(:goal (and\n<HYPOTHESIS>\n))\n)");

            File destination = new File(destinationFolder);

            File directory = new File(destinationFolder + "/" + destination.getName() + "-p" + String.format("%02d", i++));
            if (!directory.exists()) {
                directory.mkdirs();
            }

            //copy config

            File configSource = new File(configFolder);
            File configDestination = new File(directory.getAbsolutePath() + "/config");
            FileUtils.copyDirectory(configSource, configDestination);

            //create goals

            ArrayList<ArrayList<Predicate>> goals = createRandomGoals(numGoals, goalSize);

            //goals.dat

            StringBuffer goalsDatTxt = new StringBuffer();

            StringBuffer hypsDatTxt = new StringBuffer();

            int j = 0;
            for (ArrayList<Predicate> goalConds : goals) {

                for (Predicate p : goalConds) {
                    goalsDatTxt.append(p + " ");
                    hypsDatTxt.append(p + " ");

                }

                goalsDatTxt.deleteCharAt(goalsDatTxt.length() - 1);
                goalsDatTxt.append("\n");

                hypsDatTxt.deleteCharAt(hypsDatTxt.length() - 1);
                hypsDatTxt.append(";hyp" + j++);
                hypsDatTxt.append("\n");

            }

            goalsDatTxt.deleteCharAt(goalsDatTxt.length() - 1);

            hypsDatTxt.deleteCharAt(goalsDatTxt.length() - 1);


            File goalsDatFile = new File(directory.getAbsolutePath() + "/goals.dat");

            FileUtils.writeStringToFile(goalsDatFile, goalsDatTxt.toString(), "UTF-8");

            //planning setups

            File planningSetups = new File(directory.getAbsolutePath() + "/planningSetups");
            if (!planningSetups.exists()) {
                planningSetups.mkdirs();
            }

            File commonFiles = new File(planningSetups.getAbsolutePath() + "/commonFiles");
            if (!commonFiles.exists()) {
                commonFiles.mkdirs();
            }

            FileUtils.copyFile(new File(htgSourceFolder + "/domain.pddl"), new File(commonFiles.getAbsolutePath() + "/domain.pddl"));

            File hypsFile = new File(commonFiles.getAbsolutePath() + "/hyps.dat");

            FileUtils.writeStringToFile(hypsFile, hypsDatTxt.toString(), "UTF-8");

            int k = 0;
            for (ArrayList<Predicate> goalConds : goals) {

                File hypDir = new File(planningSetups.getAbsolutePath() + "/hyp" + k++ + "_0");

                if (!hypDir.exists()) {
                    hypDir.mkdirs();
                }

                StringBuffer realHypDatTxt = new StringBuffer();

                for (Predicate p : goalConds) {
                    realHypDatTxt.append(p + " ");
                }
                realHypDatTxt.deleteCharAt(realHypDatTxt.length() - 1);

                File realHypFile = new File(hypDir.getAbsolutePath() + "/real_hyp.dat");

                FileUtils.writeStringToFile(realHypFile, realHypDatTxt.toString(), "UTF-8");


                File templateFile = new File(hypDir.getAbsolutePath() + "/template.pddl");

                FileUtils.writeStringToFile(templateFile, template, "UTF-8");

                generateObservations(htgSourceFolder + "/domain.pddl", hypDir.getAbsolutePath());

            }

        }

    }


}

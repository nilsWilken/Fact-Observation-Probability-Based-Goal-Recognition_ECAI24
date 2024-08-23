package utils;

import com.hstairs.ppmajal.conditions.NotCond;
import com.hstairs.ppmajal.conditions.PDDLObject;
import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MergedPredicate {

    private static PddlDomain domain;
    private static EPddlProblem problem;
    private static HashSet<MergedPredicate> templates;
    private ArrayList<String> name;
    //possible variables for each variable in the variables list
    private ArrayList<HashSet<PDDLObject>> possibleAssignments;
    //Mapping from Predicate A to Predicate B, boolean = same value
    //relevant coz variables can differ only in one spot, but they dont have to be in the same order
    //e.g. when merging Predicate (AT package1 airport1) and (in package1 truck1), the mapping would be [(0,true),(1,false)],
    //because the first variable of AT (~the first element in the variableMappingList) corresponds to the first variable in the
    //in Predicate and they're the same, while the second variable from the AT predicate and the in predicate are not the same (~false)
    //if the predicates were (AT airport1 package1) and (in package1 truck1) instead, the mapping would be [(1,false),(0,true)]
    private ArrayList<Pair<Integer, Boolean>> variableMapping;
    //actual variable assignment
    private ArrayList<PDDLObject> variables;
    private int templateID;


    private MergedPredicate() {
        name = new ArrayList<>();
        possibleAssignments = new ArrayList<>();
        variableMapping = new ArrayList<>();
    }

    //creates new MergedPredicate by providing a predicate for the variables and a MergedPredicate template
    public MergedPredicate(Predicate p, MergedPredicate template) {

        variables = new ArrayList<>();

        name = template.name;
        possibleAssignments = template.possibleAssignments;
        variableMapping = template.variableMapping;

        if (p.getPredicateName().equals(name.get(0))) {
            for (Object o : p.getInvolvedVariables()) {
                variables.add((PDDLObject) o);
            }
        } else {
            for (Pair<Integer, Boolean> mapping : variableMapping) {
                Object o = p.getInvolvedVariables().get(mapping.getKey());
                variables.add((PDDLObject) o);
            }
        }
    }


    public static void init(PddlDomain d, EPddlProblem p) {
        domain = d;
        problem = p;
        templates = new HashSet<>();
    }

    //checks if two MergedPredicates have the same fixed variables
    public boolean hasSameFixedVariables(MergedPredicate mp){

        if(!this.getName().containsAll(mp.getName())){
            return false;
        }

        boolean same = true;

        int i=0;
        for(Pair<Integer, Boolean> p : this.variableMapping){
            if(p.getValue() == true){

                if(!this.variables.get(i).equals(mp.variables.get(i))){
                    same = false;
                    break;
                }
            }
        }

        return same;
    }

    public void setVariables(ArrayList<PDDLObject> variables){
        this.variables = variables;
    }

    //tries to merge Predicate a from the add list of an action with NotCond b from the delete list of an action
    public static MergedPredicate merge(Predicate a, NotCond b) {

        for (MergedPredicate mp : templates) {

            if (a.getPredicateName().equals(mp.getName().get(0)) && ((Predicate) b.getSon()).getPredicateName().equals(mp.getName().get(1))) {

                ArrayList aVars = a.getInvolvedVariables();
                ArrayList bVars = b.getSon().getInvolvedVariables();

                boolean mergeable = true;

                for (int i = 0; i < aVars.size(); i++) {
                    if (mp.variableMapping.get(i).getValue() == true) {
                        mergeable = aVars.get(i).equals(bVars.get(mp.variableMapping.get(i).getKey()));
                    }

                    if (!mergeable) {
                        return null;
                    }
                }

                return new MergedPredicate(a, mp);

            } else if (a.getPredicateName().equals(mp.getName().get(1)) && ((Predicate) b.getSon()).getPredicateName().equals(mp.getName().get(0))) {

                ArrayList aVars = a.getInvolvedVariables();
                ArrayList bVars = b.getSon().getInvolvedVariables();

                boolean mergeable = true;

                for (int i = 0; i < bVars.size(); i++) {
                    if (mp.variableMapping.get(i).getValue() == true) {
                        mergeable = bVars.get(i).equals(aVars.get(mp.variableMapping.get(i).getKey()));
                    }

                    if (!mergeable) {
                        return null;
                    }
                }

                return new MergedPredicate(a, mp);
            } else if (a.getPredicateName().equals(mp.getName().get(0)) && ((Predicate) b.getSon()).getPredicateName().equals(mp.getName().get(0))) {

                ArrayList aVars = a.getInvolvedVariables();
                ArrayList bVars = b.getSon().getInvolvedVariables();

                boolean mergeable = true;

                for (int i = 0; i < aVars.size(); i++) {
                    if (mp.variableMapping.get(i).getValue() == true) {
                        mergeable = aVars.get(i).equals(bVars.get(i));
                    }

                    if (!mergeable) {
                        return null;
                    }
                }

                return new MergedPredicate(a, mp);

            } else if (a.getPredicateName().equals(mp.getName().get(1)) && ((Predicate) b.getSon()).getPredicateName().equals(mp.getName().get(1))) {

                ArrayList aVars = a.getInvolvedVariables();
                ArrayList bVars = b.getSon().getInvolvedVariables();

                boolean mergeable = true;

                for (int i = 0; i < aVars.size(); i++) {
                    if (mp.variableMapping.get(i).getValue() == true) {
                        mergeable =
                                aVars.get(mp.variableMapping.get(i).getKey()).equals(bVars.get(mp.variableMapping.get(i).getKey()));
                    }

                    if (!mergeable) {
                        return null;
                    }
                }

                return new MergedPredicate(a, mp);

            }
        }

        return null;
    }

    //creates a MergedPredicate Template (variables are empty)
    public static MergedPredicate createMergedPredicateTemplate(Predicate a, Predicate b) {

        MergedPredicate mp = new MergedPredicate();
        mp.templateID = templates.size();

        mp.name.add(a.getPredicateName());
        mp.name.add(b.getPredicateName());

        for (int i = 0; i < a.getInvolvedVariables().size(); i++) {
            for (int j = 0; j < b.getInvolvedVariables().size(); j++) {

                Object aObj = a.getInvolvedVariables().get(i);
                Object bObj = b.getInvolvedVariables().get(j);
                PDDLObject aOb = (PDDLObject) aObj;
                PDDLObject bOb = (PDDLObject) bObj;

                if (aOb.equals(bOb)) {
                    mp.variableMapping.add(new ImmutablePair<>(j, true));
                }
            }

            if (mp.variableMapping.size() < (i + 1)) {
                mp.variableMapping.add(new ImmutablePair<>(-1, false));
            }
        }

        if (a.getInvolvedVariables().size() == b.getInvolvedVariables().size()) {

            //find the not matching argument
            for (int i = 0; i < mp.variableMapping.size(); i++) {

                boolean matchingArgumentFound = false;
                for (Pair<Integer, Boolean> p : mp.variableMapping) {
                    if (p.getKey().equals(i)) {
                        matchingArgumentFound = true;
                        break;
                    }
                }
                int indexOfNonMatchingArg = -1;

                if (!matchingArgumentFound) {

                    for (int j = 0; j < mp.variableMapping.size(); j++) {
                        if (mp.variableMapping.get(j).getKey().equals(-1)) {
                            indexOfNonMatchingArg = j;
                            break;
                        }
                    }

                    mp.variableMapping.remove(indexOfNonMatchingArg);
                    mp.variableMapping.add(indexOfNonMatchingArg, new ImmutablePair<>(i, false));

                }
            }
        }

        for (Predicate p : (Set<Predicate>) problem.getActualFluents().keySet()) {

            if (p.getPredicateName().equals(a.getPredicateName())) {
                for (int i = 0; i < p.getInvolvedVariables().size(); i++) {

                    try {
                        mp.possibleAssignments.get(i);

                    } catch (IndexOutOfBoundsException e) {
                        HashSet<PDDLObject> assignment = new HashSet<>();
                        mp.possibleAssignments.add(i, assignment);
                    }

                    Object o = p.getInvolvedVariables().get(i);
                    PDDLObject ob = (PDDLObject) o;
                    mp.possibleAssignments.get(i).add(ob);

                }
            } else if (p.getPredicateName().equals(b.getPredicateName())) {
                for (int i = 0; i < p.getInvolvedVariables().size(); i++) {

                    if (mp.possibleAssignments.get(mp.variableMapping.get(i).getKey()) == null) {
                        HashSet<PDDLObject> assignment = new HashSet<>();
                        mp.possibleAssignments.add(mp.variableMapping.get(i).getKey(), assignment);
                    }
                    Object o = p.getInvolvedVariables().get(i);
                    PDDLObject ob = (PDDLObject) o;
                    mp.possibleAssignments.get(mp.variableMapping.get(i).getKey()).add(ob);
                }
            }
        }

        templates.add(mp);

        return mp;

    }

    public static HashSet<MergedPredicate> getMergedPredicateTemplates() {
        return templates;
    }

    public ArrayList<String> getName() {
        return name;
    }

    public String toString() {

        StringBuffer sb = new StringBuffer();

        sb.append(name.get(0) + "-" + name.get(1) + " ");

        if (variables != null) {
            for (PDDLObject var : variables) {
                sb.append(var.toString());
            }
        } else {
            sb.append("\n");
            for (HashSet<PDDLObject> assignment : possibleAssignments) {
                sb.append(assignment.toString() + "\n");
            }
        }
        sb.append(variableMapping);

        return sb.toString();

    }

    public ArrayList<HashSet<PDDLObject>> getPossibleAssignments() {
        return possibleAssignments;
    }

    public final int hashCode() {

        int hashCode = templateID * 17;

        if (variables != null) {
            for (PDDLObject o : variables) {
                hashCode = hashCode + o.hashCode() * 31;
            }
        }

        return hashCode;

    }

    public boolean equals(Object o) {

        if (o instanceof MergedPredicate) {
            MergedPredicate mp = (MergedPredicate) o;
            if (this.getName().containsAll(mp.getName())) {
                if (this.variables.containsAll(mp.variables)) {
                    return true;
                }
            }
        }

        return false;
    }
}

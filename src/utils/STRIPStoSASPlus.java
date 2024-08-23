package utils;

import com.hstairs.ppmajal.conditions.Predicate;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.domain.Variable;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.RelState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class STRIPStoSASPlus {

    private PddlDomain domain;
    private EPddlProblem problem;
    private static HashSet<Predicate> groundedPredicates;
    private static HashSet<Predicate> constantPredicates;
    private static HashSet<Predicate> oneWayPredicates;
    private static HashSet<Predicate> reachablePredicates;
    private static HashSet<HashSet<String>> mergedPredicates;


    //initializes with already grounded domain and problem
    public void init(PddlDomain d, EPddlProblem p){
        this.domain = d;
        this.problem = p;

        getAllGroundedPredicates();

        constantPredicates = new HashSet<>();
        oneWayPredicates = new HashSet<>();
        mergedPredicates = new HashSet<>();

        MergedPredicate.init(domain, problem);
    }

    //initializes with paths to domain and problem
    public void init(String dPath, String pPath) {

        domain = new PddlDomain(dPath);
        problem = new EPddlProblem(pPath, domain.getConstants(), domain.types, domain);

        domain.substituteEqualityConditions();

        try {
            problem.transformGoal();

            problem.groundingActionProcessesConstraints();

            problem.simplifyAndSetupInit(false, false);

        } catch (Exception e) {
            e.printStackTrace();
        }

        getAllGroundedPredicates();

        constantPredicates = new HashSet<>();
        oneWayPredicates = new HashSet<>();
        mergedPredicates = new HashSet<>();

        MergedPredicate.init(domain, problem);


    }


    //returns all grounded predicates
    public static void getAllGroundedPredicates() {
        groundedPredicates = new HashSet<Predicate>();

        for (Map.Entry e : Predicate.idToPredicate.entrySet()) {

            Predicate p = (Predicate) e.getValue();

            if (p.isGrounded()) {

                groundedPredicates.add(p);

            } else {

                boolean grounded = true;

                for (Object o : p.getTerms()) {
                    if (o instanceof Variable) {
                        grounded = false;
                        break;
                    }
                }

                if (grounded) {
                    groundedPredicates.add(p);

                }
            }
        }
    }

    //finds all constant and one-way predicates
    public void findConstantAndOneWayPredicates() {

        for (Predicate p : groundedPredicates) {

            boolean inAddEffects = false;
            boolean inDelEffects = false;

            for (GroundAction ga : (Collection<GroundAction>) problem.getActions()) {

                if (inAddEffects && inDelEffects) {
                    break;
                }

                if (inAddEffects == false && ga.getAddList().getInvolvedPredicates().contains(p)) {
                    inAddEffects = true;
                }

                if (inDelEffects == false && ga.getDelList().getInvolvedPredicates().contains(p)) {
                    inDelEffects = true;
                }

            }

            if (!inAddEffects && !inDelEffects) {
                constantPredicates.add(p);
            } else if ((inAddEffects && !inDelEffects) || (!inAddEffects && inDelEffects)) {
                oneWayPredicates.add(p);
            }
        }
    }

    //checks if two predicates can be merged, by searching a delete effect that balances out the add effect
    public boolean checkIfValidMerge(Predicate a, Predicate b) {

        boolean valid = true;

        for (GroundAction ga : (Collection<GroundAction>) problem.getActions()) {

            if (!valid) {
                break;
            }

            for (Predicate add : ga.getAddList().getInvolvedPredicates()) {

                if (!valid) {
                    break;
                }

                if (add.getPredicateName().equals(a.getPredicateName())) {

                    ArrayList<Variable> addVars = add.getInvolvedVariables();

                    boolean fittingDelPredFound = false;

                    for (Predicate del : ga.getDelList().getInvolvedPredicates()) {

                        if (del.getPredicateName().equals(b.getPredicateName()) || del.getPredicateName().equals(a.getPredicateName())) {

                            ArrayList<Variable> delVars = add.getInvolvedVariables();


                            if (addVars.size() == delVars.size()) {

                                //arguments differ only in one spot (or not at all)
                                addVars.retainAll(delVars);
                                if ((addVars.size() == (delVars.size() - 1)) || (addVars.size() == (delVars.size()))) {
                                    //valid
                                    fittingDelPredFound = true;
                                } else {
                                    //invalid
                                    valid = false;
                                    break;
                                }
                            } else if ((addVars.size() - 1) == delVars.size()) {

                                addVars.retainAll(delVars);
                                if (addVars.size() == delVars.size()) {
                                    //valid
                                    fittingDelPredFound = true;
                                } else {
                                    //invalid
                                    valid = false;
                                    break;
                                }
                            } else {
                                valid = false;
                                break;
                            }
                        }

                        if (!fittingDelPredFound) {
                            valid = false;
                            break;
                        }
                    }
                }
            }
        }

        System.out.println(a + " - " + b + "    is valid: " + valid);

        return valid;
    }


    //checks if the two predicates are already in the pool of merged predicates
    public boolean checkIfPredsAreAlreadyMerged(Predicate a, Predicate b) {

        for (HashSet<String> mergedPred : mergedPredicates) {

            if (mergedPred.contains(a.getPredicateName()) && mergedPred.contains(b.getPredicateName())) {
                return true;
            }
        }

        return false;
    }

    //tries to merge more than two predicates by merging a merged predicate with another normal predicate
    public void mergeMergedPredicates() {

        boolean sthChanged = true;

        while (sthChanged) {
            sthChanged = false;

            for (HashSet<String> mergedPred : mergedPredicates) {

                for (GroundAction ga : (Collection<GroundAction>) problem.getActions()) {

                    for (Predicate add : ga.getAddList().getInvolvedPredicates()) {
                        if (mergedPred.contains(add.getPredicateName())) {

                            HashSet<String> copy = new HashSet<>(mergedPred);
                            copy.remove(add.getPredicateName());

                            HashSet<String> delPreds = new HashSet<>();

                            for (Predicate del : ga.getDelList().getInvolvedPredicates()) {
                                delPreds.add(del.getPredicateName());
                            }

                            if (delPreds.containsAll(copy)) {

                                for (Predicate del : ga.getDelList().getInvolvedPredicates()) {

                                    if (copy.contains(del.getPredicateName())) {
                                        continue;
                                    }

                                    ArrayList<Variable> addVars = add.getInvolvedVariables();
                                    ArrayList<Variable> delVars = del.getInvolvedVariables();

                                    if (addVars.size() == delVars.size()) {

                                        //arguments differ only in one spot (or not at all)
                                        addVars.retainAll(delVars);
                                        if ((addVars.size() == (delVars.size() - 1)) || (addVars.size() == (delVars.size()))) {

                                            if (checkIfValidMerge(add, del)) {

                                                mergedPred.add(del.getPredicateName());
                                                sthChanged = true;
                                            }
                                        }
                                    } else if ((addVars.size() - 1) == delVars.size()) {

                                        addVars.retainAll(delVars);
                                        if (addVars.size() == delVars.size()) {

                                            if (checkIfValidMerge(add, del)) {
                                                mergedPred.add(del.getPredicateName());
                                                sthChanged = true;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    //merges Predicates
    public void mergePredicates() {

        for (Predicate p : groundedPredicates) {

            for (GroundAction ga : (Collection<GroundAction>) problem.getActions()) {

                if (ga.getAddList().getInvolvedPredicates().contains(p)) {

                    boolean balanced = false;

                    for (Predicate del : ga.getDelList().getInvolvedPredicates()) {

                        //there is a corresponding delete effect
                        if (del.getPredicateName().equals(p.getPredicateName())) {

                            ArrayList<Variable> addVars = p.getInvolvedVariables();
                            ArrayList<Variable> delVars = del.getInvolvedVariables();

                            if (addVars.size() == delVars.size()) {

                                //arguments differ only in one spot (or not at all)
                                addVars.retainAll(delVars);
                                if ((addVars.size() == (delVars.size() - 1)) || (addVars.size() == (delVars.size()))) {

                                    balanced = true;
                                    break;
                                }
                            }
                        }
                    }


                    // continue only if not balanced???
                    if (balanced == false) {

                        for (Predicate del : ga.getDelList().getInvolvedPredicates()) {

                            if (checkIfPredsAreAlreadyMerged(p, del)) {
                                continue;
                            }

                            ArrayList<Variable> addVars = p.getInvolvedVariables();
                            ArrayList<Variable> delVars = del.getInvolvedVariables();

                            if (addVars.size() == delVars.size()) {

                                //arguments differ only in one spot (or not at all)
                                addVars.retainAll(delVars);
                                if ((addVars.size() == (delVars.size() - 1)) || (addVars.size() == (delVars.size()))) {

                                    System.out.println("Possible merged Predicate");
                                    ArrayList<Predicate> mergedPreds = new ArrayList<>();
                                    mergedPreds.add(p);
                                    mergedPreds.add(del);

                                    if (checkIfValidMerge(p, del)) {
                                        HashSet<String> merge = new HashSet<>();
                                        merge.add(p.getPredicateName());
                                        merge.add(del.getPredicateName());
                                        mergedPredicates.add(merge);

                                        MergedPredicate.createMergedPredicateTemplate(p, del);

                                    }
                                }
                            } else if ((addVars.size() - 1) == delVars.size()) {

                                addVars.retainAll(delVars);
                                if (addVars.size() == delVars.size()) {

                                    System.out.println("Possible merged Predicate");
                                    ArrayList<Predicate> mergedPreds = new ArrayList<>();
                                    mergedPreds.add(p);
                                    mergedPreds.add(del);

                                    if (checkIfValidMerge(p, del)) {
                                        HashSet<String> merge = new HashSet<>();
                                        merge.add(p.getPredicateName());
                                        merge.add(del.getPredicateName());
                                        mergedPredicates.add(merge);

                                        MergedPredicate.createMergedPredicateTemplate(p, del);

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    // explores predicate space (based on Edelkamp, S., & Helmert, M. (1999, September). Exhibiting knowledge in planning problems to minimize state encoding length.)
    public void explorePredicateSpace() {

        Queue<Predicate> queue = new LinkedList<>();
        reachablePredicates = new HashSet<>();

        RelState state = new RelState();

        for (Predicate p : problem.getPredicatesInvolvedInInit()) {
            queue.add(p);
        }

        while (!queue.isEmpty()) {

            Predicate p = queue.poll();
            reachablePredicates.add(p);
            state.makePositive(p);

            for (GroundAction ga : (Collection<GroundAction>) problem.getActions()) {
                if (ga.getPreconditions().isSatisfied(state) && ga.getPreconditions().getInvolvedPredicates().contains(p)) {

                    HashSet<Predicate> newPredicates = (HashSet<Predicate>) ga.getAddList().getInvolvedPredicates();
                    newPredicates.removeAll(reachablePredicates);
                    newPredicates.removeAll(queue);

                    queue.addAll(newPredicates);
                }
            }
        }
    }

    //converts STRIPS domain and problem into SAS+ representation
    public static void convert(PddlDomain domain, EPddlProblem problem){

        STRIPStoSASPlus stripsToSAS = new STRIPStoSASPlus();

        stripsToSAS.init(domain, problem);

        stripsToSAS.findConstantAndOneWayPredicates();
        stripsToSAS.explorePredicateSpace();
        stripsToSAS.mergePredicates();

        for(GroundAction ga : (Collection<GroundAction>)stripsToSAS.problem.getActions()){
            Action a = Action.createActionFromGA(ga);
        }
    }

    public static HashSet<Predicate> getReachablePredicates(){
        return reachablePredicates;
    }


    public static HashSet<Predicate> getGroundedPredicates() {
        return groundedPredicates;
    }

    public static void main(String[] args) {

        String domainPath = "";
        String problemPath = "";


        STRIPStoSASPlus stripsToSAS = new STRIPStoSASPlus();

        stripsToSAS.init(domainPath, problemPath);

        System.out.println("All: "+groundedPredicates.size());

        stripsToSAS.findConstantAndOneWayPredicates();
        System.out.println("Constant: "+constantPredicates.size());

        System.out.println("OneWay: "+oneWayPredicates.size());

        stripsToSAS.explorePredicateSpace();
        System.out.println("Reachable: "+reachablePredicates.size());



        stripsToSAS.mergePredicates();

        stripsToSAS.mergeMergedPredicates();

        for (MergedPredicate mp : MergedPredicate.getMergedPredicateTemplates()) {
            System.out.println(mp.getPossibleAssignments().get(0).size());
            System.out.println(mp.getPossibleAssignments().get(1).size());

        }


        //changes ppmajal groundaction to action to accommodate SAS+ representation
        for(GroundAction ga : (Collection<GroundAction>)stripsToSAS.problem.getActions()){
            Action a = Action.createActionFromGA(ga);
        }


    }

}

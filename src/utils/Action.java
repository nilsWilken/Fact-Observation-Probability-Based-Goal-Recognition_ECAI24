package utils;

import com.hstairs.ppmajal.conditions.*;
import com.hstairs.ppmajal.domain.ParametersAsTerms;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.State;

import java.util.*;

public class Action {

    private String name;
    private ParametersAsTerms parameters;
    private ArrayList<Object> effect;
    private HashSet<Object> preconditions;
    private static HashSet<Action> actions;


    public String getName() {
        return name;
    }

    public ParametersAsTerms getParameters() {
        return parameters;
    }

    public ArrayList<Object> getEffect() {
        return effect;
    }

    public HashSet<Object> getPreconditions() {
        return preconditions;
    }

    public static HashSet<Action> getActions() {
        return actions;
    }

    //creates an action from a groundaction by combining the add and delete effects into one list and merging the appropriate predicates together
    public static Action createActionFromGA(GroundAction groundAction) {


        Action action = new Action();
        action.effect = new ArrayList<>();
        action.parameters = groundAction.getParameters();
        action.name = groundAction.getName();
        action.preconditions = new HashSet<>();

        for (Condition c : (Collection<Condition>) groundAction.getAddList().sons) {
            action.effect.add(c);
        }
        for (Condition c : (Collection<Condition>) groundAction.getDelList().sons) {
            action.effect.add(c);
        }

        HashSet<Object> consToRemove = new HashSet<>();
        HashSet<Object> mpsToAdd = new HashSet<>();

        for (MergedPredicate mp : MergedPredicate.getMergedPredicateTemplates()) {

            Iterator<Object> iterA = action.effect.listIterator();
            Iterator<Object> iterB = action.effect.listIterator();

            while (iterA.hasNext()) {

                Condition conA = (Condition) iterA.next();

                if (conA instanceof Predicate && (((Predicate) conA).getPredicateName().equals(mp.getName().get(0)) || ((Predicate) conA).getPredicateName().equals(mp.getName().get(1)))) {
                    Predicate a = (Predicate) conA;

                    while (iterB.hasNext()) {

                        Condition conB = (Condition) iterB.next();

                        if (conB instanceof NotCond && (((Predicate) (((NotCond) conB).getSon())).getPredicateName().equals(mp.getName().get(0)) || ((Predicate) (((NotCond) conB).getSon())).getPredicateName().equals(mp.getName().get(1)))) {
                            NotCond b = (NotCond) conB;

                            MergedPredicate m = MergedPredicate.merge(a, b);
                            if (m != null) {
                                mpsToAdd.add(m);
                                consToRemove.add(a);
                                consToRemove.add(b);
                            }
                        }
                    }
                }
            }
        }

        action.effect.removeAll(consToRemove);
        action.effect.addAll(mpsToAdd);

        consToRemove.clear();
        mpsToAdd.clear();

        for (Object o : action.effect) {
            if (o instanceof Predicate) {
                for (MergedPredicate mp : MergedPredicate.getMergedPredicateTemplates()) {

                    if (mp.getName().contains(((Predicate) o).getPredicateName())) {
                        consToRemove.add(o);
                        mpsToAdd.add(new MergedPredicate((Predicate) o, mp));
                    }
                }
            }
        }

        action.effect.removeAll(consToRemove);
        action.effect.addAll(mpsToAdd);

        for (Condition con : (Collection<Condition>) groundAction.getPreconditions().sons) {

            if (con instanceof PDDLObjectsEquality) {
                PDDLObjectsEquality poe = (PDDLObjectsEquality) con;
                //preconditions can never be satisfied
                if (!poe.isSatisfied((State) null)) {
                    return null;
                }
            } else if (con instanceof NotCond && ((NotCond) con).getSon() instanceof PDDLObjectsEquality) {
                PDDLObjectsEquality poe = (PDDLObjectsEquality) ((NotCond) con).getSon();
                //preconditions can never be satisfied
                if (poe.isSatisfied((State) null)) {
                    return null;
                }
            } else if (con instanceof Predicate) {

                boolean foundFittingMP = false;
                for (MergedPredicate mp : MergedPredicate.getMergedPredicateTemplates()) {
                    if (mp.getName().contains(((Predicate) con).getPredicateName())) {

                        action.preconditions.add(new MergedPredicate((Predicate) con, mp));
                        foundFittingMP = true;
                        break;
                    }
                }

                if (!foundFittingMP) {
                    action.preconditions.add(con);
                }
            } else if (con instanceof NotCond) {
                //kommt bei uns nicht vor, nicht unser problem LOL
            }
        }

        if (actions == null) {
            actions = new HashSet<>();
        }

        actions.add(action);

        return action;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(name + " " + parameters + "\n");
        sb.append("Pre: " + preconditions + "\n");
        sb.append("Effect: " + effect);

        return sb.toString();
    }
}

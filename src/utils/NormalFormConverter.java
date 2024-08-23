package utils;

import com.hstairs.ppmajal.conditions.*;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.problem.EPddlProblem;
import com.hstairs.ppmajal.problem.GroundAction;
import com.hstairs.ppmajal.problem.State;
import org.logicng.datastructures.Substitution;
import org.logicng.formulas.*;
import org.logicng.io.parsers.ParserException;
import org.logicng.transformations.cnf.CNFConfig;
import org.logicng.transformations.simplification.DefaultRatingFunction;
import org.logicng.transformations.simplification.FactorOutSimplifier;

import java.util.*;

import static org.logicng.transformations.cnf.CNFConfig.builder;

public class NormalFormConverter {

    public FormulaFactory ff;
    public HashMap<Condition, Literal> map;
    public HashMap<String, Condition> reverseMap;
    public boolean ALLOW_AUXILIARY_VARIABLES = false;
    public boolean USE_SIMPLIFIER = true;
    public CNFConfig.Algorithm cnfAlgo = CNFConfig.Algorithm.FACTORIZATION; // CNFConfig.Algorithm.BDD or CNFConfig.Algorithm
    // .FACTORIZATION
    public Substitution auxVarMapping = new Substitution();


    public NormalFormConverter() {

        ff = new FormulaFactory();
        map = new HashMap<>();

        if (!ALLOW_AUXILIARY_VARIABLES) {
            CNFConfig.Builder builder = builder();
            builder.algorithm(cnfAlgo);
            CNFConfig config = builder.build();

            ff.putConfiguration(config);
        }
    }

    //generates reverse map of the formula string values to condition values map
    public void generateReverseMap() {

        if (reverseMap == null) {
            reverseMap = new HashMap<>();
        }
        for (Map.Entry<Condition, Literal> e : map.entrySet()) {
            reverseMap.put(e.getValue().name(), e.getKey());
        }
    }


    //converts formula back to ppmajal condition
    public Condition convertCNFBackToCondition(Formula formula) {

        if (formula.type() == FType.AND) {

            AndCond and = new AndCond();

            Iterator<Formula> iter = formula.iterator();

            while (iter.hasNext()) {

                Formula form = iter.next();
                and.addConditions(convertCNFBackToCondition(form));
            }

            return and;

        } else if (formula.type() == FType.OR) {

            OrCond or = new OrCond();

            Iterator<Formula> iter = formula.iterator();

            while (iter.hasNext()) {

                Formula form = iter.next();
                or.addConditions(convertCNFBackToCondition(form));
            }

            return or;

        } else if (formula.type() == FType.LITERAL) {

            Literal l = (Literal) formula;

            Condition c = reverseMap.get(l.name());

            if (l.phase() == false) {
                return NotCond.createNotCond(c);
            }

            return c;

        } else if (formula.type() == FType.TRUE) {

            return Predicate.createPredicate(Predicate.trueFalse.TRUE);

        } else if (formula.type() == FType.FALSE) {

            return Predicate.createPredicate(Predicate.trueFalse.FALSE);

        } else {

            System.out.println("Weird formula type: " + formula.type());
        }

        return null;
    }


    //simplifies and returns condition by evaluating all object equalities and applying factor out simplification
    public Condition simplifyCondition(Condition condition) {

        String formula = this.convertConditionToCNFString(condition);

        Formula f = null;
        try {
            f = this.ff.parse(formula);
        } catch (ParserException e) {
            e.printStackTrace();
        }


        FactorOutSimplifier simplifier = new FactorOutSimplifier(new DefaultRatingFunction());

        f = simplifier.apply(f, false);


        this.generateReverseMap();

        return this.convertCNFBackToCondition(f);

    }

    //transforms condition to CNF (only works for simple conditions, where no auxiliary variables are required)
    //if auxiliary variables are allowed, they will simply get replaced with "true" for for negated variables and "false" for normal auxiliary variables
    public Condition convertConditionToCNF(Condition condition) {

        String formula = this.convertConditionToCNFString(condition);

        Formula f = null;
        try {
            f = this.ff.parse(formula);
        } catch (ParserException e) {
            e.printStackTrace();
        }


        if (ALLOW_AUXILIARY_VARIABLES) {

            f = f.cnf();

            String form = f.toString();

            System.out.println(form);

            form = form.replaceAll("~@RESERVED_CNF_[0-9]*", "\\$true");

            form = form.replaceAll("@RESERVED_CNF_[0-9]*", "\\$false");
            System.out.println(form);

            try {
                f = this.ff.parse(form);
            } catch (ParserException e) {
                e.printStackTrace();
            }

            System.out.println(f);

        }


        if (USE_SIMPLIFIER) {
            FactorOutSimplifier simplifier = new FactorOutSimplifier(new DefaultRatingFunction());

            f = simplifier.apply(f, false);
        }

        this.generateReverseMap();

        return this.convertCNFBackToCondition(f);
    }

    //converts ppmajal condition to string that is readable by the formula library (also evaluates objectequalitites and replaces them with true or false)
    public String convertConditionToCNFString(Condition condition) {

        if (condition instanceof Terminal) {

            if (condition instanceof PDDLObjectsEquality) {

                PDDLObjectsEquality poe = (PDDLObjectsEquality) condition;
                if (poe.isSatisfied((State) null)) {
                    return "$true";
                } else {
                    return "$false";
                }

            } else if (condition instanceof NotCond) {

                NotCond nc = (NotCond) condition;

                if (nc.getSon() instanceof Predicate) {

                    if (map.containsKey(nc.getSon())) {

                        return "~" + map.get(nc.getSon()).name();

                    } else {

                        Variable v = ff.variable("var" + map.size());
                        map.put(nc.getSon(), v);

                        return "~" + v.name();
                    }
                }
                //son is objectequality
                else {

                    PDDLObjectsEquality poe = (PDDLObjectsEquality) nc.getSon();
                    if (poe.isSatisfied((State) null)) {
                        return "$false";
                    } else {
                        return "$true";
                    }
                }
            }
            //Predicate or Comparison
            else {

                if (map.containsKey(condition)) {
                    return map.get(condition).name();
                } else {
                    Variable v = ff.variable("var" + map.size());
                    map.put(condition, v);
                    return v.name();
                }
            }
        }
        //condition is complex cond
        else {

            if (condition instanceof AndCond) {

                StringBuffer sb = new StringBuffer("( ");
                int i = 0;
                for (Condition c : (Collection<Condition>) ((AndCond) condition).sons) {

                    if (i == ((AndCond) condition).sons.size() - 1) {
                        sb.append(convertConditionToCNFString(c) + " )");
                    } else {
                        sb.append(convertConditionToCNFString(c) + " & ");
                    }
                    i++;
                }

                return sb.toString();

            } else if (condition instanceof OrCond) {

                StringBuffer sb = new StringBuffer("( ");
                int i = 0;
                for (Condition c : (Collection<Condition>) ((OrCond) condition).sons) {

                    if (i == ((OrCond) condition).sons.size() - 1) {
                        sb.append(convertConditionToCNFString(c) + " )");
                    } else {
                        sb.append(convertConditionToCNFString(c) + " | ");
                    }
                    i++;
                }

                return sb.toString();

            } else {

                System.out.println("Weird Condition: " + condition.getClass());
            }
        }

        return "Couldn't parse condition";
    }


    //grounds, simplifies and saves domain
    //@param simplifiedDomainPath path where the simplified domain should be saved to
    public static void simplifyAndSaveDomain(String domainPath, String problemPath, String simplifiedDomainPath) throws Exception {

        PddlDomain domain = new PddlDomain(domainPath);
        EPddlProblem problem = new EPddlProblem(problemPath, domain.getConstants(), domain.types, domain);

        domain.substituteEqualityConditions();

        problem.transformGoal();
        problem.groundingActionProcessesConstraints();

        problem.simplifyAndSetupInit(false, false);

        NormalFormConverter nfc = new NormalFormConverter();

        HashSet<GroundAction> actionsToRemove = new HashSet<>();

        //set to true if activation condition of conditional effects should also be simplified
        boolean simplifyCondEffects = true;

        boolean unsatisfiable = false;

        for (GroundAction ga : (Collection<GroundAction>) problem.getActions()) {

            Condition simpleCond = nfc.simplifyCondition(ga.getPreconditions());

            if (simplifyCondEffects) {

                HashSet<ConditionalEffect> cEffectsToRemove = new HashSet<>();
                for (ConditionalEffect cEffect : (Collection<ConditionalEffect>) ga.cond_effects.sons) {

                    Condition simpleActivationCond = nfc.simplifyCondition(cEffect.activation_condition);

                    if (simpleActivationCond instanceof Predicate) {
                        if (simpleCond.isUnsatisfiable()) {
                            cEffectsToRemove.add(cEffect);
                            unsatisfiable = true;
                        } else {
                            AndCond cond = new AndCond();
                            cond.addConditions(simpleActivationCond);
                            simpleActivationCond = cond;
                        }
                    }

                    if (!unsatisfiable) {
                        cEffect.activation_condition = simpleActivationCond;
                    }
                }

                ga.cond_effects.sons.removeAll(cEffectsToRemove);
            }

            unsatisfiable = false;

            if (simpleCond instanceof Predicate) {
                if (simpleCond.isUnsatisfiable()) {
                    actionsToRemove.add(ga);
                    unsatisfiable = true;
                } else {
                    AndCond cond = new AndCond();
                    cond.addConditions(simpleCond);
                    simpleCond = cond;
                }
            }

            if (!unsatisfiable) {
                ga.setPreconditions((ComplexCondition) simpleCond);
            }
        }

        System.out.println(actionsToRemove.size());
        problem.getActions().removeAll(actionsToRemove);

        problem.saveGroundedDomain(simplifiedDomainPath);
    }


    public static void main(String[] args) throws Exception {

        String domainPath =
                "";
        String problemPath = "";

        PddlDomain domain = new PddlDomain(domainPath);
        EPddlProblem problem = new EPddlProblem(problemPath, domain.getConstants(), domain.types, domain);

        domain.substituteEqualityConditions();

        problem.transformGoal();
        problem.groundingActionProcessesConstraints();

        problem.simplifyAndSetupInit(false, false);

        String simplifiedDomainPath = "";

        simplifyAndSaveDomain(domainPath, problemPath, simplifiedDomainPath);
    }
}

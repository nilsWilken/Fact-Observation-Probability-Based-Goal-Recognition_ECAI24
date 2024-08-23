package symbolic.relaxedPlanning.utils;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.hstairs.ppmajal.conditions.PDDLObject;
import com.hstairs.ppmajal.problem.GroundAction;

import symbolic.planning.stateHandler.obsConversion.GMPDDLStateHandler;


public class FilterGroundActions {

	public static Collection<GroundAction> filterUniqueActions(String domain, String problem, String obsPath) {

		GMPDDLStateHandler stateHandler = new GMPDDLStateHandler(domain, problem);

		String[] filenames;
		File f = new File(obsPath);
		filenames = f.list();

		HashSet<GroundAction> uniqueActions = new HashSet<GroundAction>();

		int actionsOverall = 0;

		for (String file : filenames) {

			List<GroundAction> obs = stateHandler.retrieveActionListFromObsFile(obsPath + "/" + file);

			actionsOverall += obs.size();

			uniqueActions.addAll(obs);
		}

		System.out.println("Filter Actions...");
		System.out.println("All GroundActions: " + stateHandler.getProblem().getActions().size());
		System.out.println("Filtered Actions: " + uniqueActions.size()+"\n");

		return uniqueActions;

	}

	public static Collection<GroundAction> filterTouchedObjects(String domain, String problem, Set<GroundAction> observedActions, String goal) {

		long startTime = System.currentTimeMillis();
		
		Collection<GroundAction> filteredActions = new LinkedHashSet<GroundAction>();

		GMPDDLStateHandler stateHandler = new GMPDDLStateHandler(domain, problem);

		HashSet<PDDLObject> touchedObjects = new HashSet<PDDLObject>();
		for (GroundAction ga : observedActions) {
			touchedObjects.addAll(ga.getParameters());
		}
		
		for (GroundAction ga : (Collection<GroundAction>) stateHandler.getProblem().getActions()) {
			if (!Collections.disjoint(ga.getParameters(), touchedObjects)) {
				filteredActions.add(ga);
			}
		}
		
		filteredActions.addAll(observedActions);
		
		System.out.println("Filter Actions...");
		System.out.println("All GroundActions: " + stateHandler.getProblem().getActions().size());
		System.out.println("Filtered Actions: " + filteredActions.size());
		System.out.println("Filtering time: " + (System.currentTimeMillis() - startTime)+"\n");

		return filteredActions;

	}

}

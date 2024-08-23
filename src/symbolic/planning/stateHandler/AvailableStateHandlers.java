package symbolic.planning.stateHandler;

import symbolic.planning.stateHandler.obsConversion.GMPDDLStateHandler;
import symbolic.planning.stateHandler.obsConversion.IObsConversionStateHandler;
import symbolic.planning.stateHandler.planSampling.IPlanSamplingStateHandler;

public enum AvailableStateHandlers {
	
	GOAL_MIRRORING_STATE_HANDLER;
	
	public static IObsConversionStateHandler getObsConversionStateHandler(String configuredStateHandler, String domainFile, String problemFile) {
		switch(configuredStateHandler.trim().toLowerCase()) {
		case "gmpddlstatehandler":
			return new GMPDDLStateHandler(domainFile, problemFile);
		default:
			return new GMPDDLStateHandler(domainFile, problemFile);
		}
	}
	
	public static IPlanSamplingStateHandler getPlanSamplingStateHandler(String configuredStateHandler, String domainFile, String problemFile)  {
		return null;
	}

}

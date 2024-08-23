package utils;

import java.util.Random;

import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.variables.Variable;
import eu.amidst.dynamic.datastream.DynamicDataInstance;
import eu.amidst.dynamic.models.DynamicBayesianNetwork;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkGenerator;
import eu.amidst.dynamic.utils.DynamicBayesianNetworkSampler;

public class AmidstTesting {
	
	public static void main(String[] args) {
        Random random = new Random(1);

        //We first generate a dynamic Bayesian network (NB structure with class and attributes temporally linked)
        DynamicBayesianNetworkGenerator.setNumberOfContinuousVars(2);
        DynamicBayesianNetworkGenerator.setNumberOfDiscreteVars(5);
        DynamicBayesianNetworkGenerator.setNumberOfStates(3);
        DynamicBayesianNetwork extendedDBN = DynamicBayesianNetworkGenerator.generateDynamicNaiveBayes(random, 2, true);

        System.out.println(extendedDBN.toString());

        //We select the target variable for inference, in this case the class variable
        Variable classVar = extendedDBN.getDynamicVariables().getVariableByName("ClassVar");

        //We create a dynamic dataset with 3 sequences for prediction. The class var is made hidden.
        DynamicBayesianNetworkSampler dynamicSampler = new DynamicBayesianNetworkSampler(extendedDBN);
        dynamicSampler.setHiddenVar(classVar);
        DataStream<DynamicDataInstance> dataPredict = dynamicSampler.sampleToDataBase(1, 10);
        
        for(DynamicDataInstance inst : dataPredict) {
        	for(Variable var : extendedDBN.getDynamicVariables()) {
            	System.out.println(inst.getValue(var));
        	}
        }
	}

}

package dataDriven.bayes.netTranslation.bifXML;

import java.io.IOException;
import java.util.List;

import eu.amidst.core.io.BayesianNetworkWriter;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.models.ParentSet;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

public class BNBIFXMLHandler extends BIFXMLHandler {
	
	private Variables networkVariables;

	public BNBIFXMLHandler(String name) {
		super(name);
		this.networkVariables = new Variables();
	}
	
	@Override
	public void addNode(String name, List<String> values) {
		super.addNode(name, values);
		
		this.networkVariables.newMultinomialVariable(name, values);
	}
	
	@Override
	public void addArc(String parent, String child) {
		super.addArc(parent, child);
	}
	
	public BayesianNetwork convertToAmidstBN() {
		DAG DAG = new DAG(this.networkVariables);
		ParentSet cParentSet;
		Variable parentVar;
		for(Variable var : this.networkVariables.getListOfVariables()) {
			cParentSet = DAG.getParentSet(var);
			
			//If the number of parent assignments is larger than the maximum integer value (this breaks the framework)
			// if(this.connections.get(var.getName()).size() > 30) {
			// 	continue;
			// }
			for(String parent : this.connections.get(var.getName())) {
				parentVar = this.networkVariables.getVariableByName(parent);
				cParentSet.addParent(parentVar);
			}
		}
		return new BayesianNetwork(DAG);
	}
	
	public void printAmidstNetworkToConsole() {
		BayesianNetwork bn = this.convertToAmidstBN();
		System.out.println(bn);
	}
	
	public void writeNetworkAsAmidstFile(String path) {
		try {
			BayesianNetworkWriter.save(this.convertToAmidstBN(), path);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

}

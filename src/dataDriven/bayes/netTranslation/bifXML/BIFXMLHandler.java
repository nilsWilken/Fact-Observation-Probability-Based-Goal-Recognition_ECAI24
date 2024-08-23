package dataDriven.bayes.netTranslation.bifXML;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.jdom2.Attribute;
import org.jdom2.DocType;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

public class BIFXMLHandler {
	
	protected static final String bifDocType = "\t<!ELEMENT BIF ( NETWORK )*>\r\n" + 
			"	\t<!ATTLIST BIF VERSION CDATA #REQUIRED>\r\n" + 
			"	<!ELEMENT NETWORK ( NAME, ( PROPERTY | VARIABLE | DEFINITION )* )>\r\n" + 
			"	<!ELEMENT NAME (#PCDATA)>\r\n" + 
			"	<!ELEMENT VARIABLE ( NAME, ( OUTCOME |  PROPERTY )* ) >\r\n" + 
			"	\t<!ATTLIST VARIABLE TYPE (nature|decision|utility) \"nature\">\r\n" + 
			"	<!ELEMENT OUTCOME (#PCDATA)>\r\n" + 
			"	<!ELEMENT DEFINITION ( FOR | GIVEN | TABLE | PROPERTY )* >\r\n" + 
			"	<!ELEMENT FOR (#PCDATA)>\r\n" + 
			"	<!ELEMENT GIVEN (#PCDATA)>\r\n" + 
			"	<!ELEMENT TABLE (#PCDATA)>\r\n" + 
			"	<!ELEMENT PROPERTY (#PCDATA)>\n";
	
	protected static final DecimalFormat dFormat = new DecimalFormat("0.00000", new DecimalFormatSymbols(Locale.ENGLISH));
	
	private Document bifDoc;
	private Element networkEle;
	private String name;
	protected HashMap<String, Element> nodeElements;
	protected HashMap<String, List<String>> connections;
	
	protected BIFXMLHandler(String name) {
		this.bifDoc = new Document();
		DocType docType = new DocType("BIF");
		docType.setInternalSubset(BIFXMLHandler.bifDocType);
		this.bifDoc.setDocType(docType);
		
		Element root = new Element("BIF");
		Attribute version = new Attribute("VERSION", "0.3");
		root.setAttribute(version);
		this.bifDoc.setRootElement(root);
		
		this.networkEle = new Element("NETWORK");
		root.addContent(this.networkEle);
		
		this.name = name;
		Element nameEle = new Element("NAME");
		nameEle.setText(this.name);
		this.networkEle.addContent(nameEle);
		
		this.nodeElements = new HashMap<String, Element>();
		this.connections = new HashMap<String, List<String>>();
	}

	public HashMap<String, List<String>> getConnections() {
		return this.connections;
	}
	
	protected void addNode(String name, List<String> values) {
		Element node = new Element("VARIABLE");
		Attribute type = new Attribute("TYPE", "nature");
		node.setAttribute(type);
		
		Element nameEle = new Element("NAME");
		nameEle.setText(name);
		node.addContent(nameEle);
		
		Element valueEle;
		
		for(String value : values) {
			valueEle = new Element("OUTCOME");
			valueEle.setText(value);
			node.addContent(valueEle);
		}
		
		this.networkEle.addContent(node);
		this.nodeElements.put(name, node);
		this.connections.put(name, new ArrayList<String>());
	}
	
	protected void addArc(String parent, String child) {
		List<String> parents = this.connections.get(child);
		
		if(parents == null) {
			parents = new ArrayList<String>();
		}
		if(parents.contains(parent)) {
			return;
		}
		
		parents.add(parent);
		this.connections.put(child, parents);
	}
	
	public void writeDefinedNetworkToFile(String path) {
		try {
			if(path.toLowerCase().endsWith(".net")) {
				path = path.replace(".net", ".xml");
			}
			else if(!path.toLowerCase().endsWith(".xml")) {
				path = path + ".xml";
			}
			XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
			out.output(this.addConnections(), new FileWriter(new File(path)));
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	public void printDefinedNetworkToConsole() {
		try {
			XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
			out.output(this.addConnections(), System.out);
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	protected int getAmountOfValues(String nodeName) {
		return this.nodeElements.get(nodeName).getChildren("OUTCOME").size();
	}
	
	protected Document addConnections() {
		Document result = this.bifDoc.clone();
		Element network = result.getRootElement().getChild("NETWORK");
		
		for(String child : this.connections.keySet()) {
			Element definitionEle = new Element("DEFINITION");
			Element childEle = new Element("FOR");
			childEle.setText(child);
			definitionEle.addContent(childEle);
			
			for(String parent : this.connections.get(child)) {
				Element parentEle = new Element("GIVEN");
				parentEle.setText(parent);
				definitionEle.addContent(parentEle);
			}
			
			
			Element tableEle = new Element("TABLE");
			tableEle.setText(this.createProbabilitiesString(this.calcAmountOfRows(child), this.getAmountOfValues(child)));
			
			definitionEle.addContent(tableEle);
			
			network.addContent(definitionEle);
		}
		
		return result;
	}
	
	protected long calcAmountOfRows(String child) {
		long rows = 1L;
		
		for(String parent : this.connections.get(child)) {
			rows *= this.getAmountOfValues(parent);
		}
		return rows;
	}
	
	protected String createProbabilitiesString(long rows, int cols) {
		StringBuffer buff = new StringBuffer();
		double prob = 1.0/(float)cols;
		for(long i=0; i < rows; i++) {
			for(int j=0; j < cols; j++) {
				buff.append(BIFXMLHandler.dFormat.format(prob) + " ");
			}
		}
		
		return buff.toString();
	}
}

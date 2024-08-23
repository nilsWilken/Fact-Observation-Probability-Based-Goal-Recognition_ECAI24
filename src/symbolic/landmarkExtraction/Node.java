package symbolic.landmarkExtraction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import com.hstairs.ppmajal.conditions.Condition;

public class Node {
    HashMap<Integer, ArrayList<Node>> previous = new HashMap<Integer, ArrayList<Node>>();
	LinkedHashSet<Node> next = new LinkedHashSet<Node>();
	Condition node;
	HashSet<Integer> lmSetIDs = new HashSet<Integer>();
	int id;

	public Node() {
	}

	public Node(Condition c, int id) {
		this.id = id;
		this.node = c;
	}

	public HashSet<Integer> getLmSetIDs() {

		return lmSetIDs;

	}

	public void addLmSetID(Integer i) {
		this.lmSetIDs.add(i);
	}

	public Node(Condition c, Node next) {
		this.node = c;
		this.next.add(next);
	}

	public void addPrevious(Integer i, Node p) {

		if (previous.get(i) != null) {

			previous.get(i).add(p);

		} else {

			ArrayList<Node> list = new ArrayList<Node>();
			list.add(p);
			previous.put(i, list);
		}

	}

	public void addNext(Node p) {

		this.next.add(p);
	}

	public Condition getNode() {

		return node;
	}

	public HashMap<Integer, ArrayList<Node>> getPrev() {
		return previous;
	}

	public HashSet<Node> getNext() {
		return next;
	}

	public String toString() {

		StringBuffer sb = new StringBuffer("[  ");

		for (Integer key : previous.keySet()) {

			for (Node n : previous.get(key)) {

				sb.append(n.getNode() + ", ");

			}
			
			sb.append("| "+key+"; ");

		}

		sb.append("--->");

		sb.append(node);
		sb.append("| " + lmSetIDs);

		sb.append("--->");

		for (Node p : next) {

			sb.append(p.getNode() + "; ");

		}

		sb.append("]  ");

		return sb.toString();
	}
}

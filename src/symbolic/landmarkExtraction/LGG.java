package symbolic.landmarkExtraction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.hstairs.ppmajal.conditions.ComplexCondition;
import com.hstairs.ppmajal.conditions.Condition;
import com.hstairs.ppmajal.conditions.NotCond;
import com.hstairs.ppmajal.conditions.Predicate;

public class LGG {
    HashSet<Node> nodes = new HashSet<Node>();
	LinkedHashSet<Condition> facts = new LinkedHashSet<Condition>();
	ComplexCondition goals;
	HashMap<Integer, ArrayList<Condition>> LMSets = new HashMap<Integer, ArrayList<Condition>>();
	HashMap<Integer, ArrayList<Condition>> uniqueLMSetsGoalsSeparate = new HashMap<Integer, ArrayList<Condition>>();
	int ID = 0;

	public LGG() {

	}

	public boolean containsNode(Condition p) {

		if (p instanceof Predicate || p instanceof NotCond) {
			return facts.contains(p);
		} else if(p instanceof ComplexCondition) {
			return facts.contains((ComplexCondition)p);
		}

		return false;
	}

	public void addNode(Condition node) {

		Node n = new Node(node, ID);
		ID++;
		nodes.add(n);
		
		if (node instanceof Predicate) {
			facts.add(node);
		} else if (node instanceof NotCond) {
			facts.add(node);
		}
		else if(node instanceof ComplexCondition) {
			facts.addAll(((ComplexCondition) node).sons);
		}

	}

	public void removeNode(Condition node) {

		Node nodeToRemove = null;

		for (Node n : nodes) {

			if (n.getNode().equals(node)) {

				for (Integer key : n.getPrev().keySet()) {
					for (Node prev : n.getPrev().get(key)) {

						prev.getNext().remove(this.getNodeFromCond(node));
					}
				}

				for (Node next : n.getNext()) {

					for (Integer key : next.getPrev().keySet()) {

						next.getPrev().get(key).removeIf(x -> x.node.equals(node));

						if (next.getPrev().get(key).isEmpty()) {

							next.getPrev().remove(key);
						}

					}
				}

				nodeToRemove = n;
				break;

			}

		}

		if (nodeToRemove != null) {
			facts.remove(nodeToRemove.node);
			nodes.remove(nodeToRemove);
		} else {
			System.out.println("Didnt find node to remove");
		}

	}

	public LinkedHashSet<Condition> getFacts() {

		return this.facts;
	}

	public void initialize(ComplexCondition g) {

		this.goals = g;

		for (Condition c : (Collection<Condition>) g.sons) {
			this.addNode(c);
			this.getNodeFromCond(c).addLmSetID(0);
		}

	}

	public Node getNodeFromCond(Condition p) {

		for (Node n : nodes) {
			if (n.getNode().equals(p)) {
				return n;
			}
		}

		return null;

	}

	public void addEdge(Condition node, Condition next, Integer i) {

		boolean foundNode = false;
		boolean foundNext = false;

		Node nodee = null;
		Node nextt = null;

		if ((node instanceof Predicate || node instanceof NotCond || node instanceof ComplexCondition) && (next instanceof Predicate || next instanceof NotCond || next instanceof ComplexCondition)) {

			for (Node n : nodes) {

				if (!foundNode && n.node.equals(node)) {
					nodee = n;
					foundNode = true;
				}

				if (!foundNext && n.node.equals(next)) {
					nextt = n;
					foundNext = true;
				}

			}
		}

		if (foundNode == false || foundNext == false) {
			System.out.println("Couldnt find node -> foundNode = " + foundNode + ", foundNext = " + foundNext);
		}

		nodee.addNext(nextt);
		nextt.addPrevious(i, nodee);

	}

	public HashSet<Node> getNodes() {

		return this.nodes;
	}

	public void getAllPredecessors(Node n, MultiValuedMap<Integer, Node> predecessors) {
		for (Integer key : n.getPrev().keySet()) {
			for (Node prev : n.getPrev().get(key)) {
				
				if(!predecessors.containsValue(prev)) {
					predecessors.put(key, prev);
					getAllPredecessors(prev, predecessors);
				}
			}
		}
	}

	public void calulateLMSets() {

		for (Node n : nodes) {
			for (Integer i : n.lmSetIDs) {

				if (LMSets.get(i) != null) {
					LMSets.get(i).add(n.getNode());
				} else {
					ArrayList<Condition> list = new ArrayList<Condition>();
					list.add(n.getNode());
					LMSets.put(i, list);
				}
			}
		}
		
		ArrayListValuedHashMap<Integer, Condition> tmp = calculateUniqueLMSetsButGoalsSeparate();

		for (Integer key : tmp.keySet()) {
			uniqueLMSetsGoalsSeparate.put(key, new ArrayList<Condition>(tmp.get(key)));
		}

	}

	public MultiValuedMap<Integer, Condition> getLMSets() {

		ArrayListValuedHashMap<Integer, Condition> map = new ArrayListValuedHashMap<Integer, Condition>();

		for (Integer key : LMSets.keySet()) {
			map.putAll(key, LMSets.get(key));
		}

		return map;
	}

	public MultiValuedMap<Integer, Condition> getUniqueLMSetsButGoalsSeparate() {
		ArrayListValuedHashMap<Integer, Condition> map = new ArrayListValuedHashMap<Integer, Condition>();

		for (Integer key : this.uniqueLMSetsGoalsSeparate.keySet()) {
			map.putAll(key, this.uniqueLMSetsGoalsSeparate.get(key));
		}
		return map;
	}

	public ArrayListValuedHashMap<Integer, Condition> calculateUniqueLMSetsButGoalsSeparate() {

		ArrayListValuedHashMap<Integer, Condition> LMSetsGoalsSeparate = new ArrayListValuedHashMap<>();
		int count = 0;
		for (Integer setID : LMSets.keySet()) {

			if (setID == 0) {
				for (Condition c : LMSets.get(setID)) {
					LMSetsGoalsSeparate.put(count, c);
					count--;
				}
			} else {
				boolean duplicate = false;
				for (Integer ID : LMSetsGoalsSeparate.keySet()) {

					if (LMSets.get(setID).equals(LMSetsGoalsSeparate.get(ID))) {
						duplicate = true;
						break;
					}
				}
				if (duplicate) {
					System.out.println("SKIPPING DUPLICATE SETS");
					continue;
				}
				LMSetsGoalsSeparate.putAll(setID, LMSets.get(setID));
			}
		}

		return LMSetsGoalsSeparate;
	}
}

package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import Controllers.MainController;
import sun.java2d.pipe.OutlineTextRenderer;

public class Graph {

	private HashMap<Vertex, List<Vertex>> m_neighbors;
	private HashMap<Vertex, Double> m_selfLoopProb;
	private HashMap<Vertex, List<Double>> m_probabilities;
	private HashMap<Vertex, List<List<List<String>>>> m_causeSubSets;

	public Graph() {
		m_neighbors = new HashMap<Vertex, List<Vertex>>();
		m_probabilities = new HashMap<Vertex, List<Double>>();
		m_causeSubSets = new HashMap<Vertex, List<List<List<String>>>>();
		m_selfLoopProb = new HashMap<Vertex, Double>();
	}

	public void calculateEdgesProbabilities() {
		for (Vertex v : m_neighbors.keySet()) {
			double temp = 0;
			List<Vertex> neighbors = m_neighbors.get(v);
			List<List<List<String>>> verCauseSubSets = m_causeSubSets.get(v);
			for (int i = 0; i < neighbors.size(); i++) {
				double edgeProb = 0;
				Vertex n = neighbors.get(i);
				List<List<String>> edgeCauseSubSets = verCauseSubSets.get(i);
				int colNum = n.getTuple().getColumns().size();
				List<Double> probByColNum = edgesProbByColNum(colNum, 0.1);
				for (List<String> edgeCauseSubSet : edgeCauseSubSets) {
					edgeProb += probByColNum.get(edgeCauseSubSet.size());
				}
				//if (edgeProb > 1) {
				//	System.out.println("FATAL in calculateEdgesProbabilities prob sum are > 1");
				//	System.exit(0);
				//}

				m_probabilities.get(v).add(edgeProb);
				temp = m_selfLoopProb.get(v);
				m_selfLoopProb.put(v, temp + (1 - edgeProb));
			}

			// Normalize out going edges probs
			double outProbSum = m_selfLoopProb.get(v);
			for (int i = 0; i < m_neighbors.get(v).size(); i++) {
				outProbSum += m_probabilities.get(v).get(i);
			}

			double currSelfLoopProb = m_selfLoopProb.get(v);
			currSelfLoopProb = currSelfLoopProb / outProbSum;
			m_selfLoopProb.put(v, currSelfLoopProb);
			for (int i = 0; i < m_neighbors.get(v).size(); i++) {
				temp = m_probabilities.get(v).get(i);
				temp = temp / outProbSum;
				m_probabilities.get(v).set(i, temp);
			}
		}
	}

	public Double getSelfLoopPobability(Vertex v) {
		if (!m_neighbors.containsKey(v)) {
			return null;
		}
		return m_selfLoopProb.get(v);
	}

	public Double getEdgeProbability(Vertex src, Vertex dst) {

		int neighborIndex = -1;
		List<Vertex> neighbors = m_neighbors.get(src);

		for (int i = 0; i < neighbors.size(); i++) {
			if (neighbors.get(i).equals(dst)) {
				neighborIndex = i;
				break;
			}
		}

		return m_probabilities.get(src).get(neighborIndex);
	}

	public boolean addVertex(Vertex v) {

		if (m_neighbors.containsKey(v)) {
			return false;
		}
		m_neighbors.put(v, new ArrayList<Vertex>());
		m_selfLoopProb.put(v, new Double(0));
		m_probabilities.put(v, new ArrayList<Double>());
		m_causeSubSets.put(v, new ArrayList<List<List<String>>>());
		return true;
	}

	public boolean addEdge(Vertex src, Vertex dst, List<List<String>> causeSubSets) {

		if (!m_neighbors.containsKey(src)) {
			return false;
		}

		if (!m_neighbors.containsKey(dst)) {
			return false;
		}

		if (causeSubSets.size() == 0) {
			System.out.println("FATAL in addEdge because the cause subsets size is zero!");
			System.exit(0);
		}

		if (!m_neighbors.get(src).contains(dst)) {
			m_neighbors.get(src).add(dst);
			m_causeSubSets.get(src).add(new ArrayList<List<String>>());
		}

		int edgeIndex = m_neighbors.get(src).indexOf(dst);
		List<List<String>> currCauseSetsList = m_causeSubSets.get(src).get(edgeIndex);
		for (List<String> causeSubSet : causeSubSets) {
			if (!containsList(currCauseSetsList, causeSubSet)) {
				currCauseSetsList.add(causeSubSet);
			}
		}

		return true;
	}

	public boolean addEdge(DBTuple srcTuple, DBTuple dstTuple, List<List<String>> causeSubSets) {

		Vertex src = null;
		Vertex dst = null;
		for (Vertex vertex : m_neighbors.keySet()) {
			if (vertex.getTuple().equals(srcTuple)) {
				src = vertex;
			}
			if (vertex.getTuple().equals(dstTuple)) {
				dst = vertex;
			}
		}

		return addEdge(src, dst, causeSubSets);
	}

	public Set<Vertex> getVertecis() {
		return m_neighbors.keySet();
	}

	public List<Vertex> getNeighbors(Vertex v) {

		List<Vertex> neighbors = m_neighbors.get(v);
		if (neighbors == null) {
			return new ArrayList<Vertex>();
		}
		return neighbors;
	}

	public boolean hasEdge(Vertex src, Vertex dst) {

		List<Vertex> neighbors = m_neighbors.get(src);
		if (neighbors != null && neighbors.contains(dst)) {
			return true;
		}
		return false;
	}

	@Override
	public String toString() {
		String result = System.lineSeparator() + "========= Graph =========" + System.lineSeparator();
		for (Vertex v : m_neighbors.keySet()) {
			result += v.getID() + " -> " + System.lineSeparator();
			for (Vertex n : m_neighbors.get(v)) {
				result += "    " + n.getID() + System.lineSeparator();
			}
		}
		result += toJSMapStr() + System.lineSeparator();
		result += "=========================" + System.lineSeparator();
		return result;
	}

	public String toJSMapStr() {
		
		MainController mainController = MainController.getInstance();
		String result = "{ \"neighbors\" : {";
		int vertexIndex = 0;
		for (Vertex v : m_neighbors.keySet()) {
			result += "\"" + v.getTuple().toString() + "\"";
			result += " : [";
			for (int i = 0; i < m_neighbors.get(v).size(); i++) {
				Vertex neighbor = m_neighbors.get(v).get(i);
				if (i < m_neighbors.get(v).size() - 1) {
					result += "{ ";
					result += "\"id\"" + " : " + "\"" + neighbor.getTuple().toString() + "\"" + ", ";
					result += "\"table\"" + " : " + "\"" + neighbor.getTuple().getTable() + "\"" + ", ";
					result += "\"probability\"" + " : " + "\"" + m_probabilities.get(v).get(i) + "\"";
					result += "}, ";

				} else {
					if (m_selfLoopProb.get(v) != null && m_selfLoopProb.get(v) > 0) {
						result += "{ ";
						result += "\"id\"" + " : " + "\"" + neighbor.getTuple().toString() + "\"" + ", ";
						result += "\"table\"" + " : " + "\"" + neighbor.getTuple().getTable() + "\"" + ", ";
						result += "\"probability\"" + " : " + "\"" + m_probabilities.get(v).get(i) + "\"";
						result += "}, ";

						result += "{ ";
						result += "\"id\"" + " : " + "\"" + v.getTuple().toString() + "\"" + ", ";
						result += "\"table\"" + " : " + "\"" + v.getTuple().getTable() + "\"" + ", ";
						result += "\"probability\"" + " : " + "\"" + m_selfLoopProb.get(v) + "\"";
						result += "} ";
					} else {
						result += "{ ";
						result += "\"id\"" + " : " + "\"" + neighbor.getTuple().toString() + "\"" + ", ";
						result += "\"table\"" + " : " + "\"" + neighbor.getTuple().getTable() + "\"" + ", ";
						result += "\"probability\"" + " : " + "\"" + m_probabilities.get(v).get(i) + "\"";
						result += "} ";
					}
				}
			}
			result += "]";

			if (vertexIndex < m_neighbors.keySet().size() - 1) {
				result += ", ";
			}

			vertexIndex++;
		}
		result += "}, \"tables_ids\": {";

		for (Vertex v : m_neighbors.keySet()) {
			result += "\"" + v.getTuple().toString() + "\"";
			result += " : ";
			result += mainController.getTableID(v.getTuple().getTable());

			if (vertexIndex < m_neighbors.keySet().size() - 1) {
				result += ", ";
			}

			vertexIndex++;
		}
		result += "}";
		
		return result;
	}

	public JSONObject toJSONObject() {

		MainController mainController = MainController.getInstance();
		JSONObject result = new JSONObject();
		JSONObject vertexToNeighbors = new JSONObject();
		JSONObject vertexToTableID = new JSONObject();

		for (Vertex v : m_neighbors.keySet()) {
			JSONArray neighborsArray = new JSONArray();
			for (int i = 0; i < m_neighbors.get(v).size(); i++) {
				Vertex neighbor = m_neighbors.get(v).get(i);
				double prob = m_probabilities.get(v).get(i);
				JSONObject nbrJsonObj = new JSONObject();
				nbrJsonObj.put("id", neighbor.getID());
				nbrJsonObj.put("table", neighbor.getTuple().getTable());
				nbrJsonObj.put("probability", new Double(prob));
				neighborsArray.add(nbrJsonObj);
			}
			if (m_selfLoopProb.get(v) != null && m_selfLoopProb.get(v) > 0) {
				JSONObject verJsonObj = new JSONObject();
				verJsonObj.put("id", v.getID());
				verJsonObj.put("table", v.getTuple().getTable());
				verJsonObj.put("probability", m_selfLoopProb.get(v));
				neighborsArray.add(verJsonObj);
			}

			vertexToNeighbors.put(v.getID(), neighborsArray);
			vertexToTableID.put(v.getID(), mainController.getTableID(v.getTuple().getTable()));
		}

		result.put("neighbors", vertexToNeighbors);
		result.put("tables_ids", vertexToTableID);
		return result;
	}

	private List<Double> edgesProbByColNum(int colCnt, double beta) {

		List<Double> result = new ArrayList<Double>();
		for (int i = 0; i <= colCnt; i++) {
			result.add(new Double(0));
		}
		double x = calculateX(colCnt, beta);
		for (int i = 0; i <= colCnt; i++) {
			result.set(i, x * getPropOneCoefficient(colCnt, i, beta));
		}
		return result;
	}

	private double calculateX(int colCnt, double beta) {
		double sum = 0;

		for (int i = 0; i <= colCnt; i++) {
			sum += getPropSumCoefficient(colCnt, i, beta);
		}
		return 1 / sum;
	}

	private double getPropSumCoefficient(int colCnt, int k, double beta) {
		return binomialCoefficient(colCnt, k) * getPropOneCoefficient(colCnt, k, beta);
	}

	private double getPropOneCoefficient(int colCnt, int k, double beta) {
		if (k > colCnt) {
			System.out.println("FATAL in calculateEdgesProbabilities prob sum are > 1");
			System.exit(0);
		}
		if (k == colCnt) {
			return 1;
		}
		double sum = 0;
		for (int i = k + 1; i <= colCnt; i++) {
			sum += getPropSumCoefficient(colCnt, i, beta);
		}
		return (1 + beta) * sum;
	}

	private int factorial(int n) {
		if (n == 0) {
			return 1;
		}
		if (n == 1) {
			return 1;
		}

		return n * factorial(n - 1);
	}

	private double binomialCoefficient(int n, int k) {
		return factorial(n) / (factorial(k) * factorial(n - k));
	}

	private boolean containsList(List<List<String>> lstCollection, List<String> lst) {

		for (List<String> item : lstCollection) {
			if (listsEqual(item, lst)) {
				return true;
			}
		}
		return false;
	}

	private boolean listsEqual(List<String> lst1, List<String> lst2) {

		for (String item : lst1) {
			if (!lst2.contains(item)) {
				return false;
			}
		}

		for (String item : lst2) {
			if (!lst1.contains(item)) {
				return false;
			}
		}

		return true;
	}
}

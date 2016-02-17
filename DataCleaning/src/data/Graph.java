package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class Graph {

	private HashMap<Vertex, List<Vertex>> m_neighbors;
	
	public Graph() {
		m_neighbors = new HashMap<Vertex, List<Vertex>>();
	}
	
	public boolean addVertex(Vertex v) {
		
		if (m_neighbors.containsKey(v)) {
			return false;
		}
		m_neighbors.put(v, new ArrayList<Vertex>());
		return true;
	}
	
	public boolean addEdge(Vertex src, Vertex dst) {

		if (!m_neighbors.containsKey(src)) {
			return false;
		}
		
		if (!m_neighbors.containsKey(dst)) {
			return false;
		}
		
		if (m_neighbors.get(src).contains(dst)) {
			return false;
		}
		m_neighbors.get(src).add(dst);
		return true;
	}
	
	public boolean addEdge(DBTuple srcTuple, DBTuple dstTuple) {

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
		
		return addEdge(src, dst);
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
		String result = "{";
		int vertexIndex = 0;
		for (Vertex v : m_neighbors.keySet()) {
			result += "\"" + v.getTuple().toString()  + "\"";
			result += " : [";
			for (int i = 0; i < m_neighbors.get(v).size(); i++) {
				Vertex neighbor = m_neighbors.get(v).get(i);
				if (i < m_neighbors.get(v).size() - 1) {
					result += "\"" + neighbor.getTuple().toString()  + "\", ";
				} else {
					result += "\"" + neighbor.getTuple().toString()  + "\"";
				}
			}
			result += "]";
			
			if (vertexIndex < m_neighbors.keySet().size() - 1) {
				result += ", ";
			}
			
			vertexIndex ++;
		}
		result += "}";
		return result;
	}
}

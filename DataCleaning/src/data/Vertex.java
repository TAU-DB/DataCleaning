package data;

public class Vertex {

	private DBTuple m_tuple;
	
	public Vertex(DBTuple tuple) {
		
		m_tuple = tuple;
	}

	public String getID() {
		return m_tuple.toString();
	}
	
	public DBTuple getTuple() {
		return m_tuple;
	}
	
	@Override
	public boolean equals(Object o) {
		Vertex v = (Vertex) o;
		
		if (!v.getID().equals(getID())) {
			return false;
		}
		
		return true;
	}
	
	@Override 
	public int hashCode() {
		return 0;
	}
}

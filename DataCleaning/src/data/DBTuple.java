package data;

import java.util.HashMap;
import java.util.Set;

public class DBTuple {
	
	private String m_table;
	private HashMap<String, String> m_values;
	private boolean m_isAnonymous = false;
	
	public DBTuple(String table, boolean isAnonymous) {
		m_table = table;
		m_isAnonymous = isAnonymous;
		m_values = new HashMap<String, String>();
	}
	
	public String getTable() {
		return m_table;
	}
	
	public void setValue(String column, String value) {
		m_values.put(column, value);
	}
	
	public boolean isAnonymous() {
		return m_isAnonymous;
	}
	
	public String getValue(String column) {
		return m_values.get(column);
	}
	
	public Set<String> getColumns() {
		return m_values.keySet();
	}
	
	@Override 
	public String toString() {
		return m_table + "(" + m_values.toString() + ")";
	}
	
	@Override
	public boolean equals(Object o) {
		DBTuple tuple = (DBTuple) o;
		
		if (!m_table.equals(tuple.getTable())) {
			return false;
		}
		if (m_isAnonymous != tuple.isAnonymous()) {
			return false;
		}
		
		for (String column : m_values.keySet()) {
			if (!tuple.getValue(column).equals(m_values.get(column))) {
				return false;
			}
		}
		
		for (String column : tuple.getColumns()) {
			if (!m_values.get(column).equals(tuple.getValue(column))) {
				return false;
			}
		}
		
		return true;
	}
	
	@Override 
	public int hashCode() {
		return 0;
	}
}

package data;

public class Variable {

	private String m_name;
	private String m_column;
	private boolean m_isConstant = false;
	private String m_value;
	private String m_type;

	public Variable(String name, String column, boolean isConstant, String value, String type) {
		m_name = name;
		m_column = column;
		m_isConstant = isConstant;
		m_value = value;
		m_type = type;
	}

	public void assign(String value) {
		m_name = value;
		m_isConstant = true;
		m_value = value;
	}

	public String getName() {
		return m_name;
	}

	public String getColumn() {
		return m_column;
	}

	public boolean isConstant() {
		return m_isConstant;
	}

	public String getValue() {
		return m_value;
	}

	public String getType() {
		return m_type;
	}

	@Override
	public String toString() {
		return m_isConstant ? m_value : m_name;
	}

	public String toDetailedString() {
		String name = m_name == null ? "null" : m_name;
		String column = m_column == null ? "null" : m_column;
		String isConstant = m_isConstant == false ? "false" : "true";
		String value = m_value == null ? "null" : m_value;
		return "name: " + name + " column: " + column + " isConstant: " + isConstant + " value: " + value;
	}
}

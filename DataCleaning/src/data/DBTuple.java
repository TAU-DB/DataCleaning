package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;

import Controllers.MainController;

public class DBTuple {

	private String m_table;
	private HashMap<String, String> m_values;
	private boolean m_isAnonymous = false;
	private List<String> m_causeColumns = null;
	private final boolean DISPLAY_COLUMNS_NAMES = false;

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

	public void addCauseColumn(String colName) {

		if (m_causeColumns == null) {
			m_causeColumns = new ArrayList<String>();
		}

		if (m_causeColumns.contains(colName)) {
			return;
		}
		m_causeColumns.add(colName);
	}

	public List<String> getCauseColumns() {
		return m_causeColumns;
	}

	@Override
	public String toString() {

		if (DISPLAY_COLUMNS_NAMES) {
			return toJSONObject().toJSONString();
		}

		MainController mainController = MainController.getInstance();
		List<String> columns = mainController.getTableColumns(m_table);
		String result = m_table + "(";
		for (int i = 0; i < columns.size(); i++) {
			result += m_values.get(columns.get(i));
			if (i < columns.size() - 1) {
				result += ",";
			}
		}
		result += ")";
		return result;
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

	public String getJSMapStr() {

		MainController mainController = MainController.getInstance();
		List<String> columns = mainController.getTableColumns(m_table);
		String result = "{";
		result += "table_name : " + "\"" + m_table + "\"" + ", ";
		result += "is_anonymous : " + (isAnonymous() ? 1 : 0) + ", ";
		result += "columns : {";
		int columnIndex = 0;
		for (String column : columns) {
			result += "\"" + column + "\"";
			result += " : " + "\"" + m_values.get(column) + "\"";

			if (columnIndex < m_values.keySet().size() - 1) {
				result += ", ";
			}
			columnIndex++;
		}
		result += "} }";
		return result;
	}

	public JSONObject toJSONObject() {

		MainController mainController = MainController.getInstance();
		List<String> columns = mainController.getTableColumns(m_table);
		JSONObject result = new JSONObject();
		result.put("table_name", m_table);
		result.put("is_anonymous", isAnonymous() ? 1 : 0);
		JSONObject jsonValues = new JSONObject();
		for (String column : columns) {
			jsonValues.put(column, m_values.get(column));
		}
		result.put("columns", jsonValues);
		result.put("column_list", columns);
		return result;
	}

}

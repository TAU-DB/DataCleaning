package data;

import java.util.List;

public class RelationalFormula extends Formula {

	private String m_table;

	public RelationalFormula(String table, List<Variable> variables) {
		super(variables);
		m_table = table;
	}

	public String getTable() {
		return m_table;
	}

	@Override
	public String toString() {
		String varStr = "";
		for (int i = 0; i < m_variables.size(); i++) {
			if (i != m_variables.size() - 1) {
				varStr += m_variables.get(i).toString() + ",";
			} else {
				varStr += m_variables.get(i).toString();
			}
		}
		return m_table + "(" + varStr + ")";
	}
}

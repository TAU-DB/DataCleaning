package data;

import java.util.List;

public abstract class Formula {

	private int m_index = 0;
	protected List<Variable> m_variables;
	
	public Formula(int index, List<Variable> variables) {
		m_index = index;
		m_variables = variables;
	}
	
	public int getIndex() {
		return m_index;
	}
	
	public Variable getVariableAt(int index) {
		return m_variables.get(index);
	}
	
	public Variable getVariableAt(String column) {
		
		for (Variable var : m_variables) {
			if (var.getColumn() != null && var.getColumn().equals(column)) {
				return var;
			}
		}
		return null;
	}
	
	public int getVariableCount() {
		return m_variables.size();
	}
}

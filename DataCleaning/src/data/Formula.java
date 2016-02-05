package data;

import java.util.List;

public abstract class Formula {

	protected List<Variable> m_variables;
	
	public Formula(List<Variable> variables) {
		m_variables = variables;
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

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

	public boolean containsVariable(String varName) {
		for (int i = 0; i < m_variables.size(); i++) {
			Variable var = m_variables.get(i);
			if (!var.isConstant() && var.getName().equals(varName)) {
				return true;
			}
		}
		return false;
	}
}

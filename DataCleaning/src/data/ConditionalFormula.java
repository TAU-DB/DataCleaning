package data;

import java.util.List;

public class ConditionalFormula extends Formula {

	private String m_operator;

	public ConditionalFormula(List<Variable> variables, String operator) {
		super(variables);
		m_operator = operator;
	}

	public String getOperator() {
		return m_operator;
	}

	@Override
	public String toString() {
		String lhs = m_variables.get(0).toString();
		String rhs = m_variables.get(1).toString();
		return lhs + " " + m_operator + " " + rhs;
	}

	public void assign(String varName, String value) {
		for (int i = 0; i < m_variables.size(); i++) {
			if (!m_variables.get(i).isConstant() && m_variables.get(i).getName().equals(varName)) {
				m_variables.get(i).assign(value);
			}
		}
	}
}

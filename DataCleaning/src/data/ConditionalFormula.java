package data;

import java.util.List;

public class ConditionalFormula extends Formula {

	private String m_operator;
	
	public ConditionalFormula(int index, List<Variable> variables, String operator) {
		super(index, variables);
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
}

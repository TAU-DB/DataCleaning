package data;

import java.util.List;

public class Rule {

	private String m_type;
	private String m_falseQuery;
	private List<Formula> m_lhs;
	private List<Formula> m_rhs;
	
	public Rule(String type, String falseQuery, List<Formula> lhs, List<Formula> rhs) {
		m_type = type;
		m_falseQuery = falseQuery;
		m_lhs = lhs;
		m_rhs = rhs;
	}
	
	public boolean isTupleGenerating() {
		if (m_type.equals("condition_generating")) {
			return false;
		}
		return true;
	}
	
	public String getFalseQuery() {
		return m_falseQuery;
	}
	
	public int getLHSFormulaCount() {
		return m_lhs.size();
	}
	
	public Formula getLHSFormulaAt(int index) {
		return m_lhs.get(index);
	}
	
	public int getRHSFormulaCount() {
		return m_rhs.size();
	}
	
	public Formula getRHSFormulaAt(int index) {
		return m_rhs.get(index);
	}
	
	@Override
	public String toString() {
		String lhs = "";
		for (int i = 0; i < m_lhs.size(); i++) {
			if (i != m_lhs.size() - 1) {
				lhs += m_lhs.get(i).toString() + " && ";
			} else {
				lhs += m_lhs.get(i).toString();
			}
		}
		
		String rhs = "";
		for (int i = 0; i < m_rhs.size(); i++) {
			if (i != m_rhs.size() - 1) {
				rhs += m_rhs.get(i).toString() + " && ";
			} else {
				rhs += m_rhs.get(i).toString();
			}
		}
		
		String result = lhs + " --> " + rhs + System.lineSeparator() + m_falseQuery + System.lineSeparator();
		result += "LHS variables:" + System.lineSeparator();
		for (Formula formula : m_lhs) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				result += formula.getVariableAt(i).toDetailedString() + System.lineSeparator();
			}
		}
		result += "RHS variables:" + System.lineSeparator();
		for (Formula formula : m_rhs) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				result += formula.getVariableAt(i).toDetailedString() + System.lineSeparator();
			}
		}
		
		return result;
	}
}

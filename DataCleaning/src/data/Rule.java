package data;

import java.util.HashSet;
import java.util.List;

public class Rule {

	private String m_type;
	private String m_falseQuery;
	private String m_sourceQuery;
	private List<Formula> m_lhs;
	private List<Formula> m_rhs;
	
	public Rule(String type, String falseQuery, String sourceQuery, List<Formula> lhs, List<Formula> rhs) {
		m_type = type;
		m_falseQuery = falseQuery;
		m_sourceQuery = sourceQuery;
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
	
	public String getSourceQuery() {
		return m_sourceQuery;
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
		
		String result = lhs + " --> " + rhs;
		return result;
	}
	
	public String toDetailedString() {
		
		String result = toString() + System.lineSeparator();
		
		result += m_falseQuery + System.lineSeparator() + m_sourceQuery + System.lineSeparator() + "LHS variables:" + System.lineSeparator();
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
	
	public HashSet<String> getRHSDefinedVariables() {
		
		HashSet<String> rhsDefinedVars = new HashSet<String>();
		HashSet<String> lhsDefinedVars = new HashSet<String>();
		for (Formula formula : m_lhs) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				Variable var = formula.getVariableAt(i);
				lhsDefinedVars.add(var.getName());
			}
		}
		for (Formula formula : m_rhs) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				Variable var = formula.getVariableAt(i);
				if (!lhsDefinedVars.contains(var.getName())) {
					rhsDefinedVars.add(var.getName());
				}
			}
		}
		
		return rhsDefinedVars;
	}
}

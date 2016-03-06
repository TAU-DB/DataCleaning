package data;

import java.util.List;

public class Witness {

	private Rule m_rule;
	private List<DBTuple> m_tuples;
	
	public Witness(Rule rule, List<DBTuple> tuples) {
		m_rule = rule;
		m_tuples = tuples;
	}
	
	public Rule getRule() {
		return m_rule;
	}
	
	public List<DBTuple> getTuples() {
		return m_tuples;
	}
	
	@Override
	public String toString() {
		String result = "{ ";
		for (int i = 0; i < m_tuples.size(); i++) {
			if (i == m_tuples.size() - 1) {
				result += m_tuples.get(i).toString();
			} else {
				result += m_tuples.get(i).toString() + ", ";
			}
		}
		result += " }";
		return result;
	}
	
	@Override
	public boolean equals(Object o) {
		Witness witness = (Witness) o;
		
		if (!witness.getRule().toString().equals(m_rule.toString())) {
			return false;
		}
		
		if (!WitnessesManager.tuplesListsEqual(witness.getTuples(), m_tuples)) {
			return false;
		}
		
		return true;
	}
	
	@Override 
	public int hashCode() {
		return 0;
	}
	
	public Witness cloneWithNFRule() {
		
		return new Witness(m_rule.toNFRule(), m_tuples);
	}
}

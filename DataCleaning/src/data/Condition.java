package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import jp.ac.kobe_u.cs.cream.DefaultSolver;
import jp.ac.kobe_u.cs.cream.IntVariable;
import jp.ac.kobe_u.cs.cream.Network;
import jp.ac.kobe_u.cs.cream.Solution;
import jp.ac.kobe_u.cs.cream.Solver;

public class Condition {

	private Rule m_rule;
	private List<ConditionalFormula> m_lhsMappedFormulas;
	private List<ConditionalFormula> m_rhsMappedFormulas;
	private List<ConditionalFormula> m_externalStringFormulas;
	private List<ConditionalFormula> m_externalIntFormulas;
	private final Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public Condition(Rule rule) {

		m_rule = rule;
		m_lhsMappedFormulas = new ArrayList<ConditionalFormula>();
		m_rhsMappedFormulas = new ArrayList<ConditionalFormula>();
		m_externalStringFormulas = new ArrayList<ConditionalFormula>();
		m_externalIntFormulas = new ArrayList<ConditionalFormula>();

		// 1. Get the conditional formulas from the rule, assume we have (x=5
		// and y=6 -> z=7 and k=8)
		// 2. Translate the operator "->" to "and,or" using the translation x->y
		// same as not(x) or y
		// The result should be x!=5 or y!=6 or (z=7 and k=8)
		// lhsMappedFormulas will contain [x!=5,y!=6] and rhsMappedFormulas will
		// contain [z=7,k=8]

		for (int i = 0; i < m_rule.getLHSFormulaCount(); i++) {
			if (m_rule.getLHSFormulaAt(i) instanceof RelationalFormula) {
				continue;
			}

			ConditionalFormula currFormula = (ConditionalFormula) m_rule.getLHSFormulaAt(i);
			addLHSFormula(currFormula);
		}

		for (int i = 0; i < m_rule.getRHSFormulaCount(); i++) {
			if (m_rule.getRHSFormulaAt(i) instanceof RelationalFormula) {
				continue;
			}

			ConditionalFormula currFormula = (ConditionalFormula) m_rule.getRHSFormulaAt(i);
			addRHSFormula(currFormula);
		}
	}

	@Override
	public String toString() {
		String lhs = "";
		for (int i = 0; i < m_lhsMappedFormulas.size(); i++) {
			if (i != m_lhsMappedFormulas.size() - 1) {
				lhs += m_lhsMappedFormulas.get(i).toString() + " || ";
			} else {
				lhs += m_lhsMappedFormulas.get(i).toString();
			}
		}

		String rhs = "";
		for (int i = 0; i < m_rhsMappedFormulas.size(); i++) {
			if (i != m_rhsMappedFormulas.size() - 1) {
				rhs += m_rhsMappedFormulas.get(i).toString() + " && ";
			} else {
				rhs += m_rhsMappedFormulas.get(i).toString();
			}
		}
		return lhs + " || " + rhs;
	}
	
	public void addExternalFormula(ConditionalFormula formula) {
		Variable lhsVar = formula.getVariableAt(0);
		if (lhsVar.getType().toLowerCase().equals("string")) {
			m_externalStringFormulas.add(formula);
			return;
		}
		m_externalIntFormulas.add(formula);
	}
	
	private void addLHSFormula(ConditionalFormula formula) {
		String operator = formula.getOperator();
		String negOperator = getOppositeOperator(operator);

		List<Variable> conVariables = new ArrayList<Variable>();
		for (int varIndex = 0; varIndex < formula.getVariableCount(); varIndex++) {
			Variable var = formula.getVariableAt(varIndex);
			String name = var.getName();
			String column = var.getColumn();
			boolean isConstant = var.isConstant();
			String value = var.getValue();
			String type = var.getType();
			conVariables.add(new Variable(name, column, isConstant, value, type));
		}

		ConditionalFormula conFormula = new ConditionalFormula(conVariables, negOperator);
		m_lhsMappedFormulas.add(conFormula);
	}

	private void addRHSFormula(ConditionalFormula formula) {
		String operator = formula.getOperator();

		List<Variable> conVariables = new ArrayList<Variable>();
		for (int varIndex = 0; varIndex < formula.getVariableCount(); varIndex++) {
			Variable var = formula.getVariableAt(varIndex);
			String name = var.getName();
			String column = var.getColumn();
			boolean isConstant = var.isConstant();
			String value = var.getValue();
			String type = var.getType();
			conVariables.add(new Variable(name, column, isConstant, value, type));
		}

		ConditionalFormula conFormula = new ConditionalFormula(conVariables, operator);
		m_rhsMappedFormulas.add(conFormula);
	}

	public Rule getRule() {
		return m_rule;
	}

	public void assign(HashMap<String, String> assignment) {

		for (ConditionalFormula formula : m_lhsMappedFormulas) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				if (!formula.getVariableAt(i).isConstant()) {
					String varName = formula.getVariableAt(i).getName();
					if (!assignment.get(varName).equals(varName)) {
						formula.getVariableAt(i).assign(assignment.get(varName));
					}
				}
			}
		}

		for (ConditionalFormula formula : m_rhsMappedFormulas) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				if (!formula.getVariableAt(i).isConstant()) {
					String varName = formula.getVariableAt(i).getName();
					if (!assignment.get(varName).equals(varName)) {
						formula.getVariableAt(i).assign(assignment.get(varName));
					}
				}
			}
		}
	}

	public boolean hasSatisfyingAssignment() {

		boolean hasSatAssignment = false;
		List<ConditionalFormula> tempConstraints = new ArrayList<ConditionalFormula>();
		String info = System.lineSeparator() + "Checking for sat assignment" + System.lineSeparator();
		// Check if any formula of the lhsMappedFormulas has a satisfying
		// assignment, if yes return true.
		// To check that for each lhs formula append the external formula of the same type
		// and check for satisfying assignment
		info += "Checking if any of the ORs are not contradiction : " + System.lineSeparator();
		LOGGER.info(info);
		for (ConditionalFormula formula : m_lhsMappedFormulas) {
			tempConstraints = cloneExternalFormulas();
			tempConstraints.add(formula);
			hasSatAssignment = constraintsHasSatAssignment(tempConstraints);
			if (hasSatAssignment) {
				return true;
			}
		}

		info = "";
		info += "Checking if the ANDs has a satisfying assignment: " + System.lineSeparator();
		LOGGER.info(info);
		// Get rhs string/integer formulas
		tempConstraints = cloneExternalFormulas();
		tempConstraints.addAll(m_rhsMappedFormulas);
		hasSatAssignment = constraintsHasSatAssignment(tempConstraints);
		LOGGER.info(System.lineSeparator() + "returning:" + hasSatAssignment + System.lineSeparator());
		return hasSatAssignment;
	}
	
	private List<ConditionalFormula> cloneExternalFormulas() {
		List<ConditionalFormula> externalFormulas = new ArrayList<ConditionalFormula>();
		externalFormulas.addAll(m_externalIntFormulas);
		externalFormulas.addAll(m_externalStringFormulas);
		return externalFormulas;
	}
	
	private boolean constraintsHasSatAssignment(List<ConditionalFormula> constraints) {
		
		String info = System.lineSeparator() + "Checking constraints:" + System.lineSeparator();
		info += constraints.toString() + System.lineSeparator();
		List<ConditionalFormula> stringFormulas = new ArrayList<ConditionalFormula>();
		List<ConditionalFormula> integerFormulas = new ArrayList<ConditionalFormula>();
		for (ConditionalFormula formula : constraints) {
			if (formula.getVariableAt(0).getType().toLowerCase().equals("integer")) {
				integerFormulas.add(formula);
			} else {
				stringFormulas.add(formula);
			}
		}

		// Check if integer variables & string variables has a solution
		boolean hasSolutionInt = hasSolutionInteger(integerFormulas);
		boolean hasSolutionStr = hasSolutionString(stringFormulas);
		
		info += "int has solution: " + hasSolutionInt + " string has solution: " + hasSolutionStr + System.lineSeparator();

		if (hasSolutionInt && hasSolutionStr) {
			info += "returning true" + System.lineSeparator();
			LOGGER.info(info);
			return true;
		}

		info += "returning false" + System.lineSeparator();
		LOGGER.info(info);
		return false;
	}

	private boolean hasSolutionString(List<ConditionalFormula> stringFormulas) {

		if (stringFormulas.size() == 0) {
			return true;
		}
		
		List<String> variables = new ArrayList<String>();
		for (ConditionalFormula formula : stringFormulas) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				Variable var = formula.getVariableAt(i);
				if (!variables.contains(var.getName()) && !var.isConstant()) {
					variables.add(var.getName());
				}
			}
		}

		// Define a group for each variable and all variables that are equal
		// must be in the same group
		HashMap<String, List<String>> groups = new HashMap<String, List<String>>();
		for (String varName : variables) {
			groups.put(varName, new ArrayList<String>());
			groups.get(varName).add(varName);
		}

		for (ConditionalFormula formula : stringFormulas) {
			Variable lhsVar = formula.getVariableAt(0);
			Variable rhsVar = formula.getVariableAt(1);
			if (!lhsVar.isConstant() && !rhsVar.isConstant() && formula.getOperator().equals("==")) {
				List<String> lhsVarGroup = groups.get(lhsVar.getName());
				if (!lhsVarGroup.contains(rhsVar.getName())) {
					lhsVarGroup.addAll(groups.get(rhsVar.getName()));
					groups.put(rhsVar.getName(), lhsVarGroup);
				}
			}
		}

		for (List<String> group : groups.values()) {
			String groupValue = null;
			for (ConditionalFormula formula : stringFormulas) {
				Variable lhsVar = formula.getVariableAt(0);
				Variable rhsVar = formula.getVariableAt(1);
				// Case formula is: var == const
				if (!lhsVar.isConstant() && rhsVar.isConstant() && formula.getOperator().equals("==")
						&& group.contains(lhsVar.getName())) {
					if (groupValue != null && !groupValue.equals(rhsVar.getValue())) {
						return false;
					}
				}
				// Case formula is: const == var
				if (lhsVar.isConstant() && !rhsVar.isConstant() && formula.getOperator().equals("==")
						&& group.contains(rhsVar.getName())) {
					if (groupValue != null && !groupValue.equals(lhsVar.getValue())) {
						return false;
					}
				}
			}

			// After getting the group value check if there is a formula which
			// demands group value != groupValue
			for (ConditionalFormula formula : stringFormulas) {
				Variable lhsVar = formula.getVariableAt(0);
				Variable rhsVar = formula.getVariableAt(1);
				// Case formula is: var == const
				if (!lhsVar.isConstant() && rhsVar.isConstant() && formula.getOperator().equals("!=")
						&& group.contains(lhsVar.getName())) {
					if (groupValue != null && groupValue.equals(rhsVar.getValue())) {
						return false;
					}
				}
				// Case formula is: const == var
				if (lhsVar.isConstant() && !rhsVar.isConstant() && formula.getOperator().equals("!=")
						&& group.contains(rhsVar.getName())) {
					if (groupValue != null && groupValue.equals(lhsVar.getValue())) {
						return false;
					}
				}
			}
		}

		// Check if there is a formula const != const or const1 == const2
		for (ConditionalFormula formula : stringFormulas) {
			Variable lhsVar = formula.getVariableAt(0);
			Variable rhsVar = formula.getVariableAt(1);
			if (lhsVar.isConstant() && rhsVar.isConstant() && formula.getOperator().equals("==")) {
				if (!lhsVar.getValue().equals(rhsVar.getValue())) {
					return false;
				}
			}
			if (lhsVar.isConstant() && rhsVar.isConstant() && formula.getOperator().equals("!=")) {
				if (lhsVar.getValue().equals(rhsVar.getValue())) {
					return false;
				}
			}
		}

		return true;
	}

	private boolean hasSolutionInteger(List<ConditionalFormula> integerFormulas) {

		if (integerFormulas.size() == 0) {
			return true;
		}
		
		List<String> varNames = new ArrayList<String>();
		for (ConditionalFormula formula : integerFormulas) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				Variable var = formula.getVariableAt(i);
				if (!var.isConstant() && !varNames.contains(formula.getVariableAt(i).getName())) {
					varNames.add(formula.getVariableAt(i).getName());
				}
			}
		}

		Network net = new Network();

		HashMap<String, IntVariable> varToIntVar = new HashMap<String, IntVariable>();
		for (String varName : varNames) {
			varToIntVar.put(varName, new IntVariable(net));
		}

		for (ConditionalFormula formula : integerFormulas) {

			Variable lhsVar = formula.getVariableAt(0);
			Variable rhsVar = formula.getVariableAt(1);
			if (lhsVar.isConstant() && rhsVar.isConstant()) {
				if (isContradiction(Integer.parseInt(lhsVar.getValue()), Integer.parseInt(rhsVar.getValue()),
						formula.getOperator())) {
					return false;
				}
			}

			if (!lhsVar.isConstant() && rhsVar.isConstant()) {
				addConstraint(varToIntVar.get(lhsVar.getName()), Integer.parseInt(rhsVar.getValue()), formula.getOperator());
			}

			if (lhsVar.isConstant() && !rhsVar.isConstant()) {
				addConstraint(varToIntVar.get(rhsVar.getName()), Integer.parseInt(lhsVar.getValue()), flipOperator(formula.getOperator()));
			}

			if (!lhsVar.isConstant() && !rhsVar.isConstant()) {
				addConstraint(varToIntVar.get(lhsVar.getName()), varToIntVar.get(rhsVar.getName()), formula.getOperator());
			}
		}

		// Solve the problem
		Solver solver = new DefaultSolver(net);
		Solution solution = solver.findFirst();
		if (solution == null) {
			return false;
		}

		return true;

	}

	private void addConstraint(IntVariable intVar, int constant, String operator) {
		switch (operator) {
		case "==":
			intVar.equals(constant);
			break;
		case "!=":
			intVar.notEquals(constant);
			break;
		case "<=":
			intVar.le(constant);
			break;
		case "<":
			intVar.lt(constant);
			break;
		case ">=":
			intVar.ge(constant);
			break;
		case ">":
			intVar.gt(constant);
			break;
		default:
			return;
		}
	}

	private void addConstraint(IntVariable intVar, IntVariable constant, String operator) {
		switch (operator) {
		case "==":
			intVar.equals(constant);
			break;
		case "!=":
			intVar.notEquals(constant);
			break;
		case "<=":
			intVar.le(constant);
			break;
		case "<":
			intVar.lt(constant);
			break;
		case ">=":
			intVar.ge(constant);
			break;
		case ">":
			intVar.gt(constant);
			break;
		default:
			return;
		}
	}

	private boolean isContradiction(int lhs, int rhs, String operator) {

		boolean result;

		switch (operator) {
		case "==":
			result = (lhs != rhs);
			break;
		case "!=":
			result = (lhs == rhs);
			break;
		case "<=":
			result = (lhs > rhs);
			break;
		case "<":
			result = (lhs >= rhs);
			break;
		case ">=":
			result = (lhs < rhs);
			break;
		case ">":
			result = (lhs <= rhs);
			break;
		default:
			return true;
		}
		return result;
	}

	private boolean isContradiction(ConditionalFormula formula) {

		Variable lhsVar = formula.getVariableAt(0);
		Variable rhsVar = formula.getVariableAt(1);

		// If formula is x != x || x > x || x < x return true
		if (!lhsVar.isConstant() && !rhsVar.isConstant()) {
			// Check if var[0] and var[1] has the same name and the operator
			// does not contain equality
			// if yes return true
			if (lhsVar.getName().equals(rhsVar.getName()) && !hasEqualityOperator(formula.getOperator())) {
				return true;
			}
		}

		// If formula is const1 op const2 and it's a contradiction then return
		// true
		if (lhsVar.isConstant() && rhsVar.isConstant()
				&& isContradiction(lhsVar.getValue(), rhsVar.getValue(), formula.getOperator(), lhsVar.getType())) {
			return true;
		}
		return false;
	}

	private boolean isContradiction(String const1, String const2, String operator, String type) {

		if (!type.equals("Integer")) {
			if ((operator.equals("==") && !const1.equals(const2)) || (operator.equals("!=") && const1.equals(const2))) {
				return true;
			}
			return false;
		}

		int const1Int = Integer.parseInt(const1);
		int const2Int = Integer.parseInt(const2);

		if ((operator.equals("==") && const1Int != const2Int) || (operator.equals("!=") && const1Int == const2Int)
				|| (operator.equals("<=") && const1Int > const2Int) || (operator.equals(">=") && const1Int < const2Int)
				|| (operator.equals("<") && const1Int >= const2Int)
				|| (operator.equals(">") && const1Int <= const2Int)) {
			return true;
		}
		return false;
	}

	private boolean hasEqualityOperator(String operator) {

		if (operator.equals("==") || operator.equals("<=") || operator.equals(">=")) {
			return true;
		}
		return false;
	}

	private String flipOperator(String operator) {

		if (operator.equals("==")) {
			return "==";
		}

		if (operator.equals("!=")) {
			return "!=";
		}

		if (operator.equals(">")) {
			return "<";
		}

		if (operator.equals("<")) {
			return ">";
		}

		if (operator.equals(">=")) {
			return "<=";
		}

		if (operator.equals("<=")) {
			return ">=";
		}
		return null;
	}
	
	private String getOppositeOperator(String operator) {

		if (operator.equals("==")) {
			return "!=";
		}

		if (operator.equals("!=")) {
			return "==";
		}

		if (operator.equals(">")) {
			return "<=";
		}

		if (operator.equals("<")) {
			return ">=";
		}

		if (operator.equals(">=")) {
			return "<";
		}

		if (operator.equals("<=")) {
			return ">";
		}
		return null;
	}
}

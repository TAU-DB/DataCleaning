package data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.sf.javailp.Linear;
import net.sf.javailp.OptType;
import net.sf.javailp.Problem;
import net.sf.javailp.Result;
import net.sf.javailp.Solver;
import net.sf.javailp.SolverFactory;
import net.sf.javailp.SolverFactoryLpSolve;

public class Condition {

	private Rule m_rule;
	private List<ConditionalFormula> m_lhsMappedFormulas;
	private List<ConditionalFormula> m_rhsMappedFormulas;

	public Condition(Rule rule) {

		m_rule = rule;
		m_lhsMappedFormulas = new ArrayList<ConditionalFormula>();
		m_rhsMappedFormulas = new ArrayList<ConditionalFormula>();

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
			String operator = currFormula.getOperator();
			String negOperator = getOppositeOperator(operator);

			List<Variable> conVariables = new ArrayList<Variable>();
			for (int varIndex = 0; varIndex < currFormula.getVariableCount(); varIndex++) {
				Variable var = currFormula.getVariableAt(varIndex);
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

		for (int i = 0; i < m_rule.getRHSFormulaCount(); i++) {
			if (m_rule.getRHSFormulaAt(i) instanceof RelationalFormula) {
				continue;
			}

			ConditionalFormula currFormula = (ConditionalFormula) m_rule.getRHSFormulaAt(i);
			String operator = currFormula.getOperator();

			List<Variable> conVariables = new ArrayList<Variable>();
			for (int varIndex = 0; varIndex < currFormula.getVariableCount(); varIndex++) {
				Variable var = currFormula.getVariableAt(varIndex);
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

		// Check if any formula of the lhsMappedFormulas has a satisfying
		// assignment, if yes return true.
		// A lhsMapped formula doesn't have a satisfying assignment <=> its
		// const_1 op const_2 and it's contradiction
		// or it's x != x || x > x || x < x
		for (ConditionalFormula formula : m_lhsMappedFormulas) {
			if (!isContradiction(formula)) {
				return true;
			}
		}

		// Get rhs not integer/integer formulas
		List<ConditionalFormula> stringFormulas = new ArrayList<ConditionalFormula>();
		List<ConditionalFormula> integerFormulas = new ArrayList<ConditionalFormula>();
		for (ConditionalFormula formula : m_rhsMappedFormulas) {
			if (formula.getVariableAt(0).getType().toLowerCase().equals("integer")) {
				integerFormulas.add(formula);
			} else {
				stringFormulas.add(formula);
			}
		}

		// Check if integer variables & string variables has a solution
		boolean hasSolutionInt = hasSolutionInteger(integerFormulas);
		boolean hasSolutionStr = hasSolutionString(stringFormulas);

		if (hasSolutionInt && hasSolutionStr) {
			return true;
		}

		return false;
	}

	private boolean hasSolutionString(List<ConditionalFormula> stringFormulas) {

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

		List<String> varNames = new ArrayList<String>();
		for (ConditionalFormula formula : integerFormulas) {
			for (int i = 0; i < formula.getVariableCount(); i++) {
				if (!varNames.contains(formula.getVariableAt(i).getName())) {
					varNames.add(formula.getVariableAt(i).getName());
				}
			}
		}

		Problem problem = new Problem();

		Linear linear = new Linear();
		for (String varName : varNames) {
			linear.add(1, varName);
		}

		problem.setObjective(linear, OptType.MAX);

		for (ConditionalFormula formula : integerFormulas) {

			Variable lhsVar = formula.getVariableAt(0);
			Variable rhsVar = formula.getVariableAt(1);
			if (lhsVar.isConstant() && rhsVar.isConstant()) {
				linear = new Linear();
				problem.add(linear, formula.getOperator(),
						Integer.parseInt(rhsVar.getValue()) - Integer.parseInt(lhsVar.getValue()));
			}

			if (!lhsVar.isConstant() && rhsVar.isConstant()) {
				linear = new Linear();
				linear.add(1, lhsVar.getName());
				problem.add(linear, formula.getOperator(), Integer.parseInt(rhsVar.getValue()));
			}

			if (lhsVar.isConstant() && !rhsVar.isConstant()) {
				linear = new Linear();
				linear.add(1, rhsVar.getName());
				problem.add(linear, getOppositeOperator(formula.getOperator()), Integer.parseInt(rhsVar.getValue()));
			}

			if (!lhsVar.isConstant() && !rhsVar.isConstant()) {
				linear = new Linear();
				linear.add(1, lhsVar.getName());
				linear.add(-1, rhsVar.getName());
				problem.add(linear, formula.getOperator(), 0);
			}
		}

		SolverFactory factory = new SolverFactoryLpSolve(); // use lp_solve
		factory.setParameter(Solver.VERBOSE, 0);
		factory.setParameter(Solver.TIMEOUT, 100);

		Solver solver = factory.get(); // you should use this solver only once
										// for one problem
		Result result = solver.solve(problem);

		if (result != null) {
			return true;
		}
		return false;

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

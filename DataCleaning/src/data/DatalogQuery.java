package data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import Controllers.MainController;

public class DatalogQuery {

	static final List<String> SUPPORTED_OPS = Arrays.asList(new String[] { "==", "!=", "<=", "<", ">", ">=" });
	List<String> m_headVariables;
	List<Formula> m_tail;
	HashMap<String, Variable> m_variablesHash;
	String m_query;
	String m_sqlQuery;

	public DatalogQuery(String query) {

		m_query = query;
		m_headVariables = new ArrayList<String>();
		m_tail = new ArrayList<Formula>();
		m_variablesHash = new HashMap<String, Variable>();
		parseHead();
		parseTail();

		m_sqlQuery = toSQLQuery();
	}

	public String getSQLQuery() {
		return m_sqlQuery;
	}

	public String getHeadVarType(int headVarIndex) {
		return m_variablesHash.get(m_headVariables.get(headVarIndex)).getType();
	}

	public String toDetailedString() {
		String result = "Datalog query: " + m_query + System.lineSeparator();
		result += "head: " + m_headVariables.toString() + System.lineSeparator();
		result += "tail: ";
		for (int i = 0; i < m_tail.size(); i++) {
			result += m_tail.get(i).toString();
			if (i < m_tail.size() - 1) {
				result += " and ";
			}
		}
		result += System.lineSeparator() + "variables: " + System.lineSeparator();
		for (String varName : m_variablesHash.keySet()) {
			result += m_variablesHash.get(varName).toDetailedString() + System.lineSeparator();
		}

		result += "SQL: " + System.lineSeparator();
		result += m_sqlQuery + System.lineSeparator();

		return result;
	}

	public Rule createDeleteRule(String[] values) {

		List<Formula> lhs = new ArrayList<Formula>();
		List<Formula> rhs = new ArrayList<Formula>();
		for (int i = 0; i < m_headVariables.size(); i++) {

			Variable headVar = m_variablesHash.get(m_headVariables.get(i));
			Variable valueVar = new Variable(values[i], null, true, values[i], headVar.getType());
			List<Variable> valueFormulaVars = new ArrayList<Variable>();
			valueFormulaVars.add(headVar);
			valueFormulaVars.add(valueVar);
			ConditionalFormula valueFormula = new ConditionalFormula(valueFormulaVars, "==");
			lhs.add(valueFormula);
		}

		for (int i = 0; i < m_tail.size(); i++) {

			Formula tailFormula = m_tail.get(i);
			lhs.add(tailFormula);
		}

		Variable zeroConst = new Variable("0", null, true, "0", "integer");
		Variable oneConst = new Variable("1", null, true, "1", "integer");
		List<Variable> rhsFlaseFormulaVars = new ArrayList<Variable>();
		rhsFlaseFormulaVars.add(zeroConst);
		rhsFlaseFormulaVars.add(oneConst);
		ConditionalFormula rhsFalseFormula = new ConditionalFormula(rhsFlaseFormulaVars, "==");
		rhs.add(rhsFalseFormula);

		String falseSQLQuery = createFalseSQLQuery(lhs, rhs);

		Rule result = new Rule("condition_generating", falseSQLQuery, "", "", lhs, rhs);
		return result;
	}

	public int getRelationalFormulaCount() {

		return getRelationalFormulaCount(m_tail);
	}

	public RelationalFormula getRelationalFormulaAt(int index) {

		int currIndex = 0;
		for (int i = 0; i < m_tail.size(); i++) {
			Formula formula = m_tail.get(i);
			if (formula instanceof ConditionalFormula) {
				continue;
			}
			
			if (currIndex == index) {
				return ((RelationalFormula) formula);
			}
			currIndex++;
		}

		return null;
	}

	private String createFalseSQLQuery(List<Formula> lhs, List<Formula> rhs) {

		HashMap<String, String> variableToTableAlias = getVariableToTableAlias(lhs);
		List<String> relFormulasTables = getRelationalFormulasTables(lhs);
		int relFormulaCount = getRelationalFormulaCount(lhs);
		
		String sql = "SELECT ";
		String selectColumnsStr = "";
		for (Formula formula : lhs) {
			
			if (formula instanceof ConditionalFormula) {
				continue;
			}
			
			for (int i = 0; i < formula.getVariableCount(); i++) {
				Variable var = formula.getVariableAt(i);
				selectColumnsStr += variableToTableAlias.get(var.getName()) + "." + var.getColumn() + ", ";
			}
		}
		selectColumnsStr = selectColumnsStr.substring(0, selectColumnsStr.length() - 2);
		sql += selectColumnsStr;
		
		sql += " FROM ";
		for (int i = 0; i < relFormulaCount; i++) {

			sql += relFormulasTables.get(i) + " AS T" + i;
			if (i < relFormulaCount - 1) {
				sql += ", ";
			}
		}
		
		boolean hasConditionalFormula = false;
		String whereConStr = "";
		for (int i = 0; i < lhs.size(); i++) {
			Formula formula = lhs.get(i);
			if (formula instanceof RelationalFormula) {
				continue;
			}

			hasConditionalFormula = true;
			ConditionalFormula conFormula = (ConditionalFormula) formula;

			whereConStr += getConditionalFormulaCondition(conFormula, variableToTableAlias, m_variablesHash, false);

			whereConStr += " AND ";
		}
		
		for (int i = 0; i < rhs.size(); i++) {
			hasConditionalFormula = true;
			ConditionalFormula conFormula = (ConditionalFormula) rhs.get(i);

			whereConStr += getConditionalFormulaCondition(conFormula, variableToTableAlias, m_variablesHash, true);

			whereConStr += " AND ";
		}

		if (hasConditionalFormula) {
			whereConStr = whereConStr.substring(0, whereConStr.length() - 5);
			sql += " WHERE " + whereConStr;
		}
		sql += ";";

		return sql;
	}

	private String toSQLQuery() {

		HashMap<String, String> variableToTableAlias = getVariableToTableAlias(m_tail);
		List<String> relFormulasTables = getRelationalFormulasTables(m_tail);
		int relFormulaCount = getRelationalFormulaCount();

		String sql = "SELECT DISTINCT ";
		for (int i = 0; i < m_headVariables.size(); i++) {
			String headVar = m_headVariables.get(i);
			sql += variableToTableAlias.get(headVar) + "." + m_variablesHash.get(headVar).getColumn();
			if (i < m_headVariables.size() - 1) {
				sql += ", ";
			}
		}

		sql += " FROM ";
		for (int i = 0; i < relFormulaCount; i++) {

			sql += relFormulasTables.get(i) + " AS T" + i;
			if (i < relFormulaCount - 1) {
				sql += ", ";
			}
		}

		boolean hasConditionalFormula = false;
		String whereConStr = "";
		for (int i = 0; i < m_tail.size(); i++) {
			Formula formula = m_tail.get(i);
			if (formula instanceof RelationalFormula) {
				continue;
			}

			hasConditionalFormula = true;
			ConditionalFormula conFormula = (ConditionalFormula) formula;

			whereConStr += getConditionalFormulaCondition(conFormula, variableToTableAlias, m_variablesHash, false);

			whereConStr += " AND ";
		}

		if (hasConditionalFormula) {
			whereConStr = whereConStr.substring(0, whereConStr.length() - 5);
			sql += " WHERE " + whereConStr;
		}
		sql += ";";
		return sql;
	}

	private List<String> getRelationalFormulasTables(List<Formula> formulas) {

		List<String> relFormulasTables = new ArrayList<String>();

		for (int i = 0; i < formulas.size(); i++) {
			Formula formula = formulas.get(i);
			if (formula instanceof ConditionalFormula) {
				continue;
			}
			relFormulasTables.add(((RelationalFormula) (formula)).getTable());
		}

		return relFormulasTables;
	}

	private HashMap<String, String> getVariableToTableAlias(List<Formula> formulas) {

		HashMap<String, String> variableToTableAlias = new HashMap<String, String>();
		int relFormulaIndex = 0;

		for (int i = 0; i < formulas.size(); i++) {
			Formula formula = formulas.get(i);
			if (formula instanceof ConditionalFormula) {
				continue;
			}
			for (int varIndex = 0; varIndex < formula.getVariableCount(); varIndex++) {
				Variable var = formula.getVariableAt(varIndex);
				if (var.isConstant()) {
					continue;
				}
				variableToTableAlias.put(var.getName(), "T" + relFormulaIndex);
			}
			relFormulaIndex++;
		}

		return variableToTableAlias;
	}

	private int getRelationalFormulaCount(List<Formula> formulas) {

		int count = 0;
		for (int i = 0; i < formulas.size(); i++) {
			Formula formula = formulas.get(i);
			if (formula instanceof ConditionalFormula) {
				continue;
			}
			count++;
		}

		return count;
	}

	private String getConditionalFormulaCondition(ConditionalFormula conFormula,
			HashMap<String, String> variableToTableAlias, HashMap<String, Variable> variablesHash, boolean not) {

		String result = "";
		Variable lhsVar = conFormula.getVariableAt(0);
		Variable rhsVar = conFormula.getVariableAt(1);

		if (lhsVar.isConstant()) {
			if (lhsVar.getType().toLowerCase().equals("string")) {
				result += "'" + lhsVar.getValue() + "'";
			} else {
				result += lhsVar.getValue();
			}
		} else {
			result += variableToTableAlias.get(lhsVar.getName()) + "."
					+ variablesHash.get(lhsVar.getName()).getColumn();
		}

		result += " " + toSQLOperator(conFormula.getOperator(), not) + " ";

		if (rhsVar.isConstant()) {
			if (rhsVar.getType().toLowerCase().equals("string")) {
				result += "'" + rhsVar.getValue() + "'";
			} else {
				result += rhsVar.getValue();
			}
		} else {
			result += variableToTableAlias.get(rhsVar.getName()) + "."
					+ variablesHash.get(rhsVar.getName()).getColumn();
		}

		return result;
	}

	private String toSQLOperator(String operation, boolean not) {

		switch (operation) {
		case "==":
			return (not ? "<>" : "=");
		case "!=":
			return (not ? "=" : "<>");
		case "<=":
			return (not ? ">" : "<=");
		case "<":
			return (not ? ">=" : "<");
		case ">=":
			return (not ? "<" : ">=");
		case ">":
			return (not ? "<=" : ">");
		default:
			return "";
		}
	}

	private void parseHead() {

		String head = m_query.split(":-")[0].trim();
		String[] headSplitted = head.split(",");
		for (int i = 0; i < headSplitted.length; i++) {
			String varStr = headSplitted[i].trim();
			m_headVariables.add(varStr.substring(1, varStr.length()));
		}
	}

	private void parseTail() {

		String tailStr = m_query.split(":-")[1].trim();
		String[] tailSplitted = tailStr.split("and");

		// First parse relational formulas in order to collect all variables
		for (int i = 0; i < tailSplitted.length; i++) {
			String formulaStr = tailSplitted[i].trim();
			if (!isConditionalFormula(formulaStr)) {
				parseRelationalFormula(formulaStr);
			}
		}

		for (int i = 0; i < tailSplitted.length; i++) {
			String formulaStr = tailSplitted[i].trim();
			if (isConditionalFormula(formulaStr)) {
				parseConditionalFormula(formulaStr);
			}
		}
	}

	private void parseRelationalFormula(String formulaStr) {

		String tableName = formulaStr.split("\\(")[0].trim();
		MainController mainController = MainController.getInstance();
		List<String> tableColumns = mainController.getTableColumns(tableName);
		HashMap<String, String> columnToType = mainController.getColumnsTypes(tableName);
		String[] varsSplitted = formulaStr.split("\\(")[1].split("\\)")[0].split(",");
		List<Variable> variables = new ArrayList<Variable>();

		for (int i = 0; i < varsSplitted.length; i++) {
			String varName = varsSplitted[i].trim();
			varName = varName.substring(1, varName.length());
			String column = tableColumns.get(i);
			String type = "integer";
			if (columnToType.get(column).toLowerCase().contains("text")
					|| columnToType.get(column).toLowerCase().contains("char")) {
				type = "string";
			}
			Variable var = new Variable(varName, column, false, varName, type);
			variables.add(var);
			m_variablesHash.put(varName, var);
		}

		m_tail.add(new RelationalFormula(tableName, variables));
	}

	private void parseConditionalFormula(String formulaStr) {

		String op = getOperator(formulaStr);
		String lhsVar = formulaStr.split(op)[0].trim();
		String rhsVar = formulaStr.split(op)[1].trim();

		List<Variable> variables = new ArrayList<Variable>();

		// Parse LHS variable
		if (isConstant(lhsVar)) {
			String value = parseConstValue(lhsVar);
			String type = getConstType(lhsVar);
			Variable var = new Variable(value, "", true, value, type);
			variables.add(var);
		} else {
			String varName = lhsVar.trim().substring(1, lhsVar.trim().length());
			Variable cpyVar = m_variablesHash.get(varName);
			Variable var = new Variable(varName, cpyVar.getName(), false, cpyVar.getValue(), cpyVar.getType());
			variables.add(var);
		}

		// Parse RHS variable
		if (isConstant(rhsVar)) {
			String value = parseConstValue(rhsVar);
			String type = getConstType(rhsVar);
			Variable var = new Variable(value, "", true, value, type);
			variables.add(var);
		} else {
			String varName = rhsVar.trim().substring(1, rhsVar.trim().length());
			Variable cpyVar = m_variablesHash.get(varName);
			Variable var = new Variable(varName, cpyVar.getName(), false, cpyVar.getValue(), cpyVar.getType());
			variables.add(var);
		}

		m_tail.add(new ConditionalFormula(variables, op));
	}

	private String getConstType(String varStr) {

		if (varStr.charAt(0) == '\'' || varStr.charAt(0) == '"') {
			return "string";
		}
		return "integer";
	}

	private String parseConstValue(String varStr) {

		if (varStr.charAt(0) == '\'' || varStr.charAt(0) == '"') {
			return varStr.substring(1, varStr.length() - 1).trim();
		}
		return varStr.trim();
	}

	private boolean isConstant(String varStr) {

		if (varStr.charAt(0) == '?') {
			return false;
		}
		return true;
	}

	private String getOperator(String formulaStr) {

		for (String op : SUPPORTED_OPS) {
			if (formulaStr.contains(op)) {
				return op;
			}
		}
		return null;
	}

	private boolean isConditionalFormula(String formulaStr) {

		for (String op : SUPPORTED_OPS) {
			if (formulaStr.contains(op)) {
				return true;
			}
		}
		return false;
	}
}

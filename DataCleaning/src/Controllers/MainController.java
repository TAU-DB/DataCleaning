package Controllers;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.simple.JSONObject;

import data.ConditionalFormula;
import data.DBTuple;
import data.Formula;
import data.Graph;
import data.RelationalFormula;
import data.Rule;
import data.RulesReader;
import data.Variable;
import data.Vertex;
import data.Witness;
import data.WitnessesManager;
import edu.uci.ics.jung.algorithms.scoring.PageRank;
import edu.uci.ics.jung.graph.DirectedSparseGraph;

public class MainController {

	public static String convertRanksMapToStr(HashMap<DBTuple, Double> ranksMap) {
		String ranksMapStr = "{";
		int vertexIndex = 0;
		for (DBTuple tuple : ranksMap.keySet()) {
			ranksMapStr += "\"" + tuple.toString() + "\"";
			ranksMapStr += " : " + ranksMap.get(tuple);

			if (vertexIndex < ranksMap.keySet().size() - 1) {
				ranksMapStr += ", ";
			}
			vertexIndex++;
		}
		ranksMapStr += "}";
		return ranksMapStr;
	}

	public static JSONObject convertRanksMapToJSONObject(HashMap<DBTuple, Double> ranksMap) {
		JSONObject result = new JSONObject();
		for (DBTuple tuple : ranksMap.keySet()) {
			result.put(tuple.toString(), ranksMap.get(tuple));
		}
		return result;
	}

	public static DBTuple getMaxRankTuple(HashMap<DBTuple, Double> ranksMap) {

		Double maxRank = 0.0;
		DBTuple maxTuple = null;
		for (DBTuple tuple : ranksMap.keySet()) {

			Double currRank = ranksMap.get(tuple);
			if (currRank > maxRank) {
				maxRank = currRank;
				maxTuple = tuple;
			}
		}
		return maxTuple;
	}

	private static MainController m_instance = null;

	private String m_dbName = null;
	private List<Rule> m_rules;
	private WitnessesManager m_witManager = null;
	private String m_validatedDBName = null;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	public static MainController getInstance() {
		if (m_instance == null) {
			m_instance = new MainController();
		}

		return m_instance;
	}

	private MainController() {

		buildSimpleDB();
		buildValidatedDB();

		RulesReader reader = new RulesReader("C:" + File.separator + "temp" + File.separator + "rules.xml");
		m_rules = reader.getRules();
		m_witManager = new WitnessesManager(reader.getRules(), m_dbName);

		LOGGER.setLevel(Level.INFO);
	}
	
	public String getValidatedDBName() {
		return m_validatedDBName;
	}

	public Graph generateGraph() {
		m_witManager.calculateWitnesses();

		Graph graph = new Graph();
		List<DBTuple> suspiciousTuples = m_witManager.getSuspiciousTuples();
		for (DBTuple tuple : suspiciousTuples) {
			graph.addVertex(new Vertex(tuple));
		}
		for (DBTuple tuple : suspiciousTuples) {
			List<Witness> tupleWitnesses = m_witManager.getTupleWitnesses(tuple);

			for (Witness tupleWitness : tupleWitnesses) {
				updateGraphEdges(graph, tuple, tupleWitness);
			}
		}

		LOGGER.info(graph.toString());

		return graph;
	}

	public HashMap<DBTuple, Double> calculateRanks(Graph graph) {
		HashMap<DBTuple, Double> ranksMap = new HashMap<DBTuple, Double>();

		DirectedSparseGraph<String, Integer> jungGraph = new DirectedSparseGraph<String, Integer>();
		for (Vertex v : graph.getVertecis()) {
			jungGraph.addVertex(v.getID());
		}

		int edgeCount = 0;
		for (Vertex v : graph.getVertecis()) {
			String srcVertex = v.getID();

			for (Vertex n : graph.getNeighbors(v)) {
				String dstVertex = n.getID();
				jungGraph.addEdge(edgeCount, srcVertex, dstVertex);
				edgeCount++;
			}
		}

		long start = System.currentTimeMillis();
		PageRank<String, Integer> ranker = new PageRank<String, Integer>(jungGraph, 0.2);
		ranker.setTolerance(0.001);
		ranker.setMaxIterations(200);
		ranker.evaluate();

		String info = System.lineSeparator();
		info += "Tolerance = " + ranker.getTolerance() + System.lineSeparator();
		info += "Dump factor = " + (1.00d - ranker.getAlpha()) + System.lineSeparator();
		info += "Max iterations = " + ranker.getMaxIterations() + System.lineSeparator();
		info += "PageRank computed in " + (System.currentTimeMillis() - start) + " ms" + System.lineSeparator();

		for (Vertex v : graph.getVertecis()) {
			ranksMap.put(v.getTuple(), ranker.getVertexScore(v.getID()));
			info += "Tuple: " + v.getID() + " rank: " + ranksMap.get(v.getTuple()) + System.lineSeparator();
		}

		LOGGER.info(info);

		return ranksMap;
	}

	public void setValidated(DBTuple tuple) {

		if (tuple.isAnonymous()) {
			System.out.println("FATAL in set validated because tuple is anonymous");
			System.exit(0);
		}

		int rowIndex = getRowIndex(tuple);
		try {
			Connection dbConn = null;
			Connection validatedDBConn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);
			validatedDBConn = DriverManager.getConnection("jdbc:sqlite:" + m_validatedDBName);

			stmt = validatedDBConn.createStatement();
			String sql = "UPDATE " + tuple.getTable() + " SET ";
			int columnIndex = 0;
			for (String column : tuple.getColumns()) {
				sql += column + "=1";

				if (columnIndex < tuple.getColumns().size() - 1) {
					sql += ", ";
				}
				columnIndex++;
			}
			sql += " WHERE rowid=" + rowIndex + ";";

			stmt.executeUpdate(sql);
			stmt.close();

			dbConn.close();
			validatedDBConn.close();
		} catch (Exception e) {
			System.err.println("In set validated " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}

	}

	public void deleteTuple(DBTuple tuple) {

		if (tuple.isAnonymous()) {
			System.out.println("FATAL in set validated because tuple is anonymous");
			System.exit(0);
		}


		int rowIndex = getRowIndex(tuple);
		
		try {
			Connection dbConn = null;
			Connection validatedDBConn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);
			validatedDBConn = DriverManager.getConnection("jdbc:sqlite:" + m_validatedDBName);

			stmt = dbConn.createStatement();
			String sql = "DELETE FROM " + tuple.getTable() + " WHERE rowid=" + rowIndex + ";";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = validatedDBConn.createStatement();
			sql = "DELETE FROM " + tuple.getTable() + " WHERE rowid=" + rowIndex + ";";
			stmt.executeUpdate(sql);
			stmt.close();

			dbConn.close();
			validatedDBConn.close();
		} catch (Exception e) {
			System.err.println("In delete tuple " + e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}

	}

	public void updateTuple(DBTuple tuple, DBTuple newTuple) {

		if (tuple.isAnonymous()) {
			System.out.println("FATAL in set validated because tuple is anonymous");
			System.exit(0);
		}


		HashMap<String, String> columnToType = getColumnsTypes(tuple);
		int rowIndex = getRowIndex(tuple);
		
		try {
			Connection dbConn = null;
			Connection validatedDBConn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);
			validatedDBConn = DriverManager.getConnection("jdbc:sqlite:" + m_validatedDBName);

			stmt = dbConn.createStatement();
			String sql = "UPDATE " + tuple.getTable() + " SET ";
			int columnIndex = 0;
			for (String column : tuple.getColumns()) {
				String type = columnToType.get(column);
				if (type.toLowerCase().contains("text") || type.toLowerCase().contains("char")) {
					sql += column + "=" + "'" + newTuple.getValue(column) + "'";
				} else {
					sql += column + "=" + "" + newTuple.getValue(column);
				}

				if (columnIndex < tuple.getColumns().size() - 1) {
					sql += ", ";
				}
				columnIndex++;
			}
			sql += " WHERE rowid=" + rowIndex + ";";

			stmt.executeUpdate(sql);
			stmt.close();

			dbConn.close();
			validatedDBConn.close();
		} catch (Exception e) {
			System.err.println("In update tuple " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}

		setValidated(newTuple);
	}

	public void updateValidatedDB() {

		boolean rhsValidatedChanged = false;
		for (Rule rule : m_rules) {
			if (!rule.isTupleGenerating()) {
				continue;
			}

			try {
				Connection dbConn = null;
				Statement stmt = null;
				Class.forName("org.sqlite.JDBC");
				dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);

				stmt = dbConn.createStatement();
				String sql = rule.getTrueQuery();
				ResultSet rs = stmt.executeQuery(sql);
				while (rs.next()) {

					List<DBTuple> tuples = extractTuples(rs, rule);
					List<DBTuple> validatedDBTuples = extractValidatedDBTuples(tuples, rule);
					boolean isLHSValidated = checkLHSValidated(validatedDBTuples, rule);
					if (isLHSValidated) {
						boolean temp = validateRHS(tuples.get(tuples.size() - 1),
								validatedDBTuples.get(validatedDBTuples.size() - 1), rule);
						if (temp) {
							rhsValidatedChanged = true;
						}
					}
				}
				rs.close();
				stmt.close();

				dbConn.close();
			} catch (Exception e) {
				System.err.println("In update validated DB " + e.getClass().getName() + ": " + e.getMessage());
				e.printStackTrace();
				System.exit(0);
			}
		}
		
		if (rhsValidatedChanged) {
			updateValidatedDB();
		}
	}

	public int getRowIndex(DBTuple tuple) {
		int rowIndex = -1;
		try {
			Connection dbConn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);

			HashMap<String, String> columnToType = new HashMap<String, String>();
			stmt = dbConn.createStatement();
			String sql = "PRAGMA table_info('" + tuple.getTable() + "');";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				columnToType.put(rs.getString(2), rs.getString(3));
			}
			rs.close();
			stmt.close();

			stmt = dbConn.createStatement();
			sql = "SELECT rowid FROM " + tuple.getTable() + " WHERE ";
			int columnIndex = 0;
			for (String column : tuple.getColumns()) {
				String type = columnToType.get(column);
				if (type.toLowerCase().contains("text") || type.toLowerCase().contains("char")) {
					sql += column + "=" + "'" + tuple.getValue(column) + "'";
				} else {
					sql += column + "=" + "" + tuple.getValue(column);
				}

				if (columnIndex < tuple.getColumns().size() - 1) {
					sql += " AND ";
				}
				columnIndex++;
			}

			rs = stmt.executeQuery(sql);
			rs.next();
			rowIndex = rs.getInt(1);
			rs.close();
			stmt.close();

			dbConn.close();
		} catch (Exception e) {
			System.err.println("In update validated DB " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		
		return rowIndex;
	}

	private boolean validateRHS(DBTuple tuple, DBTuple validatedDBTuple, Rule rule) {

		int currRFIndex = 0;
		for (int i = 0; i < rule.getLHSFormulaCount() + rule.getRHSFormulaCount(); i++) {
			Formula formula = null;
			if (i < rule.getLHSFormulaCount()) {
				formula = rule.getLHSFormulaAt(i);
			} else {
				formula = rule.getRHSFormulaAt(i - rule.getLHSFormulaCount());
			}

			if (formula instanceof ConditionalFormula) {
				continue;
			}
			currRFIndex++;
		}

		List<String> conditionalColumns = rule.getConditionalColumns(currRFIndex - 1);
		boolean isValidated = true;
		for (String column : conditionalColumns) {
			if (validatedDBTuple.getValue(column).equals("0")) {
				isValidated = false;
			}
		}
		
		if (isValidated) {
			return false;
		}

		int rowIndex = getRowIndex(tuple);
		try {
			Connection dbConn = null;
			Connection validatedDBConn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);
			validatedDBConn = DriverManager.getConnection("jdbc:sqlite:" + m_validatedDBName);

			stmt = validatedDBConn.createStatement();
			String sql = "UPDATE " + tuple.getTable() + " SET ";
			for (String column : tuple.getColumns()) {
				if (conditionalColumns.contains(column)) {
					sql += column + "=1,";
				}
			}
			if (sql.charAt(sql.length() - 1) == ',') {
				sql = sql.substring(0, sql.length()-1);
			}
			sql += " WHERE rowid=" + rowIndex + ";";
			stmt.executeUpdate(sql);
			stmt.close();

			dbConn.close();
			validatedDBConn.close();
		} catch (Exception e) {
			System.err.println("In update validated DB " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		
		return true;
	}
	
	private HashMap<String, String> getColumnsTypes(DBTuple tuple) {

		HashMap<String, String> columnToType = null;
		try {
			Connection dbConn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);

			columnToType = new HashMap<String, String>();
			stmt = dbConn.createStatement();
			String sql = "PRAGMA table_info('" + tuple.getTable() + "');";
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				columnToType.put(rs.getString(2), rs.getString(3));
			}
			rs.close();
			stmt.close();

			dbConn.close();
		} catch (Exception e) {
			System.err.println("In update validated DB " + e.getClass().getName() + ": " + e.getMessage());
			e.printStackTrace();
			System.exit(0);
		}
		
		return columnToType;
	}
	
	private boolean checkLHSValidated(List<DBTuple> validatedDBTuples, Rule rule) {

		int relFormulaIndex = 0;
		for (int i = 0; i < rule.getLHSFormulaCount(); i++) {
			Formula formula = rule.getLHSFormulaAt(i);
			if (formula instanceof ConditionalFormula) {
				continue;
			}

			DBTuple formulaTuple = validatedDBTuples.get(relFormulaIndex);
			List<String> conditionalColumns = rule.getConditionalColumns(relFormulaIndex);
			for (String conColumn : conditionalColumns) {
				if (formulaTuple.getValue(conColumn).equals("0")) {
					return false;
				}
			}

			relFormulaIndex++;
		}

		return true;
	}

	private List<DBTuple> extractValidatedDBTuples(List<DBTuple> tuples, Rule rule) {
		List<DBTuple> result = new ArrayList<DBTuple>();

		for (DBTuple tuple : tuples) {

			int rowIndex = getRowIndex(tuple);
			try {
				Connection dbConn = null;
				Connection validatedDBConn = null;
				Statement stmt = null;
				Class.forName("org.sqlite.JDBC");
				dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);
				validatedDBConn = DriverManager.getConnection("jdbc:sqlite:" + m_validatedDBName);

				stmt = validatedDBConn.createStatement();
				String sql = "SELECT * FROM " + tuple.getTable() + " WHERE rowid = " + rowIndex + ";";
				ResultSet rs = stmt.executeQuery(sql);
				rs.next();
				DBTuple validatedDBTuple = new DBTuple(tuple.getTable(), false);
				for (String column : tuple.getColumns()) {
					validatedDBTuple.setValue(column, rs.getString(column));
				}
				result.add(validatedDBTuple);
				rs.close();
				stmt.close();

				dbConn.close();
				validatedDBConn.close();
			} catch (Exception e) {
				System.err.println("In update validated DB " + e.getClass().getName() + ": " + e.getMessage());
				e.printStackTrace();
				System.exit(0);
			}
		}

		return result;
	}

	private List<DBTuple> extractTuples(ResultSet rs, Rule rule) {

		List<DBTuple> result = new ArrayList<DBTuple>();
		int rsColIndex = 1;
		for (int i = 0; i < rule.getLHSFormulaCount() + rule.getRHSFormulaCount(); i++) {
			Formula formula = null;
			if (i < rule.getLHSFormulaCount()) {
				formula = rule.getLHSFormulaAt(i);
			} else {
				formula = rule.getRHSFormulaAt(i - rule.getLHSFormulaCount());
			}

			if (formula instanceof ConditionalFormula) {
				continue;
			}
			RelationalFormula relFormula = (RelationalFormula) formula;
			DBTuple tuple = new DBTuple(relFormula.getTable(), false);
			for (int varIndex = 0; varIndex < relFormula.getVariableCount(); varIndex++) {
				Variable var = relFormula.getVariableAt(varIndex);
				try {
					tuple.setValue(var.getColumn(), rs.getString(rsColIndex));
				} catch (SQLException e) {
					e.printStackTrace();
				}
				rsColIndex++;
			}
			result.add(tuple);
		}

		return result;
	}

	private void updateGraphEdges(Graph graph, DBTuple suspiciousTuple, Witness tupleWitness) {

		for (int i = 0; i < tupleWitness.getTuples().size(); i++) {

			DBTuple currTuple = tupleWitness.getTuples().get(i);
			if (currTuple.equals(suspiciousTuple)) {
				continue;
			}

			boolean isEdge = false;
			if (tupleWitness.getRule().isTupleGenerating()) {
				isEdge = isEdgeByTGR(currTuple, suspiciousTuple, tupleWitness);
			} else {
				isEdge = isEdgeByCGR(currTuple, suspiciousTuple, tupleWitness);
			}

			if (isEdge) {
				graph.addEdge(currTuple, suspiciousTuple);
			}
		}
	}

	private boolean isEdgeByTGR(DBTuple srcTuple, DBTuple dstTuple, Witness tupleWitness) {

		Rule rule = tupleWitness.getRule();
		if (!rule.isTupleGenerating()) {
			System.out.println("FATAL in isEdgeByTGR because it's a CGR");
			System.exit(0);
		}

		int indexOfSrcTuple = tupleWitness.getTuples().indexOf(srcTuple);
		int indexOfDstTuple = tupleWitness.getTuples().indexOf(dstTuple);
		int witSize = tupleWitness.getTuples().size();

		if (indexOfSrcTuple == indexOfDstTuple) {

			System.out.println("FATAL in isEdgeByTGR because src tuple and dst tuple are the same");
			System.exit(0);
		}

		// If src tuple is in the RHS and dst tuple is the LHS then it's an edge
		if (indexOfSrcTuple == witSize - 1 && indexOfDstTuple < witSize - 1 && indexOfDstTuple >= 0) {
			return true;
		}

		// IF both tuples are in the LHS of the rule then it's an edge
		if (indexOfSrcTuple < witSize - 1 && indexOfSrcTuple >= 0 && indexOfDstTuple < witSize - 1
				&& indexOfDstTuple >= 0) {
			return true;
		}
		return false;
	}

	private boolean isEdgeByCGR(DBTuple srcTuple, DBTuple dstTuple, Witness tupleWitness) {

		Rule rule = tupleWitness.getRule();
		if (rule.isTupleGenerating()) {
			System.out.println("FATAL in isEdgeByCGR because it's a TGR");
			System.exit(0);
		}

		int indexOfSrcTuple = tupleWitness.getTuples().indexOf(srcTuple);
		int indexOfDstTuple = tupleWitness.getTuples().indexOf(dstTuple);

		if (indexOfSrcTuple == indexOfDstTuple) {

			System.out.println("FATAL in isEdgeByCGR because src tuple and dst tuple are the same");
			System.exit(0);
		}
		/*
		 * HashMap<String, String> assignment = fillAssignment(tupleWitness,
		 * currTuple); // find the new rule, put the assignment and try to find
		 * an assignment for the unchanged variables // if you found add edge
		 * else don't add edge LOGGER.warning("Assignment = " +
		 * assignment.toString());
		 * 
		 * Condition condition = new Condition(tupleWitness.getRule());
		 * condition.assign(assignment); if
		 * (condition.hasSatisfyingAssignment()) { graph.addEdge(currTuple,
		 * suspiciousTuple); }
		 */
		return true;
	}

	private HashMap<String, String> fillAssignment(Witness witness, DBTuple unchanged) {
		HashMap<String, String> assignment = new HashMap<String, String>();

		int i = 0;
		for (int j = 0; j < witness.getRule().getLHSFormulaCount(); j++) {
			if (witness.getRule().getLHSFormulaAt(j) instanceof ConditionalFormula) {
				continue;
			}
			RelationalFormula formula = (RelationalFormula) witness.getRule().getLHSFormulaAt(j);
			DBTuple tuple = witness.getTuples().get(i);
			if (tuple.equals(unchanged)) {
				for (int varIndex = 0; varIndex < formula.getVariableCount(); varIndex++) {
					String varName = formula.getVariableAt(varIndex).getName();
					assignment.put(varName, varName);
				}
			} else {
				for (int varIndex = 0; varIndex < formula.getVariableCount(); varIndex++) {
					String varName = formula.getVariableAt(varIndex).getName();
					String value = tuple.getValue(formula.getVariableAt(varIndex).getColumn());
					assignment.put(varName, value);
				}
			}
			i++;
		}
		return assignment;
	}

	private void buildSimpleDB() {
		m_dbName = "C:" + File.separator + "temp" + File.separator + "example.db";
		LOGGER.info("Generated the simple database " + m_dbName);
		try {
			File dbFile = new File(m_dbName);
			if (!dbFile.exists()) {
				dbFile.createNewFile();
			}
			Connection conn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			conn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);
			LOGGER.info("Opened simple database successfully");

			stmt = conn.createStatement();
			String sql = "DROP TABLE IF EXISTS Games;";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "DROP TABLE IF EXISTS Teams;";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "DROP TABLE IF EXISTS Countries;";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "DROP TABLE IF EXISTS Allowed;";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "CREATE TABLE Games " + "(ID INT PRIMARY KEY NOT NULL," + " first_team TEXT NOT NULL, "
					+ " second_team TEXT NOT NULL, " + " first_team_goals INTEGER NOT NULL, "
					+ " second_team_goals INTEGER NOT NULL, " + " first_team_pen INTEGER NOT NULL, "
					+ " second_team_pen INTEGER NOT NULL);";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "CREATE TABLE Teams " + "(ID INT PRIMARY KEY NOT NULL," + " team TEXT NOT NULL, "
					+ " country TEXT NOT NULL);";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "CREATE TABLE Countries " + "(ID INT PRIMARY KEY NOT NULL," + " country TEXT NOT NULL, "
					+ " continent TEXT NOT NULL);";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "CREATE TABLE Allowed " + "(ID INT PRIMARY KEY NOT NULL," + " team TEXT NOT NULL);";
			stmt.executeUpdate(sql);
			stmt.close();

			// Insert values to games table
			stmt = conn.createStatement();
			sql = "INSERT INTO Games (ID, first_team, second_team, first_team_goals, second_team_goals, first_team_pen, second_team_pen) VALUES "
					+ "(1,'Milan','Bayren',3,2,0,0);";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Games (ID, first_team, second_team, first_team_goals, second_team_goals, first_team_pen, second_team_pen) VALUES "
					+ "(2,'Italy','Bayren',3,2,0,0);";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Games (ID, first_team, second_team, first_team_goals, second_team_goals, first_team_pen, second_team_pen) VALUES "
					+ "(3,'Barcelona','Real',1,1,0,0);";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Games (ID, first_team, second_team, first_team_goals, second_team_goals, first_team_pen, second_team_pen) VALUES "
					+ "(4,'Barcelona','Bayren',3,1,0,0);";
			stmt.executeUpdate(sql);
			stmt.close();

			// Insert values to teams table
			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES " + "(1,'Milan','Italy');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES " + "(2,'Bayren','Germany');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES " + "(3,'Barcelona','Spain');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES " + "(4,'Italy','Italy');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES " + "(5,'Real','Spain');";
			stmt.executeUpdate(sql);
			stmt.close();

			// Insert values to countries table
			stmt = conn.createStatement();
			sql = "INSERT INTO Countries (ID, country, continent) VALUES " + "(1,'Italy','Asia');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Countries (ID, country, continent) VALUES " + "(2,'Italy','Europe');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Countries (ID, country, continent) VALUES " + "(3,'Germany','Europe');";
			stmt.executeUpdate(sql);
			stmt.close();

			// Insert values to allowed table
			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES " + "(1,'Barcelona');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES " + "(2,'Real');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES " + "(3,'Bayren');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES " + "(4,'Milan');";
			stmt.executeUpdate(sql);

			stmt.close();
			conn.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		LOGGER.info("Simple database created successfully");
	}

	private void buildValidatedDB() {

		m_validatedDBName = new String(m_dbName);
		if (m_validatedDBName.indexOf(".") > 0) {
			m_validatedDBName = m_validatedDBName.substring(0, m_validatedDBName.lastIndexOf("."));
		}
		m_validatedDBName = m_validatedDBName + "_validated.db";

		LOGGER.info("Generated the validated database " + m_validatedDBName);
		try {
			File dbFile = new File(m_validatedDBName);
			if (dbFile.exists()) {
				dbFile.delete();
			}
			dbFile.createNewFile();

			Connection dbConn = null;
			Connection validatedDBConn = null;
			Statement stmt = null;
			Class.forName("org.sqlite.JDBC");
			dbConn = DriverManager.getConnection("jdbc:sqlite:" + m_dbName);
			validatedDBConn = DriverManager.getConnection("jdbc:sqlite:" + m_validatedDBName);
			LOGGER.info("Opened validated database successfully");

			List<String> tables = new ArrayList<String>();
			stmt = dbConn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT name FROM sqlite_master WHERE type='table';");
			while (rs.next()) {
				tables.add(rs.getString(1));
			}
			rs.close();
			stmt.close();

			for (String table : tables) {
				List<String> columns = new ArrayList<String>();
				stmt = dbConn.createStatement();
				rs = stmt.executeQuery("PRAGMA table_info('" + table + "');");
				while (rs.next()) {
					columns.add(rs.getString(2));
				}
				stmt.close();

				long rowsCount = 0;
				stmt = dbConn.createStatement();
				rs = stmt.executeQuery("SELECT COUNT(*) FROM '" + table + "';");
				rs.next();
				rowsCount = rs.getLong(1);
				rs.close();
				stmt.close();

				// Create the table
				String sql = "CREATE TABLE " + table + " ";
				String columnsStr = "(";
				for (int i = 0; i < columns.size(); i++) {
					String column = columns.get(i);
					if (i == columns.size() - 1) {
						columnsStr += column + " INT NOT NULL";
					} else {
						columnsStr += column + " INT NOT NULL, ";
					}
				}
				columnsStr += ")";
				sql += columnsStr + ";";
				stmt = validatedDBConn.createStatement();
				stmt.executeUpdate(sql);
				stmt.close();

				for (long rowIndex = 0; rowIndex < rowsCount; rowIndex++) {
					// For each table row insert zeroes row
					columnsStr = "(";
					for (int i = 0; i < columns.size(); i++) {
						String column = columns.get(i);
						if (i == columns.size() - 1) {
							columnsStr += column + "";
						} else {
							columnsStr += column + ", ";
						}
					}
					columnsStr += ")";

					sql = "INSERT INTO " + table + " " + columnsStr + " VALUES ";
					String valuesStr = "(";
					for (int i = 0; i < columns.size(); i++) {
						if (i == columns.size() - 1) {
							valuesStr += "0";
						} else {
							valuesStr += "0, ";
						}
					}
					valuesStr += ")";
					sql += valuesStr + ";";
					stmt = validatedDBConn.createStatement();
					stmt.executeUpdate(sql);
					stmt.close();
				}

			}

			dbConn.close();
			validatedDBConn.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		LOGGER.info("Validated database created successfully");
	}
}

package servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import data.Condition;
import data.ConditionalFormula;
import data.DBTuple;
import data.Graph;
import data.RelationalFormula;
import data.RulesReader;
import data.Vertex;
import data.Witness;
import data.WitnessesManager;

/**
 * Servlet implementation class HellowWorld
 */
@WebServlet("/*")
public class Initialize extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String m_dbName = null;
	private String m_validatedDBName = null;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Initialize() {
		super();
		LOGGER.setLevel(Level.INFO);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		buildSimpleDB();
		buildValidatedDB();
		RulesReader reader = new RulesReader("C:" + File.separator + "temp" + File.separator + "rules.xml");

		WitnessesManager witManager = new WitnessesManager(reader.getRules(), m_dbName);
		witManager.calculateWitnesses();

		Graph graph = new Graph();
		List<DBTuple> suspiciousTuples = witManager.getSuspiciousTuples();
		for (DBTuple tuple : suspiciousTuples) {
			graph.addVertex(new Vertex(tuple));
		}
		for (DBTuple tuple : suspiciousTuples) {
			List<Witness> tupleWitnesses = witManager.getTupleWitnesses(tuple);

			for (Witness tupleWitness : tupleWitnesses) {
				updateGraphEdges(graph, tuple, tupleWitness);
			}
		}

		LOGGER.info(graph.toString());

		// Set response content type
		response.setContentType("text/html");

		// Actual logic goes here.
		/*
		 * PrintWriter out = response.getWriter(); String str =
		 * "<html><head><script type=\"text/javascript\" src=\"WebContent/js/classes.js\"></script>"
		 * ; str +=
		 * "<script type=\"text/javascript\" src=\"WebContent/js/jquery.min.js\"></script>"
		 * ; str +=
		 * "<script type=\"text/javascript\" src=\"WebContent/js/jquery.qtip-1.0.0-rc3.min.js\"></script>"
		 * ; str +=
		 * "</head><body><script type=\"text/javascript\">buildGraph();</script></body></html>"
		 * ; out.println(str);
		 */
		response.getWriter().append("Served at: ").append(request.getContextPath());
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private void updateGraphEdges(Graph graph, DBTuple suspiciousTuple, Witness tupleWitness) {

		if (tupleWitness.getRule().isTupleGenerating()) {
			return;
		}

		for (int i = 0; i < tupleWitness.getTuples().size(); i++) {

			DBTuple currTuple = tupleWitness.getTuples().get(i);
			if (currTuple.equals(suspiciousTuple)) {
				continue;
			}

			graph.addEdge(currTuple, suspiciousTuple);
			/*
			 * HashMap<String, String> assignment = fillAssignment(tupleWitness,
			 * currTuple); // find the new rule, put the assignment and try to
			 * find an assignment for the unchanged variables // if you found
			 * add edge else don't add edge LOGGER.warning("Assignment = " +
			 * assignment.toString());
			 * 
			 * Condition condition = new Condition(tupleWitness.getRule());
			 * condition.assign(assignment); if
			 * (condition.hasSatisfyingAssignment()) { graph.addEdge(currTuple,
			 * suspiciousTuple); }
			 */
		}
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
					//For each table row insert zeroes row 
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

package servlets;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import data.RulesReader;
import data.WitnessesManager;

/**
 * Servlet implementation class HellowWorld
 */
@WebServlet("/*")
public class Initialize extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String m_dbName = null;
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
		RulesReader reader = new RulesReader("C:" + File.separator + "temp" + File.separator + "rules.xml");
		
		WitnessesManager witManager = new WitnessesManager(reader.getRules(), m_dbName);
		witManager.calculateWitnesses();
		
		// Set response content type
		response.setContentType("text/html");

		// Actual logic goes here.
		PrintWriter out = response.getWriter();
		out.println("<h1>" + "Hello World!!!" + "</h1>");
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

	private void buildSimpleDB() {
		m_dbName = "C:" + File.separator + "temp" + File.separator + "example.db";
		LOGGER.info("Generated the simple database " + m_dbName);
		try {
			File dbFile = new File(m_dbName);
			if(!dbFile.exists()) {
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
			sql = "CREATE TABLE Games " + "(ID INT PRIMARY KEY NOT NULL,"
					+ " first_team TEXT NOT NULL, " + " second_team TEXT NOT NULL, "
					+ " first_team_goals INTEGER NOT NULL, " + " second_team_goals INTEGER NOT NULL, "
					+ " first_team_pen INTEGER NOT NULL, " + " second_team_pen INTEGER NOT NULL);";
			stmt.executeUpdate(sql);
			stmt.close();
			
			stmt = conn.createStatement();
			sql = "CREATE TABLE Teams " + "(ID INT PRIMARY KEY NOT NULL,"
					+ " team TEXT NOT NULL, " + " country TEXT NOT NULL);";
			stmt.executeUpdate(sql);
			stmt.close();
			
			stmt = conn.createStatement();
			sql = "CREATE TABLE Countries " + "(ID INT PRIMARY KEY NOT NULL,"
					+ " country TEXT NOT NULL, " + " continent TEXT NOT NULL);";
			stmt.executeUpdate(sql);
			stmt.close();
			
			stmt = conn.createStatement();
			sql = "CREATE TABLE Allowed " + "(ID INT PRIMARY KEY NOT NULL,"
					+ " team TEXT NOT NULL);";
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
			sql = "INSERT INTO Teams (ID, team, country) VALUES "
				+ "(1,'Milan','Italy');";
			stmt.executeUpdate(sql);
			stmt.close();
			
			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES "
				+ "(2,'Bayren','Germany');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES "
				+ "(3,'Barcelona','Spain');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES "
				+ "(4,'Italy','Italy');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Teams (ID, team, country) VALUES "
				+ "(5,'Real','Spain');";
			stmt.executeUpdate(sql);
			stmt.close();
			
			// Insert values to countries table
			stmt = conn.createStatement();
			sql = "INSERT INTO Countries (ID, country, continent) VALUES "
				+ "(1,'Italy','Asia');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Countries (ID, country, continent) VALUES "
				+ "(2,'Italy','Europe');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Countries (ID, country, continent) VALUES "
				+ "(3,'Germany','Europe');";
			stmt.executeUpdate(sql);
			stmt.close();
			
			// Insert values to allowed table
			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES "
				+ "(1,'Barcelona');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES "
				+ "(2,'Real');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES "
				+ "(3,'Bayren');";
			stmt.executeUpdate(sql);
			stmt.close();

			stmt = conn.createStatement();
			sql = "INSERT INTO Allowed (ID, team) VALUES "
				+ "(4,'Milan');";
			stmt.executeUpdate(sql);
			
			stmt.close();
			conn.close();
		} catch (Exception e) {
			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			System.exit(0);
		}
		LOGGER.info("Simple database created successfully");
	}

}

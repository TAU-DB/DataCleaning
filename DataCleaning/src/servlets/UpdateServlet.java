package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import Controllers.MainController;
import data.DBTuple;
import data.DatalogQuery;
import data.Graph;
import data.RelationalFormula;
import data.Rule;

/**
 * Servlet implementation class UpdateServlet
 */
@WebServlet("/UpdateServlet/*")
public class UpdateServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private final static Logger LOGGER = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public UpdateServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String queryStr = null;
		JSONArray queryResult = null;
		boolean isRunQuery = false;
		if (request.getPathInfo().equals("/ValidateTuple")) {
			validateTuple(request, response);
		}

		if (request.getPathInfo().equals("/DeleteTuple")) {
			deleteTuple(request, response);
		}

		if (request.getPathInfo().equals("/UpdateTuple")) {
			updateTuple(request, response);
		}

		if (request.getPathInfo().equals("/AddTuple")) {
			addTuple(request, response);
		}

		if (request.getPathInfo().equals("/RunQuery")) {
			setLastQuery(request, response);
			isRunQuery = true;
		}

		if (request.getPathInfo().equals("/DeleteAnswer")) {
			deleteAnswer(request, response);
		}

		if (request.getPathInfo().equals("/AddAnswer")) {
			getTuplesToFill(request, response);
		}

		queryResult = runQuery(request, response);
		MainController mainController = MainController.getInstance();
		queryStr = mainController.getLastQueriesStr();
		JSONObject result = new JSONObject();
		if (queryResult != null) {
			result.put("query_result", queryResult);
			result.put("query", queryStr);
		}

		if (isRunQuery) {

			insertJsonObjectToResponse(result, response);
			response.setStatus(200);
			return;
		}
		
		mainController.updateValidatedDB();
		Graph graph = mainController.generateGraph();
		graph.calculateEdgesProbabilities();

		List<DBTuple> anonymousTuples = mainController.getAnonymousTuples();
		for (DBTuple anonymousTuple : anonymousTuples) {
			if (mainController.isAnonymousTupleValidated(anonymousTuple)) {
				result = new JSONObject();
				result.put("is_graph", "0");
				result.put("tuple", anonymousTuple.toJSONObject());
				insertJsonObjectToResponse(result, response);
				response.setStatus(200);
				return;
			}
		}

		HashMap<DBTuple, Double> ranks = mainController.calculateRanks(graph);
		DBTuple maxRankedTuple = MainController.getMaxRankTuple(ranks);

		if (maxRankedTuple == null) {
			result.put("is_empty", "1");
		} else {
			result.put("is_empty", "0");
			result.put("is_graph", "1");
			result.put("graph", graph.toJSONObject());
			result.put("ranks", MainController.convertRanksMapToJSONObject(ranks));
			result.put("max", maxRankedTuple.toJSONObject());
		}
		insertJsonObjectToResponse(result, response);
		response.setStatus(200);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	private void validateTuple(HttpServletRequest request, HttpServletResponse response) {

		MainController mainController = MainController.getInstance();

		String tableName = request.getParameter("table_name");
		String[] tableColumns = request.getParameterValues("table_columns[]");
		String[] tupleValues = request.getParameterValues("tuple_values[]");

		DBTuple tuple = new DBTuple(tableName, false);
		for (int i = 0; i < tableColumns.length; i++) {
			tuple.setValue(tableColumns[i], tupleValues[i]);
		}

		LOGGER.info("Validating tuple: " + tuple.toString());

		mainController.setValidated(tuple);

		LOGGER.info("Added and Validated the tuple: " + tuple.toString());
	}

	private void deleteTuple(HttpServletRequest request, HttpServletResponse response) {

		MainController mainController = MainController.getInstance();

		String tableName = request.getParameter("table_name");
		String[] tableColumns = request.getParameterValues("table_columns[]");
		String[] tupleValues = request.getParameterValues("tuple_values[]");

		DBTuple tuple = new DBTuple(tableName, false);
		for (int i = 0; i < tableColumns.length; i++) {
			tuple.setValue(tableColumns[i], tupleValues[i]);
		}

		mainController.deleteTuple(tuple);
		LOGGER.info("Deleted the tuple: " + tuple.toString());
	}

	private void updateTuple(HttpServletRequest request, HttpServletResponse response) {

		MainController mainController = MainController.getInstance();

		String tableName = request.getParameter("table_name");
		String[] tableColumns = request.getParameterValues("table_columns[]");
		String[] tupleValues = request.getParameterValues("tuple_values[]");
		String[] tupleNewValues = request.getParameterValues("tuple_new_values[]");

		DBTuple tuple = new DBTuple(tableName, false);
		DBTuple newTuple = new DBTuple(tableName, false);
		for (int i = 0; i < tableColumns.length; i++) {
			tuple.setValue(tableColumns[i], tupleValues[i]);
			newTuple.setValue(tableColumns[i], tupleNewValues[i]);
		}

		mainController.updateTuple(tuple, newTuple);
		String info = System.lineSeparator();
		info += "Updated the tuple: " + tuple.toString() + System.lineSeparator();
		info += "With the tuple: " + newTuple.toString() + System.lineSeparator();
		LOGGER.info(info);
	}

	private void addTuple(HttpServletRequest request, HttpServletResponse response) {

		MainController mainController = MainController.getInstance();

		String tableName = request.getParameter("table_name");
		String[] tableColumns = request.getParameterValues("table_columns[]");
		String[] tupleValues = request.getParameterValues("tuple_values[]");

		DBTuple tuple = new DBTuple(tableName, false);
		for (int i = 0; i < tableColumns.length; i++) {
			tuple.setValue(tableColumns[i], tupleValues[i]);
		}

		mainController.addTuple(tuple);

		LOGGER.info("Added and Validated the tuple: " + tuple.toString());
	}

	private void setLastQuery(HttpServletRequest request, HttpServletResponse response) {

		MainController mainController = MainController.getInstance();

		List<DatalogQuery> queries = new ArrayList<DatalogQuery>();
		
		String queryStr = request.getParameter("query");
		if (!queryStr.contains(";")) {
			DatalogQuery query = new DatalogQuery(queryStr);
			queries.add(query);
		} else {
			String[] queryStrSplitted = queryStr.split(";");
			for (int i = 0; i < queryStrSplitted.length; i++) {
				DatalogQuery query = new DatalogQuery(queryStrSplitted[i]);
				queries.add(query);
			}
		}
		mainController.setLastQueries(queries, queryStr);

		LOGGER.info("Setting last query as: " + queryStr);
	}
	
	private JSONArray runQuery(HttpServletRequest request, HttpServletResponse response) {

		JSONArray resultArr = new JSONArray();
		MainController mainController = MainController.getInstance();
		List<DatalogQuery> queries = mainController.getLastQueries();
		if (queries != null) {
			for (DatalogQuery query : queries) {
				String sqlQuery = query.getSQLQuery();
				resultArr.addAll(mainController.runQuery(query));
				LOGGER.info("Running the query: " + sqlQuery);
			}
		}

		return resultArr;
	}

	private void deleteAnswer(HttpServletRequest request, HttpServletResponse response) {

		MainController mainController = MainController.getInstance();

		List<DatalogQuery> queries = mainController.getLastQueries();
		int rowsToDeleteCount = Integer.parseInt(request.getParameter("row_delete_count"));

		for (int i = 0; i < rowsToDeleteCount; i++) {
			String paramKey = "row" + i + "[]";
			String[] values = request.getParameterValues(paramKey);

			// Create new rule and add it to main controller
			for (DatalogQuery query : queries) {
				Rule rule = query.createDeleteRule(values);
				mainController.addRule(rule);
				LOGGER.info("Deleting the answer: " + values.toString());
				LOGGER.info("And added the rule: " + rule.toString());
			}
		}
	}

	private void getTuplesToFill(HttpServletRequest request, HttpServletResponse response) {

		MainController mainController = MainController.getInstance();

		String queryStr = request.getParameter("query");
		String[] values = request.getParameterValues("values[]");

		// Create new rule and add it to main controller
		DatalogQuery query = new DatalogQuery(queryStr);
		int relFormulaCount = query.getRelationalFormulaCount();
		
		for (int i = 0; i < relFormulaCount; i++) {
			RelationalFormula formula = query.getRelationalFormulaAt(i);
		}
		

		LOGGER.info("Adding the answer: " + values.toString());
	}

	private void insertJsonObjectToResponse(JSONObject obj, HttpServletResponse response) {
		response.setContentType("application/json");
		PrintWriter out;
		try {
			out = response.getWriter();
			out.print(obj);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

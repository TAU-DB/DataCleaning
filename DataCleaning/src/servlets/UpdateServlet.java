package servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import Controllers.MainController;
import data.DBTuple;
import data.Graph;

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
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		if (request.getPathInfo().equals("/AddTuple")) {
			addTuple(request, response);
		}

		if (request.getPathInfo().equals("/DeleteTuple")) {
			deleteTuple(request, response);
		}
		
		if (request.getPathInfo().equals("/UpdateTuple")) {
			updateTuple(request, response);
		}

		MainController mainController = MainController.getInstance();
		mainController.updateValidatedDB();
		Graph graph = mainController.generateGraph();
		HashMap<DBTuple, Double> ranks = mainController.calculateRanks(graph);
		DBTuple maxRankedTuple = MainController.getMaxRankTuple(ranks);
		
		JSONObject result = new JSONObject();
		result.put("graph", graph.toJSONObject());
		result.put("ranks", MainController.convertRanksMapToJSONObject(ranks));
		result.put("max", maxRankedTuple.toJSONObject());
		insertJsonObjectToResponse(result, response);
		response.setStatus(200);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
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
	
	private void insertJsonObjectToResponse(JSONObject obj,
			HttpServletResponse response) {
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

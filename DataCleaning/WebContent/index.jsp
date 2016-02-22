<%@page import="data.DBTuple"%>
<%@page import="java.util.HashMap"%>
<%@page import="data.Graph"%>
<%@page import="Controllers.MainController"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<script type="text/javascript" src="js/raphael-min.js"></script>
<script type="text/javascript" src="js/dracula_graffle.js"></script>
<script type="text/javascript" src="js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="js/jquery.qtip-1.0.0-rc3.min.js"></script>
<script type="text/javascript" src="js/dracula_graph.js"></script>
<script type="text/javascript" src="js/dracula_algorithms.js"></script>
<script type="text/javascript" src="js/graphViewer.js"></script>
<script type="text/javascript" src="js/index.js"></script>
<link rel="stylesheet" type="text/css" href="css/table.css">
<link rel="stylesheet" type="text/css" href="css/index.css">
<link rel="stylesheet" type="text/css" href="css/buttons.css">
<title>Data Cleaning</title>
</head>
<body>
	<%
		MainController mainController = MainController.getInstance();
		Graph graph = mainController.generateGraph();
		HashMap<DBTuple, Double> ranks = mainController.calculateRanks(graph);
		String ranksMapStr = MainController.convertRanksMapToStr(ranks);
		DBTuple maxRankedTuple = MainController.getMaxRankTuple(ranks);
	%>
</body>
<center>
	<h2 id="graph_title">Tuples Graph</h2>
</center>
<div id="graph_canvas"
	style="">
</div>

<h2 id="table_title">Question:</h2>

<table id="question_table" class="table-fill">
</table>

<div id="buttons">
	<a href="#" onclick="addTupleRequest();return false;" class="btn green">Add&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</a> 
	<a href="#" onclick="deleteTupleRequest();return false;" class="btn red">Delete&nbsp;&nbsp;&nbsp;&nbsp;</a> 
	<a href="#" onclick="updateTupleRequest();return false;" class="btn orange">Update&nbsp;&nbsp;&nbsp;&nbsp;</a> 
</div>
<script type="text/javascript">
	var g =
<%=graph.toJSMapStr()%>
	;
	var ranks =
<%=ranksMapStr%>
	;
	var maxTupleStr =
<%=maxRankedTuple.getJSMapStr()%>
	;
	buildGraph(g, ranks);
	updateQuestionTable(maxTupleStr);
</script>
</html>
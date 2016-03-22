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
<script type="text/javascript" src="js/go.js"></script>
<script type="text/javascript" src="js/go-debug.js"></script>
<script type="text/javascript" src="js/graphViewer.js"></script>
<script type="text/javascript" src="js/index.js"></script>
<link rel="stylesheet" type="text/css" href="css/table.css">
<link rel="stylesheet" type="text/css" href="css/index.css">
<link rel="stylesheet" type="text/css" href="css/buttons.css">
<link rel="stylesheet" type="text/css" href="css/spinner.css">
<title>Data Cleaning</title>
</head>
<body>
	<div id="content"></div>
	<%
		MainController mainController = MainController.getInstance();
		Graph graph = mainController.generateGraph();
		graph.calculateEdgesProbabilities();
		HashMap<DBTuple, Double> ranks = mainController.calculateRanks(graph);
		String ranksMapStr = MainController.convertRanksMapToStr(ranks);
		DBTuple maxRankedTuple = MainController.getMaxRankTuple(ranks);
		String maxTupleJSMap = "{}";
		if (maxRankedTuple != null) {
			maxTupleJSMap = maxRankedTuple.getJSMapStr();
		}
	%>
	<script type="text/javascript">
		buildGraphModeContent("0", undefined);
		var g =
	<%=graph.toJSONObject().toJSONString()%>
		;
		var ranks =
	<%=ranksMapStr%>
		;
		var maxTupleStr =
	<%=maxTupleJSMap%>
		;
		if ($.isEmptyObject(maxTupleStr))
		{
			buildGraphModeContent("1", undefined);
		} else {
			buildGraph(g, ranks);
			updateQuestionTable(maxTupleStr);
		}
	</script>
</body>
</html>
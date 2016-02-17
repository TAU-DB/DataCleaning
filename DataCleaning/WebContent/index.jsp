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
<script type="text/javascript" src="js/dracula_graph.js"></script>
<script type="text/javascript" src="js/dracula_algorithms.js"></script>
<script type="text/javascript" src="js/graphViewer.js"></script>
<title>Insert title here</title>
</head>
<body>
	<%
		MainController mainController = MainController.getInstance();
		Graph graph = mainController.generateGraph();
		HashMap<String, Double> ranks = mainController.calculateRanks(graph);
		
		String ranksMapStr = "{";
		int vertexIndex = 0;
		for (String v : ranks.keySet()) {
			ranksMapStr += "\"" + v + "\"";
			ranksMapStr += " : " + ranks.get(v);
			
			if (vertexIndex < ranks.keySet().size() - 1) {
				ranksMapStr += ", ";
			}
			vertexIndex ++;
		}
		ranksMapStr += "}";
	%>
	This is a page a simple page
	<div id="canvas"></div>
	<script type="text/javascript">
		var g = <%=graph.toJSMapStr()%>
		var ranks = <%=ranksMapStr%>
		buildGraph(g, ranks);
	</script>
</body>
</html>
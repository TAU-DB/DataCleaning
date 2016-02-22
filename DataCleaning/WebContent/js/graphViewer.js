//Show UCLA CS class dependencies (not complete)

function buildGraph(graphMap, ranks) {
	$(document).ready(function() {
		$("#graph_canvas").empty();
		var width = $("#graph_canvas").width();
		var height = $("#graph_canvas").height();
		var g = new Graph();
		g.edgeFactory.template.style.directed = true;

		var nodeCnt = Object.keys(graphMap).length;
		var i = 0;
		while (nodeCnt >= i*i) {
			i++
		}
		var currX = 0;
		var currY = 0;
		for ( var source in graphMap) {
			var nodeName = source + " \n "+ ranks[source];
			g.addNode(nodeName, {
				x : currX,
				y : currY
			});
			if (currX == i - 1) {
				currX = 0;
				currY = currY + 1;
			} else {
				currX = currX + 1;
			}
		}
		for ( var source in graphMap) {
			var srcNodeName = source + " \n "+ ranks[source];
			var neighbors = graphMap[source];
			for (i = 0; i < neighbors.length; i++) {
				var dstNodeName = neighbors[i] + " \n "+ ranks[neighbors[i]];
				g.addEdge(srcNodeName, dstNodeName);
			}
		}

		var layouter = new Graph.Layout.Fixed(g);
		layouter.layout();
		var renderer = new Graph.Renderer.Raphael('graph_canvas', g, width, height);
		renderer.draw();
	});
}

Graph.Layout.Fixed = function(graph) {
	this.graph = graph;
	this.layout();
};

Graph.Layout.Fixed.prototype = {
	layout : function() {
		this.layoutPrepare();
		this.layoutCalcBounds();
	},

	layoutPrepare : function() {
		for (i in this.graph.nodes) {
			var node = this.graph.nodes[i];
			if (node.x) {
				node.layoutPosX = node.x;
			} else {
				node.layoutPosX = 0;
			}
			if (node.y) {
				node.layoutPosY = node.y;
			} else {
				node.layoutPosY = 0;
			}
		}
	},

	layoutCalcBounds : function() {
		var minx = Infinity, maxx = -Infinity, miny = Infinity, maxy = -Infinity;

		for (i in this.graph.nodes) {
			var x = this.graph.nodes[i].layoutPosX;
			var y = this.graph.nodes[i].layoutPosY;

			if (x > maxx)
				maxx = x;
			if (y > maxy)
				maxy = y;
			if (y < miny)
				miny = y;
			if (x < minx)
				minx = x;
		}

		this.graph.layoutMinX = minx;
		this.graph.layoutMaxX = maxx;

		this.graph.layoutMinY = miny;
		this.graph.layoutMaxY = maxy;
	}
};
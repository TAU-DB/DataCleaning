
//var colors = [ "#FF0000", "#FFFF00", "#008000", "#008080", "#00FFFF",
//		"#FF00FF", "#0000FF", "#C0C0C0", "#00FF00", "#000080" ];
var colors = [ "#D3FF59", "#6AB3EB", "#FF696B", "#8CDE9C", "#FF87E7",
		"#FF87E7", "#8CFFF5", "#8CFFDE", "#FFA18C", "#3BFF55" ];
function buildGraph(graphMap, ranks) {
	$(document).ready(
			function() {

				$("#graph_canvas").empty();
				var width = $("#graph_canvas").width();
				var height = $("#graph_canvas").height();
				
			    var myDiagram = go.GraphObject.make(go.Diagram, "graph_canvas",  // create
																					// a
																					// Diagram
																					// for
														// the DIV HTML element
			            {
			    	 //initialAutoScale: go.Diagram.UniformToFill, // an initial
						// automatic zoom-to-fit
			         contentAlignment: go.Spot.Center, // align document
						// to the center of the viewport
			          layout:
			        	  go.GraphObject.make(go.ForceDirectedLayout,  // automatically
																		// spread
																		// nodes
																		// apart
			              { defaultSpringLength: 80, defaultElectricalCharge: 50 })
			        });
			    // define a simple Node template
			    myDiagram.nodeTemplate =
			    	go.GraphObject.make(go.Node, "Auto",  // the Shape will go
															// around the
										// TextBlock
			    			go.GraphObject.make(go.Shape, "RoundedRectangle",
			          // Shape.fill is bound to Node.data.color
			          new go.Binding("fill", "color")),
			          go.GraphObject.make(go.TextBlock,
			          { margin: 3 },  // some room around the text
			          // TextBlock.text is bound to Node.data.key
			          new go.Binding("text", "key"))
			      );
			    
			 // replace the default Link template in the linkTemplateMap
			    var makefunc = go.GraphObject.make;
			    myDiagram.linkTemplate =
			    	makefunc(go.Link,  { curve: go.Link.Bezier, curviness : 10 },  // the
																					// whole
																					// link
																					// panel
			    			makefunc(go.Shape,  // the link shape
			          { stroke: "black" }),
			          makefunc(go.Shape,  // the arrowhead
			          { toArrow: "standard", stroke: null }),
			          makefunc(go.Panel, "Auto",
			        		  makefunc(go.Shape,  // the link shape
			            { fill: makefunc(go.Brush, "Radial", { 0: "rgb(240, 240, 240)", 0.3: "rgb(240, 240, 240)", 1: "rgba(240, 240, 240, 0)" }),
			              stroke: null }),
			              makefunc(go.TextBlock,  // the label
			            { textAlign: "center",
			              font: "10pt helvetica, arial, sans-serif",
			              stroke: "#555555",
			              margin: 4 },
			            new go.Binding("text", "text"))
			        )
			      );
			    myDiagram.scale = 2;
				var neighborsMap = graphMap["neighbors"];
				var tablesIDsMap = graphMap["tables_ids"];

				var nodes = [];
				var edges = [];
				var count = {};
				for ( var source in neighborsMap) {
					var nodeName = source + " \n " + (Math.round(ranks[source] * 1000) / 1000);
					var tableID = tablesIDsMap[source];
					if (count[tableID] == undefined) {
						count[tableID] = 0;
					}
					count[tableID] += 1;
					var colorIndex = tableID % colors.length;
					nodes.push({ key: nodeName, color: colors[colorIndex] });
				}

				var str = "";
				for (var table_id in count) {
					str += table_id + "    " + count[table_id] + "\n";
				}
				//alert("The vertexes count is " + nodes.length);
				//alert(str);
				for ( var source in neighborsMap) {
					var srcNodeName = source + " \n " + (Math.round(ranks[source] * 1000) / 1000);
					var neighbors = neighborsMap[source];
					for (i = 0; i < neighbors.length; i++) {
						var currNeighbor = neighbors[i];
						var dstNodeName = currNeighbor["id"] + " \n "
								+ (Math.round(ranks[currNeighbor["id"]] * 1000) / 1000);
						var edgeProb = Math.round(currNeighbor["probability"] * 1000) / 1000;
						edges.push({ from: srcNodeName, to: dstNodeName, text: edgeProb.toString()});
					}
				}
				
				

			    // but use the default Link template, by not setting
				// Diagram.linkTemplate
			    // create the model data that will be represented by Nodes and
				// Links
			    myDiagram.model = new go.GraphLinksModel(nodes, edges);
			}
	);

};
/**
 * index.js
 */

function updateQuestionTable(tuple) {

	var table = $("#question_table");
	table.empty();

	// Add the header
	var thead = $("<thead>").appendTo(table);
	var headerTR = $("<tr>").appendTo(thead);
	$("<th>").attr("id", "table_name").attr("class", "text-left").text(
			tuple["table_name"]).appendTo(headerTR);
	$("<th>").attr("class", "text-left").text("Current Value").appendTo(
			headerTR);
	$("<th>").attr("class", "text-left").text("New Value").appendTo(headerTR);

	// Add the rows
	var columns = tuple["columns"];
	var tbody = $("<tbody>").attr("id", "table_body").appendTo(table);
	for ( var column in columns) {
		var row = $("<tr>").appendTo(tbody);
		$("<td>").attr("class", "text-left").text(column).appendTo(row);
		$("<td>").attr("class", "text-left").text(columns[column])
				.appendTo(row);
		var inputTD = $("<td>").attr("class", "text-left").appendTo(row);
		var textbox = $('<input/>');
		textbox.attr("id", column);
		textbox.attr("type", "textbox");
		textbox.appendTo(inputTD);
	}
}

function addTupleRequest() {
	var tableName = getTupleTableName();
	var columns = getTupleColumns();
	var values = getTupleValues();
	var newValues = getTupleNewValues();
	
	$.ajax({
		type : "POST",
		cache : false,
		url : "UpdateServlet/AddTuple",
		data : {
			table_name : tableName,
			table_columns : columns,
			tuple_values : values,
			tuple_new_values : newValues
		}
	}).done(function(result) {
		var graph = result["graph"];
		var ranks = result["ranks"];
		var maxTuple = result["max"];
		buildGraph(graph, ranks);
		updateQuestionTable(maxTuple);
	});
}

function deleteTupleRequest() {

	var tableName = getTupleTableName();
	var columns = getTupleColumns();
	var values = getTupleValues();
	var newValues = getTupleNewValues();
	
	$.ajax({
		type : "POST",
		cache : false,
		url : "UpdateServlet/DeleteTuple",
		data : {
			table_name : tableName,
			table_columns : columns,
			tuple_values : values,
			tuple_new_values : newValues
		}
	}).done(function(result) {
		var graph = result["graph"];
		var ranks = result["ranks"];
		var maxTuple = result["max"];
		buildGraph(graph, ranks);
		updateQuestionTable(maxTuple);
	});
}

function updateTupleRequest() {

	var tableName = getTupleTableName();
	var columns = getTupleColumns();
	var values = getTupleValues();
	var newValues = getTupleNewValues();
	
	$.ajax({
		type : "POST",
		cache : false,
		url : "UpdateServlet/UpdateTuple",
		data : {
			table_name : tableName,
			table_columns : columns,
			tuple_values : values,
			tuple_new_values : newValues
		}
	}).done(function(result) {
		var graph = result["graph"];
		var ranks = result["ranks"];
		var maxTuple = result["max"];
		buildGraph(graph, ranks);
		updateQuestionTable(maxTuple);
	});
}

function getTupleTableName() {
	var tableName = $("#table_name").text();
	return tableName;
}

function getTupleColumns() {
	var columns = [];
	var childrenTR = $("#table_body tr");
	for (var i = 0; i < childrenTR.length; i++) {
		var colName = childrenTR[i].getElementsByTagName("td")[0].textContent;
		columns[i] = colName;
	}
	return columns;
}

function getTupleValues() {
	var values = [];
	var childrenTR = $("#table_body tr");
	for (var i = 0; i < childrenTR.length; i++) {
		var value = childrenTR[i].getElementsByTagName("td")[1].textContent;
		values[i] = value;
	}
	return values;
}

function getTupleNewValues() {
	var newValues = [];
	var childrenTR = $("#table_body tr");
	for (var i = 0; i < childrenTR.length; i++) {
		var newValue = childrenTR[i].getElementsByTagName("td")[2]
				.getElementsByTagName("input")[0].value;
		newValues[i] = newValue;
	}
	return newValues;
}
/**
 * index.js
 */

function buildSpinnerMode() {

	var contentDiv = $("#content");
	contentDiv.empty();
	var spinnerDiv = $("<div>").attr("class", "spinner").appendTo(contentDiv);
	var small1Div = $("<div>").attr("class", "small1").appendTo(spinnerDiv);
	$("<div>").attr("class", "small ball smallball1").appendTo(small1Div);
	$("<div>").attr("class", "small ball smallball2").appendTo(small1Div);
	$("<div>").attr("class", "small ball smallball3").appendTo(small1Div);
	$("<div>").attr("class", "small ball smallball4").appendTo(small1Div);

	var small2Div = $("<div>").attr("class", "small2").appendTo(spinnerDiv);
	$("<div>").attr("class", "small ball smallball5").appendTo(small2Div);
	$("<div>").attr("class", "small ball smallball6").appendTo(small2Div);
	$("<div>").attr("class", "small ball smallball7").appendTo(small2Div);
	$("<div>").attr("class", "small ball smallball8").appendTo(small2Div);

	var bigconDiv = $("<div>").attr("class", "bigcon").appendTo(spinnerDiv);
	$("<div>").attr("class", "big ball").appendTo(bigconDiv);
}

function buildGraphModeContent(isEmpty) {

	var contentDiv = $("#content");
	contentDiv.empty();
	var graphTitleCenter = $("<center>").appendTo(contentDiv);
	$("<h2>").attr("id", "graph_title").text("Tuples Graph").appendTo(
			graphTitleCenter);
	$("<div>").attr("id", "graph_canvas").appendTo(contentDiv);
	if (isEmpty == "0") {
		$("<h2>").attr("id", "table_title").text("Question:").appendTo(contentDiv);
		$("<h2>").attr("id", "question_table").attr("class", "table-fill")
				.appendTo(contentDiv);
		var buttonsDiv = $("<div>").attr("id", "buttons").appendTo(contentDiv);
		var validateButton = $("<a>").attr("href", "#");
		validateButton.attr("onclick", "validateTupleRequest();return false;");
		validateButton.attr("class", "btn green");
		validateButton.text("Validate  ");
		validateButton.appendTo(buttonsDiv);
		var deleteButton = $("<a>").attr("href", "#");
		deleteButton.attr("onclick", "deleteTupleRequest();return false;");
		deleteButton.attr("class", "btn red");
		deleteButton.text("Delete    ");
		deleteButton.appendTo(buttonsDiv);
		var updateButton = $("<a>").attr("href", "#");
		updateButton.attr("onclick", "updateTupleRequest();return false;");
		updateButton.attr("class", "btn orange");
		updateButton.text("Update    ");
		updateButton.appendTo(buttonsDiv);
	}
}

function buildQuestionModeContent() {

	var contentDiv = $("#content");
	contentDiv.empty();
	$("<h2>").attr("id", "table_title").text("Fill Tuple:")
			.appendTo(contentDiv);
	$("<h2>").attr("id", "question_table").attr("class", "table-fill")
			.appendTo(contentDiv);
	var buttonsDiv = $("<div>").attr("id", "buttons").appendTo(contentDiv);
	var addButton = $("<a>").attr("href", "#");
	addButton.attr("onclick", "addTupleRequest();return false;");
	addButton.attr("class", "btn green");
	addButton.text("Add       ");
	addButton.appendTo(buttonsDiv);
}

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

function updateFillTable(tuple) {

	var table = $("#question_table");
	table.empty();

	// Add the header
	var thead = $("<thead>").appendTo(table);
	var headerTR = $("<tr>").appendTo(thead);
	$("<th>").attr("id", "table_name").attr("class", "text-left").text(
			tuple["table_name"]).appendTo(headerTR);
	$("<th>").attr("class", "text-left").text("Value").appendTo(headerTR);

	// Add the rows
	var columns = tuple["columns"];
	var tbody = $("<tbody>").attr("id", "table_body").appendTo(table);
	for ( var column in columns) {
		var row = $("<tr>").appendTo(tbody);
		$("<td>").attr("class", "text-left").text(column).appendTo(row);
		if (columns[column] == ".*") {
			var inputTD = $("<td>").attr("class", "text-left").appendTo(row);
			var textbox = $('<input/>');
			textbox.attr("id", column);
			textbox.attr("type", "textbox");
			textbox.appendTo(inputTD);
		} else {
			$("<td>").attr("class", "text-left").text(columns[column])
					.appendTo(row);
		}
	}
}

function validateTupleRequest() {
	var tableName = getTupleTableName();
	var columns = getTupleColumns();
	var values = getTupleValues();
	var newValues = getTupleNewValues();

	buildSpinnerMode();
	
	$.ajax({
		type : "POST",
		cache : false,
		url : "UpdateServlet/ValidateTuple",
		data : {
			table_name : tableName,
			table_columns : columns,
			tuple_values : values,
			tuple_new_values : newValues
		}
	}).done(function(result) {
		buildContent(result);
	});
}

function deleteTupleRequest() {

	var tableName = getTupleTableName();
	var columns = getTupleColumns();
	var values = getTupleValues();
	var newValues = getTupleNewValues();

	buildSpinnerMode();

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
		buildContent(result);
	});
}

function updateTupleRequest() {

	var tableName = getTupleTableName();
	var columns = getTupleColumns();
	var values = getTupleValues();
	var newValues = getTupleNewValues();

	buildSpinnerMode();

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
		buildContent(result);
	});
}

function addTupleRequest() {
	var tableName = getTupleTableName();
	var columns = getTupleColumns();
	var values = getTupleValuesFromFillTable();

	buildSpinnerMode();

	$.ajax({
		type : "POST",
		cache : false,
		url : "UpdateServlet/AddTuple",
		data : {
			table_name : tableName,
			table_columns : columns,
			tuple_values : values,
			tuple_new_values : []
		}
	}).done(function(result) {
		buildContent(result);
	});
}

function buildContent(ajaxResult) {
	var isEmpty = ajaxResult["is_empty"];
	if (isEmpty == "1") {
		buildGraphModeContent("1");
		return;
	}
	
	var isGraph = ajaxResult["is_graph"];
	if (isGraph == "1") {
		buildGraphModeContent("0");
		var graph = ajaxResult["graph"];
		var ranks = ajaxResult["ranks"];
		var maxTuple = ajaxResult["max"];
		buildGraph(graph, ranks);
		updateQuestionTable(maxTuple);
		return;
	}
	buildQuestionModeContent();
	var tuple = ajaxResult["tuple"];
	updateFillTable(tuple);
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

function getTupleValuesFromFillTable() {

	var values = [];
	var childrenTR = $("#table_body tr");
	for (var i = 0; i < childrenTR.length; i++) {
		var value = "";
		var tdsArray = childrenTR[i].getElementsByTagName("td");
		var inputs = tdsArray[1].getElementsByTagName("input");
		if (inputs.length == 0) {
			value = tdsArray[1].textContent;
		} else {
			value = inputs[0].value;
		}
		values[i] = value;
	}
	return values;
}
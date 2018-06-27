<%@page contentType="text/html; charset=UTF-8" %><!doctype>
<html>
	<head>
		<meta charset="utf-8">
		<title>WShell JDBC</title>
	</head>
	<body>
		<div>
			<span style="display:inline-block;width:80px;">Driver: </span>
			<input id="driver" type="text" style="width:400px;" value="com.mysql.jdbc.Driver" />
		</div>
		<div>
			<span style="display:inline-block;width:80px;">URL: </span>
			<input id="url" type="text" style="width:400px;" value="jdbc:mysql://{host}:3306/{database}" />
		</div>
		<div>
			<span style="display:inline-block;width:80px;">User: </span>
			<input id="user" type="text" style="width:400px;" value="" />
		</div>
		<div>
			<span style="display:inline-block;width:80px;">Password: </span>
			<input id="password" type="text" style="width:400px;" value="" />
		</div>
		<div>
			<span style="display:inline-block;width:80px;">Limit: </span>
			<input id="limit" type="text" style="width:400px;" value="100" />
		</div>
		<div>
			<span style="display:inline-block;width:80px;">SQL: </span>
			<textarea id="sql" style="width:75%;height:60px"></textarea>
		</div>
		<div>
			<input type="button" value="Execute" onclick="execute()" />
		</div>
		<hr/>
		<div id="output"></div>
		<script
			src="https://code.jquery.com/jquery-2.2.4.min.js"
			integrity="sha256-BbhdlvQf/xTY9gja0Dq3HiwQF8LaCRTXxZKRutelT44="
			crossorigin="anonymous"></script>
		<script>
			function execute() {
				var params = {};
				params.driver = $("#driver").val();
				params.url = $("#url").val();
				params.user = $("#user").val();
				params.password = $("#password").val();
				params.sql = $("#sql").val();
				$.ajax({
					url: "jdbcService.jsp",
					type: "POST",
					data: params,
					dataType: "json",
					success: function(resp) {
						if (resp.resultData) {
							var html = "<table border='1' width='100%'>"
							var headerRow = resp.resultData[0];
							html += "<tr>";
							for(var i = 0; i < headerRow.length; i++) {
								html += "<th>" + headerRow[i] + "</th>";
							}
							html += "</tr>";
							for(var r = 1; r < resp.resultData.length; r++) {
								var row = resp.resultData[r];
								html += "<tr>";
								for(var i = 0; i < row.length; i++) {
									html += "<td>" + row[i] + "</td>";
								}
								html += "</tr>";
							}
							html += "</table>";
							$("#output").html(html);
						} else {
							$("#output").text(resp.message);
						}
					},
					error: function() {
						$("#output").html("<span style='color:red'>Request Error.</span>");
					}
				});
			}
		</script>
	</body>
</html>
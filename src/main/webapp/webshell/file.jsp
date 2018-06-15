<%@page contentType="text/html; charset=UTF-8" %><!doctype>
<html>
	<head>
		<meta charset="utf-8">
		<title>WShell</title>
	</head>
	<body>
		<div id="divUpload">
			<h2>Upload</h2>
			<form id="fmUpload" method="post" action="uploadService.jsp" enctype="multipart/form-data" target="_blank">
				<div>
					<span style="display:inline-block;width:100px;">Upload To:</span>
					<span><input name="uploadTo" type="text" style="width:200px;" value="/root" /></span>
				</div>
				<div>
					<span style="display:inline-block;width:100px;">Upload File:</span>
					<span><input name="uploadFile" type="file" style="width:200px;" /></span>
				</div>
				<div>
					<input type="submit" value="Upload" />
				</div>
			</form>
		</div>
		<div id="divDownload" style="margin-top:30px">
			<h2>Download</h2>
			<form id="fmDownload" method="post" action="downloadService.jsp" target="_blank">
				<div>
					<span style="display:inline-block;width:100px;">Download File:</span>
					<span><input name="downloadFile" type="text" style="width:200px;" value="/root/test.txt" /></span>
				</div>
				<div>
					<input type="submit" value="Download" />
				</div>
			</form>
		</div>
	</body>
</html>
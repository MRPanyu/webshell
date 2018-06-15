package webshell.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class DownloadServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String downloadFile = request.getParameter("downloadFile");
		File f = new File(downloadFile);
		if (!f.isFile()) {
			throw new ServletException("File not found.");
		}
		FileInputStream fin = new FileInputStream(f);
		try {
			String name = f.getName();
			response.addHeader("content-disposition", "attachment; filename=" + URLEncoder.encode(name, "UTF-8")
					+ ";filename*=UTF-8''" + URLEncoder.encode(name, "UTF-8"));
			response.setHeader("Content-Type", "application/octet-stream");
			OutputStream out = response.getOutputStream();
			byte[] buf = new byte[1024];
			int len = fin.read(buf);
			while (len > 0) {
				out.write(buf, 0, len);
				len = fin.read(buf);
			}
			out.flush();
			response.flushBuffer();
		} finally {
			fin.close();
		}
	}

}

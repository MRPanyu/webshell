package webshellclient.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

public class ProxyedSocket implements Closeable {

	private String key;
	private String socketProxyServletURL;
	private String host;
	private int port;

	private PipedInputStream pipedIn;
	private PipedOutputStream pipedOut;

	private ProxyedSocketOutputStream proxyedSocketOutputStream;

	private boolean closed;

	public ProxyedSocket(String socketProxyServletURL, String host, int port) throws IOException {
		this.key = UUID.randomUUID().toString();
		this.socketProxyServletURL = socketProxyServletURL;
		this.host = host;
		this.port = port;
		pipedOut = new PipedOutputStream();
		pipedIn = new PipedInputStream(pipedOut);
		proxyedSocketOutputStream = new ProxyedSocketOutputStream();
		open();
		Thread t = new Thread() {
			public void run() {
				processRead();
			}
		};
		t.setDaemon(true);
		t.setName("proxyedSocket[" + key + "]-read");
		t.start();
	}

	public void close() throws IOException {
		closed = true;
		pipedOut.close();
		proxyedSocketOutputStream.close();
		Map<String, String> headers = new LinkedHashMap<String, String>();
		headers.put("fn", "close");
		headers.put("key", key);
		httpPost(headers, null);
	}

	public InputStream getInputStream() throws IOException {
		return pipedIn;
	}

	public OutputStream getOutputStream() throws IOException {
		return proxyedSocketOutputStream;
	}

	public boolean isClosed() {
		return closed;
	}

	private void open() throws IOException {
		Map<String, String> headers = new LinkedHashMap<String, String>();
		headers.put("fn", "open");
		headers.put("key", key);
		headers.put("to_host", host);
		headers.put("to_port", String.valueOf(port));
		httpPost(headers, null);
	}

	private void processRead() {
		while (true) {
			try {
				Map<String, String> headers = new LinkedHashMap<String, String>();
				headers.put("fn", "read");
				headers.put("key", key);
				byte[] data = httpPost(headers, null);
				if (data.length > 0) {
					pipedOut.write(data);
				}
				if ("true".equals(headers.get("eof"))) {
					pipedOut.close();
					break;
				}
			} catch (Exception e) {
				e.printStackTrace();
				try {
					pipedOut.close();
				} catch (IOException e1) {
				}
				break;
			}
		}
	}

	private byte[] httpPost(Map<String, String> headers, byte[] data) throws IOException {
		HttpURLConnection conn = (HttpURLConnection) new URL(socketProxyServletURL).openConnection();
		try {
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setDoInput(true);
			for (Map.Entry<String, String> entry : headers.entrySet()) {
				String headerName = entry.getKey();
				String headerValue = entry.getValue();
				conn.setRequestProperty(headerName, headerValue);
			}
			OutputStream out = conn.getOutputStream();
			if (data != null) {
				out.write(data);
			}
			out.close();
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				byte[] resp = IOUtils.toByteArray(conn.getInputStream());
				if ("true".equals(conn.getHeaderField("eof"))) {
					headers.put("eof", "true");
				}
				return resp;
			} else {
				throw new IOException("http response: " + conn.getResponseCode() + "-" + conn.getResponseMessage());
			}
		} finally {
			conn.disconnect();
		}
	}

	private class ProxyedSocketOutputStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			write(new byte[] { (byte) b }, 0, 1);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			byte[] data = new byte[len];
			System.arraycopy(b, off, data, 0, len);
			if (len > 0) {
				Map<String, String> headers = new LinkedHashMap<String, String>();
				headers.put("fn", "write");
				headers.put("key", key);
				httpPost(headers, data);
			}
		}

		@Override
		public void close() throws IOException {
			try {
				super.close();
				Map<String, String> headers = new LinkedHashMap<String, String>();
				headers.put("fn", "closeOutput");
				headers.put("key", key);
				httpPost(headers, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}

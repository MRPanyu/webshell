package webshellclient.util;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.security.auth.login.Configuration;

public class Sock5ProxyHandler implements Runnable {

	private String socketProxyServletURL;
	private Socket socket;
	private ProxyedSocket proxySocket;
	private InputStream in0;
	private OutputStream out0;
	private InputStream in1;
	private OutputStream out1;

	public Sock5ProxyHandler(Socket socket, String socketProxyServletURL) {
		this.socket = socket;
		this.socketProxyServletURL = socketProxyServletURL;
	}

	@Override
	public void run() {
		try {
			in0 = socket.getInputStream();
			out0 = socket.getOutputStream();
			proxySocket = handleHead();
			if (proxySocket != null) {
				in1 = proxySocket.getInputStream();
				out1 = proxySocket.getOutputStream();
				Thread t1 = copyStream(in0, out1);
				Thread t2 = copyStream(in1, out0);
				t1.join();
				t2.join();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			closeQuiet(in0);
			closeQuiet(out0);
			closeQuiet(in1);
			closeQuiet(out1);
			closeQuiet(socket);
			closeQuiet(proxySocket);
			System.out.println("closing...");
		}
	}

	private ProxyedSocket handleHead() throws Exception {
		// authentication phase
		// version
		byte[] buf = new byte[2];
		readFully(in0, buf);
		if (buf[0] != 0x05) {
			throw new IOException("Version is not sock5");
		}
		// method
		int nmethods = buf[1];
		if (nmethods < 0) {
			nmethods += 256;
		}
		buf = new byte[nmethods];
		readFully(in0, buf);
		// return no authentication required
		buf = new byte[] { 0x05, 0x00 };
		out0.write(buf);
		out0.flush();

		// address port
		buf = new byte[4];
		readFully(in0, buf);
		byte cmd = buf[1];
		byte atyp = buf[3];
		String host = null;
		if (atyp == 0x01) { // ipv4
			buf = new byte[4];
			readFully(in0, buf);
			host = InetAddress.getByAddress(buf).getHostAddress();
		} else if (atyp == 0x03) { // domain name
			int len = in0.read();
			buf = new byte[len];
			readFully(in0, buf);
			host = new String(buf);
		} else if (atyp == 0x04) { // ipv6
			buf = new byte[16];
			readFully(in0, buf);
			host = InetAddress.getByAddress(buf).getHostAddress();
		}
		buf = new byte[2];
		readFully(in0, buf);
		int port = ByteBuffer.wrap(buf).asShortBuffer().get() & 0xFFFF;
		// make proxy socket
		byte rep = 0x00;
		try {
			if (cmd == 0x01) { // connect
				proxySocket = new ProxyedSocket(socketProxyServletURL, host, port);
			} else if (cmd == 0x02) { // bind
				rep = 0x05;
			} else { // 0x03-udp
				rep = 0x05;
			}
		} catch (Exception e) {
			rep = 0x05;
		}
		// reply
		ByteBuffer reply = ByteBuffer.allocate(10);
		reply.put((byte) 0x05);
		reply.put(rep);
		reply.put((byte) 0x00);
		reply.put((byte) 0x01);
		reply.put(socket.getLocalAddress().getAddress());
		short localPort = (short) ((socket.getLocalPort()) & 0xFFFF);
		reply.putShort(localPort);
		buf = reply.array();
		out0.write(buf);
		out0.flush();

		return proxySocket;
	}

	public static void readFully(InputStream in, byte[] buf) throws IOException {
		int readBytes = 0;
		while (readBytes < buf.length) {
			int r = in.read(buf, readBytes, buf.length - readBytes);
			if (r <= 0) {
				throw new IOException("EOF encountered when try to read fully");
			}
			readBytes += r;
		}
	}

	public static Thread copyStream(final InputStream in, final OutputStream out) {
		Thread t = new Thread() {
			public void run() {
				byte[] buf = new byte[4096];
				try {
					int len = in.read(buf);
					while (len > 0) {
						out.write(buf, 0, len);
						out.flush();
						len = in.read(buf);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		t.start();
		return t;
	}

	public static void closeQuiet(Closeable closable) {
		if (closable != null) {
			try {
				closable.close();
			} catch (Exception e) {
			}
		}
	}

	public static void closeQuiet(Socket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}

	public static void closeQuiet(ServerSocket socket) {
		if (socket != null) {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}
}

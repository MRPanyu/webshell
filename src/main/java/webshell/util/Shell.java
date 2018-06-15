package webshell.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public class Shell {

	private Process process;
	private Reader inputReader;
	private Reader errorReader;
	private Writer outputWriter;

	private StringBuilder bufferedInput = new StringBuilder();
	private StringBuilder bufferedError = new StringBuilder();
	private Thread bufferInputThread;
	private Thread bufferErrorThread;

	private long lastUsedTime = System.currentTimeMillis();

	public Shell(String name, String encoding) throws IOException {
		process = Runtime.getRuntime().exec(name);
		inputReader = new InputStreamReader(process.getInputStream(), encoding);
		errorReader = new InputStreamReader(process.getErrorStream(), encoding);
		outputWriter = new OutputStreamWriter(process.getOutputStream(), encoding);
		bufferInputThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						int len = bufferInput();
						if (len < 0) {
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}
			}
		};
		bufferInputThread.setName("shell-buffer-input[" + process + "]");
		bufferInputThread.setDaemon(true);
		bufferInputThread.start();

		bufferErrorThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					try {
						int len = bufferError();
						if (len < 0) {
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
						break;
					}
				}
			}
		};
		bufferErrorThread.setName("shell-buffer-error[" + process + "]");
		bufferErrorThread.setDaemon(true);
		bufferErrorThread.start();
	}

	public void execute(String cmd) throws IOException {
		this.lastUsedTime = System.currentTimeMillis();
		outputWriter.write(cmd);
		outputWriter.write("\n");
		outputWriter.flush();
	}

	public String nextInput() {
		this.lastUsedTime = System.currentTimeMillis();
		synchronized (bufferedInput) {
			String text = bufferedInput.toString();
			bufferedInput.setLength(0);
			return text;
		}
	}

	public String nextError() {
		this.lastUsedTime = System.currentTimeMillis();
		synchronized (bufferedError) {
			String text = bufferedError.toString();
			bufferedError.setLength(0);
			return text;
		}
	}

	public void close() {
		try {
			process.destroy();
		} catch (Exception e) {
		}
	}

	private int bufferInput() throws IOException {
		char[] cbuf = new char[1024];
		int len = inputReader.read(cbuf);
		if (len > 0) {
			synchronized (bufferedInput) {
				bufferedInput.append(cbuf, 0, len);
			}
		}
		return len;
	}

	private int bufferError() throws IOException {
		char[] cbuf = new char[1024];
		int len = errorReader.read(cbuf);
		if (len > 0) {
			synchronized (bufferedError) {
				bufferedError.append(cbuf, 0, len);
			}
		}
		return len;
	}

	public long getLastUsedTime() {
		return lastUsedTime;
	}

}

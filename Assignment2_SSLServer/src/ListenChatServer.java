
/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class is used to listen on the port for chat servers and create a thread for each connection
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;

public class ListenChatServer extends Thread {
	private int port;
	private String serverID, serverAddr;

	public ListenChatServer(String sID, String sAddr, int p) {
		serverID = sID;
		serverAddr = sAddr;
		port = p;
		/* Specify the keystore details (this can be specified as VM arguments as well)
		   the keystore file contains an application's own certificate and private key
		   keytool -genkey -keystore <keystorename> -keyalg RSA 
		*/
		// for jar file
		String path = ListenChatServer.class.getResource("").getPath().replaceAll("%20", " ").replaceAll("/bin", "")+"mykeystore";
		System.setProperty("javax.net.ssl.keyStore", path);
		// for eclipse run
//		System.setProperty("javax.net.ssl.keyStore", "lib/mykeystore");
		// Password to access the private key from the keystore file
		System.setProperty("javax.net.ssl.keyStorePassword", "19831010");

		// Enable debugging to view the handshake and communication which
		// happens between the SSLClient and the SSLServer
		System.setProperty("javax.net.debug", "all");
		//Location of the Java keystore file containing the collection of 
		//certificates trusted by this application (trust store).
		// for jar file
		System.setProperty("javax.net.ssl.trustStore", path);
		// for eclipse run
//		System.setProperty("javax.net.ssl.trustStore", "lib/mykeystore");
	}

	@Override
	public void run() {

		SSLServerSocket sslserversocket = null;

		try {
			// Create a SSL server socket listening on port for chat clients
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			sslserversocket = (SSLServerSocket) sslserversocketfactory
					.createServerSocket();
			sslserversocket.bind(new InetSocketAddress(serverAddr, port));

			System.out.println(
					Thread.currentThread().getName() + " - Server listening on "
							+ port + " for chat servers' connection");

			int clientNum = 0;

			// Listen for incoming connections for ever
			while (true) {

				// Accept an incoming client connection request
				SSLSocket clientSocket = (SSLSocket) sslserversocket.accept();
				System.out.println(Thread.currentThread().getName()
						+ " - chat server conection accepted");
				clientNum++;

				// Create a client connection to listen for and process all the
				// messages
				// sent by the client
				ChatServerConnection clientConnection = new ChatServerConnection(
						clientSocket, serverID, clientNum);
				clientConnection.setName("Thread" + clientNum);
				clientConnection.start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (sslserversocket != null) {
				try {
					sslserversocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

}

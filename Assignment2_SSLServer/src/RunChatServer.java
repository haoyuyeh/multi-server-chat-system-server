
/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class is used to load server's configuration and listen on two ports
 * 			one is for chat clients and the other is for chat servers 
 *          as well as checking the other chat servers's condition regularly
 */

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.net.InetSocketAddress;

import org.json.simple.JSONObject;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

public class RunChatServer {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {

		/*
		 * Specify the keystore details (this can be specified as VM arguments
		 * as well) the keystore file contains an application's own certificate
		 * and private key keytool -genkey -keystore <keystorename> -keyalg RSA
		 */
		// for jar file
		String path = RunChatServer.class.getResource("").getPath()
				.replaceAll("%20", " ").replaceAll("/bin", "") + "mykeystore";
		System.setProperty("javax.net.ssl.keyStore", path);
		// for eclipse run
		// System.setProperty("javax.net.ssl.keyStore", "lib/mykeystore");
		// Password to access the private key from the keystore file
		System.setProperty("javax.net.ssl.keyStorePassword", "19831010");

		// Enable debugging to view the handshake and communication which
		// happens between the SSLClient and the SSLServer
		System.setProperty("javax.net.debug", "all");
		// Location of the Java keystore file containing the collection of
		// certificates trusted by this application (trust store).
		// for jar file
		System.setProperty("javax.net.ssl.trustStore", path);
		// for eclipse run
		// System.setProperty("javax.net.ssl.trustStore", "lib/mykeystore");

		String serverID = "", serverAddr = "", serverConfigPath = "";
		int clientPort = 4444, serverPort = 5555;
		// get arguments from command line
		// Object that will store the parsed command line arguments
		CmdLineArgs argsBean = new CmdLineArgs();
		// Parser provided by args4j
		CmdLineParser parser = new CmdLineParser(argsBean);
		try {
			// Parse the arguments
			parser.parseArgument(args);
			// After parsing, the fields in argsBean have been updated with the
			// given
			// command line arguments
			serverID = argsBean.getHost();
			serverConfigPath = argsBean.getServerConfigPath();
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			// Print the usage to help the user understand the arguments
			// expected
			// by the program
			parser.printUsage(System.err);
		}
		// load server's configuration
		try {
			Scanner read;
			read = new Scanner(new FileInputStream(serverConfigPath));
			String str = "";
			// read all the data in the file
			while (read.hasNextLine()) {
				str = read.nextLine();
				String[] content;
				content = str.split("\t");
				if (content.length == 4) {
					if (str.startsWith(serverID)) {
						// adding default chat room into list
						String rID = "MainHall-" + serverID;
						ChatServerState.getInstance()
								.addChatRoom(new ChatRoom(rID, serverID, ""));
						serverAddr = content[1];
						clientPort = Integer.parseInt(content[2]);
						serverPort = Integer.parseInt(content[3]);
					} else {
						String rID = "MainHall-" + content[0];
						ChatServerState.getInstance()
								.addChatRoom(new ChatRoom(rID, content[0], ""));
						ChatServerState.getInstance().addChatServer(
								new ChatServer(content[0], content[1],
										Integer.parseInt(content[2]),
										Integer.parseInt(content[3])));
					}
				} else if (content.length == 3) {
					// set config of authentic server
					if (str.startsWith("s0")) {
						ChatServerState.getInstance()
								.setAuthenticServer(new ChatServer(content[1],
										Integer.parseInt(content[2])));
					}
				}
			}
			read.close();
			// terminated the system because of not existed server ID
			if(serverAddr.equals("")){
				System.out.println("Input not existed server ID.");
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		// this part for scalability by
		// lp!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		boolean addserver = false;
		// addserver is from command line arguments
		addserver = argsBean.isNewServer();
		if (addserver) {
			// send message to all chat servers and authentic server
			try {
				// new sockets for all server expected this one connected to
				// server ports
				ArrayList<BufferedWriter> writer = new ArrayList<BufferedWriter>();
				ArrayList<SSLSocket> toServers = new ArrayList<SSLSocket>();
				ArrayList<ChatServer> serverList = new ArrayList<ChatServer>();
				serverList = (ArrayList<ChatServer>) ChatServerState
						.getInstance().getServerList().clone();

				// Create SSL socket and connect it to the remote server
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
						.getDefault();
				for (ChatServer cs : serverList) {
					toServers.add((SSLSocket) sslsocketfactory.createSocket(
							cs.getServerAddr(), cs.getServerPort()));
				}
				// for authentic server
				toServers.add((SSLSocket) sslsocketfactory.createSocket(
						ChatServerState.getInstance().getAuthenticServer()
								.getServerAddr(),
						ChatServerState.getInstance().getAuthenticServer()
								.getClientPort()));

				// Get the output streams for writing data to the socket
				for (SSLSocket s : toServers) {
					writer.add(new BufferedWriter(new OutputStreamWriter(
							s.getOutputStream(), "UTF-8")));
				}
				// create json object
				JSONObject serMessage = new JSONObject();
				serMessage.put("type", "addserver");
				serMessage.put("serverid", serverID);
				serMessage.put("serveraddress", serverAddr);
				serMessage.put("clientport", Integer.toString(clientPort));
				serMessage.put("serverPort", Integer.toString(serverPort));

				for (int i = 0; i < writer.size(); i++) {
					writer.get(i).write(serMessage.toJSONString() + "\n");
					writer.get(i).flush();
				}
				// close socket
				for (SSLSocket s : toServers) {
					s.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1);
			}
		}
		// for scalability end here by
		// lp!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

		SSLServerSocket sslserversocket = null;
		try {
			// create a thread to check the condition of each other chat server
			// regularly
			// first arg is the time for set up all servers
			// second arg is the interval for check server's existence
			// the unit of two args are milliseconds
			CheckChatServerCondition ccsc = new CheckChatServerCondition(60000,
					10000);
			ccsc.setName("CheckChatServersAlive");
			ccsc.start();
			// create a thread to listen on port for chat servers
			ListenChatServer lcs = new ListenChatServer(serverID, serverAddr,
					serverPort);
			lcs.setName("ListenForChatServer");
			lcs.start();

			// Create a SSL server socket listening on port for chat clients
			SSLServerSocketFactory sslserversocketfactory = (SSLServerSocketFactory) SSLServerSocketFactory
					.getDefault();
			sslserversocket = (SSLServerSocket) sslserversocketfactory
					.createServerSocket();
			sslserversocket.bind(new InetSocketAddress(serverAddr, clientPort));
			System.out.println(
					Thread.currentThread().getName() + " - Server listening on "
							+ clientPort + " for chat clients' connection");

			int clientNum = 0;

			// Listen for incoming connections for ever
			while (true) {

				// Accept an incoming client connection request
				SSLSocket clientSocket = (SSLSocket) sslserversocket.accept();
				System.out.println(Thread.currentThread().getName()
						+ " - Client conection accepted");
				clientNum++;

				// Create a client connection to listen for and process all the
				// messages
				// sent by the client
				ChatClientConnection clientConnection = new ChatClientConnection(
						clientSocket, serverID, clientNum);
				clientConnection.setName("Thread" + clientNum);
				clientConnection.start();

				// Update the server state to reflect the new connected client
				ChatServerState.getInstance().clientConnected(clientConnection);
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

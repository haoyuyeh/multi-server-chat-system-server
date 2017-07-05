
/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class is used to check the other chat server's alive condition
 *          as well as deal with the situation when some server is failed
 */

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.util.ArrayList;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.json.simple.JSONObject;

public class CheckChatServerCondition extends Thread {
	private long serverPrepareTime, timeInterval;
	private BufferedWriter writer;

	/**
	 * 
	 * @param time:
	 *            unit is millisecond
	 * @param interval:
	 *            unit is millisecond
	 */
	public CheckChatServerCondition(long time, long interval) {

		/*
		 * Specify the keystore details (this can be specified as VM arguments
		 * as well) the keystore file contains an application's own certificate
		 * and private key keytool -genkey -keystore <keystorename> -keyalg RSA
		 */
		// for jar file
		String path = CheckChatServerCondition.class.getResource("").getPath()
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
		serverPrepareTime = time;
		timeInterval = interval;
	}

	// Needs to be synchronized because multiple threads can be invoking this
	// method at the same
	// time
	public synchronized void write(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
			// System.out.println(Thread.currentThread().getName() + " - Message
			// sent to client " + clientNum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run() {
		// time for all the chat servers to be established
		try {
			Thread.sleep(serverPrepareTime);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		while (true) {
			// check whether other chat servers are working correctly every
			// timeInterval time
			ArrayList<ChatServer> serverList;
			serverList = (ArrayList<ChatServer>) ChatServerState.getInstance()
					.getServerList().clone();
			for (int i = 0; i < serverList.size(); i++) {
				try {
					SSLSocket sslsocket = null;
					// Create SSL socket and connect it to the remote server
					SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
							.getDefault();
					sslsocket = (SSLSocket) sslsocketfactory.createSocket(
							serverList.get(i).getServerAddr(),
							serverList.get(i).getServerPort());
					writer = new BufferedWriter(new OutputStreamWriter(
							sslsocket.getOutputStream(), "UTF-8"));
					JSONObject junk = new JSONObject();
					junk.put("type", "junk");
					this.write(junk.toJSONString());
					sslsocket.close();
				} catch (SocketException e) {
					maintainStates(serverList.get(i));
					e.getStackTrace();
				} catch (IOException e) {
					e.getStackTrace();
				}
			}
			try {
				Thread.sleep(timeInterval);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}

	/**
	 * remove all the data associated with the failed chat server
	 */
	@SuppressWarnings("unchecked")
	private void maintainStates(ChatServer cs) {
		// delete all the chat rooms associated with cs
		ArrayList<ChatRoom> chatRoomList;
		String serverID = cs.getServerID();
		chatRoomList = (ArrayList<ChatRoom>) ChatServerState.getInstance()
				.getChatRoomList().clone();
		for (ChatRoom c : chatRoomList) {
			if (c.getServerID().equals(serverID)) {
				ChatServerState.getInstance().deleteChatRoom(c);
			}
		}
		// delete cs from chat server list
		ChatServerState.getInstance().deleteChatServer(cs);
	}
}

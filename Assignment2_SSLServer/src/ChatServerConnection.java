
/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class is used to process the communications between servers
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import javax.net.ssl.SSLSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ChatServerConnection extends Thread {

	private SSLSocket clientSocket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private int clientNum;
	private String serverID, fromServer;

	public ChatServerConnection(SSLSocket clientSocket, String sID,
			int clientNum) {
		try {
			this.clientSocket = clientSocket;
			reader = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(
					clientSocket.getOutputStream(), "UTF-8"));
			this.clientNum = clientNum;
			serverID = sID;
			/* Specify the keystore details (this can be specified as VM arguments as well)
			   the keystore file contains an application's own certificate and private key
			   keytool -genkey -keystore <keystorename> -keyalg RSA 
			*/
			// for jar file
			String path = ChatServerConnection.class.getResource("").getPath().replaceAll("%20", " ").replaceAll("/bin", "")+"mykeystore";
			System.setProperty("javax.net.ssl.keyStore", path);
			// for eclipse run
//			System.setProperty("javax.net.ssl.keyStore", "lib/mykeystore");
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
//			System.setProperty("javax.net.ssl.trustStore", "lib/mykeystore");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {

		try {

			// System.out.println(Thread.currentThread().getName()
			// + " - Reading messages from client's " + clientNum + "
			// connection");

			String clientMsg = null;
			while ((clientMsg = reader.readLine()) != null) {
				// System.out.println(Thread.currentThread().getName()
				// + " - Message from client " + clientNum + " received: " +
				// clientMsg);

				// process msg from chat server
				processMessage(clientMsg);

			}

			clientSocket.close();
			// System.out.println(Thread.currentThread().getName()
			// + " - Client " + clientNum + " disconnected");

		} catch (Exception e) {
			e.printStackTrace();
		}
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
	public void processMessage(String msg) {
		// msg is in JSON format
		JSONParser parser = new JSONParser();
		try {
			JSONObject jObj = (JSONObject) parser.parse(msg);
			switch ((String) jObj.get("type")) {
			case ChatCommands.ADDSERVER:
				String sID = (String) jObj.get("serverid");
				String sAddr = (String) jObj.get("serveraddress");
				int cPort = Integer.parseInt((String)jObj.get("clientport"));
				int sPort = Integer.parseInt((String) jObj.get("serverPort"));
				ChatServerState.getInstance().addChatServer(new ChatServer(sID,sAddr,cPort,sPort));
				// add MainHall of new server into chat room list
				String roomID = "MainHall-" + sID;
				ChatServerState.getInstance().addChatRoom(new ChatRoom(roomID,sID,""));
				break;
			case ChatCommands.LOCKIDENTITY:
				String id = (String) jObj.get("identity");
				fromServer = (String) jObj.get("serverid");
				if (ChatServerState.getInstance().checkID(id)) {
					JSONObject liMessage = new JSONObject();
					liMessage.put("type", "lockidentity");
					liMessage.put("serverid", serverID);
					liMessage.put("identity", id);
					liMessage.put("locked", "true");
					this.write(liMessage.toJSONString());
				} else {
					JSONObject liMessage = new JSONObject();
					liMessage.put("type", "lockidentity");
					liMessage.put("serverid", serverID);
					liMessage.put("identity", id);
					liMessage.put("locked", "false");
					this.write(liMessage.toJSONString());
				}
				ChatServerState.getInstance().addLockID(id);
				break;
			case ChatCommands.RELEASEIDENTITY:
				if (jObj.get("serverid").equals(fromServer)) {
					ChatServerState.getInstance()
							.deleteLockID((String) jObj.get("identity"));
				}
				break;
			case ChatCommands.LOCKROOMID:
				String rid = (String) jObj.get("roomid");
				fromServer = (String) jObj.get("serverid");
				if (ChatServerState.getInstance().checkRoomID(rid)) {
					JSONObject lriMessage = new JSONObject();
					lriMessage.put("type", "lockroomid");
					lriMessage.put("serverid", serverID);
					lriMessage.put("roomid", rid);
					lriMessage.put("locked", "true");
					this.write(lriMessage.toJSONString());
				} else {
					JSONObject lriMessage = new JSONObject();
					lriMessage.put("type", "lockroomid");
					lriMessage.put("serverid", serverID);
					lriMessage.put("roomid", rid);
					lriMessage.put("locked", "false");
					this.write(lriMessage.toJSONString());
				}
				ChatServerState.getInstance().addLockRoomID(rid);
				;
				break;
			case ChatCommands.RELEASEROOMID:
				if (jObj.get("serverid").equals(fromServer)) {
					if (jObj.get("approved").equals("true")) {
						String rId = (String) jObj.get("roomid");
						ChatServerState.getInstance().deleteLockRoomID(rId);
						ChatServerState.getInstance()
								.addChatRoom(new ChatRoom(rId, fromServer, ""));
					} else {
						ChatServerState.getInstance()
								.deleteLockRoomID((String) jObj.get("roomid"));
					}
				}
				break;
			case ChatCommands.DELETEROOM:
				ChatRoom target = ChatServerState.getInstance()
						.getChatRoom((String) jObj.get("roomid"));
				if (target.getServerID()
						.equals((String) jObj.get("serverid"))) {
					ChatServerState.getInstance().deleteChatRoom(target);
				} else {
					System.out.println("delete room: serverID not match");
				}
				break;
			default:
				System.out.println("unknown contents");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

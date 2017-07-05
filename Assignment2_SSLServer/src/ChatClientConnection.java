
/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class is used to process the communications between server and client
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import java.util.ArrayList;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class ChatClientConnection extends Thread {

	private SSLSocket clientSocket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private int clientNum;
	// use to determine whether to stop this thread
	private volatile Thread terminated; 
	private String clientName, roomID, serverID;

	public ChatClientConnection(SSLSocket clientSocket, String sID,
			int clientNum) {
		try {
			this.clientSocket = clientSocket;
			reader = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream(), "UTF-8"));
			writer = new BufferedWriter(new OutputStreamWriter(
					clientSocket.getOutputStream(), "UTF-8"));
			this.clientNum = clientNum;
			serverID = sID;
			clientName = "";
			/*
			 * Specify the keystore details (this can be specified as VM
			 * arguments as well) the keystore file contains an application's
			 * own certificate and private key keytool -genkey -keystore
			 * <keystorename> -keyalg RSA
			 */
			// for jar file
			String path = ChatClientConnection.class.getResource("").getPath()
					.replaceAll("%20", " ").replaceAll("/bin", "")
					+ "mykeystore";
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
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		terminated = Thread.currentThread();
		try {

			// System.out.println(Thread.currentThread().getName()
			// + " - Reading messages from client's " + clientNum
			// + " connection");
			// process msg from client
			String clientMsg = null;
			while ((terminated == Thread.currentThread())
					&& ((clientMsg = reader.readLine()) != null)) {
				// System.out.println(Thread.currentThread().getName()
				// + " - Message from client " + clientNum + " received: "
				// + clientMsg);
				processMessage(clientMsg);

			}
			clientSocket.close();
			// remove client from client list after client normal
			// disconnected
			ChatServerState.getInstance().clientDisconnected(this);
			// System.out.println(Thread.currentThread().getName() + " -
			// Client
			// "
			// + clientNum + " disconnected");
		} catch (SocketException e) {
			e.printStackTrace();
			quit(true);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	// Needs to be synchronized because multiple threads can be invoking this
	// method at the same time
	public synchronized void write(String msg) {
		try {
			writer.write(msg + "\n");
			writer.flush();
			// System.out.println(Thread.currentThread().getName()
			// + " - Message sent to client " + clientNum);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public String getClientName() {
		return clientName;
	}

	public void setRoomID(String rID) {
		roomID = rID;
	}

	public String getRoomID() {
		return roomID;
	}

	@SuppressWarnings("unchecked")
	public void processMessage(String msg) {
		// msg is in JSON format
		JSONParser parser = new JSONParser();
		try {
			JSONObject jObj = (JSONObject) parser.parse(msg);
			switch ((String) jObj.get("type")) {
			case ChatCommands.CONNECT:
				// Broadcast the client message to all other clients connected
				// to the server.
				JSONObject cMessage = new JSONObject();
				cMessage.put("type", "connect");
				cMessage.put("approved", "true");
				this.write(cMessage.toJSONString());
				break;
			case ChatCommands.NEWIDENTITY:
				checkID((String) jObj.get("identity"));
				break;
			case ChatCommands.LIST:
				getRoomList();
				break;
			case ChatCommands.WHO:
				getRoomMemberList();
				break;
			case ChatCommands.CREATEROOM:
				checkRoomID((String) jObj.get("roomid"));
				break;
			case ChatCommands.JOINROOM:
				joinRoom((String) jObj.get("roomid"));
				break;
			case ChatCommands.MOVEJOIN:
				clientName = (String) jObj.get("identity");
				moveJoin((String) jObj.get("roomid"),
						(String) jObj.get("former"));
				break;
			case ChatCommands.DELETEROOM:
				deleteRoom((String) jObj.get("roomid"));
				break;
			case ChatCommands.MESSAGE:
				// Broadcast the client message to all other clients connected
				// to the server.
				JSONObject mMessage = new JSONObject();
				mMessage.put("type", "message");
				mMessage.put("identity", clientName);
				mMessage.put("content", jObj.get("content"));
				ChatServerState.getInstance().sendMsgWithinChatRoom(roomID,
						mMessage.toJSONString());
				break;
			case ChatCommands.QUIT:
				quit(false);
				break;
			// throw an invalid command exception
			default:
				System.out.println("unknown contents");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * check whether the id can be created
	 * 
	 * @param id
	 */
	@SuppressWarnings("unchecked")
	private void checkID(String id) {
		if (checkLocalID(id)) {
			// send lock identity to all the other servers
			if (checkGlobalID(id)) {
				clientName = id;
				String rID = "MainHall-" + serverID;
				roomID = rID;
				ChatServerState.getInstance().addChatRoomMember(roomID, this);
				// send allow message to client
				JSONObject idMessage = new JSONObject();
				idMessage.put("type", "newidentity");
				idMessage.put("approved", "true");
				this.write(idMessage.toJSONString());
				// send roomchange msg to all room members
				JSONObject rcMessage = new JSONObject();
				rcMessage.put("type", "roomchange");
				rcMessage.put("identity", clientName);
				rcMessage.put("former", "");
				rcMessage.put("roomid", this.roomID);
				ChatServerState.getInstance().sendMsgWithinChatRoom(roomID,
						rcMessage.toJSONString());
			} else {
				// send deny message to client
				JSONObject idMessage = new JSONObject();
				idMessage.put("type", "newidentity");
				idMessage.put("approved", "false");
				this.write(idMessage.toJSONString());
			}
		} else {
			// send deny message to client
			JSONObject idMessage = new JSONObject();
			idMessage.put("type", "newidentity");
			idMessage.put("approved", "false");
			this.write(idMessage.toJSONString());
		}
	}

	/**
	 * check whether the id can be created
	 * 
	 * @param id
	 * @return true means this id not found in local server
	 */
	private boolean checkLocalID(String id) {
		// check the id is valid regards to length and syntax
		String regEX = "^[a-zA-Z][a-zA-Z0-9]{2,15}";
		if (id.matches(regEX)) {
			// check the id has been used or locked
			if (ChatServerState.getInstance().checkID(id)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * check whether the id can be created
	 * 
	 * @param id
	 * @return true means this id not found in other servers
	 */
	@SuppressWarnings("unchecked")
	private boolean checkGlobalID(String id) {
		boolean result = true;
		ArrayList<BufferedReader> reader = new ArrayList<BufferedReader>();
		ArrayList<BufferedWriter> writer = new ArrayList<BufferedWriter>();
		ArrayList<SSLSocket> toServers = new ArrayList<SSLSocket>();
		ArrayList<ChatServer> serverList = new ArrayList<ChatServer>();
		serverList = ChatServerState.getInstance().getServerList();
		JSONObject liMessage = new JSONObject();
		JSONObject riMessage = new JSONObject();

		liMessage.put("type", "lockidentity");
		liMessage.put("serverid", this.serverID);
		liMessage.put("identity", id);
		riMessage.put("type", "releaseidentity");
		riMessage.put("serverid", this.serverID);
		riMessage.put("identity", id);
		try {
			// Create SSL socket and connect it to the remote server
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
					.getDefault();
			for (ChatServer cs : serverList) {
				toServers.add((SSLSocket) sslsocketfactory
						.createSocket(cs.getServerAddr(), cs.getServerPort()));
			}
			// Get the input/output streams for reading/writing data from/to the
			// socket
			for (SSLSocket s : toServers) {
				reader.add(new BufferedReader(
						new InputStreamReader(s.getInputStream(), "UTF-8")));
				writer.add(new BufferedWriter(
						new OutputStreamWriter(s.getOutputStream(), "UTF-8")));
			}

			// send lock identity msg to all the other servers
			for (int i = 0; i < writer.size(); i++) {
				writer.get(i).write(liMessage.toJSONString() + "\n");
				writer.get(i).flush();
			}
			// receive responds from other servers
			JSONParser parser = new JSONParser();
			for (int i = 0; i < reader.size(); i++) {
				String msg = reader.get(i).readLine();
				JSONObject jObj = (JSONObject) parser.parse(msg);
				if (jObj.get("type").equals("lockidentity")) {
					result &= jObj.get("locked").equals("true");
				} else {
					System.out.println("unknown msg from server: " + msg);
					throw new Exception();
				}
			}
			// send release identity msg to other servers
			for (BufferedWriter bw : writer) {
				bw.write(riMessage.toJSONString() + "\n");
				bw.flush();
			}
			// close socket
			for (SSLSocket s : toServers) {
				s.close();
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private void getRoomList() {
		ArrayList<ChatRoom> chatRoomList = ChatServerState.getInstance()
				.getChatRoomList();
		JSONObject listMessage = new JSONObject();
		listMessage.put("type", "roomlist");
		JSONArray list = new JSONArray();
		for (ChatRoom room : chatRoomList) {
			list.add(room.getRoomID());
		}
		listMessage.put("rooms", list);
		this.write(listMessage.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private void getRoomMemberList() {
		ArrayList<ChatClientConnection> chatRoomMember = ChatServerState
				.getInstance().getChatRoom(roomID).getChatRoomMember();
		JSONObject whoMessage = new JSONObject();
		whoMessage.put("type", "roomcontents");
		whoMessage.put("roomid", roomID);
		JSONArray who = new JSONArray();
		for (ChatClientConnection client : chatRoomMember) {
			who.add(client.getClientName());
		}
		whoMessage.put("identities", who);
		whoMessage.put("owner", ChatServerState.getInstance()
				.getChatRoom(roomID).getRoomOwner());
		this.write(whoMessage.toJSONString());
	}

	/**
	 * check whether the chat room can be created
	 * 
	 * @param rID
	 */
	@SuppressWarnings("unchecked")
	private void checkRoomID(String rID) {
		if (checkLocalRoomID(rID)) {
			// send lock room id to all the other servers
			if (checkGlobalRoomID(rID)) {
				// send allow message to client
				JSONObject ridMessage = new JSONObject();
				ridMessage.put("type", "createroom");
				ridMessage.put("roomid", rID);
				ridMessage.put("approved", "true");
				this.write(ridMessage.toJSONString());
				// send roomchange msg to all room members
				JSONObject rcMessage = new JSONObject();
				rcMessage.put("type", "roomchange");
				rcMessage.put("identity", clientName);
				rcMessage.put("former", roomID);
				rcMessage.put("roomid", rID);
				ChatServerState.getInstance().sendMsgWithinChatRoom(roomID,
						rcMessage.toJSONString());
				// delete client from former chat room
				ChatServerState.getInstance().deleteChatRoomMember(roomID,
						this);
				// add client into new chat room
				roomID = rID;
				ChatServerState.getInstance().addChatRoom(
						new ChatRoom(roomID, serverID, clientName));
				ChatServerState.getInstance().addChatRoomMember(roomID, this);
			} else {
				// send deny message to client
				JSONObject ridMessage = new JSONObject();
				ridMessage.put("type", "createroom");
				ridMessage.put("roomid", rID);
				ridMessage.put("approved", "false");
				this.write(ridMessage.toJSONString());
			}
		} else {
			// send deny message to client
			JSONObject ridMessage = new JSONObject();
			ridMessage.put("type", "createroom");
			ridMessage.put("roomid", rID);
			ridMessage.put("approved", "false");
			this.write(ridMessage.toJSONString());
		}
	}

	/**
	 * check whether the chat room can be created
	 * 
	 * @param rID
	 * @return true means this room id not found in local server
	 */
	private boolean checkLocalRoomID(String rID) {
		// check the id is valid regards to length and syntax
		String regEX = "^[a-zA-Z][a-zA-Z0-9]{2,15}";
		if (rID.matches(regEX)) {
			if (ChatServerState.getInstance().checkRoomOwner(this.clientName)) {
				// check the id has been used or locked
				if (ChatServerState.getInstance().checkRoomID(rID)) {
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * check whether the chat room can be created
	 * 
	 * @param rID
	 * @return true means this room id not found in other servers
	 */
	@SuppressWarnings("unchecked")
	private boolean checkGlobalRoomID(String rID) {
		boolean result = true;
		ArrayList<BufferedReader> reader = new ArrayList<BufferedReader>();
		ArrayList<BufferedWriter> writer = new ArrayList<BufferedWriter>();
		ArrayList<SSLSocket> toServers = new ArrayList<SSLSocket>();
		ArrayList<ChatServer> serverList = new ArrayList<ChatServer>();
		serverList = ChatServerState.getInstance().getServerList();
		JSONObject lriMessage = new JSONObject();
		JSONObject rriMessage = new JSONObject();

		lriMessage.put("type", "lockroomid");
		lriMessage.put("serverid", this.serverID);
		lriMessage.put("roomid", rID);
		rriMessage.put("type", "releaseroomid");
		rriMessage.put("serverid", this.serverID);
		rriMessage.put("roomid", rID);
		try {
			// Create SSL socket and connect it to the remote server
			SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
					.getDefault();
			for (ChatServer cs : serverList) {
				toServers.add((SSLSocket) sslsocketfactory
						.createSocket(cs.getServerAddr(), cs.getServerPort()));
			}
			// Get the input/output streams for reading/writing data from/to the
			// socket
			for (SSLSocket s : toServers) {
				reader.add(new BufferedReader(
						new InputStreamReader(s.getInputStream(), "UTF-8")));
				writer.add(new BufferedWriter(
						new OutputStreamWriter(s.getOutputStream(), "UTF-8")));
			}

			// send lock room id msg to all the other servers
			for (int i = 0; i < writer.size(); i++) {
				writer.get(i).write(lriMessage.toJSONString() + "\n");
				writer.get(i).flush();
			}
			// receive responds from other servers
			JSONParser parser = new JSONParser();
			for (int i = 0; i < reader.size(); i++) {
				String msg = reader.get(i).readLine();
				JSONObject jObj = (JSONObject) parser.parse(msg);
				if (jObj.get("type").equals("lockroomid")) {
					result &= jObj.get("locked").equals("true");
				} else {
					System.out.println("unknown msg from server: " + msg);
					throw new Exception();
				}
			}
			// send release room id msg to other servers
			if (result) {
				rriMessage.put("approved", "true");
				for (BufferedWriter bw : writer) {
					bw.write(rriMessage.toJSONString() + "\n");
					bw.flush();
				}
			} else {
				rriMessage.put("approved", "false");
				for (BufferedWriter bw : writer) {
					bw.write(rriMessage.toJSONString() + "\n");
					bw.flush();
				}
			}
			// close socket
			for (SSLSocket s : toServers) {
				s.close();
			}
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	@SuppressWarnings("unchecked")
	private void joinRoom(String rID) {
		if (ChatServerState.getInstance().checkRoomExist(rID)) {
			if (ChatServerState.getInstance().checkRoomOwner(clientName)) {
				// successfully join new room
				ChatRoom destination = ChatServerState.getInstance()
						.getChatRoom(rID);
				if (destination.getServerID().equals(serverID)) {
					// chat room and client are in the same server
					String formerRID = roomID;
					roomID = rID;
					ChatServerState.getInstance()
							.deleteChatRoomMember(formerRID, this);
					ChatServerState.getInstance().addChatRoomMember(roomID,
							this);
					// send roomchange msg to all room members
					JSONObject rcMessage = new JSONObject();
					rcMessage.put("type", "roomchange");
					rcMessage.put("identity", clientName);
					rcMessage.put("former", formerRID);
					rcMessage.put("roomid", roomID);
					ChatServerState.getInstance().sendMsgWithinChatRoom(
							formerRID, rcMessage.toJSONString());
					ChatServerState.getInstance().sendMsgWithinChatRoom(roomID,
							rcMessage.toJSONString());
				} else {
					// chat room is in another server
					// delete client from chat room members
					ChatServerState.getInstance().deleteChatRoomMember(roomID,
							this);
					// send roomchange msg to all former room members
					JSONObject rcMessage = new JSONObject();
					rcMessage.put("type", "roomchange");
					rcMessage.put("identity", clientName);
					rcMessage.put("former", roomID);
					rcMessage.put("roomid", destination.getRoomID());
					ChatServerState.getInstance().sendMsgWithinChatRoom(roomID,
							rcMessage.toJSONString());
					// send route msg to client
					ChatServer target = ChatServerState.getInstance()
							.getChatServer(destination.getServerID());
					JSONObject rMessage = new JSONObject();
					rMessage.put("type", "route");
					rMessage.put("roomid", rID);
					rMessage.put("host", target.getServerAddr());
					rMessage.put("port",
							Integer.toString(target.getClientPort()));
					this.write(rMessage.toJSONString());
				}
			} else {
				// client is a room owner
				// send roomchange msg to client
				JSONObject rcMessage = new JSONObject();
				rcMessage.put("type", "roomchange");
				rcMessage.put("identity", clientName);
				rcMessage.put("former", rID);
				rcMessage.put("roomid", rID);
				this.write(rcMessage.toJSONString());
			}
		} else {
			// chat room is not existing
			// send roomchange msg to client
			JSONObject rcMessage = new JSONObject();
			rcMessage.put("type", "roomchange");
			rcMessage.put("identity", clientName);
			rcMessage.put("former", rID);
			rcMessage.put("roomid", rID);
			this.write(rcMessage.toJSONString());
		}
	}

	@SuppressWarnings("unchecked")
	private void moveJoin(String rID, String formerServer) {
		if (ChatServerState.getInstance().checkRoomExist(rID)) {
			roomID = rID;
		} else {
			roomID = "MainHall-" + serverID;
		}
		ChatServerState.getInstance().addChatRoomMember(roomID, this);
		// send serverchange msg to client
		JSONObject scMessage = new JSONObject();
		scMessage.put("type", "serverchange");
		scMessage.put("approved", "true");
		scMessage.put("serverid", serverID);
		this.write(scMessage.toJSONString());
		// send roomchange msg to all room members
		JSONObject rcMessage = new JSONObject();
		rcMessage.put("type", "roomchange");
		rcMessage.put("identity", clientName);
		rcMessage.put("former", formerServer);
		rcMessage.put("roomid", roomID);
		ChatServerState.getInstance().sendMsgWithinChatRoom(roomID,
				rcMessage.toJSONString());
	}

	@SuppressWarnings("unchecked")
	private void deleteRoom(String rID) {
		ChatRoom target = ChatServerState.getInstance().getChatRoom(rID);
		if (target.getRoomOwner().equals(clientName)) {
			// send delete room msg to other servers
			try {
				ArrayList<BufferedWriter> writer = new ArrayList<BufferedWriter>();
				ArrayList<SSLSocket> toServers = new ArrayList<SSLSocket>();
				ArrayList<ChatServer> serverList = new ArrayList<ChatServer>();
				serverList = ChatServerState.getInstance().getServerList();

				// Create SSL socket and connect it to the remote server
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
						.getDefault();
				for (ChatServer cs : serverList) {
					toServers.add((SSLSocket) sslsocketfactory.createSocket(
							cs.getServerAddr(), cs.getServerPort()));
				}
				// Get the input/output streams for reading/writing data from/to
				// the
				// socket
				for (SSLSocket s : toServers) {
					writer.add(new BufferedWriter(new OutputStreamWriter(
							s.getOutputStream(), "UTF-8")));
				}
				JSONObject drMessage = new JSONObject();
				drMessage.put("type", "deleteroom");
				drMessage.put("serverid", serverID);
				drMessage.put("roomid", rID);
				for (int i = 0; i < writer.size(); i++) {
					writer.get(i).write(drMessage.toJSONString() + "\n");
					writer.get(i).flush();
				}
				// close socket
				for (SSLSocket s : toServers) {
					s.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// delete room from room list and move room members to main hall
			// send approved to delete room msg to client
			JSONObject drMessage = new JSONObject();
			drMessage.put("type", "deleteroom");
			drMessage.put("approved", "true");
			drMessage.put("roomid", rID);
			this.write(drMessage.toJSONString());
			// move room members to main hall
			ArrayList<ChatClientConnection> members = target
					.getChatRoomMember();
			String des = "MainHall-" + serverID;
			for (ChatClientConnection c : members) {
				c.setRoomID(des);
				ChatServerState.getInstance().addChatRoomMember(des, c);
			}
			// send roomchange msg to all room members
			for (ChatClientConnection c : members) {
				JSONObject rcMessage = new JSONObject();
				rcMessage.put("type", "roomchange");
				rcMessage.put("identity", c.getClientName());
				rcMessage.put("former", rID);
				rcMessage.put("roomid", des);
				ChatServerState.getInstance().sendMsgWithinChatRoom(roomID,
						rcMessage.toJSONString());
			}
			// delete room from room list
			ChatServerState.getInstance().deleteChatRoom(target);
		} else {
			// send not approved to delete room msg to client
			JSONObject drMessage = new JSONObject();
			drMessage.put("type", "deleteroom");
			drMessage.put("approved", "false");
			drMessage.put("roomid", rID);
			this.write(drMessage.toJSONString());
		}
	}

	@SuppressWarnings("unchecked")
	private void quit(boolean abrupt) {
		// delete client from chat room members
		ChatServerState.getInstance().deleteChatRoomMember(roomID, this);
		// check client whether is an owner of a chat room
		ChatRoom target = ChatServerState.getInstance().getChatRoom(roomID);
		if (target.getRoomOwner().equals(clientName)) {
			// send delete room msg to other servers
			try {
				ArrayList<BufferedWriter> writer = new ArrayList<BufferedWriter>();
				ArrayList<SSLSocket> toServers = new ArrayList<SSLSocket>();
				ArrayList<ChatServer> serverList = new ArrayList<ChatServer>();
				serverList = ChatServerState.getInstance().getServerList();

				// Create SSL socket and connect it to the remote server
				SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory
						.getDefault();
				for (ChatServer cs : serverList) {
					toServers.add((SSLSocket) sslsocketfactory.createSocket(
							cs.getServerAddr(), cs.getServerPort()));
				}
				// Get the output streams for writing data to the socket
				for (SSLSocket s : toServers) {
					writer.add(new BufferedWriter(new OutputStreamWriter(
							s.getOutputStream(), "UTF-8")));
				}
				JSONObject drMessage = new JSONObject();
				drMessage.put("type", "deleteroom");
				drMessage.put("serverid", serverID);
				drMessage.put("roomid", roomID);
				for (int i = 0; i < writer.size(); i++) {
					writer.get(i).write(drMessage.toJSONString() + "\n");
					writer.get(i).flush();
				}
				// close socket
				for (SSLSocket s : toServers) {
					s.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			// delete room from room list and move room members to main hall
			// move room members to main hall
			ArrayList<ChatClientConnection> members = target
					.getChatRoomMember();
			String des = "MainHall-" + serverID;
			for (ChatClientConnection c : members) {
				c.setRoomID(des);
				ChatServerState.getInstance().addChatRoomMember(des, c);
			}
			// send roomchange msg to all room members
			for (ChatClientConnection c : members) {
				JSONObject rcMessage = new JSONObject();
				rcMessage.put("type", "roomchange");
				rcMessage.put("identity", c.getClientName());
				rcMessage.put("former", target.getRoomID());
				rcMessage.put("roomid", des);
				ChatServerState.getInstance().sendMsgWithinChatRoom(
						c.getRoomID(), rcMessage.toJSONString());
			}
			// delete room from room list
			ChatServerState.getInstance().deleteChatRoom(target);
		}
		// send a client quit msg to all clients
		JSONObject cqMessage = new JSONObject();
		cqMessage.put("type", "roomchange");
		cqMessage.put("identity", clientName);
		cqMessage.put("former", roomID);
		cqMessage.put("roomid", "");
		ChatServerState.getInstance()
				.sendMsgToAllClients(cqMessage.toJSONString());
		if (!abrupt) {
			// send roomchange msg to client
			JSONObject rcMessage = new JSONObject();
			rcMessage.put("type", "roomchange");
			rcMessage.put("identity", clientName);
			rcMessage.put("former", roomID);
			rcMessage.put("roomid", "");
			this.write(rcMessage.toJSONString());
		} else {
			// because the client is abrupt, it needs to remove from client list
			ChatServerState.getInstance().clientDisconnected(this);
		}
		// terminate this thread
		terminated = null;
	}
}

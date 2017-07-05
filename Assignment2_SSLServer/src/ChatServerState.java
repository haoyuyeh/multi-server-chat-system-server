
/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class store the client and server lists 
 * 			as well as all the functions that a server needs 
 * 			when server processes the messages from clients
 */

import java.util.ArrayList;

//Singleton object that manages the server state
public class ChatServerState {

	private static ChatServerState instance;
	private ChatServer authenticServer;
	private ArrayList<ChatServer> serverList;
	private ArrayList<ChatClientConnection> connectedClients;
	private ArrayList<String> lockIdentity, lockRoomID;
	private ArrayList<ChatRoom> chatRoomList;

	private ChatServerState() {
		authenticServer = null;
		serverList = new ArrayList<ChatServer>();
		connectedClients = new ArrayList<ChatClientConnection>();
		lockIdentity = new ArrayList<String>();
		lockRoomID = new ArrayList<String>();
		chatRoomList = new ArrayList<ChatRoom>();
	}

	public static synchronized ChatServerState getInstance() {
		if (instance == null) {
			instance = new ChatServerState();
		}
		return instance;
	}

	public void setAuthenticServer(ChatServer s) {
		authenticServer = s;
	}

	public ChatServer getAuthenticServer() {
		return authenticServer;
	}

	public synchronized void addChatServer(ChatServer s) {
		serverList.add(s);
	}

	public synchronized void deleteChatServer(ChatServer s) {
		serverList.remove(s);
	}

	public synchronized ChatServer getChatServer(String sID) {
		ChatServer target = null;
		for (ChatServer cs : serverList) {
			if (cs.getServerID().equals(sID)) {
				target = cs;
				break;
			}
		}
		return target;
	}

	public synchronized ArrayList<ChatServer> getServerList() {
		return serverList;
	}

	public synchronized void clientConnected(ChatClientConnection client) {
		connectedClients.add(client);
	}

	public synchronized void clientDisconnected(ChatClientConnection client) {
		connectedClients.remove(client);
	}

	public synchronized ArrayList<ChatClientConnection> getConnectedClients() {
		return connectedClients;
	}

	public synchronized void addLockID(String ID) {
		lockIdentity.add(ID);
	}

	public synchronized void deleteLockID(String ID) {
		lockIdentity.remove(ID);
	}

	/**
	 * return false means this id can't be used
	 * 
	 * @param id
	 * @return
	 */
	public synchronized boolean checkID(String id) {
		// check local user
		for (ChatClientConnection client : connectedClients) {
			if (client.getClientName().equals(id)) {
				return false;
			}
		}
		// check lock list
		if (lockIdentity.size() != 0) {
			for (String str : lockIdentity) {
				if (str.equals(id)) {
					return false;
				}
			}
		}
		// id can be used
		return true;
	}

	public synchronized void addLockRoomID(String rID) {
		lockRoomID.add(rID);
	}

	public synchronized void deleteLockRoomID(String rID) {
		lockRoomID.remove(rID);
	}

	/**
	 * return false means this room id can't be used
	 * 
	 * @param id
	 * @return
	 */
	public synchronized boolean checkRoomID(String rID) {
		// check chat room list
		for (ChatRoom cr : chatRoomList) {
			if (cr.getRoomID().equals(rID)) {
				return false;
			}
		}
		// check lock list
		if (lockRoomID.size() != 0) {
			for (String str : lockRoomID) {
				if (str.equals(rID)) {
					return false;
				}
			}
		}
		// room id can be used
		return true;
	}

	/**
	 * 
	 * @param rID
	 * @return true means the room is existing
	 */
	public synchronized boolean checkRoomExist(String rID) {
		// check chat room list
		for (ChatRoom cr : chatRoomList) {
			if (cr.getRoomID().equals(rID)) {
				return true;
			}
		}
		// not a existing room
		return false;
	}

	/**
	 * 
	 * @param ro
	 * @return true means not a room owner
	 */
	public synchronized boolean checkRoomOwner(String ro) {
		// check chat room owner
		for (ChatRoom cr : chatRoomList) {
			if (cr.getRoomOwner().equals(ro)) {
				return false;
			}
		}
		// not a room owner
		return true;
	}

	public synchronized void addChatRoom(ChatRoom cr) {
		chatRoomList.add(cr);
	}

	public synchronized void deleteChatRoom(ChatRoom rID) {
		chatRoomList.remove(rID);
	}

	public synchronized ArrayList<ChatRoom> getChatRoomList() {
		return chatRoomList;
	}

	public synchronized ChatRoom getChatRoom(String rID) {
		for (int i = 0; i < chatRoomList.size(); i++) {
			if (chatRoomList.get(i).getRoomID().equals(rID)) {
				return chatRoomList.get(i);
			}
		}
		// room ID not found
		return null;
	}

	public synchronized void addChatRoomMember(String rID,
			ChatClientConnection client) {
		getChatRoom(rID).addRoomMember(client);
	}

	public synchronized void deleteChatRoomMember(String rID,
			ChatClientConnection client) {
		getChatRoom(rID).deleteRoomMember(client);
	}

	public synchronized void sendMsgWithinChatRoom(String rID, String msg) {
		ArrayList<ChatClientConnection> roomMember = getChatRoom(rID)
				.getChatRoomMember();
		for (ChatClientConnection client : roomMember) {
			client.write(msg);
		}
	}

	public synchronized void sendMsgToAllClients(String msg) {
		for (ChatClientConnection client : connectedClients) {
			client.write(msg);
		}
	}
}

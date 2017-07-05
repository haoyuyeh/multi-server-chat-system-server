
/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class stores the name of chat room and members(including the owner) belonged to it
 */

import java.util.ArrayList;

public class ChatRoom {
	private ArrayList<ChatClientConnection> roomMember;
	private String roomID, serverID, roomOwner;

	public ChatRoom(String rID, String sID, String rOwner) {
		roomID = rID;
		serverID = sID;
		roomOwner = rOwner;
		roomMember = new ArrayList<ChatClientConnection>();
	}

	public String getRoomID() {
		return roomID;
	}

	public String getServerID() {
		return serverID;
	}

	public String getRoomOwner() {
		return roomOwner;
	}

	/**
	 * adding a client into the particular chat room
	 * 
	 * @param c
	 *            : client connection
	 */
	public void addRoomMember(ChatClientConnection c) {
		roomMember.add(c);
	}

	/**
	 * deleting a client from the particular chat room
	 * 
	 * @param c
	 *            : client connection
	 */
	public void deleteRoomMember(ChatClientConnection c) {
		roomMember.remove(c);
	}

	public ArrayList<ChatClientConnection> getChatRoomMember() {
		return roomMember;
	}
}

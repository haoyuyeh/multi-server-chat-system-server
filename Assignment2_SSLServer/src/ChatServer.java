/**
 * Author: Hao Yu Yeh 
 * Date: 2016¦~10¤ë12¤é 
 * Project: Assignment2 of Distributed System 
 * Comment: this class store the configuration of server
 */

public class ChatServer {
	private String serverID, serverAddr;
	private int clientPort, serverPort;

	public ChatServer(String sID, String sAddr, int cPort, int sPort) {
		serverID = sID;
		serverAddr = sAddr;
		clientPort = cPort;
		serverPort = sPort;
	}
	
	public ChatServer(String sAddr, int cPort) {
		serverAddr = sAddr;
		clientPort = cPort;
	}

	public String getServerID() {
		return serverID;
	}

	public String getServerAddr() {
		return serverAddr;
	}

	public int getClientPort() {
		return clientPort;
	}

	public int getServerPort() {
		return serverPort;
	}
}

/**
 * Author: Hao Yu Yeh
 * Date: 2016¦~10¤ë12¤é
 * Project: Assignment2 of Distributed System
 * Comment: this class list all the commands used in chat room
 */

public class ChatCommands {
	// for client and server communication
	public final static String CONNECT = "connect", ADDSERVER = "addserver";
	public final static String NEWIDENTITY = "newidentity", LIST = "list", WHO = "who", CREATEROOM = "createroom";
	public final static String JOINROOM = "join", MOVEJOIN = "movejoin", DELETEROOM = "deleteroom"; 
	public final static String MESSAGE = "message", QUIT = "quit";
	
	// for servers communication
	public final static String LOCKIDENTITY = "lockidentity", RELEASEIDENTITY = "releaseidentity";
	public final static String LOCKROOMID = "lockroomid", RELEASEROOMID = "releaseroomid";
}

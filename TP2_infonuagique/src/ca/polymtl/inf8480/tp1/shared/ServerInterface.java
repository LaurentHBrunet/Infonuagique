package ca.polymtl.inf8480.tp1.shared;

import java.io.File;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface ServerInterface extends Remote {
	
	Boolean openSession(String login, String password)  throws RemoteException;
	
	ArrayList<FileData> getGrouplist(int checksum, String login) throws RemoteException;

	Boolean deleteMail(int id, String login) throws RemoteException;
	
	Boolean pushGroupList(ArrayList<FileData> grouplist, String login) throws RemoteException;
	
	Boolean lockGroupList(String login) throws RemoteException;
	
	Boolean sendMail(String subject, String addrDest, String content, String login) throws RemoteException;
	
	ArrayList<FileData> listMails(Boolean justUnread, String login) throws RemoteException;
	
	FileData readMail(int id, String login) throws RemoteException;
	
	ArrayList<FileData> searchMail(String[] keywords, String login) throws RemoteException;
}

package ca.polymtl.inf8480.tp1.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

import ca.polymtl.inf8480.tp1.shared.FileData;
import ca.polymtl.inf8480.tp1.shared.ServerInterface;

public class Server implements ServerInterface {
	
	private static String validLoginsPath = "./server/files/validLogins/";
	private static String groupFilesPath = "./server/files/groups/";
	private static String baseFilePath = "./server/files/";
	private static String unread = "unread\n";
	private static String read = "read\n";
	
	private ArrayList<String> connectedUsers = new ArrayList<String>();
	private String userWithLock;
	

	public static void main(String[] args) {
		Server server = new Server();
		server.run();
	}

	public Server() {
		super();
	}

	private void run() {
		System.out.println("TESTING TESTING");

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		try {
			ServerInterface stub = (ServerInterface) UnicastRemoteObject
					.exportObject(this, 0);

			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("server", stub);
			System.out.println("Server ready.");
		} catch (ConnectException e) {
			System.err
					.println("Impossible de se connecter au registre RMI. Est-ce que rmiregistry est lancé ?");
			System.err.println();
			System.err.println("Erreur: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Erreur: " + e.getMessage());
		}
	}
	
	private File[] getFilesInDir(String dirPath) {
		File[] files;
	    try {
			File dir = new File(dirPath);

			files = dir.listFiles();
			return files;
		} catch (Exception e) {
	    	System.out.println(e.getMessage());
	    	return null;
		}
	}
	
	private ArrayList<String> readFileLines(File groupFile) {

		String line;
		ArrayList<String> fileLines = new ArrayList();
		try {
			
			BufferedReader fileReader = new BufferedReader(new FileReader(groupFile));
		
			line = fileReader.readLine();
			while (line != null) {
				fileLines.add(line);
				
				line = fileReader.readLine();
			}
			
			fileReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return fileLines;
	}
	
	private Boolean confirmSessionOpen(String login) {
		
		for (String connectedUser : connectedUsers) {
			if (login.equals(connectedUser)) {
				return true;
			}
		}
		System.out.println("User is not connected");
		return false;		
	}

	@Override
	public Boolean openSession(String login, String password) {

		File[] validLogins = getFilesInDir(validLoginsPath);

		for (File validLogin : validLogins) {
			ArrayList<String> fileContent = readFileLines(validLogin);

			if (fileContent.get(0).equals(login) &&
				fileContent.get(1).equals(password)) {
				connectedUsers.add(login);
				return true;
			}
		}
		
		return false;
	}

	@Override
	public ArrayList<FileData> getGrouplist(int checksum, String login) {
		if (!confirmSessionOpen(login)) {
			return null;
		}

		File[] groupFiles = getFilesInDir(groupFilesPath);

		return convertFileList(groupFiles);
	}

	@Override
	public Boolean pushGroupList(ArrayList<FileData> fileDataGrouplist, String login) {
		if (!confirmSessionOpen(login)) {
			return false;
		}

		if (userWithLock.equals(login)) {
			File[] currentFiles = getFilesInDir(groupFilesPath);
			
			for (File file: currentFiles) {
				file.delete();
			}

			File[] groupList = convertFileDataList(groupFilesPath, fileDataGrouplist);

			for (File newFile: groupList) {
				try {
					newFile.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			userWithLock = null;
			
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Boolean lockGroupList(String login) {
		if (!confirmSessionOpen(login)) {
			return false;
		}
		
		if (userWithLock == null) {
			userWithLock = login;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public Boolean sendMail(String subject, String addrDest, String content, String login) {
		if (!confirmSessionOpen(login)) {
			return false;
		}
		File newMail = new File(baseFilePath + addrDest + "/" + subject);
		
		FileWriter fw;
		try {
			fw = new FileWriter(newMail, true);
			fw.write(unread);
			fw.write("From : " + login + "\n");
			fw.write(content);
			fw.close();
			newMail.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}

	@Override
	public ArrayList<FileData> listMails(Boolean justUnread, String login) {
		if (!confirmSessionOpen(login)) {
			return null;
		}		
		
		File[] mailList = getFilesInDir(baseFilePath + login + "/");
		
		if (justUnread) {
			ArrayList<File> unreadMailList = new ArrayList();
			
			for(File mail: mailList) {
				ArrayList<String> mailContent = readFileLines(mail);

				if (mailContent.get(0).equals("unread")) {
					unreadMailList.add(mail);
				}
			}

			File[] unreadFileArray = new File[unreadMailList.size()];
			unreadMailList.toArray(unreadFileArray);

			return convertFileList(unreadFileArray);

		} else {
			return convertFileList(mailList);
		}
	}

	@Override
	public FileData readMail(int id, String login) {
		if (!confirmSessionOpen(login)) {
			return null;
		}		
		
		File[] mailList = getFilesInDir(baseFilePath + login + "/");
		
		File mailToRead = mailList[id];

		ArrayList<String> currentMailContent = readFileLines(mailToRead);

		mailToRead.delete();

		FileWriter fw;
		try {
			fw = new FileWriter(mailToRead, true);
			fw.write(read);

			for(int i = 1; i < currentMailContent.size(); i++) {
				fw.write(currentMailContent.get(i) + "\n");
			}

			fw.close();

			mailToRead.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return new FileData(mailToRead);
	}

	@Override
	public Boolean deleteMail(int id, String login) throws RemoteException {
		if (!confirmSessionOpen(login)) {
			return false;
		}

		File[] mailList = getFilesInDir(baseFilePath + login + "/");

		return mailList[id].delete();
	}

	@Override
	public ArrayList<FileData> searchMail(String[] keywords, String login) {

		ArrayList<File> searchedMailList = new ArrayList();

		for (File mailFile : getFilesInDir(baseFilePath + login + "/")) {
			ArrayList<String> fileContent = readFileLines(mailFile);

			for (String keyword : keywords) {
				if (fileContent.get(2).contains(keyword)) {
					searchedMailList.add(mailFile);
					break;
				}
			}
		}

		File[] searchedMailArray = new File[searchedMailList.size()];
		searchedMailList.toArray(searchedMailArray);

		return convertFileList(searchedMailArray);
	}
	

	private ArrayList<FileData> convertFileList(File[] files) {
		ArrayList<FileData> convertedData = new ArrayList();
		for (File file : files) {			
			convertedData.add(new FileData(file));
		}
		
		return convertedData;
	}

	private File[] convertFileDataList(String path, ArrayList<FileData> filesData) {
		File[] files = new File[filesData.size()];

		int index = 0;
		for (FileData fileData : filesData) {
			File file = new File(path + fileData.fileName);

			FileWriter fw;
			try {
				fw = new FileWriter(file, true);
				fw.write(fileData.fileContent);
				fw.close();
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

			files[index] = file;

		    index++;
		}

		return files;
	}

}

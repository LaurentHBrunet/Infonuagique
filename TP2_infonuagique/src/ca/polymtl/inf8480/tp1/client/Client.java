package ca.polymtl.inf8480.tp1.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Scanner;

import ca.polymtl.inf8480.tp1.shared.FileData;
import ca.polymtl.inf8480.tp1.shared.ServerInterface;
public class Client {
	
	private static String methodName;
	private static String arg1;
	private static String arg2;
	private static String arg3;
	private static String[] allArgs;
	private static String groupFilesPath = "./client/files/groups/";
	private static String loggedInPath = "./client/files/loggedUser/loggedUser";
	
	public static void main(String[] args) {
		String distantHostname = null;	

		if (args.length > 0) {
			methodName = args[0];
		}
		
		allArgs = args;
		
		if (args.length > 1) {
			arg1 = args[1];
		}
		
		if (args.length > 2) {
			arg2 = args[2];
		}

		if (args.length > 3) {
			arg3 = args[3];
		}
					
		
		Client client = new Client(distantHostname);
		client.run();
	}

	private ServerInterface localServerStub = null;

	public Client(String distantServerHostname) {
		super();

		if (System.getSecurityManager() == null) {
			System.setSecurityManager(new SecurityManager());
		}

		localServerStub = loadServerStub("127.0.0.1");
	}

	private void run() {
		switch (methodName) {
			case "login":
				login();
				break;
			case "lock-group-list":
				lockGrouplist();
				break;
			case "create-group":
				createGroup();
				break;
			case "join-group":
				joinGroup();
				break;
			case "publish-group-list": 
				publishGrouplist();
				break;
			case "send": 
				send();
				break;
			case "list":
				list();
				break;
			case "read": 
				read();
				break;
			case "delete": 
				delete();
				break;
			case "search": 
				search();
				break;
			default: break;
		}
	}
	
	private void login() {
		String login;
		String password;
		
		if (arg1 != null) {
			login = arg1;
		} else {
			System.out.println("Username required");
			return;
		}
		
		if (arg2 != null) {
			password = arg2;
		} else {
			System.out.println("Password required");
			return;
		}
		try {
			if (localServerStub.openSession(login, password)) {
			    File loggedInUser = new File(loggedInPath);
			    loggedInUser.delete();

				File f = new File(loggedInPath);

				FileWriter fw = new FileWriter(f, true);
				fw.write(login);
				fw.close();
				f.createNewFile();

				replaceGroupList();

				System.out.println("Logged in as " + login);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String getCurrentUserLogin() {
		ArrayList<String> userLogin = readFileLines(new File(loggedInPath));
		return userLogin.get(0);
	}
	
	private void lockGrouplist() {
		try {
			if (localServerStub.lockGroupList(getCurrentUserLogin())) {
			    replaceGroupList();
				System.out.println("La liste de groupes est verouille avec succes");
			} else {
				System.out.println("La liste de groupes n'a pas pu etre verouille");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void createGroup() {
		String groupName;
		String groupDescription;
		
		if (arg1 != null) {
			groupName = arg1;
		} else {
			System.out.println("No group defined");
			return;
		}

		File f = new File(groupFilesPath + groupName);
		
		if(f.exists() && !f.isDirectory()) {
			System.out.println("Group already exists");
			return;
		}
		
		try {
			Boolean success = f.createNewFile();
			
			if (success) {
				System.out.println("Le groupe " + groupName + "@poly.ca  a ete cree avec succes");
			} 
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}	
		

	}
	
	private void joinGroup() {
		String groupName;
		String userName;
		
		if (arg1 != null) {
			groupName = arg1;
		} else {
			System.out.println("No group defined");
			return;
		}
		
		if (arg2 != null) {
			userName = arg2;
		} else {
			System.out.println("No user defined");
			return;
		}
		
		//Get all group files
		File[] groupFiles = getFilesInDir(groupFilesPath);
	
		for (File groupFile: groupFiles) {
			if (groupFile.getName().equals(groupName)) {
				ArrayList<String> userList = readFileLines(groupFile);
								
				for (String user : userList) {
					if (user.equals(userName)) {
						System.out.println("User already in this group");
						return;											
					}
				}
				
				try {
					FileWriter fw = new FileWriter(groupFile, true);
					fw.write(userName + "\n");
					fw.close();
				} catch (Exception e) {
					System.out.println(e.getMessage());
					return;
				}
			}			
		}
		System.out.println("L’utilisateur " + userName + "@poly.ca a été ajouté au groupe" + groupName + "@poly.ca");
	}
	
	private ArrayList<String> readFileLines(File file) {

		String user;
		ArrayList<String> fileLines = new ArrayList();
		try {
			
			BufferedReader fileReader = new BufferedReader(new FileReader(file));
		
			user = fileReader.readLine();
			while (user != null) {
				fileLines.add(user);
				
				user = fileReader.readLine();
			}
			
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return fileLines;
	}
	
	private File[] getFilesInDir(String dirPath) {
		File dir = new File(dirPath);
		
		return dir.listFiles();
	}
	
	private void publishGrouplist() {
		try {
			if (localServerStub.pushGroupList(convertFileList(getFilesInDir(groupFilesPath)), getCurrentUserLogin())) {
				System.out.println("Les modifications apportées à la liste de groupes globale sont publiées avec succès.");
			} else {
				System.out.println("Les modifications a la liste de groupes n'ont pas pu etre realise");
			}		
		} catch (IOException e) {
			e.printStackTrace();
		}	
	}
	
	private void send() {
		String mailSubject;
		ArrayList<String> mailAddresses = new ArrayList<>();
		
		if (arg1 != null) {
			mailSubject = arg1;
		} else {
			System.out.println("No subject defined");
			return;
		}
		
		if (arg2 != null) {
		    if (arg2.equals("-g") && arg3 != null) {
		        replaceGroupList();
				File groupFile = new File(groupFilesPath + arg3);
				mailAddresses = readFileLines(groupFile);
			} else {
				mailAddresses.add(arg2);
			}
		} else {
			System.out.println("No mail destination defined");
			return;
		}
		
		System.out.println("Enter your message: ");
		
		Scanner scanner = new Scanner(System.in);
		String message = scanner.nextLine();
		
		try {
		    for (String mailAddress: mailAddresses) {
				if (localServerStub.sendMail(mailSubject, mailAddress, message, getCurrentUserLogin()) ) {
					System.out.println("Le mail a ete envoye avec succes");
				} else {
					System.out.println("Le mail n'a pas pu etre envoye avec succes");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void list() {
		Boolean justUnread = false;
		if (arg1 != null && arg1.equals("true")) {
			justUnread = true;
		}
		
		try {
			ArrayList<FileData> mails = localServerStub.listMails(justUnread, getCurrentUserLogin());

			if (mails != null && mails.size() > 0) {
				int i = 0;
				for (FileData mailData : mails) {
					String[] mailContent = readFileDataLines(mailData);

					System.out.println(i + "    " + mailContent[0] + "    " + mailContent[1] + "@polymtl.ca    " + mailData.fileName);
					i++;
				}
			} else {
				System.out.println("You have no mail");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	
	}
	private void read() {
		int emailId;
		
		if (arg1 != null) {
			emailId = Integer.parseInt(arg1);
		} else {
			System.out.println("Doit specifier un id d'email");
			return;
		}
		
		try {
			FileData mail = localServerStub.readMail(emailId, getCurrentUserLogin());

			String[] mailContent = readFileDataLines(mail);
		
			
			System.out.println( mailContent[1] + "@polymtl.ca");
			System.out.println( "Sujet : " + mail.fileName);
			System.out.println( mailContent[2]);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void delete() {
		int emailId;

		if (arg1 != null) {
			emailId = Integer.parseInt(arg1);
		} else {
			System.out.println("Doit specifier un id d'email");
			return;
		}

		try {
			if(localServerStub.deleteMail(emailId, getCurrentUserLogin())) {
				System.out.println("Mail delete - succes");
			} else {
				System.out.println("Mail delete - erreur");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		}


	}
	
	private void search() {
		int argsLength = allArgs.length;
		String[] keywords = new String[argsLength - 1];
		
		if (argsLength > 1) {
			System.arraycopy(allArgs, 1, keywords, 0, argsLength - 1);

			try {
				ArrayList<FileData> mails = localServerStub.searchMail(keywords, getCurrentUserLogin());

				if (mails != null && mails.size() > 0) {
					int i = 0;
					for (FileData mailData : mails) {
						String[] mailContent = readFileDataLines(mailData);

						System.out.println(i + "    " + mailContent[0] + "    " + mailContent[1] + "@polymtl.ca    " + mailData.fileName);
						i++;
					}
				} else {
					System.out.println("No mail found");
				}
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}

		} else {
			System.out.println("No keywords detemined");
		}		
	}
	

	private ServerInterface loadServerStub(String hostname) {
		ServerInterface stub = null;

		try {
			Registry registry = LocateRegistry.getRegistry(hostname);
			stub = (ServerInterface) registry.lookup("server");
		} catch (NotBoundException e) {
			System.out.println("Erreur: Le nom '" + e.getMessage()
					+ "' n'est pas défini dans le registre.");
		} catch (AccessException e) {
			//System.out.println("Access Exception ---------------------------------------");
			System.out.println("Erreur: " + e.getMessage());
		} catch (RemoteException e) {
			//System.out.println("RemoteException  ---------------------------------------");
			System.out.println("Erreur: " + e.getMessage());
			//System.out.println("Detail " + e.getCause().getCause());
		}

		return stub;
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
		    System.out.println("File nb " + index);

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

	private String[] readFileDataLines(FileData fileData) {
	    return fileData.fileContent.split("\\n");
	}


	private void replaceGroupList() {
		try {
			ArrayList<FileData> groupListData = localServerStub.getGrouplist(0, getCurrentUserLogin());

			if (groupListData != null) {

				for (File groupFile : getFilesInDir(groupFilesPath)) {
					groupFile.delete();
				}

				for (FileData filedata : groupListData) {
					filedata.convertToFile(groupFilesPath + filedata.fileName).createNewFile();
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
}

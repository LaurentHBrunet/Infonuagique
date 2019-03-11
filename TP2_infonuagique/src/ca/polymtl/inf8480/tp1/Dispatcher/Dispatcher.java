package ca.polymtl.inf8480.tp1.Dispatcher;

import ca.polymtl.inf8480.tp1.shared.CalculatorServerInterface;
import ca.polymtl.inf8480.tp1.shared.NameServiceInterface;
import com.sun.tools.javac.util.Assert;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

public class Dispatcher {

    private String calculationFilePath;
    private boolean isSecured;
    private String nameServerAddress;
    private NameServiceInterface nameServiceInterfaceStub;
    private String username;
    private String password;

    public static void main(String[] args) {

        if (args.length > 3) {
            String calculationFilePath = args[0];
            String nameServerAddress = args[1];
            String userName = args[2];
            String password = args[3];

            boolean secured = true; //Secured if nothing is specified in launch script
            if (args.length > 4 && args[4].equals("notsecured")) {
                secured = false;
            }

            Dispatcher dispatcher= new Dispatcher(calculationFilePath, nameServerAddress, userName, password, secured);
            dispatcher.run();
        } else {
            System.out.println("Must specify calculation file path, nameServer address, username, password");
        }
    }

    public Dispatcher(String calculationFilePath, String nameServerAddress, String username, String password, boolean isSecured) {
        this.calculationFilePath = calculationFilePath;
        this.isSecured = isSecured;
        this.username = username;
        this.password = password;

        nameServiceInterfaceStub = loadNameServiceStub(nameServerAddress);
    }

    private void run() {
        //Get op list from file
        List<Pair<String, Integer>> opList = readOperationList();

        //Get list of calculator servers from the NameService server
        List<Pair<String, Integer>> calculatorServersInfo; //String is serverAddress, Integer is server capacity
        try {
            calculatorServersInfo = nameServiceInterfaceStub.getCalculatorServerList(username, password);

            //TODO: Generate CalculatorServerInteface stubs for each calculator server info
            //TODO: Put it in hashmap, or list of pairs or whatever to have capacity accessible

            if (isSecured) {
                //TODO: Dispatch parts of op list to different calculator servers
                // don't need to confirm return value with other server.
            } else {
                //TODO: Dispatch parts of op list to different calculator servers
                // Need to confirm return value with other server.
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //TODO: Consolidate all tasks and print final answer
    }

    private List<Pair<String, Integer>> readOperationList() {
        //Split calculation file in operation lines
        ArrayList<String> operationLines = readCalculationFileLines(new File(calculationFilePath));

        List<Pair<String, Integer>> opList = new ArrayList<>();

        for (String operationLine : operationLines) {
            //Split operation and number
            String[] operationContent = operationLine.split(" ");

            Assert.check(operationContent.length == 2);

            //Add to operation list
            opList.add(new Pair<>(operationContent[0], Integer.parseInt(operationContent[1])));
        }

        return opList;
    }

    private ArrayList<String> readCalculationFileLines(File calculationFile) {

        String line;
        ArrayList<String> fileLines = new ArrayList();
        try {

            BufferedReader fileReader = new BufferedReader(new FileReader(calculationFile));

            line = fileReader.readLine();
            while (line != null) {
                fileLines.add(line);

                line = fileReader.readLine();
            }

            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileLines;
    }

    private NameServiceInterface loadNameServiceStub(String hostname) {
        NameServiceInterface stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            stub = (NameServiceInterface) registry.lookup("nameServer");
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

    private CalculatorServerInterface loadCalculatorServerStub(String hostname) {
        CalculatorServerInterface stub = null;

        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            stub = (CalculatorServerInterface) registry.lookup("calculatorServer");
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
}

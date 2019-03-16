package ca.polymtl.inf8480.tp1.CalculatorServer;

import ca.polymtl.inf8480.tp1.shared.CalculatorServerInterface;
import ca.polymtl.inf8480.tp1.shared.NameServiceInterface;
import ca.polymtl.inf8480.tp1.shared.Tuple;

import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class CalculatorServer implements CalculatorServerInterface {

    private int serverCapacity; //QI in PDF for lab
    private int maliciousPercentage; //m in PDF
    private String nameServerHostname;
    private NameServiceInterface nameServiceStub;

    public static void main(String[] args) {
        if (args.length > 2) {
            CalculatorServer server = new CalculatorServer(Integer.parseInt(args[0]), //Server capacity
                                                           Integer.parseInt(args[1]), //Malicious percentage
                                                           args[2]);                  //NameService address
            server.run();
        } else {
            System.out.println("Calculator server requires ServerCapacity, MaliciousPercentage and" +
                    " name service address as arguments");
        }
    }

    public CalculatorServer(int serverCapacity, int maliciousPercentage, String nameServerHostName) {
        super();

        this.serverCapacity = serverCapacity;
        this.maliciousPercentage = maliciousPercentage;
        this.nameServerHostname = nameServerHostName;

        nameServiceStub = loadNameServiceStub(nameServerHostName);

        //TODO: Confirm how we want to go about adding calculator server to name service
        // Probably want to pass IP address, so the dipatcher can get a list of IP
        // addresses and create stubs on its side when it starts.
        try {
            //TODO: get current IP address
            nameServiceStub.addCalculatorToNameServer("THIS IS A PLACEHOLDER", serverCapacity);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            CalculatorServerInterface stub = (CalculatorServerInterface) UnicastRemoteObject
                    .exportObject(this, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("calculatorServer", stub);

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

    // Returns -1 if the server is unable to do the task because of lack of resources.
    @Override
    public int calculateTaskList(List<Tuple<String, Integer>> operationList, String username, String password) throws RemoteException {
        if (!confirmResourcesAvailable(operationList.size()) || confirmDispatcherLogin(username, password)) {
            return -1;
        }

        int total = 0;

        for (Tuple<String, Integer> operation : operationList) {
            if (operation.getKey().equals("pell")) {
                total += Operations.pell(operation.getValue()) % 5000;
                total = total % 5000;
            } else if (operation.getKey().equals("prime")) {
                total += Operations.prime(operation.getValue()) % 5000;
                total = total % 5000;
            } else {
                System.out.println("Error wrong operation type used");
                break;
            }
        }

        Random rdm = new Random();
        if (rdm.nextInt(100 + 1) >= maliciousPercentage) {
            return total; //Returns right answer
        } else {
            return rdm.nextInt(total);  //Returns malicious answer,
                                        //random between 0 and correct answer for simulation purposes
        }
    }

    @Override
    public int getCalculatorCapacity() {
        return serverCapacity;
    }

    private boolean confirmResourcesAvailable(int taskSize) {
        // Chances T of resources not being available
        float t = ((taskSize - serverCapacity) / (5.0f * serverCapacity)) * 100;

        Random rdm = new Random();
        if (rdm.nextInt(100 + 1) > t) { //If value is bigger than the chances of failing, it's a success
            return true;
        } else {
            return false;
        }
    }

    private boolean confirmDispatcherLogin(String username, String password) {
        try {
            return nameServiceStub.authenticateUser(username, password);

        } catch (RemoteException e) {
            e.printStackTrace();
        }

        return false;
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
}

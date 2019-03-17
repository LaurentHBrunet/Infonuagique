package ca.polymtl.inf8480.tp1.calculatorServer;

import ca.polymtl.inf8480.tp1.shared.CalculatorServerInterface;
import ca.polymtl.inf8480.tp1.shared.NameServiceInterface;
import ca.polymtl.inf8480.tp1.shared.Tuple;

import java.net.Inet4Address;
import java.rmi.AccessException;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CalculatorServer implements CalculatorServerInterface {

    private int serverCapacity; //QI in PDF for lab
    private int maliciousPercentage; //m in PDF
    private String nameServerHostname;
    private NameServiceInterface nameServiceStub;
    private String hostname;

    public static void main(String[] args) {

        if (args.length > 3) {
            CalculatorServer server = new CalculatorServer(args[0],
                                                           Integer.parseInt(args[1]), //Server capacity
                                                           Integer.parseInt(args[2]), //Malicious percentage
                                                           args[3]);                  //nameService address
            server.run();
        } else {
            System.out.println("Calculator server requires Hostname, ServerCapacity, MaliciousPercentage and" +
                    " name service address as arguments");
        }
    }

    public CalculatorServer(String hostname, int serverCapacity, int maliciousPercentage, String nameServerHostName) {
        super();

        this.hostname = hostname;
        this.serverCapacity = serverCapacity;
        this.maliciousPercentage = maliciousPercentage;
        this.nameServerHostname = nameServerHostName;

        nameServiceStub = loadNameServiceStub(nameServerHostName);

        try {
            //TODO: get current IP address
            nameServiceStub.addCalculatorToNameServer(hostname, serverCapacity);
        } catch (Exception e) {
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
    public int calculateTaskList(ArrayList<Tuple<String, Integer>> operationList, String username, String password) throws RemoteException {
        System.out.println("Calculating task of size " + operationList.size());

        if (!confirmResourcesAvailable(operationList.size()) || confirmDispatcherLogin(username, password)) {
            System.out.println("Error cannot calculate task, returning -1");
            return -1;
        }

        System.out.println("Calculating task");

        int total = 0;

        for (Tuple<String, Integer> operation : operationList) {
            if (operation.x.equals("pell")) {
                total += Operations.pell(operation.y) % 5000;
                total = total % 5000;
            } else if (operation.x.equals("prime")) {
                total += Operations.prime(operation.y) % 5000;
                total = total % 5000;
            } else {
                System.out.println("Error wrong operation type used");
                break;
            }
        }

        Random rdm = new Random();
        if (rdm.nextInt(100 + 1) >= maliciousPercentage) {
            System.out.println("returning correct value of " + total);
            return total; //Returns right answer
        } else {
            System.out.println("returning wrong value because of malicious server");
            return rdm.nextInt(total);  //Returns malicious answer,
                                        //random between 0 and correct answer for simulation purposes
        }
    }

    private boolean confirmResourcesAvailable(int taskSize) {
        // Chances T of resources not being available
        float t = ((taskSize - serverCapacity) / (5.0f * serverCapacity)) * 100;
        System.out.println("Chances of failure " + t);

        Random rdm = new Random();
        int test = rdm.nextInt(100 + 1);
        System.out.println("Random chances " + test);
        if (test > t) { //If value is bigger than the chances of failing, it's a success
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

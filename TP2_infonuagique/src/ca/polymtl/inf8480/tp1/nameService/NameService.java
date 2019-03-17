package ca.polymtl.inf8480.tp1.nameService;

import ca.polymtl.inf8480.tp1.shared.NameServiceInterface;
import ca.polymtl.inf8480.tp1.shared.Tuple;

import java.rmi.ConnectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class NameService implements NameServiceInterface {

    private String dispatcherUsername;
    private String dispatcherPassword;
    private List<Tuple<String, Integer>> calculatorServerList = new ArrayList<>();

    public static void main(String[] args) {
        NameService nameService = new NameService();
        nameService.run();
    }

    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            NameServiceInterface stub = (NameServiceInterface) UnicastRemoteObject
                    .exportObject(this, 0);

            Registry registry = LocateRegistry.getRegistry();
            registry.rebind("nameServer", stub);

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

    @Override
    public List<Tuple<String, Integer>> getCalculatorServerList(String username, String password) throws RemoteException {
        dispatcherUsername = username;
        dispatcherPassword = password;
        return calculatorServerList;
    }

    @Override
    public boolean authenticateUser(String userName, String password) throws RemoteException {
        if (dispatcherUsername == userName && dispatcherPassword == password) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean addCalculatorToNameServer(String serverAddress, Integer serverCapacity) throws RemoteException {
        System.out.println("Adding server " + serverAddress + " to name server, Capacity : " + serverCapacity);
        return calculatorServerList.add(new Tuple<>(serverAddress, serverCapacity));
    }
}

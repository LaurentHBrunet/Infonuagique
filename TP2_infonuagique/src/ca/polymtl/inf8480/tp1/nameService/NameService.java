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

    /**
     * Se connecte au registre RMI et y attache un stub du nameServer
     */
    private void run() {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        try {
            NameServiceInterface stub = (NameServiceInterface) UnicastRemoteObject
                    .exportObject(this,5001);

            Registry registry = LocateRegistry.getRegistry(5030);
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

    /**
     * Permet d'obtenir la liste des serveurs de calculs disponibles. Utilisé ar le dispatcher.
     * Cette liste doit avoir étée remplie par l'appel de addCalculatorToNameServer par les calculatorServers
     * avant l'appel de cette fonction
     * enregistre le username et password associés au répartiteur
     * @param username nom d'utilisatuer associé au répartiteur qui fait l'appel
     * @param password mot de passe associé au répartiteur qui fait l'appel
     * @return Liste de Tuple de (addresse de serveur de calcul, capacité de serveur de calcul) représentant tous les
     * serveurs de calculs enregistrés
     * @throws RemoteException
     */
    @Override
    public List<Tuple<String, Integer>> getCalculatorServerList(String username, String password) throws RemoteException {
        dispatcherUsername = username;
        dispatcherPassword = password;
        return calculatorServerList;
    }

    /**
     * vérifie l'authentification des appelants
     * @param userName nom d'utilisateur du server de calcul fesant l'appel
     * @param password mot de passe du server de calcul fesant l'appel
     * @return true si le mot de passe et le nom d'utilisateurs sont bons
     * @throws RemoteException
     */
    @Override
    public boolean authenticateUser(String userName, String password) throws RemoteException {
        if (dispatcherUsername == userName && dispatcherPassword == password) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * ajoute l'adresse et la capacité du serveur de calculs appelant dans la liste calculatorServerList
     * @param serverAddress adresse du serveur de calcul
     * @param serverCapacity capacité du serveur de calcul
     * @return true si les informations ont pu etre ajoutées à la liste
     * @throws RemoteException
     */
    @Override
    public boolean addCalculatorToNameServer(String serverAddress, Integer serverCapacity) throws RemoteException {
        System.out.println("Adding server " + serverAddress + " to name server, Capacity : " + serverCapacity);
        return calculatorServerList.add(new Tuple<>(serverAddress, serverCapacity));
    }
}

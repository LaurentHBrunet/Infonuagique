package ca.polymtl.inf8480.tp1.shared;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface NameServiceInterface extends Remote {
    List<Tuple<String, Integer>> getCalculatorServerList(String username, String password) throws RemoteException;
    boolean authenticateUser(String userName, String password) throws RemoteException;

    //TODO: Confirm what needs to be passed here, how to save calculator servers in name service
    boolean addCalculatorToNameServer(String serverAddress, Integer serverCapcity) throws RemoteException;
}

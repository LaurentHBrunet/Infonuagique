package ca.polymtl.inf8480.tp2.shared;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface CalculatorServerInterface extends Remote {
    int calculateTaskList(ArrayList<Tuple<String, Integer>> operationList, String username, String password) throws RemoteException;
}

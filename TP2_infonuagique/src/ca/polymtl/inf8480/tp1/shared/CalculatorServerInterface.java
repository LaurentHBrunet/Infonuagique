package ca.polymtl.inf8480.tp1.shared;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public interface CalculatorServerInterface extends Remote {
    int calculateTaskList(ArrayList<Tuple<String, Integer>> operationList, String username, String password) throws RemoteException;
}

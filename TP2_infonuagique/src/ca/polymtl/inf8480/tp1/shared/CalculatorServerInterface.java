package ca.polymtl.inf8480.tp1.shared;
import java.util.concurrent.Callable;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CalculatorServerInterface extends Remote, Callable<Integer> {
    int calculateTaskList(List<Tuple<String, Integer>> operationList, String username, String password) throws RemoteException;
    int getCalculatorCapacity();
    Integer calculateTaskList() throws RemoteException;
    void setTaskList(List<Tuple<String, Integer>> operationList, String username, String password);
}

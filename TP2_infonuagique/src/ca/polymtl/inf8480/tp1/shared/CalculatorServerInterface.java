package ca.polymtl.inf8480.tp1.shared;



import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface CalculatorServerInterface extends Remote {
    int calculateTaskList(List<Tuple<String, Integer>> operationList, String username, String password) throws RemoteException;
    int getCalculatorCapacity();
}

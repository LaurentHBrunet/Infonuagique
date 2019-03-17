package ca.polymtl.inf8480.tp1.dispatcher;

import ca.polymtl.inf8480.tp1.shared.CalculatorServerInterface;
import ca.polymtl.inf8480.tp1.shared.NameServiceInterface;
import ca.polymtl.inf8480.tp1.shared.Tuple;

import java.util.concurrent.*;

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
    private List<Tuple<CalculatorServerInterface, Integer>> calculatorServerList = new ArrayList<>();

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
        //Get op list from file0
        int result = -1;
        ArrayList<Tuple<String, Integer>> opList = readOperationList();

        //Get list of calculator servers from the nameService server
        List<Tuple<String, Integer>> calculatorServersInfo; //String is serverAddress, Integer is server capacity
        try {
            //Generate stubs for each calculator server
            calculatorServersInfo = nameServiceInterfaceStub.getCalculatorServerList(username, password);
            for (Tuple<String, Integer> calculatorServerInfo : calculatorServersInfo) {
                System.out.println("Creating calculator server stub with hostname " + calculatorServerInfo.x + " And capacity " + calculatorServerInfo.y);
                calculatorServerList.add(new Tuple<>(loadCalculatorServerStub(calculatorServerInfo.x), calculatorServerInfo.y));
            }

            if (isSecured) {
                result = dispatchSecured(opList);
                //TODO: Dispatch parts of op list to different calculator servers
                // don't need to confirm return value with other server.

            } else {
                result = dispatchUnsecured();
                //TODO: Dispatch parts of op list to different calculator servers
                // Need to confirm return value with other server.
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        //TODO: Consolidate all tasks and print final answer
        System.out.println(result);
    }

    private int dispatchSecured(ArrayList<Tuple<String, Integer>> opList) {
        int taskTotal = 0;
        Executor executor = Executors.newCachedThreadPool();
        ExecutorCompletionService<Integer> ecs = new ExecutorCompletionService<>(executor);
        List<Callable<Integer>> callableList = new ArrayList<>();

        while (opList.size() != 0) {
            for (Tuple<CalculatorServerInterface, Integer> calculatorServer : calculatorServerList) {
                //Get sub task list and remove it from  the full task list
                int requestSize = Math.min(opList.size(), getOptimalTaskSize(calculatorServer.y));
                System.out.println("Request of size " + requestSize);

                //If opList is complete stop trying to send requests
                if (requestSize == 0)
                    break;

                ArrayList<Tuple<String, Integer>> subTaskList = new ArrayList<>(opList.subList(0, requestSize));
                opList.subList(0, requestSize).clear();

                //Submit all tasks through the Executor Completion service, creating new threads
                CallableCalculatorServer calculatorServerCall = new CallableCalculatorServer(calculatorServer.x, subTaskList);
                callableList.add(calculatorServerCall);
                ecs.submit(calculatorServerCall);
            }
        }

        try {
            while (callableList.size() != 0) {
                for (int i = 0; i < callableList.size(); i++) {
                    Integer result = ecs.take().get();
                    if (result != null && result != -1) {
                        taskTotal = (taskTotal + result) % 5000;
                        callableList.remove(i);
                        break;
                    } else {
                        ecs.submit(callableList.get(i));
                    }
                }

            }
        } catch (Exception e){
            System.out.println(e);
        }

        return taskTotal;
    }

    private int dispatchUnsecured() {
        return -1;
    }

    private ArrayList<Tuple<String, Integer>> readOperationList() {
        //Split calculation file in operation lines
        ArrayList<String> operationLines = readCalculationFileLines(new File(calculationFilePath));

        ArrayList<Tuple<String, Integer>> opList = new ArrayList<>();

        for (String operationLine : operationLines) {
            //Split operation and number
            String[] operationContent = operationLine.split(" ");

            //Add to operation list
            opList.add(new Tuple<>(operationContent[0], Integer.parseInt(operationContent[1])));
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

        System.out.println("Loading name service stub at address " + hostname);
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
            System.out.println("Creating calculator server stub for " + hostname);

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
        } catch (Exception e) {
            System.out.println("This is an error" + e);
        }

        return stub;
    }

    //Triple server capacity which should give roughly 60% success rate
    private int getOptimalTaskSize(int serverCapacity) {
        return serverCapacity * 5;
    }

    private class CallableCalculatorServer implements Callable<Integer> {

        private CalculatorServerInterface stub;
        private ArrayList<Tuple<String, Integer>> taskList;

        CallableCalculatorServer(CalculatorServerInterface calculatorServerInterface, ArrayList<Tuple<String, Integer>> taskList) {
            this.stub = calculatorServerInterface;
            this.taskList = taskList;
        }

        @Override
        public Integer call() throws Exception {
            int result = 0;
            try {
                result = stub.calculateTaskList(taskList, username, password);
            } catch (Exception e) {
                System.out.println("Failed task call");
                System.out.println(e);
            }

            return result;
        }
    }
}



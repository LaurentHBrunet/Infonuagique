package ca.polymtl.inf8480.tp2.dispatcher;

import ca.polymtl.inf8480.tp2.shared.CalculatorServerInterface;
import ca.polymtl.inf8480.tp2.shared.NameServiceInterface;
import ca.polymtl.inf8480.tp2.shared.Tuple;

import java.util.*;
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
//répartiteur
public class Dispatcher {

    private String calculationFilePath;
    private boolean isSecured;
    private String nameServerAddress;
    private NameServiceInterface nameServiceInterfaceStub;
    private String username;
    private String password;
    private List<Tuple<CalculatorServerInterface, Integer>> calculatorServerList = new ArrayList<>();

    /**
     *
     * @param args
     */
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

    /**
     *
     * @param calculationFilePath
     * @param nameServerAddress
     * @param username
     * @param password
     * @param isSecured
     */
    public Dispatcher(String calculationFilePath, String nameServerAddress, String username, String password, boolean isSecured) {
        this.calculationFilePath = calculationFilePath;
        this.isSecured = isSecured;
        this.username = username;
        this.password = password;

        nameServiceInterfaceStub = loadNameServiceStub(nameServerAddress);
    }

    /**
     *
     */
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
                long start = System.nanoTime();

                result = dispatchSecured(opList);

                long end = System.nanoTime();


                System.out.println("Temps écoulé appel secure: " + (end - start)
                        + " ns");
                System.out.println("Résultat appel secure: " + result);
            } else {
                long start = System.nanoTime();

                result = dispatchUnsecured(opList);

                long end = System.nanoTime();

                System.out.println("Temps écoulé appel non secure: " + (end - start)
                        + " ns");
                System.out.println("Résultat appel secure: " + result);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     *
     * @param opList
     * @return
     */
    private int dispatchSecured(ArrayList<Tuple<String, Integer>> opList) {
        int taskTotal = 0;
        ExecutorService executor = Executors.newCachedThreadPool();
//        List<Tuple<Future<Integer>, CallableCalculatorServer>> asyncResponseList = new ArrayList<>();
        List<Request> asyncResponseList = new ArrayList<>();

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
                asyncResponseList.add(new Request(calculatorServerCall, executor.submit(calculatorServerCall), 0));
            }
        }

        try {
            while (asyncResponseList.size() != 0) {
                for (int i = 0; i < asyncResponseList.size(); i++) {
                    Integer result = asyncResponseList.get(i).response.get();

                    if (result != null && result != -1 && result != -2) {
                        System.out.println("Got correct response");
                        taskTotal = (taskTotal + result) % 5000;
                        asyncResponseList.remove(i);
                        break;
                    } else if (result == -2){
                        System.out.println("Server is unreachable, rerouting request");
                        //Server is probably disconnected, send the task to a different server randomly to even the load
                        Random random = new Random();
                        int replacementCalculatorServerId = random.nextInt(calculatorServerList.size());

                        //Change the stub used to send the request so that it uses a different server
                        asyncResponseList.get(i).callableCalculatorServer.setStub(calculatorServerList.get(replacementCalculatorServerId).x);

                    }
                    System.out.println("Resending incorrect request");
                    asyncResponseList.add(new Request(asyncResponseList.get(i).callableCalculatorServer, executor.submit(asyncResponseList.get(i).callableCalculatorServer),0));
                    asyncResponseList.remove(i);
                }
            }
        } catch (Exception e){
            System.out.println(e);
        }

        return taskTotal;
    }

    /**
     *
     */
    private class Request {
        protected CallableCalculatorServer callableCalculatorServer;
        protected Future<Integer> response;
        protected int requestId;

        Request(CallableCalculatorServer callableCalculatorServer, Future<Integer> response, int requestId) {
           this.callableCalculatorServer = callableCalculatorServer;
           this.response = response;
           this.requestId = requestId;
        }
    }

    /**
     *
     * @param opList
     * @return
     */
    private int dispatchUnsecured(ArrayList<Tuple<String, Integer>> opList) {
        int taskTotal = 0;
        ExecutorService executor = Executors.newCachedThreadPool();
        List<Request> asyncResponseList = new ArrayList<>();
        List<Set<Integer>> possibleAnswerList = new ArrayList<>();
        int requestId = 0;

        while (opList.size() != 0) {
            for(int i = 0; i < calculatorServerList.size(); i++) {

                //Get sub task list and remove it from  the full task list
                int requestSize = Math.min(opList.size(), getOptimalTaskSize(calculatorServerList.get(i).y));
                System.out.println("Request of size " + requestSize);

                //If opList is complete stop trying to send requests
                if (requestSize == 0)
                    break;

                ArrayList<Tuple<String, Integer>> subTaskList = new ArrayList<>(opList.subList(0, requestSize));
                opList.subList(0, requestSize).clear();

                //Submit all tasks through the Executor Completion service, creating new threads
                CallableCalculatorServer calculatorServerCall1 = new CallableCalculatorServer(calculatorServerList.get(i).x, subTaskList);
                CallableCalculatorServer calculatorServerCall2;

                if (i == calculatorServerList.size() - 1) {
                    calculatorServerCall2 = new CallableCalculatorServer(calculatorServerList.get(0).x, subTaskList);
                } else {
                    calculatorServerCall2 = new CallableCalculatorServer(calculatorServerList.get(i + 1).x, subTaskList);
                }

                asyncResponseList.add(new Request(calculatorServerCall1, executor.submit(calculatorServerCall1), requestId));
                asyncResponseList.add(new Request(calculatorServerCall2, executor.submit(calculatorServerCall2), requestId));
                possibleAnswerList.add(new HashSet<>());
                requestId++;
            }
        }

        try {
            while (asyncResponseList.size() != 0) {
                for (int i = 0; i < asyncResponseList.size(); i++) {
                    Integer result = asyncResponseList.get(i).response.get();

                    if (result != null && result != -1 && result != -2) {
                        System.out.println("Got correct response");
                        boolean isNotPresent = possibleAnswerList.get(asyncResponseList.get(i).requestId).add(result);
                        if (!isNotPresent) {
                            removeRequestsWidhtId(asyncResponseList, asyncResponseList.get(i).requestId);
                            taskTotal = (taskTotal + result) % 5000;
                            break;
                        }
                    } else if (result == -2) {
                        System.out.println("Server is unreachable, rerouting request");
                        //Server is probably disconnected, send the task to a different server randomly to even the load
                        Random random = new Random();
                        int replacementCalculatorServerId = random.nextInt(calculatorServerList.size());

                        //Change the stub used to send the request so that it uses a different server
                        asyncResponseList.get(i).callableCalculatorServer.setStub(calculatorServerList.get(replacementCalculatorServerId).x);

                    }
                    System.out.println("Resending incorrect request");
                    asyncResponseList.add(new Request(asyncResponseList.get(i).callableCalculatorServer, executor.submit(asyncResponseList.get(i).callableCalculatorServer), asyncResponseList.get(i).requestId));
                    asyncResponseList.remove(i);
                }
            }
        } catch (Exception e){
            System.out.println(e);
        }

        return taskTotal;
    }

    /**
     *
     * @param requestList
     * @param requestId
     */
    private void removeRequestsWidhtId(List<Request> requestList, int requestId) {
        requestList.removeIf(request -> request.requestId == requestId);
    }

    /**
     *
     * @return
     */
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

    /**
     *
     * @param calculationFile
     * @return
     */
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

    /**
     *
     * @param hostname
     * @return
     */
    private NameServiceInterface loadNameServiceStub(String hostname) {
        NameServiceInterface stub = null;

        System.out.println("Loading name service stub at address " + hostname);
        try {
            Registry registry = LocateRegistry.getRegistry(hostname, 5030);
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

    /**
     *
     * @param hostname
     * @return
     */
    private CalculatorServerInterface loadCalculatorServerStub(String hostname) {
        CalculatorServerInterface stub = null;

        try {
            System.out.println("Creating calculator server stub for " + hostname);

            Registry registry = LocateRegistry.getRegistry(hostname, 5030);
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

    /**
     *
     * @param serverCapacity
     * @return
     */
    //Triple server capacity which should give roughly 60% success rate
    private int getOptimalTaskSize(int serverCapacity) {
        return serverCapacity * 3;
    }

    /**
     *Classe interne Callable utilisée par un executor service pour threader les appels aux serveurs de calculs
     *
     */
    private class CallableCalculatorServer implements Callable<Integer> {

        private CalculatorServerInterface stub;
        private ArrayList<Tuple<String, Integer>> taskList;
        /**
         * Constucteur
         * @param calculatorServerInterface stub du serveur de calcul auquel on envoit l'appel
         * @param taskList liste de tuple d'opération - opérande représentant les calculs à effectuer
         */
        CallableCalculatorServer(CalculatorServerInterface calculatorServerInterface, ArrayList<Tuple<String, Integer>> taskList) {
            this.stub = calculatorServerInterface;
            this.taskList = taskList;
        }

        /**
         * Appelle le calcul des équations via le stub du calculator serveur
         * @return valeur de retour du calcul (malicieux ou non) ou -1 en cas de refus de calcul
         * @throws Exception
         */
        @Override
        public Integer call() throws Exception {
            try {
                return stub.calculateTaskList(taskList, username, password);
            } catch (Exception e) {
                System.out.println("Failed task call");
                System.out.println(e);
                return -2;
            }
        }

        public void setStub(CalculatorServerInterface stub) {
            this.stub = stub;
        }
    }
}


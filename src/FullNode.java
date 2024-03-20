// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Abdelrahmane Bekhli
// 220011666
// abdelrahmane.bekhli@city.ac.uk

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

interface FullNodeInterface {
    boolean listen(String ipAddress, int portNumber);
    void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}

public class FullNode implements FullNodeInterface {
    private ServerSocket serverSocket;
    private final Map<String, String> dataStore = new HashMap<>();
    private final Map<Integer, ArrayList<NodeInfo>> networkMap = new HashMap<>();
    private final String IPaddress = "127.0.0.1";
    private int portNumber = 0;
    private String name = "";
    private String connectingNode = "";
    private boolean closed = false;
    private final int[] usedPorts = new int[]{5040, 5939, 7680};

    private boolean checkUsedPort(int port) {
        for (int usedPort : usedPorts) {
            if (usedPort == port) {
                return true;
            }
        }
        return false;
    }

    public boolean listen(String ipAddress, int portNumber) {
        try {
            // Open a server socket to listen for incoming connections
            serverSocket = new ServerSocket(portNumber, 50, InetAddress.getByName(ipAddress));
            this.portNumber = portNumber;
            System.out.println("Listening for incoming connections on " + ipAddress + ":" + portNumber);
            return true;
        } catch (Exception e) {
            System.err.println("Exception while setting up server socket: " + e);
            return false;
        }
    }

    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        try {
            name = startingNodeName;
            NodeInfo thisNode = new NodeInfo(name, portNumber, getCurrentTime());
            ArrayList<NodeInfo> list = new ArrayList<>();
            list.add(thisNode);
            networkMap.put(0, list);


            System.out.println("Scanning for nodes on port 3000 - 5000");
            for (int port = 3000; port <= 5000; port++) {
                if (port != portNumber & !(checkUsedPort(port))) {
                    try {
                        // Create a socket and attempt to connect to the target host and port
                        Socket socket = new Socket();
                        socket.connect(new InetSocketAddress(IPaddress, port), 500);
                        // Initialize reader and writer
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        //checks if the connection was successful
                        if(sendStart(reader, writer, startingNodeName)){
                            updateNetworkMap(connectingNode, port);
                        }
                    } catch (IOException e) {
                        continue;
                    }
                }
            }

            System.out.println("Scan complete!");
            printNetworkMap();
            while (!closed) {
                Socket clientSocket = serverSocket.accept();
                // Start a new thread to handle the connection
                Thread handlerThread = new Thread(() -> handleConnection(clientSocket));
                handlerThread.start();
            }
        } catch (Exception e) {
            if(!closed) {
                System.err.println("Exception during incoming connection handling: " + e);
            }
        }
    }

    private boolean sendStart(BufferedReader reader, BufferedWriter writer, String name){
        try {
            writer.write("START 1 "+ name + "\n");
            writer.flush();

            String startMessage = reader.readLine();
            if (startMessage != null && startMessage.startsWith("START")) {
                String[] parts = startMessage.split(" ");
                this.connectingNode = parts[2];
                return true;
            }
        } catch (Exception e){
            System.out.println(e);
        }
        return false;
    }

    private boolean checkStart(BufferedReader reader, BufferedWriter writer){
        try {
            // Receive START message from the connecting node
            String startMessage = reader.readLine();
            if (startMessage.startsWith("START")) {
                // Respond with the corresponding START message
                writer.write("START 1 " + name + "\n");
                writer.flush();;
                return true;
            } else {
                // Invalid START message
                System.err.println("Invalid START message received");
            }

        } catch (Exception e) {
            System.err.println("Exception during connection handling: " + e);
        }
        return false;
    }

    private void handleConnection(Socket clientSocket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            // Receive START message from the connecting node
            if (checkStart(reader, writer)){
                handleRequests(reader, writer);
            } else {
                // Invalid START message
                System.err.println("Invalid START message received");
            }
        } catch (Exception e) {
            System.err.println("Exception during connection handling: " + e);
        }
    }

    private void handleRequests(BufferedReader reader, BufferedWriter writer) {
        try {
            // Handle requests from the connecting node
            String request;
            while ((request = reader.readLine()) != null) {
                // Process the request here
                String[] parts = request.split(" ");
                String command = parts[0];

                switch (command) {
                    case "PUT?":
                        store(parts, reader, writer);
                        break;
                    case "GET?":
                        retrieve(parts, reader, writer);
                        break;
                    case "ECHO?":
                        writer.write("OHCE\n");
                        writer.flush();
                        break;
                    case "NOTIFY?":
                        String nodeName = reader.readLine();
                        String nodeAddress = reader.readLine();
                        writer.write("NOTIFIED\n");
                        writer.flush();
                        break;
                    case "END":
                        closed = true;
                        writer.write("END\n");
                        writer.flush();
                        StringBuilder message = new StringBuilder();
                        for (int i = 1; i < parts.length; i++){
                            message.append(parts[i]).append(" ");
                        }
                        System.err.println("Closed connection with reason: " + message);
                        serverSocket.close();
                        break;
                    default:
                        System.err.println("Invalid request received: " + command);
                        // Respond with an error
                        writer.write("ERROR\n");
                        writer.flush();
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Exception during request handling: " + e);
        }
    }

    private void store(String[] parts, BufferedReader reader, BufferedWriter writer) throws IOException {
        // Initialization
        int keyLength = Integer.parseInt(parts[1]);
        int valueLength = Integer.parseInt(parts[2]);
        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        // Create key
        for (int i = 0; i < keyLength; i++) {
            keyBuilder.append(reader.readLine()).append("\n");
        }
        // Create Value
        for (int i = 0; i < valueLength; i++) {
            valueBuilder.append(reader.readLine()).append("\n");
        }
        String key = keyBuilder.toString();
        String value = valueBuilder.toString();
        // Store the key-value pair
        dataStore.put(key.trim(), value);
        // Respond with success
        writer.write("SUCCESS\n");
        writer.flush();
    }
    private void retrieve(String[] parts, BufferedReader reader, BufferedWriter writer) {
        try {
            // Initialization
            int keyLength = Integer.parseInt(parts[1]);
            StringBuilder value = new StringBuilder();
            //get value of each key
            for(int i = 0; i < keyLength; i++) {
                String key = reader.readLine();
                if (dataStore.containsKey(key)) {
                    value.append(dataStore.get(key).trim());
                    String[] lines = value.toString().split("\n");
                    int numberOfLines = lines.length;
                    writer.write("VALUE " + numberOfLines + "\n" + value + "\n");
                    writer.flush();
                } else{
                    writer.write("NOPE" + "\n");
                    writer.flush();
                }
            }
        } catch (Exception e) {
            System.out.println("Error at retrieve: " + e);
        }
    }

    private String getCurrentTime(){
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return currentTime.format(formatter);
    }

    private synchronized void updateNetworkMap(String nodeName, int port) {
        //if(Objects.equals(nodeName.split(":")[1], "FullNode")) {
            NodeInfo node = new NodeInfo(nodeName, port, getCurrentTime());
            ArrayList<NodeInfo> list = new ArrayList<>();
            list.add(node);

            //find distance
            try {
                int current_dist = HashID.getDistance(name + "\n", nodeName + "\n");
                // if node is not in network map
                if (networkMap.get(current_dist) == null) {
                    networkMap.put(current_dist, list);
                }
                // If direction (distance) has less than 3 nodes in it
                else if (networkMap.get(current_dist).size() < 3) {
                    networkMap.get(current_dist).add(node);
                }
                // remove the shortest running node
                else {
                    // Initialization
                    NodeInfo toRemove = null;
                    LocalTime minLastRunTime = LocalTime.MAX;

                    // find node with the shortest time
                    for (NodeInfo n : networkMap.get(current_dist)) {
                        String nodeTimeString = n.getStartTime();
                        LocalTime nodeTime = LocalTime.parse(nodeTimeString, DateTimeFormatter.ofPattern("HH:mm:ss"));

                        if (nodeTime.isBefore(minLastRunTime)) {
                            minLastRunTime = nodeTime;
                            toRemove = n;
                        }
                    }
                    // remove node
                    if (toRemove != null) {
                        networkMap.get(current_dist).remove(toRemove);
                        networkMap.get(current_dist).add(node);
                    }
                }
            } catch (Exception e) {
                System.out.println("Error at update Network map: " + e);
            }
        //}
    }
    private void printNetworkMap(){
        for (Map.Entry<Integer, ArrayList<NodeInfo>> entry : networkMap.entrySet()) {
            int key = entry.getKey();
            ArrayList<NodeInfo> nodeList = entry.getValue();

            System.out.print(key + ": [");
            for (int i = 0; i < nodeList.size(); i++) {
                NodeInfo node = nodeList.get(i);
                System.out.print("[" + node.getNodeName() + ", " + node.getPort() + ", " + node.getStartTime() + "]");
                if (i < nodeList.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
        }
    }
}

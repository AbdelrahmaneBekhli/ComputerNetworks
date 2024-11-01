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
import java.util.*;
import java.util.concurrent.RecursiveTask;

// DO NOT EDIT starts
interface FullNodeInterface {
    public boolean listen(String ipAddress, int portNumber);
    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}
// DO NOT EDIT ends


public class FullNode implements FullNodeInterface {
    private ServerSocket serverSocket;
    private final Map<String, String> dataStore = new HashMap<>();
    private Map<Integer, ArrayList<NodeInfo>> networkMap;
    private String IpAddress = "127.0.0.1";
    private int portNumber = 0;
    private String name = "";
    private String connectingNode = "";
    private final ArrayList<Socket> connectedSockets = new ArrayList<>();
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
            InetAddress addr = InetAddress.getByName(ipAddress);
            serverSocket = new ServerSocket(portNumber, 50, addr);



            this.portNumber = portNumber;
            this.IpAddress = ipAddress;
            return true;
        } catch (Exception e) {
            System.err.println("Exception while setting up server socket: " + e);
            return false;
        }
    }

    private void activeMapping(String nodeAddress){
        networkMap = new HashMap<>();
        NodeInfo thisNode = new NodeInfo(serverSocket, name, portNumber, getCurrentTime(), nodeAddress);
        ArrayList<NodeInfo> list = new ArrayList<>();
        list.add(thisNode);
        networkMap.put(0, list);
        System.out.println("Scanning for nodes on port " + (portNumber - 500) + " to " + (portNumber + 500));
        for (int port = portNumber - 500; port <= portNumber + 500; port++) {
            if (port != portNumber & !(checkUsedPort(port))) {
                try {
                    // Create a socket and attempt to connect to the target host and port
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(IpAddress, port), 100);
                    // Initialize reader and writer
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                    BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));


                    //checks if the connection was successful
                    if(sendStart(reader, writer)) {
                        if (notifyNode(reader, writer)) {
                            updateNetworkMap(socket,connectingNode, port, IpAddress+":"+port);
                        }
                    }
                } catch (Exception e) {
                    //ignore offline nodes
                }
            }
        }
        System.out.println("Scan complete!");
    }

    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        try {
            name = startingNodeName;
            activeMapping(startingNodeAddress);
            System.out.println("Listening for incoming connections on " + IpAddress + ":" + portNumber);
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

    private boolean notifyNode(BufferedReader reader, BufferedWriter writer){
        try{
            writer.write("NOTIFY?\n" + name + "\n" + IpAddress + ":" + portNumber + "\n");
            writer.flush();

            String response = reader.readLine();
            if(response.startsWith("NOTIFIED")){
                return true;
            }

        } catch (Exception e){
            System.err.println("Exception during notify node: " + e);
        }
        return false;
    }

    private boolean sendStart(BufferedReader reader, BufferedWriter writer){
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
            System.out.println("Exception during sending start request: " + e);
        }
        return false;
    }

    private boolean checkStart(BufferedReader reader, BufferedWriter writer, Socket socket){
        try {
            // Receive START message from the connecting node
            String startMessage = reader.readLine();
            if(checkName(startMessage.split(" ")[2], writer, socket)) {
                if (startMessage.startsWith("START")) {
                    // Respond with the corresponding START message
                    writer.write("START 1 " + name + "\n");
                    writer.flush();

                    return true;
                } else {
                    // Invalid START message
                    writer.write("END Invalid START message\n");
                    writer.flush();
                    socket.close();
                    return false;
                }
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
            if (checkStart(reader, writer, clientSocket)){
                handleRequests(reader, writer, clientSocket);
            }
        } catch (Exception e) {
            System.err.println("Exception during connection handling: " + e);
        }
    }

    private void handleRequests(BufferedReader reader, BufferedWriter writer, Socket socket) {
        try {
            // Handle requests from the connecting node
            String request;
            while ((request = reader.readLine()) != null) {
                // Process the request
                String[] parts = request.split(" ");
                String command = parts[0];

                switch (command) {
                    case "PUT?":
                        store(parts, reader, writer);
                        break;
                    case "GET?":
                        retrieve(parts, reader, writer, socket);
                        break;
                    case "ECHO?":
                        writer.write("OHCE\n");
                        writer.flush();
                        break;
                    case "NOTIFY?":
                        String nodeName = reader.readLine();
                        String nodeAddress = reader.readLine();
                        // Check if name is in a valid format
                        if(checkName(nodeName, writer, socket)) {
                            updateNetworkMap(socket, nodeName, Integer.parseInt(nodeAddress.split(":")[1]), nodeAddress);
                            writer.write("NOTIFIED\n");
                            writer.flush();
                        }
                        break;
                    case "NEAREST?":
                        String key = parts[1];
                        ArrayList<NodeInfo>nearestNodes = findNearest(key);
                        writer.write("NODES " + nearestNodes.size() + "\n");
                        for (NodeInfo node : nearestNodes) {
                            writer.write(node.getNodeName() + "\n");
                            writer.write(node.getAddress() + "\n");
                        }
                        writer.flush();
                        break;
                    case "END":
                        writer.write("END\n");
                        writer.flush();
                        StringBuilder message = new StringBuilder();
                        for (int i = 1; i < parts.length; i++){
                            message.append(parts[i]).append(" ");
                        }
                        removeNode(socket.getPort());
                        socket.close();
                        break;
                    default:
                        // Respond with an error
                        writer.write("END Invalid request received\n");
                        writer.flush();
                        socket.close();
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Exception during connection handling requests: " + e);
        }
    }

    private boolean checkName(String name, BufferedWriter writer, Socket socket) throws IOException {
        try{
            String email = name.split("@")[0];
            try{
                String details = name.split(":")[1];
                try {
                    String[] parts = details.split(",");
                    return true;
                } catch (Exception e){
                    writer.write("END name must have a comma\n");
                    writer.flush();
                    socket.close();
                }
            } catch (Exception e){
                writer.write("END name must have a colon\n");
                writer.flush();
                socket.close();
            }
        } catch (Exception e){
            writer.write("END name must have a valid email address\n");
            writer.flush();
            socket.close();
        }
        return false;
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
        ArrayList<NodeInfo> nearestNodes = findNearest(HashID.hexHash(key));
        boolean closet = false;
        for(NodeInfo n: nearestNodes) {
            if (Objects.equals(n.getNodeName(), name) && n.getPort() == portNumber) {
                closet = true;
                break;
            }
        }
        if (closet) {
            dataStore.put(key.trim(), value);
            // Respond with success
            writer.write("SUCCESS\n");
            writer.flush();
        } else {
            // Respond with FAILED
            writer.write("FAILED\n");
            writer.flush();
        }

    }
    private void retrieve(String[] parts, BufferedReader reader, BufferedWriter writer, Socket s) {
        try {
            // Initialization
            int keyLength = Integer.parseInt(parts[1]);
            StringBuilder value = new StringBuilder();
            StringBuilder keyBuilder = new StringBuilder();
            // Get value of each key
            for(int i = 0; i < keyLength; i++) {
                keyBuilder.append(reader.readLine()).append("\n");
            }
            String key = keyBuilder.toString().trim();

            if (dataStore.containsKey(key)) {
                value.append(dataStore.get(key).trim());
                String[] lines = value.toString().split("\n");
                int numberOfLines = lines.length;
                writer.write("VALUE " + numberOfLines + "\n" + value + "\n");
                writer.flush();
            } else {
                writer.write("NOPE\n");
                writer.flush();
            }
        } catch (Exception e) {
            System.out.println("Error at retrieve: " + e);
        }
    }

    private ArrayList<NodeInfo> findNearest(String key) {
        ArrayList<ArrayList<Object>> distances = new ArrayList<>();
        for (Map.Entry<Integer, ArrayList<NodeInfo>> entry : networkMap.entrySet()) {
            ArrayList<NodeInfo> nodeList = entry.getValue();
            for (NodeInfo node : nodeList) {
                int distance = HashID.getDistance(HashID.hexHash(node.getNodeName() + "\n"), key);
                ArrayList<Object> pair = new ArrayList<>();
                pair.add(distance);
                pair.add(node);
                distances.add(pair);
            }
        }

        // Define a custom comparator to compare distances [[0,node],[1,node2]] the integer is the custom comparator
        Comparator<ArrayList<Object>> comparator = new Comparator<ArrayList<Object>>() {
            @Override
            public int compare(ArrayList<Object> pair1, ArrayList<Object> pair2) {
                int distance1 = (int) pair1.get(0);
                int distance2 = (int) pair2.get(0);
                return Integer.compare(distance1, distance2);
            }
        };

        // Sort the distances ArrayList using the custom comparator
        distances.sort(comparator);


        // Create a new array containing the first 'nodeCount' nodes
        ArrayList<NodeInfo> nearestNodes = new ArrayList<>();
        int count = 0;
        for (ArrayList<Object> pair : distances) {
            nearestNodes.add((NodeInfo) pair.get(1));
            count++;
            if (count == 3) {
                break;
            }
        }
        return nearestNodes;

    }

    private String getCurrentTime(){
        LocalDateTime currentTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return currentTime.format(formatter);
    }

    // Find node ID
    private int findID(String nodeName){
        for (Map.Entry<Integer, ArrayList<NodeInfo>> entry : networkMap.entrySet()) {
            ArrayList<NodeInfo> nodeList = entry.getValue();
            for (NodeInfo node : nodeList) {
                if (Objects.equals(node.getNodeName(), nodeName)) {
                    return node.getID();
                }
            }
        }
        return 0;
    }

    // Find node's socket
    private boolean searchSocket(int port){
        for(Socket s: connectedSockets){
            if(s.getPort() == port){
                return true;
            }
        }
        return false;
    }

    private void updateNetworkMap(Socket socket, String nodeName, int port, String address) {
        String nodeType;
        try {
            nodeType = nodeName.split(",")[1];
        } catch (Exception e){
            nodeType = "tempNode";
        }
        if(nodeType.startsWith("fullNode")) {
            NodeInfo node = new NodeInfo(socket, nodeName, port, getCurrentTime(), address);
            ArrayList<NodeInfo> list = new ArrayList<>();
            list.add(node);
            if (!searchSocket(socket.getPort())) {
                connectedSockets.add(socket);

                //find distance
                try {
                    int current_dist = HashID.getDistance(HashID.hexHash(name + "\n"), HashID.hexHash(nodeName + "\n"));
                    if (current_dist != 0) {
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
                    }
                } catch (Exception e) {
                    System.out.println("Error at update Network map: " + e);
                }
            printNetworkMap();
            }
        }
    }

    private void removeNode(int toRemove) {
        // Iterate over the map entries
        for (Map.Entry<Integer, ArrayList<NodeInfo>> entry : networkMap.entrySet()) {
            Integer key = entry.getKey();
            ArrayList<NodeInfo> nodeList = entry.getValue();

            // Iterate over the list of NodeInfo objects
            Iterator<NodeInfo> iterator = nodeList.iterator();
            while (iterator.hasNext()) {
                NodeInfo node = iterator.next();
                // Check if the ID matches the ID to remove
                if (node.getID() == toRemove) {
                    // Remove the node from the list
                    iterator.remove();
                    // If the list becomes empty after removal, remove the key from the map
                    if (nodeList.isEmpty()) {
                        networkMap.remove(key);
                    }
                    // Exit the loop after removing the node
                    break;
                }
            }
        }
    }
    private void printNetworkMap(){
        for (Map.Entry<Integer, ArrayList<NodeInfo>> entry : networkMap.entrySet()) {
            int key = entry.getKey();
            ArrayList<NodeInfo> nodeList = entry.getValue();

            System.out.print(key + ": [");
            for (int i = 0; i < nodeList.size(); i++) {
                NodeInfo node = nodeList.get(i);
                System.out.print("[" +node.getID() + ", " + node.getNodeName() + ", " + node.getPort() + ", " + node.getStartTime() + "]");
                if (i < nodeList.size() - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println("]");
        }
    }
}

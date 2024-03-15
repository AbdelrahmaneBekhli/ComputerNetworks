import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

interface FullNodeInterface {
    boolean listen(String ipAddress, int portNumber);
    void handleIncomingConnections(String startingNodeName, String startingNodeAddress);
}

public class FullNode implements FullNodeInterface {

    private ServerSocket serverSocket;
    private final Map<String, String> dataStore = new HashMap<>();

    private boolean closed = false;

    public boolean listen(String ipAddress, int portNumber) {
        try {
            // Open a server socket to listen for incoming connections
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Listening for incoming connections on " + ipAddress + ":" + portNumber);
            return true;
        } catch (Exception e) {
            System.err.println("Exception while setting up server socket: " + e);
            return false;
        }
    }

    public void handleIncomingConnections(String startingNodeName, String startingNodeAddress) {
        try {
            while (!closed) {
                // Accept incoming connections
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted incoming connection from: " + clientSocket.getInetAddress());

                // Start a new thread to handle the connection
                Thread handlerThread = new Thread(() -> handleConnection(clientSocket));
                handlerThread.start();
            }
        } catch (Exception e) {
            System.err.println("Exception during incoming connection handling: " + e);
        }
    }

    private void handleConnection(Socket clientSocket) {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))
        ) {
            // Receive START message from the connecting node
            String startMessage = reader.readLine();
            if (startMessage != null && startMessage.startsWith("START")) {
                // Extract the protocol version and node name
                String[] parts = startMessage.split(" ");
                int protocolVersion = Integer.parseInt(parts[1]);
                String nodeName = parts[2];

                // Respond with the corresponding START message
                writer.write("START " + protocolVersion + " " + nodeName + "\n");
                writer.flush();

                // Handle further communication with the connecting node
                handleRequests(reader, writer);
            } else {
                // Invalid START message
                System.err.println("Invalid START message received");
            }

            // Close the connection
            clientSocket.close();
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
                    default:
                        System.err.println("Invalid request received");
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
        int keyLength = Integer.parseInt(parts[1]);
        int valueLength = Integer.parseInt(parts[2]);
        StringBuilder keyBuilder = new StringBuilder();
        StringBuilder valueBuilder = new StringBuilder();
        for (int i = 0; i < keyLength; i++) {
            keyBuilder.append(reader.readLine()).append("\n");
        }
        for (int i = 0; i < valueLength; i++) {
            valueBuilder.append(reader.readLine()).append("\n");
        }
        String key = keyBuilder.toString();
        String value = valueBuilder.toString();
        try {
            byte[] b = HashID.computeHashID(value);
            System.out.println(Arrays.toString(b));
        } catch (Exception e){
            System.out.println("hmmmmmmmmmmmm");
        }
        // Store the key-value pair
        dataStore.put(key.trim(), value);
        // Respond with success
        writer.write("SUCCESS\n");
        writer.flush();
    }
    private void retrieve(String[] parts, BufferedReader reader, BufferedWriter writer) {
        try {
            boolean end = false;
            int keyLength = Integer.parseInt(parts[1]);
            StringBuilder value = new StringBuilder();
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

            // Print the key
            //System.out.println("Received key: " + key);
        } catch (Exception e) {
            try {
                writer.write("NOPE");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}

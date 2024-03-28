// IN2011 Computer Networks
// Coursework 2023/2024
//
// Submission by
// Abdelrahmane Bekhli
// 220011666
// abdelrahmane.bekhli@city.ac.uk
import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

// DO NOT EDIT starts
interface TemporaryNodeInterface {
    public boolean start(String startingNodeName, String startingNodeAddress);
    public boolean store(String key, String value);
    public String get(String key);
}
// DO NOT EDIT ends
public class TemporaryNode implements TemporaryNodeInterface {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private static boolean open = true;
    public boolean start(String startingNodeName, String startingNodeAddress) {
        try {
            // Connect to the starting node
            socket = new Socket(startingNodeAddress.split(":")[0], Integer.parseInt(startingNodeAddress.split(":")[1]));
            // Initialize reader and writer
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            // Send START message
            writer.write("START 1 " + startingNodeName + "\n");
            writer.flush();
            // Receive START message from the starting node
            String startResponse = reader.readLine();
            if (startResponse != null && startResponse.startsWith("START")) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Exception during node start: " + e);
        }
        return false;
    }
    public boolean store(String key, String value) {
        try {
            if(key.split("\n").length >= 1) {
                if(value.split("\n").length >= 1) {
                    // Construct PUT? request message
                    writer.write("PUT? " + key.split("\n").length + " " + value.split("\n").length + "\n" + key + value);
                    writer.flush();
                    // Receive response
                    String response = reader.readLine();
                    if (response != null && response.equals("SUCCESS")) {
                        return true;
                    } else {
                        System.err.println("Store operation failed. Server response: " + response);
                    }
                } else{
                    System.err.println("Error at Store: Invalid number of lines of value");
                }
            } else{
                System.err.println("Error at Store: Invalid number of lines of key");
            }
        } catch (Exception e) {
            System.err.println("Exception during store operation: " + e);
        }
        return false;
    }
    public String get(String key) {
        String value;
        try {
            if(key.split("\n").length >= 1) {
                // Send GET? request
                writer.write("GET? " + key.split("\n").length + "\n" + key);
                writer.flush();
                // Receive response
                String response = reader.readLine();  // Read the response line
                if (response.startsWith("VALUE")) {
                    System.out.println("found");
                    System.out.println("reply: " + response);
                    return readValues(reader, Integer.parseInt(response.split(" ")[1]));
                    // If first node doesn't have the values, ask the nearest nodes
                } else if (response.startsWith("NOPE")) {
                    System.out.println("not found");
                    HashMap<String, String> nearestNodes = nearest(key, writer, reader);
                    return askNearest(key, nearestNodes, new HashSet<>());
                }
            } else {
                System.err.println("Error at Get: Invalid number of lines of key");
            }
        } catch (Exception e) {
            System.err.println("Exception during get operation: " + e);
        }
        return null;
    }

    private String askNearest(String key, HashMap<String, String> nearestNodes, Set<String> visited){
        try {
            for (HashMap.Entry<String, String> entry : nearestNodes.entrySet()) {
                String name = entry.getKey();
                String address = entry.getValue();
                System.out.println(name);
                if (!visited.contains(name)) {
                    visited.add(name);
                    // Attempt to connect to nodes
                    Socket tempSocket = new Socket(address.split(":")[0], Integer.parseInt(address.split(":")[1]));
                    BufferedReader tempReader = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));
                    BufferedWriter tempWriter = new BufferedWriter(new OutputStreamWriter(tempSocket.getOutputStream()));
                    tempWriter.write("START 1 " + name + "\n");
                    tempWriter.flush();
                    // If connection was successful
                    String startReply = tempReader.readLine();
                    if (startReply.startsWith("START")) {

                        tempWriter.write("GET? " + key.split("\n").length + "\n" + key);
                        tempWriter.flush();

                        String reply = tempReader.readLine();
                        if (reply.startsWith("VALUE")) {
                            return readValues(tempReader, Integer.parseInt(reply.split(" ")[1]));
                        } else {
                            askNearest(key, nearest(key, tempWriter, tempReader), visited);
                        }
                    }
                }
            }
        } catch (Exception e){
            System.err.println(e);
        }
        return null;
    }
    private String readValues(BufferedReader reader, int numberOfLines) {
        try {
            System.out.println("number of lines: " + numberOfLines);
            StringBuilder value = new StringBuilder();
            for (int i = 0; i < numberOfLines; i++) {
                String v = reader.readLine();
                value.append(v).append("\n");
            }
            // Remove extra \n at the end of the value
            if (value.charAt(value.length() - 1) == '\n') {
                value.deleteCharAt(value.length() - 1);
            }
            System.out.println("Value: " + value);
            return value.toString();
        } catch (Exception e){
            System.out.println("Exception during get reading values: " + e);
        }
        return null;
    }
    public boolean echo(){
        try {
            writer.write("ECHO?\n");
            writer.flush();
            String response = reader.readLine();  // Read the response line
            if (response != null && response.startsWith("OHCE")){
                return true;
            }
        } catch (Exception e) {
            System.err.println("Exception during echo operation: " + e);
        }
        return false;
    }
    public Boolean notifyRequest(String startingNodeName, String startingNodeAddress){
        try{
            writer.write("NOTIFY?\n" + startingNodeName + "\n" + startingNodeAddress + "\n");
            writer.flush();
            String response = reader.readLine();  // Read the response line
            if (response.startsWith("NOTIFIED")){
                return true;
            }
        } catch (Exception e){
            System.out.println("Exception during notify operation: " + e);
        }
        return false;
    }
    public HashMap<String, String> nearest(String key, BufferedWriter w, BufferedReader r){
        HashMap<String,String> nodes = new HashMap<>();
        try{
            String hashedKey = HashID.hexHash(key);
            w.write("NEAREST? " + hashedKey + "\n");
            w.flush();
            String response = r.readLine();
            if(response.startsWith("NODES")){
                int nodesNum = Integer.parseInt(response.split(" ")[1]);
                for(int i = 0; i < nodesNum; i++){
                    String name = r.readLine();
                    String address = r.readLine();
                    nodes.put(name, address);
                }
            }
        } catch (Exception e){
            System.err.println("Exception during nearest operation: " + e);
        }
        return nodes;
    }
    public void end(String reason){
        try{
            writer.write("END " + reason + "\n");
            writer.flush();
            String response = reader.readLine();  // Read the response line
            if (response.startsWith("END")){
                socket.close();
                open = false;
            }
        } catch(Exception e){
            System.err.println(e);
        }
    }
}
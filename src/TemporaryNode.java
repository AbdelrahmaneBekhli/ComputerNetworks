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
        try {
            if(key.split("\n").length >= 1) {
                // Send GET? request
                writer.write("GET? " + key.split("\n").length + "\n" + key + "\n");
                writer.flush();
                // Receive response
                String response = reader.readLine();  // Read the response line
                if (response != null && response.startsWith("VALUE")) {
                    int numberOfLines = Integer.parseInt(response.split(" ")[1]);
                    StringBuilder value = new StringBuilder();
                    for (int i = 0; i < numberOfLines; i++) {
                        String v = reader.readLine();
                        value.append(v).append("\n");
                    }
                    //remove extra \n at the end of the value
                    if (!value.isEmpty() && value.charAt(value.length() - 1) == '\n') {
                        value.deleteCharAt(value.length() - 1);
                    }
                    return value.toString();
                } else if (response != null && response.equals("NOPE")) {
                    return null;
                }
            } else {
                System.err.println("Error at Get: Invalid number of lines of key");
            }

        } catch (Exception e) {
            System.err.println("Exception during get operation: " + e);
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
            System.err.println("Exception during end operation: " + e);
        }
    }
}

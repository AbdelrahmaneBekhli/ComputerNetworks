import java.net.ServerSocket;
import java.net.Socket;

class NodeInfo {
    private final int ID;
    private final String nodeName;
    private final int port;
    private final String StartTime;
    private final String address;

    private Socket socket;
    private ServerSocket serverSocket;

    public NodeInfo(Socket socket, String nodeName, int port, String startTime, String address) {
        this.ID = socket.getPort();
        this.nodeName = nodeName;
        this.port = port;
        this.StartTime = startTime;
        this.address = address;
        this.socket = socket;
    }

    public NodeInfo(ServerSocket socket, String nodeName, int port, String startTime, String address) {
        this.ID = socket.getLocalPort();
        this.nodeName = nodeName;
        this.port = port;
        this.StartTime = startTime;
        this.address = address;
        this.serverSocket = socket;
    }

    public String getNodeName() {
        return nodeName;
    }
    public String getAddress() {
        return address;
    }
    public String getStartTime() {
        return StartTime;
    }
    public int getPort() {return port;}
    public int getID() {return ID;}
    public Socket getClientSocket() { return socket;}
}

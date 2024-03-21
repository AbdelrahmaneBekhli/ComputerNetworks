class NodeInfo {
    private final int ID;
    private final String nodeName;
    private final int port;
    private final String StartTime;
    private final String address;

    public NodeInfo(int ID, String nodeName, int port, String startTime, String address) {
        this.ID = ID;
        this.nodeName = nodeName;
        this.port = port;
        this.StartTime = startTime;
        this.address = address;
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
}

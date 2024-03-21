class NodeInfo {
    private final int ID;
    private final String nodeName;
    private final int port;
    private final String StartTime;

    public NodeInfo(int ID, String nodeName, int port, String startTime) {
        this.ID = ID;
        this.nodeName = nodeName;
        this.port = port;
        this.StartTime = startTime;
    }

    public String getNodeName() {
        return nodeName;
    }

    public int getPort() {
        return port;
    }
    public String getStartTime() {
        return StartTime;
    }
    public int getID() {
        return ID;
    }
}

class NodeInfo {
    private final String nodeName;
    private final int port;

    private final String StartTime;

    public NodeInfo(String nodeName, int port, String startTime) {
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
}

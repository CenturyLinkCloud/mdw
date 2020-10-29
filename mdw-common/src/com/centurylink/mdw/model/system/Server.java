package com.centurylink.mdw.model.system;

import java.net.URL;
import java.util.Map;

public class Server {

    private String host;
    public String getHost() {
        return host;
    }

    private int port;
    public int getPort() {
        return port;
    }

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public Server(String hostPort) {
        int colon = hostPort.indexOf(':');
        if (colon == -1) {
            this.host = hostPort;
        }
        else {
            this.host = hostPort.substring(0, colon);
            this.port = Integer.parseInt(hostPort.substring(colon + 1));
        }
    }

    public Server(URL url) {
        this.host = url.getHost();
        this.port = url.getPort();
        if (this.port == -1) {
            this.port = url.getProtocol().equalsIgnoreCase("https") ? 443 : 80;
        }
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Server) {
            Server otherServer = (Server)other;
            return otherServer.host.equals(host) && otherServer.port == port;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }
}

package ece454_project2;

import java.io.Serializable;

public class PeerInfo implements Serializable{
    String host;
    int port;
    
    public PeerInfo(String h, int p) {
        host = h;
        port = p;
    }
}

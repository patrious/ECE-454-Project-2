/*
 * Contains the information required to request a file from another client on 
 * the VFS network.
 */
package ece454_project2;

import java.io.Serializable;
import java.util.UUID;

public class FileDataRequest implements Serializable {
    public String host;
    public int port;
    public UUID file;
    
    public FileDataRequest(UUID uuid, String h, int p) {
        host = h;
        port = p;
        file = uuid;
    }
}

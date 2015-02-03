/*
 * Data type used to request a file list from another host. When an object of 
 * this type is received (as detected by Sender), reply with a FileList object.
 */
package ece454_project2;

import java.io.Serializable;

public class FileListRequest implements Serializable {
    public String host;
    public int port;
    
    public FileListRequest(String h, int p) {
        host = h;
        port = p;
    }
}

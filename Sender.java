/*
 * The Sender is responsible for sending a single data object to the given 
 * socket. This can be anything; it's just sent as an Object.
 * 
 * This is used when the NetworkManager starts up to send FileListRequests to 
 * each other client on the VFS network or by the FileManager to send a 
 * FileDataRequest.
 */

package ece454_project2;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

public class Sender implements Runnable {
    private Socket socket;
    private Object data;
    
      public Sender(ArrayList<PeerInfo> locations, Object obj) {
        boolean succeed = false;
        for (PeerInfo location : locations) {
            try {                
                socket = new Socket(location.host, location.port);
                socket.setSoTimeout(1000);
                succeed = true;
            } catch (IOException ex) {
                succeed = false;
                System.out.println("Sender: Unable to connect to " + location.host + ":" + location.port);
            }
            data = obj;
            if (succeed) {
                break;
            }
        }
    }
    public Sender(String host, int port, Object obj) {
        try {
            socket = new Socket(host, port);
            socket.setSoTimeout(1000);
        } catch (IOException ex) {
            //Other host isn't on, ignore
        }
        data = obj;
    }
    
    @Override public void run() {
        try {
            if (socket != null) {
                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                oos.writeObject(data);
                oos.flush();

                oos.close();
                socket.close();
            }
        } catch (SocketException ex) {
            //Occurs if other client disconnects while socket is still open, ignore
        } catch (Exception ex) {
            System.out.println("Sender: Error");
            ex.printStackTrace();
        }
    }
}

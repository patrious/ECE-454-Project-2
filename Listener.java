/*
 * Listener just listens on a given port for incoming connections. When a 
 * connection request comes in, it creates a Receiver with that socket.
 */

package ece454_project2;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Listener implements Runnable {
    private ServerSocket listener;
    private FileManager fm;
    
    public Listener(int port, FileManager filemgr) {
        fm = filemgr;
        try {
            listener = new ServerSocket(port);
            listener.setSoTimeout(1000);
        } catch (IOException ex) {
            System.out.println("Listener: Error binding to port " + port);
            ex.printStackTrace();
        }
    }
    
    @Override public void run() {
        Socket socket;
        
        while (true) {
            try {
                //Accept incoming connection
                socket = listener.accept();
                
                //Create server connection thread to wait for incoming file
                Thread t = new Thread(new Receiver(socket, fm));
                t.start();
            } catch (IOException ex) {
                //Caused by timeout, ignore
            }
            //Listener thread can be interrupted by NetMgr when client shuts down
            if (Thread.interrupted()) {
                try {
                    listener.close();
                } catch (IOException ex) {
                    System.out.println("Listener: Error shutting down server.");
                }
            }
        }
    }
}

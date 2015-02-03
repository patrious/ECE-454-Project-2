package ece454_project2;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class NetworkManager {
    int listenerPort;
    public String localIP;
    private FileManager fm;
    private Thread listenerThread;
    public ArrayList<PeerInfo> knownPeers = new ArrayList<>();
    
    public NetworkManager() {
    }
    
    public void StartManager(FileManager fileMgr, int port) {
        listenerPort = port;
        fm = fileMgr;
        
        listenerThread = new Thread(new Listener(listenerPort, fm));
        listenerThread.start();
        
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("peerlist.txt"));
        } catch (FileNotFoundException ex) {
            try {
                br = new BufferedReader(new FileReader("../peerlist.txt"));
            } catch (FileNotFoundException ex2) {
                System.err.println("peerlist.txt file not found!");
                System.exit(-1);
            }
        }
        try {
            /*
             * Bit of an odd way to do it, but this gets the local IP
             */
            Socket s = new Socket("google.ca", 80);
            localIP = s.getLocalAddress().getHostAddress();
            s.close();
            
            System.out.println("Me: " + localIP + ":" + listenerPort);
            
            /* 
             * Go through the list of other peers, and get their file lists.
             */
            String line = br.readLine();
            while (line != null) {
                String peerHost = line.split(",")[0];
                int peerPort = Integer.parseInt(line.split(",")[1]);
                
                //If it isn't us, then connect to it
                if (!localIP.equals(peerHost) || peerPort != listenerPort) {
                    knownPeers.add(new PeerInfo(peerHost, peerPort));
                    Thread t = new Thread(new Sender(peerHost, peerPort, 
                            new FileListRequest(localIP, listenerPort)));
                    t.start();
                }
                
                line = br.readLine();
            }
            
            br.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
       public void getFileData(UUID uuid, ArrayList<PeerInfo> locations) {
        System.out.println("NM: Requesting " + uuid + " from lots of places");
        locations.remove(GetLocalIpAddr());
        Thread t = new Thread(new Sender(locations, new FileDataRequest(uuid, localIP, listenerPort)));
        t.start();
    }
    //Gets the specified file
    public void getFileData(UUID uuid, String host, int port) {
        Thread t = new Thread(new Sender(host, port, 
                new FileDataRequest(uuid, localIP, listenerPort)));
        t.start();
    }
    
    public void removeFile(UUID uuid) {
        for (PeerInfo peer : knownPeers) {
            Thread t = new Thread(new Sender(peer.host, peer.port, new RemoveFile(uuid)));
            t.start();
        }
    }
    
    public void updateOtherFileLists() {
        for (PeerInfo peer : knownPeers) {
            Thread t = new Thread(new Sender(peer.host, peer.port, 
                    fm.FileListContainer.fileList));
            t.start();
        }
    }
    
    public PeerInfo GetLocalIpAddr() {
        return new PeerInfo(localIP, listenerPort);
    }
    
    public void StopManager() {
        listenerThread.interrupt();
    }
}
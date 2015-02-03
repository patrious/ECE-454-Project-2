/*
 * Receiver is created with a socket from Listener. It will receive a 
 * single object via the connection. The object is checked to see if it's a 
 * file list, or a file, and is dealt with accordingly. The Receiver then 
 * closes the connection and terminates the thread (only one object is received 
 * by the Receiver).
 */

package ece454_project2;

import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.HashMap;

public class Receiver extends Thread {
    Socket socket;
    FileManager fm;
    
    public Receiver(Socket sock, FileManager filemgr) {
        socket = sock;
        fm = filemgr;
    }
    
    @Override public void run() {
        /*
         * In general, we're only sending one object at a time (whether it be a 
         * file list, Object containing a requested file, etc.), so just read 
         * the input stream, give the data to the file manager, and close the 
         * connection.
         */
        try {
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            
            Object input = new Object();
            //Get the aforementioned one object
            try {
                input = ois.readObject();
            } catch (Exception ex) {
                System.out.println("Receiver: Error reading input buffer.");
                ex.printStackTrace();
            }
            
            //Now check what we just received, to figure out what to do with it
            if (input instanceof HashMap) {
                //If we're receiving a FileList, it means somebody else updated
                //theirs, so update ours.
                fm.UpdateFileList((HashMap)input);
            } else if (input instanceof FileData) {
                //Indicates that a file has been sent to us, unpack as required
                fm.fileRecieved((FileData)input);
            } else if (input instanceof FileListRequest) {
                //If we get a request for the file list, send back our file list
                FileListRequest flr = (FileListRequest)input;
                Thread t = new Thread(new Sender(flr.host, flr.port, 
                        fm.FileListContainer.fileList));
                t.start();
            } else if (input instanceof FileDataRequest) {
                //Received a request for a file, call the FileManager's sendFile
                //Get FileData object from FileManager
                FileDataRequest fdr = (FileDataRequest)input;
                FileData fd = fm.ReadDiskFromFile(fdr.file);
                //Create Sender thread, send FileData
                Thread t = new Thread(new Sender(fdr.host, fdr.port, fd));
                t.start();
            } else if (input instanceof RemoveFile) {
                //Received instruction to remove a file
                RemoveFile rf = (RemoveFile) input;
                fm.RemoveFile(rf.file);
            }
            
            ois.close();
            socket.close();
        } catch (SocketException ex) {
            //Occurs if the other peer disconnects before we do
        } catch (SocketTimeoutException ex) {
            //Caused by getObject() not getting anything
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}

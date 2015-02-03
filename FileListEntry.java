package ece454_project2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author patrious
 */
public class FileListEntry implements Serializable{
    public UUID fileID;
    public String filename;
    public int version;
    public Date timestamp;
    public byte[] fileHash;
    public ArrayList<PeerInfo> locations = new ArrayList<>();
     
    public FileListEntry(UUID FID, String FN, int ver, byte[] Hash, ArrayList<PeerInfo> peerInfos)
    {
        fileID = FID;
        filename = FN;
        version = ver;
        fileHash = Hash;
        timestamp = new Date();
        locations.addAll(peerInfos);
    }

    FileListEntry(UUID FID, String FN, int ver, byte[] Hash, PeerInfo peerInfo) {
        fileID = FID;
        filename = FN;
        version = ver;
        fileHash = Hash;
        timestamp = new Date();
        locations.add(peerInfo);
        //TODO: Add yourself to this item, locations.addAll(Locations);
    }
    
    @Override public String toString()
    {   
        return String.format("%s, %s, %s, %s, %s", filename,version,timestamp.toString(),fileID,fileHash.hashCode());
    }
   
}

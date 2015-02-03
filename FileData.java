package ece454_project2;

import java.io.Serializable;
import java.util.UUID;

public class FileData implements Serializable {
    public UUID id;
    public byte[] Data;
    
    public FileData(UUID uuid, byte[] data) {
        id = uuid;
        Data = data;
    }
}

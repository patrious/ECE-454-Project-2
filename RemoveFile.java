package ece454_project2;

import java.io.Serializable;
import java.util.UUID;

public class RemoveFile implements Serializable {
    UUID file;
    
    public RemoveFile(UUID uuid) {
        file = uuid;
    }
}

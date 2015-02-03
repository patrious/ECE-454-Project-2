package ece454_project2;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

public class FileManager {

    final Object FileAccessToken = new Object();
    FileList FileListContainer;
    final NetworkManager nm;
    //Constructor

    public FileManager(NetworkManager NM) {
        nm = NM;
        FileListContainer = new FileList(NM);
    }

    public void StartFileManager() {
        //Read in local File list
        FileListContainer.LoadFileList();
    }

    public void CloseFileManager() {
        FileListContainer.SaveFileList();
    }

    public void UpdateFileList(HashMap<UUID,FileListEntry> newList) {
        FileListContainer.UpdateFileList(newList);
    }

    public FileData ReadDiskFromFile(UUID fileID) {

        FileListEntry file = FileListContainer.fileList.get(fileID);
        //Check if file exists, if not check using the HASH.
        File f = new File(file.filename);

        String fileToReadFrom = null;
        if (f.exists()) {
            fileToReadFrom = file.filename;
        } else {
            fileToReadFrom =FileListContainer.FindMatchingHashOnDisk(".",file.fileHash);
        }
        if (fileToReadFrom != null) {
            byte[] data = ReadFromFile(fileToReadFrom);
            return new FileData(fileID, data);
        } else {
            return null;
        }
    }

    
    public void RemoveFile(UUID uuid) {
        FileListContainer.RemoveFile(uuid);
    }

    private byte[] ReadFromFile(String fileName) {
        try (InputStream ins = Files.newInputStream(Paths.get(fileName))) {
            byte[] readin = new byte[ins.available()];
            ins.read(readin, 0, ins.available());
            return readin;
        } catch (IOException ex) {
            System.out.println(String.format("\nError opening to read file: %s", ex.toString()));
            return null;
        }
    }

    public void fileRecieved(FileData incomingData) {
        FileListEntry flea = FileListContainer.fileList.get(incomingData.id);


        if (flea == null) {
            System.out.println(String.format("\nMessage received to write to file, but no such entry found UUID: %s", incomingData.id));
            return;
        }
        try (FileOutputStream outs = new FileOutputStream(flea.filename, false)) {
            outs.write(incomingData.Data);
            flea.timestamp = new Date();
            flea.locations.add(nm.GetLocalIpAddr());
            nm.updateOtherFileLists();
        } catch (IOException ex) {
            System.out.println(String.format("\nError opening to write file: %s", ex.toString()));
        }

    }

    public void Open(String FileName) {
        FileListContainer.OpenFile(FileName);
        FileListEntry flea = FileListContainer.FindEntry(FileName);
        flea.timestamp = new Date();
        flea.locations.add(nm.GetLocalIpAddr());
        nm.updateOtherFileLists();
    }

    public void Update(String FileName) {
        FileListContainer.UpdateFile(FileName);
        nm.updateOtherFileLists();

    }

    public void NewRevision(String FileName) {
        FileListContainer.NewRevision(FileName);
        nm.updateOtherFileLists();
    }

    public void RemoveFile(String FileName)
    {
        FileListContainer.RemoveFile(FileName);        
    }
    public void AddNewFile(String FileName) {
        FileListContainer.AddNewFile(FileName);
        nm.updateOtherFileLists();
    }
}

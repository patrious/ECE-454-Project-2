package ece454_project2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author patrious
 */
public class FileList implements Serializable {

    public HashMap<UUID, FileListEntry> fileList = new HashMap<>();
    NetworkManager nm;

    public FileList(NetworkManager NM) {
        nm = NM;
    }

    public boolean SaveFileList(String location) {
        try {
            FileOutputStream f_outs = new FileOutputStream(location);
            ObjectOutputStream o_outs = new ObjectOutputStream(f_outs);
            o_outs.writeObject(fileList);
            o_outs.flush();
            return true;
        } catch (IOException ex) {
            System.out.println(String.format("\nError saving filelist to disk: %s", ex.toString()));
        }
        return false;
    }

    public boolean SaveFileList() {
        return SaveFileList("filelist.data");
    }

    public boolean LoadFileList() {
        return LoadFileList("filelist.data");
    }

    public boolean LoadFileList(String location) {

        try {
            // Read from disk using FileInputStream
            InputStream file = new FileInputStream(location);
            InputStream buffer = new BufferedInputStream(file);
            ObjectInput input = new ObjectInputStream(buffer);
            this.fileList = (HashMap) input.readObject();
            return true;
        } catch (IOException | ClassNotFoundException ex) {
            System.out.println(String.format("\nError loading filelist to memory: %s", ex.toString()));
        }
        return false;
    }

    public boolean UpdateFileList(HashMap<UUID, FileListEntry> newList) {
        //Check the time stamps, Only add the new items.        
        for (UUID item : newList.keySet()) {
            FileListEntry remoteEntry = newList.get(item);
            FileListEntry localEntry = this.fileList.get(item);
            //If no local entry exits || the remote entry is newer
            if (localEntry == null || remoteEntry.timestamp.after(localEntry.timestamp)) {
                this.fileList.put(item, remoteEntry);
            }
        }
        return false;
    }

    public boolean AddNewFile(String fileName) {
        UUID newID = UUID.randomUUID();
        byte[] fileHash = GetFileHash(fileName);
        if (fileHash == null || newID == null) {
            return false;
        }
        FileListEntry flea = new FileListEntry(newID, fileName, 0, fileHash, nm.GetLocalIpAddr());
        fileList.put(newID, flea);
        return true;
    }

    public boolean RemoveFile(UUID uuid) {
        try {
            fileList.remove(uuid);

        } catch (Exception e) {
            //NOM NOM
            return false;
        }
        return true;
    }

    public boolean RemoveFile(String fileName) {
        FileListEntry flea = FindEntry(fileName);
        nm.removeFile(flea.fileID);
        return RemoveFile(flea.fileID);

    }

    public boolean NewRevision(String fileName) {
        //Create a copy, with _rev_2 at the end (or more)
        //Copy the file over.. with the new name.
        try {
            String newFileName = UpdateFileNameRevision(fileName);
            File f = new File(newFileName);
            while (f.exists()) {
                newFileName = UpdateFileNameRevision(newFileName);
                f = new File(newFileName);
            }
            CopyFile_NewName(fileName, newFileName);
            AddNewFile(newFileName);
            return true;
        } catch (Exception ex) {
            return false;
        }

    }

    public FileListEntry FindEntry(String fileName) {
        for (FileListEntry item : fileList.values()) {
            if (item.filename.equals(fileName)) {
                return item;
            }
        }
        return null;
    }

    public boolean UpdateFile(String fileName) {
        //Rehash the file
        //increment the version numberlist
        byte[] fileHash = GetFileHash(fileName);
        FileListEntry flea;
        if ((flea = FindEntry(fileName)) != null) {
            if (Arrays.equals(fileHash, flea.fileHash)) {
                System.out.println("----- Adding a file with the same hash -----");
                return true;
            }
            FileListEntry mod = fileList.get(flea.fileID);
            FileListEntry orginal = fileList.get(flea.fileID);
            try {
                System.arraycopy(fileHash, 0, mod.fileHash, 0, fileHash.length);
                mod.version++;
                mod.timestamp = new Date();
                mod.locations = new ArrayList<>();
                mod.locations.add(nm.GetLocalIpAddr());
                fileList.put(mod.fileID, mod);
                return true;
            } catch (Exception ex) {
                fileList.put(orginal.fileID, orginal);
                System.out.println(String.format("\nError Updating File to memory: %s", ex.toString()));
                return false;
            }
        } else {
            System.out.println("Attempted to update a fileName that doesn't "
                    + "exist in our records. Adding it as a new file.");
            AddNewFile(fileName);
        }

        return false;
    }

    public boolean DownloadMostRecentFile(String fileName) {
        FileListEntry flea = FindEntry(fileName);
        //Apparently doing this Aync
        nm.getFileData(flea.fileID, flea.locations);
        //TODO: How do I know if I need to request this again?
        return true;
    }

    private boolean CopyFile_NewName(String orginal, String newName) {
        if (!LocalFileUpToDate(orginal)) {
            if (!DownloadMostRecentFile(orginal)) {
                return false;
            }
        }
        try {
            System.out.println(newName);
            FileInputStream fins = new FileInputStream(orginal);
            FileOutputStream fouts = new FileOutputStream(newName);
            fins.getChannel().transferTo(0, fins.getChannel().size(), fouts.getChannel());
            return true;
        } catch (IOException ex) {
            System.out.println(String.format("\nError saving filelist to disk: %s", ex.toString()));
        }
        return false;
    }

    public boolean OpenFile(String fileName) {
        FileListEntry flea = FindEntry(fileName);
        if (flea == null) {
            UUID fileID = FindMatchinHashInList(GetFileHash(fileName));
            flea = fileList.get(fileID);
        }
        if (flea == null) {
            return false;
        }
        fileName = flea.filename;
        try {
            if (LocalFileUpToDate(fileName)) {
                OpenFileSystem(fileName);
            } else {
                if (!DownloadMostRecentFile(fileName)) {
                    System.out.println("\nError Opening File, could not download more recent file.");
                }
                OpenFileSystem(fileName);
            }
        } catch (Exception ex) {
            System.out.println(String.format("\nError opening file: %s", ex.toString()));
            return false;
        }
        return true;
    }

    private void OpenFileSystem(String fileName) {
        //TODO: Get filesystem to open with default program
    }

    private boolean LocalFileUpToDate(String fileName) {

        File file = new File(fileName);
        if (!file.exists()) {
            return false;
        }
        byte[] hash = GetFileHash(fileName);
        if (MatchingHash(fileName, hash)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean MatchingHash(String fileName, byte[] hash) {
        for (FileListEntry item : fileList.values()) {
            if (item.filename.equalsIgnoreCase(fileName) && (hash.hashCode() == item.fileHash.hashCode())) {
                return true;
            }
        }
        return false;
    }

    public String FindMatchingHashOnDisk(String location, byte[] hashValue) {
        File f = new File(location);
        File[] files = f.listFiles();
        for (File file : files) {
            if (file.isFile() && GetFileHash(file.getPath()) == hashValue) {
                return file.getPath();
            }
        }
        return null;

    }

    public UUID FindMatchinHashInList(byte[] hash) {
        for (FileListEntry flea : fileList.values()) {
            if (flea.fileHash.hashCode() == hash.hashCode()) {
                return flea.fileID;
            }
        }
        return null;
    }

    private String UpdateFileNameRevision(String fileName) {

        //read the string and update from x_rev_2.txt -> x_rev_3.txt
        int index = -1;
        int dot_index = fileName.lastIndexOf(".");
        if (dot_index == -1) {
            dot_index = fileName.length();
        }
        if ((index = fileName.indexOf("_rev_")) != -1) {
            String parseme = fileName.substring(index + 5, dot_index);
            int current_Rev = Integer.parseInt(parseme);
            StringBuilder strb = new StringBuilder(fileName).delete(index + 5, dot_index);
            strb.insert(index + 5, ++current_Rev);
            return strb.toString();

        } else {
            //First time, add it before the last '.', if no dot, append            
            StringBuilder strb = new StringBuilder(fileName).insert(dot_index, "_rev_1");
            return strb.toString();
        }
    }

    public byte[] GetFileHash(String fileName) {
        File f = new File(fileName);
        if (!f.exists()) {
            return null;
        }
        byte[] digest = null;
        try (FileInputStream is = new FileInputStream(fileName)) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            DigestInputStream dis = new DigestInputStream(is, md);
            /* Read stream to EOF as normal... */
            digest = md.digest();
        } catch (NoSuchAlgorithmException | IOException ex) {
            Logger.getLogger(FileList.class.getName()).log(Level.SEVERE, null, ex);
        }
        return digest;
    }
}

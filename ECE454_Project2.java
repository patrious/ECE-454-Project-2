package ece454_project2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ECE454_Project2 {

    public static void main(String[] args) throws IOException {
        int myPort = Integer.parseInt(args[0]);
        NetworkManager nm = new NetworkManager();
        FileManager fm = new FileManager(nm);
        fm.StartFileManager();
        nm.StartManager(fm, myPort);
        
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Input VFS command (list, open, newrev, update, add, remove, exit)");
        
        while (true) {
            String input = br.readLine();

            //List all files in the VFS
            if (input.equals("list")) {
                System.out.println("filename, version, timestamp, ID: ");
                for (FileListEntry item : fm.FileListContainer.fileList.values()) {
                    System.out.println(String.format("%s", item.toString()));
                }
            } //Check out a file
            else if (input.equals("open")) {
                System.out.println("Please input file name to open");
                String fname = br.readLine();
                fm.Open(fname);
            } else if (input.equals("newrev")) {
                System.out.println("Create new revision of file (please type in filename): ");
                String fname = br.readLine();
                fm.NewRevision(fname);
            } //Check in a file
            else if (input.equals("update")) {
                System.out.println("Please input file name to close/update");
                String fname = br.readLine();
                fm.Update(fname);
            } else if (input.equals("add")) {
                System.out.println("Please input file name to add to VFS");
                String fname = br.readLine();
                fm.AddNewFile(fname);
            } else if (input.equals("remove")) {
                System.out.println("Please input file name to remove:");
                String fname = br.readLine();
                fm.RemoveFile(fname);
            } else if (input.equals("exit")) {
                nm.StopManager();
                fm.CloseFileManager();
                System.exit(0);
            } else {
                System.out.println("Invalid command.");
            }
        }
    }
}

package ca.concordia;


import ca.concordia.server.FileServer;
import ca.concordia.filesystem.FileSystemManager;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.print("Hello and welcome!");

        // totalSize = metadataSize + (MAXBLOCKS × BLOCKSIZE)
        // MAXBLOCKS = 10
        // BLOCKSIZE = 128

        /*
        // Temporary Test for File System Operations
        System.out.println("\n=== FILE SYSTEM OPERATIONS TEST ===");
        String diskFile = "filesystem.dat";
        int totalSize = 10 * 128; // 10 blocks of 128 bytes
        FileSystemManager fsm = new FileSystemManager(diskFile, totalSize);
        // Create
        System.out.println("Creating test file...");
        fsm.createFile("file1.txt");
        // Write
        System.out.println("Writing to file1.txt...");
        fsm.writeFile("file1.txt", "Hello COEN317!".getBytes());

        System.out.println("Listing files after write:");
        String[] listAfterWrite = fsm.listFiles();
        for (String name : listAfterWrite) {
            System.out.println(" -> " + name);
        }
        // Read
        System.out.println("Reading file1.txt...");
        byte[] contents = fsm.readFile("file1.txt");
        System.out.println("Contents: " + new String(contents));
        // List
        System.out.println("Listing files:");
        String[] list = fsm.listFiles();
        for (String name : list) {
            System.out.println(" -> " + name);
        }
        // Delete
        System.out.println("Deleting file1.txt...");
        fsm.deleteFile("file1.txt");

        System.out.println("Listing files after deletion:");
        String[] listAfter = fsm.listFiles();
        for (String name : listAfter) {
            System.out.println(" -> " + name);

        System.out.println("=== FILE SYSTEM OPERATIONS TEST COMPLETE ===\n");
        }
        */
        /*
        // Temporary Test for Multithreading
        System.out.println("\n=== MULTITHREADING TEST ===");

        FileSystemManager fsm = new FileSystemManager("filesystem.dat", 1024 * 20);

        // Create initial file
        fsm.createFile("shared.txt");
        fsm.writeFile("shared.txt", "Initial data".getBytes());

        // Thread lists
        java.util.List<Thread> allThreads = new java.util.ArrayList<>();

        // Writers: 10 threads
        for (int i = 0; i < 10; i++) {
            final int id = i;
            Thread writer = new Thread(() -> {
                try {
                    String data = "Writer-" + id + " was here!";
                    fsm.writeFile("shared.txt", data.getBytes());
                    System.out.println("[Writer " + id + "] write successful");
                } catch (Exception e) {
                    System.out.println("[Writer " + id + "] ERROR: " + e.getMessage());
                }
            });
            writer.start();
            allThreads.add(writer);
        }

        // Readers: 20 threads
        for (int i = 0; i < 20; i++) {
            final int id = i;
            Thread reader = new Thread(() -> {
                try {
                    byte[] data = fsm.readFile("shared.txt");
                    System.out.println("[Reader " + id + "] read: " + new String(data));
                } catch (Exception e) {
                    System.out.println("[Reader " + id + "] ERROR: " + e.getMessage());
                }
            });
            reader.start();
            allThreads.add(reader);
        }

        // Wait for all threads to finish
        for (Thread t : allThreads) {
            try { t.join(); } catch (InterruptedException ignored) {}
        }

        System.out.println("=== MULTITHREADING TEST COMPLETE ===\n");
        */
        /*
        if (args.length != 3){
            System.out.println("ERROR IN SERVER parameters- <port> <filename> <totalSize> ");
            return;
        }

        int port = Integer.parseInt(args[0]);
        String filename = args[1]; // filename
        int totalsize = Integer.parseInt(args[2]);

        // starting server
        FileServer server = new FileServer(port, filename, totalsize);
        server.start();
        */
    }
}
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



    }
}
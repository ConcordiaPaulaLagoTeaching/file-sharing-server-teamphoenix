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

            // initialisation from origin code
            FileServer server = new FileServer(12345, "filesystem.dat", 10 * 128);

            // Start the file server
            System.out.print("Please enter 1 of the following: \n");
            System.out.print("CREATE <filename> \n");
            System.out.print("READ <filename> \n");
            System.out.print("DELETE <filename> \n");
            System.out.print("WRITE <filename> <data> \n");
            System.out.print("LIST \n");
            System.out.print("QUIT \n");


            server.start();


    }
}
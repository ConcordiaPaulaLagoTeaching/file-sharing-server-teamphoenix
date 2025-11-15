package ca.concordia.server; // Server package

import ca.concordia.filesystem.FileSystemManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class FileServer {

    private final FileSystemManager fsManager;  // Shared filesystem manager
    private final int port;     // Server port

    public FileServer(int port, String fileSystemName, int totalSize) throws Exception {
        this.port = port; // Save port
        this.fsManager = new FileSystemManager(fileSystemName, totalSize); // Initialize fs
    }

    public void start() {
        // Create listening socket
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started. Listening on port " + port + "...");

            // Accept clients forever
            while (true) {
                Socket clientSocket = serverSocket.accept();    // Block until client connects
                System.out.println("New client connected: " + clientSocket);
                new Thread(new ClientHandler(clientSocket)).start();    // Start worker thread
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Could not start server on port " + port);
        }
    }

    // Worker thread class
    private class ClientHandler implements Runnable {

        private final Socket clientSocket; // Client connection

        ClientHandler(Socket socket) {
            this.clientSocket = socket; // Store client socket
        }

        @Override
        public void run() {
            try (
                    // Read client input
                    BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    // Send responses
                    PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true)
            ) {

                String line; // Holds client command
                while ((line = reader.readLine()) != null) { // Read until disconnect

                    System.out.println("Received from " + clientSocket + ": " + line); // Log

                    String[] parts = line.split(" ", 3); // Split into at most 3 parts
                    String command = parts[0].toUpperCase(); // Get command keyword

                    try {
                        switch (command) {
                            // CREATE command
                            case "CREATE": {
                                if (parts.length < 2) { writer.println("ERROR: CREATE requires a filename."); break; }
                                String filename = parts[1]; // Extract filename
                                if (filename.length() > 11) { writer.println("ERROR: filename too large"); break; }

                                synchronized (fsManager) { fsManager.createFile(filename); } // Create file safely
                                writer.println("SUCCESS: File '" + filename + "' created.");
                                break;
                            }

                            // WRITE command
                            case "WRITE": {
                                if (parts.length < 2) { writer.println("ERROR: WRITE requires a filename."); break; }
                                String filename = parts[1]; // Filename
                                if (filename.length() > 11) { writer.println("ERROR: filename too large"); break; }

                                // Extract content correctly (support spaces)
                                int prefixLength = command.length() + 1 + filename.length() + 1;
                                String content = "";
                                if (line.length() > prefixLength) {
                                    content = line.substring(prefixLength);
                                }

                                byte[] data = content.getBytes(); // Convert to bytes

                                try {
                                    synchronized (fsManager) { fsManager.writeFile(filename, data); } // Write safely
                                    writer.println("SUCCESS: Wrote " + data.length + " bytes to '" + filename + "'.");
                                } catch (Exception e) {
                                    String msg = e.getMessage().toLowerCase(); // Normalize error text
                                    if (msg.contains("not found")) writer.println("ERROR: file " + filename + " does not exist");
                                    else if (msg.contains("space")) writer.println("ERROR: file too large");
                                    else writer.println("ERROR: " + e.getMessage());
                                }
                                break;
                            }

                            // READ command
                            case "READ": {
                                if (parts.length < 2) { writer.println("ERROR: READ requires a filename."); break; }
                                String filename = parts[1];
                                if (filename.length() > 11) { writer.println("ERROR: filename too large"); break; }

                                try {
                                    byte[] data;
                                    synchronized (fsManager) { data = fsManager.readFile(filename); } // Read safely

                                    writer.println("SUCCESS:"); // Start output block
                                    writer.println(new String(data)); // File contents
                                    writer.println("END"); // End block
                                } catch (Exception e) {
                                    String msg = e.getMessage().toLowerCase(); // Normalize
                                    if (msg.contains("not found")) writer.println("ERROR: file " + filename + " does not exist");
                                    else writer.println("ERROR: " + e.getMessage());
                                }
                                break;
                            }

                            // DELETE command
                            case "DELETE": {
                                if (parts.length < 2) { writer.println("ERROR: DELETE requires a filename."); break; }
                                String filename = parts[1];
                                if (filename.length() > 11) { writer.println("ERROR: filename too large"); break; }

                                try {
                                    synchronized (fsManager) { fsManager.deleteFile(filename); } // Delete safely
                                    writer.println("SUCCESS: File '" + filename + "' deleted.");
                                } catch (Exception e) {
                                    String msg = e.getMessage().toLowerCase(); // Normalize
                                    if (msg.contains("not found")) writer.println("ERROR: file " + filename + " does not exist");
                                    else writer.println("ERROR: " + e.getMessage());
                                }
                                break;
                            }

                            // LIST command
                            case "LIST": {
                                try {
                                    String[] names;
                                    synchronized (fsManager) { names = fsManager.listFiles(); } // Fetch safely

                                    writer.println("SUCCESS:"); // Begin list
                                    for (String n : names) writer.println(n); // Print each filename
                                    writer.println("END"); // End block
                                } catch (Exception e) {
                                    writer.println("ERROR: " + e.getMessage());
                                }
                                break;
                            }

                            // QUIT command
                            case "QUIT": {
                                writer.println("SUCCESS: Disconnecting.");
                                return;
                            }
                            default: writer.println("ERROR: Unknown command.");
                        }

                    } catch (Exception e) {
                        writer.println("ERROR: " + e.getMessage()); // Fallback error
                    }
                }

            } catch (Exception e) {
                e.printStackTrace(); // Print I/O errors
            } finally {
                try { clientSocket.close(); } catch (Exception ignored) {} // Close socket
            }
        }
    }
}

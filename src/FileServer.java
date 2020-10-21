import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;

public class FileServer {

    static String rootDirectory = "/home/gm/shared";

    /**
     * Creates a file server on port 8040 that listens for incoming requests
     * until terminated. Only serves files from the root directory /shared,
     * and only supports GET requests.
     */
    public static void main(String[] args) {
        ServerSocket serverSocket;
        int PORT = 8040;

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Listening on port " + PORT);
        } catch (Exception e) {
            System.out.println("Failed to create listening socket.");
            return;
        }

        try {
            while (true) {
                Socket connection = serverSocket.accept();
                System.out.println("\nConnection from " + connection.getRemoteSocketAddress());
                ConnectionThread thread = new ConnectionThread(connection);
                thread.start();
            }
        } catch (Exception e) {
            System.out.println("Server socket shut down unexpectedly.");
            System.out.println("Error: " + e);
            System.out.println("Exiting.");
        }
    }

    /**
     * Handles all requests for files sent to the given socket. Sends back
     * the requested file as a response, or an error if it cannot be found.
     *
     * @param connection a connected socket which received a request.
     */
    public static void handleConnection(Socket connection) {

        try {
            Scanner in = new Scanner(connection.getInputStream());
            String line = in.nextLine();
            if (line.startsWith("GET")) {
                String fileName = line.substring(4, line.indexOf(" HTTP"));
                handleGet(fileName, connection);
                connection.close();
            } else {
                // TODO: send 501
            }
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            try {
                connection.close();
            } catch (Exception e) {
                System.out.println("Connection terminated.");
            }
        }
    }

    /**
     * Handles GET requests sent to a connected socket. If the fileName
     * is valid and is not a directory, it writes a successful response
     * to the output stream with all the relevant headers; if the given
     * fileName is a readable file, it sends its content as a response;
     * else, sends back the appropriate response and HTTP status code.
     * Throughout the code, \r\n is used instead of the standard \n in
     * Linux to prevent images from being corrupted during transfer.
     *
     * @param fileName   the file requested from the connected socket.
     * @param connection a connected socket used for the output stream.
     */
    private static void handleGet(String fileName, Socket connection) {

        try {
            File requestedFile = new File(rootDirectory + fileName);
            PrintWriter out = new PrintWriter(connection.getOutputStream());

            // Legal requests
            if (requestedFile.exists() && !requestedFile.isDirectory()) {
                out.print("HTTP/1.1 200 OK\r\n");
                out.print("Connection: close\r\n");
                out.print("Content-Length: " + requestedFile.length() + "\r\n");
                out.print("Content-Type: " + getMimeType(fileName) + "\r\n");
                sendFile(requestedFile, connection.getOutputStream());
            }
            // File not found
            else if (!requestedFile.exists()) {
                out.print("HTTP/1.1 404 Not Found\r\n");
                out.print("Connection: close\r\n");
                out.print("Content-Type: text/html\r\n\r\n");
                out.print("<html><head><title>Error</title></head><body>" +
                      "<h2>Error: 404 Not Found</h2><p>The resource that you requested does not exist on this server.</p>" +
                      "</body></html>");
            }
            // No read rights
            else if (!requestedFile.canRead()) {
                out.print("HTTP/1.1 403 Forbidden\r\n");
                out.print("Connection: close\r\n");
                out.print("Content-Type: text/html\r\n\r\n");
                out.print("<html><head><title>Error</title></head><body>" +
                      "<h2>Error: 403 Forbidden</h2><p>You do not have read permission for the requested resource.</p>" +
                      "</body></html>");
            }
            out.flush();
            out.close();

        } catch (Exception e) {
            System.out.println("Error while handling GET: " + e);
        }
    }

    /**
     * Returns the appropriate MIME Type of a given file name.
     *
     * @param fileName a full file name, including extension.
     */
    private static String getMimeType(String fileName) {
        int pos = fileName.lastIndexOf('.');
        // if the file has no extension
        if (pos < 0) return "x-application/x-unknown";

        String ext = fileName.substring(pos + 1).toLowerCase();
        switch (ext) {
            case "txt":
                return "text/plain";
            case "html":
            case "htm":
                return "text/html";
            case "css":
                return "text/css";
            case "js":
                return "text/javascript";
            case "java":
                return "text/x-java";
            case "jpeg":
            case "jpg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "ico":
                return "image/x-icon";
            case "class":
                return "application/java-vm";
            case "jar":
                return "application/java-archive";
            case "zip":
                return "application/zip";
            case "xml":
                return "application/xml";
            case "xhtml":
                return "application/xhtml+xml";
            default:
                return "x-application/x-unknown";
        }
    }

    /**
     * Reads the content of a given file and sends it to an OutputStream.
     *
     * @param file      the file that will be transferred.
     * @param socketOut the OutputStream that will receive the file.
     */
    private static void sendFile(File file, OutputStream socketOut) throws IOException {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        OutputStream out = new BufferedOutputStream(socketOut);

        while (true) {
            int x = in.read();
            if (x < 0)
                break;
            out.write(x);
        }
        out.flush();
    }

    private static class ConnectionThread extends Thread {
        Socket connection;

        ConnectionThread(Socket connection) {
            this.connection = connection;
        }

        public void run() {
            handleConnection(connection);
        }
    }
}
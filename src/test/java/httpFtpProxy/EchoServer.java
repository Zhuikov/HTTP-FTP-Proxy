package httpFtpProxy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer extends Thread {

    public static final int port = 7500;
    public static final String host = "127.0.0.1";

    private String message;
    private ServerSocket serverSocket;

    @Override
    public void run() {
        try {
            Socket clientSocket = serverSocket.accept();
            OutputStream os = clientSocket.getOutputStream();
            os.write(message.getBytes());
        } catch (IOException e) {
            System.out.println("FTP server thread exception " + e.getMessage());
        }
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void begin() throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public void close() throws IOException {
        serverSocket.close();
    }
}

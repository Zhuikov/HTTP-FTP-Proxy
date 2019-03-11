package httpFtpProxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Proxy {

    public static void main(String[] args) throws IOException {


        FTPClient ftpClient = new FTPClient();

        System.out.println(ftpClient.connect("ftp.sunet.se"));

        System.out.println(ftpClient.auth("anonymous", "easy_pass"));

        byte[] data = null;
        System.out.println(ftpClient.list("favicon.ico"));

    }

    public static void main1(String[] args) {

        int port = 7500;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server run on port : " + port);

            String line;
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Connected");
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                    if (line.isEmpty())
                        break;
                }
                out.write("HTTP/1.1 200 OK\r\n");
//                out.write("Date: Mon, 25 Feb 2019 18:12:20 GMT\r\n");
//                out.write("Server: Apache/1.3.27\r\n");
                out.write("Content-Type: text/html\r\n");
                out.write("Content-Length: 57\r\n");
                out.write("\r\n");
                out.write("<title>Hello JAVA</title>");
                out.write("<p>This is the best example!</p>");
//                out.flush();

                out.close();
                in.close();
                clientSocket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}

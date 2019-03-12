package httpFtpProxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Proxy {

    static class ReplyDataStructure {
        private String code = null;
        private ArrayList<Character> data = null;

        public ReplyDataStructure() {}

        public String getCode() {
            return code;
        }

        public ArrayList<Character> getData() {
            return data;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public void setData(ArrayList<Character> data) {
            this.data = data;
        }
    }
    private ServerSocket listeningSocket;
    private Socket clientSocket;

    public static void main(String[] args) {

        Proxy proxy = new Proxy();
        try {
            proxy.start(7500);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Proxy() {}

    public void start(int port) throws IOException {

        listeningSocket = new ServerSocket(7500);
        String line;

//        while (true) {
            clientSocket = listeningSocket.accept();

            BufferedReader clientBR = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            while ((line = clientBR.readLine()) != null) {
                if (line.isEmpty()) {
                    System.out.println("EMPTY LINE");
                    break;
                }
                System.out.println(line);
            }

            clientBR.close();
            clientSocket.close();

//        }
    }

    // ftp testing
    public static void main2(String[] args) throws IOException {

        FTPClient ftpClient = new FTPClient();

        System.out.println("connect = " + ftpClient.connect("speedtest.tele2.net"));

        System.out.println("auth = " + ftpClient.auth("anonymous", "easy_pass"));

        ReplyDataStructure replyDataStructure = ftpClient.sendDataCommand("list","", 'A');
        System.out.println("list = " + replyDataStructure.getCode());
        for (char c : replyDataStructure.getData()) {
            System.out.print(c);
        }

        replyDataStructure = ftpClient.sendDataCommand("retr",
                "512KB.zip", 'I');
        System.out.println("retr = " + replyDataStructure.getCode());

        // save retr file test:
        OutputStream fileOut = new FileOutputStream("/home/artem/Documents/512KB.zip");
        for (char b : replyDataStructure.getData()) {
            fileOut.write(b);
        }
        fileOut.close();

//        replyDataStructure = ftpClient.sendDataCommand("retr","README", 'A');
//        System.out.println("retr = " + replyDataStructure.getCode());
//
//        // save retr file test:
//        fileOut = new FileOutputStream("/home/artem/Documents/README");
//        for (char b : replyDataStructure.getData()) {
//            fileOut.write(b);
//        }
//        fileOut.close();

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

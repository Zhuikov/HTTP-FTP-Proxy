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

    public static void main(String[] args) throws IOException {

        int a = 1000;
        System.out.println((char)a + " " + (char) (a + 4));

        FTPClient ftpClient = new FTPClient();

        System.out.println("connect = " + ftpClient.connect("ftp.funet.fi"));

        System.out.println("auth = " + ftpClient.auth("anonymous", "easy_pass"));

        byte[] data = null;
        System.out.println("list = " + ftpClient.list("").getCode());

        System.out.println("retr = " + ftpClient.retr("/pub/sports/shooting/ipsc/graphics/diagrams/rifle_target_dimensions.gif",
                "/home/artem/Documents/test").getCode());

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

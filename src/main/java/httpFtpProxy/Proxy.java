package httpFtpProxy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Proxy {

    static class DataAndCode {
        private String code = null;
        private ArrayList<Character> data = null;

        public DataAndCode() {}

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
    static class HTTPRequest {
        private Method method = null;
        private String path = null;
        private String hostName = null;
        private ArrayList<Character> body = null;

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getHostName() {
            return hostName;
        }

        public void setHostName(String hostName) {
            this.hostName = hostName;
        }

        public ArrayList<Character> getBody() {
            return body;
        }

        public void setBody(ArrayList<Character> body) {
            this.body = body;
        }
    }

    public enum Method { GET, PUT };

    private ServerSocket listeningSocket;
    private Socket clientSocket;
    private FTPClient ftpClient = new FTPClient();

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

        HTTPHandler httpHandler = new HTTPHandler();

        listeningSocket = new ServerSocket(7500);
        String line;


        clientSocket = listeningSocket.accept();
        HTTPRequest request = httpHandler.receiveRequest(clientSocket);

        System.out.println("Method = " + request.getMethod() +
                "\nHost = " + request.getHostName() +
                "\nPath = " + request.getPath() +
                "\nBody = " + request.getBody());

        clientSocket.close();

    }

    private DataAndCode processGET(HTTPRequest httpRequest) {

        return new DataAndCode();
    }

    private String processPUT (HTTPRequest httpRequest) {

        return "150 ?";
    }

    // ftp testing
    public static void main3(String[] args) throws IOException {

        FTPClient ftpClient = new FTPClient();

        System.out.println("connect = " + ftpClient.connect("192.168.0.27"));

        System.out.println("auth = " + ftpClient.auth("artem", "artem"));

        FileInputStream fileInputStream = new FileInputStream("/home/artem/Pictures/RPN.jpg");
        int i;
        ArrayList<Character> file = new ArrayList<>();
        while ((i = fileInputStream.read()) != -1) {
            file.add((char)i);
        }
        fileInputStream.close();
        System.out.println("stor = " + ftpClient.stor(file, "/ftp/RPN"));

        DataAndCode dataAndCode = ftpClient.sendDataCommand("list","/ftp/", 'A');
        System.out.println("list = " + dataAndCode.getCode());
        for (char c : dataAndCode.getData()) {
            System.out.print(c);
        }

        dataAndCode = ftpClient.sendDataCommand("retr","/ftp/RPN", 'I');
        System.out.println("retr = " + dataAndCode.getCode());

        // save retr file test:
        OutputStream fileOut = new FileOutputStream("/home/artem/Documents/downloadedRPN.jpg");
        for (char b : dataAndCode.getData()) {
            fileOut.write(b);
        }
        fileOut.close();
    }

}

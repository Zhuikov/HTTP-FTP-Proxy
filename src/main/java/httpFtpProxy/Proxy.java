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
        private String login = null;
        private String password = null;
        private ArrayList<Character> body = null;
        private String param = null;
        private String ftpCommand = null;
        private boolean file = false;

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

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

        public String getParam() {
            return param;
        }

        public void setParam(String param) {
            this.param = param;
        }

        public ArrayList<Character> getBody() {
            return body;
        }

        public void setBody(ArrayList<Character> body) {
            this.body = body;
        }

        public String getFtpCommand() {
            return ftpCommand;
        }

        public void setFtpCommand(String ftpCommand) {
            this.ftpCommand = ftpCommand;
        }

        public boolean isFile() {
            return file;
        }

        public void setFile(boolean file) {
            this.file = file;
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

        listeningSocket = new ServerSocket(port);

        while (true) {
            clientSocket = listeningSocket.accept();
            HTTPRequest httpRequest = httpHandler.receiveRequest(clientSocket);
            // todo может быть добавить метод валидации пакета

            System.out.println("------Method = " + httpRequest.getMethod() +
                    "\nHost = " + httpRequest.getHostName() +
                    "\nPath = " + httpRequest.getPath() +
                    "\nLogin = " + httpRequest.getLogin() +
                    "\nPass = " + httpRequest.getPassword() +
                    "\nFile = " + httpRequest.isFile() +
                    "\nFtp command = " + httpRequest.getFtpCommand() +
                    "\nParam = " + httpRequest.getParam() +
                    "\nBody = " + httpRequest.getBody());

            if (!ftpClient.isConnected()) {
                String connectResponse = ftpClient.connect(httpRequest.path.substring(0, httpRequest.path.indexOf('/')));
                if (!connectResponse.equals("220")) {
                    sendResponse(clientSocket, "500", "Connection error");
                    continue;
                }
            }

            if (!ftpClient.isAuth()) {
                String authResponse = ftpClient.auth(httpRequest.login, httpRequest.password);
                if (!authResponse.equals("230")) {
                    sendResponse(clientSocket, "400", "Authentication error");
                    continue;
                }
            }

            DataAndCode response;
            String putResponse;

            if (httpRequest.isFile()) {
                switch (httpRequest.getMethod()) {
                    case GET: {
                        response = processRequestGET(httpRequest);
                        System.out.println("Process done!");
                        System.out.println("get request code = " + response.getCode());
                        sendResponse(clientSocket, response);
                        System.out.println("GET sent!");
                        break;
                    }
                    case PUT: {
                        putResponse = processRequestPUT(httpRequest);
                    }
                }
            } else {
                if (httpRequest.getFtpCommand().equals("pwd")) {
                    response = ftpClient.pwd();
                    if (response.getCode().equals("257")) {
                        sendResponse(clientSocket, response);
                        System.out.println("pwd request code = " + response.getCode() + "\nresponse sent");
                    }
                    else
                        sendResponse(clientSocket, "500", "Cannot execute command");
                } else if (httpRequest.getFtpCommand().equals("cwd")) {
                    String cwdResponseCode = ftpClient.cwd(httpRequest.getParam());
                    if (cwdResponseCode.equals("250")) {
                        sendResponse(clientSocket, "200", "Ok");
                        System.out.println("cwd request code = " + cwdResponseCode + "\nresponse sent");
                    }
                    else
                        sendResponse(clientSocket, "500", "Cannot change directory");
                }
            }
        }
    }

    private DataAndCode processRequestGET(HTTPRequest httpRequest) throws IOException {

        DataAndCode dataAndCode;
        String path = httpRequest.getPath();
        if (path.charAt(path.length() - 1) == '/')
            dataAndCode = ftpClient.sendDataCommand("list", path.substring(path.indexOf('/')),
                    httpRequest.getParam().charAt(0));
        else
            dataAndCode = ftpClient.sendDataCommand("retr", path.substring(path.indexOf('/')),
                    httpRequest.getParam().charAt(0));

        return dataAndCode;
    }

    private void sendResponse(Socket socket, String code, String message) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(("HTTP/1.1 " + code + ' ' + message + "\nContent-Length: 0" +"\n\n").getBytes());
    }

    private void sendResponse(Socket socket, DataAndCode dataAndCode) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(("HTTP/1.1 200 Ok\nContent-Length: " + dataAndCode.data.size() + "\n\n").getBytes());
        if (dataAndCode.data.size() != 0) {
            for (char c : dataAndCode.data) {
                os.write(c);
            }
        }
    }

    private String processRequestPUT(HTTPRequest httpRequest) {

        return "150 ?";
    }

//    private String makeResponseHeaders(String code, int contentLength) {
//        String responseHeader = "HTTP/1.1 200 Ok\nContent-Length: ";
//
//    }

    // ftp testing
    public static void main2(String[] args) throws IOException {

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
        OutputStream fileOut = new FileOutputStream("/home/artem/Documents/downloadedRPN1.jpg");
        for (char b : dataAndCode.getData()) {
            fileOut.write(b);
        }
        fileOut.close();
    }

    public static void main1(String[] args) throws IOException {
        FTPClient ftpClient = new FTPClient();

        ftpClient.connect("ftp.funet.fi");
        ftpClient.auth("anonymous", "pass");
        DataAndCode response = ftpClient.sendDataCommand("list", "/", 'A');
        System.out.println(response.code);
        for (char c : response.getData()) {
            System.out.print(c);
        }

    }

}

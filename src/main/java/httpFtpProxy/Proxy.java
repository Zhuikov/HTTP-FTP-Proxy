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

            System.out.println("------Method = " + httpRequest.getMethod() +
                    "\nHost = " + httpRequest.getHostName() +
                    "\nPath = " + httpRequest.getPath() +
                    "\nLogin = " + httpRequest.getLogin() +
                    "\nPass = " + httpRequest.getPassword() +
                    "\nFile = " + httpRequest.isFile() +
                    "\nFtp command = " + httpRequest.getFtpCommand() +
                    "\nParam = " + httpRequest.getParam() +
                    "\nBody = " + httpRequest.getBody());

            //todo проверить коннект
            // отправить 500, если вернулся плохой код ответа
            System.out.println("connect = " + ftpClient.connect(httpRequest.path.substring(0, httpRequest.path.indexOf('/'))));
            //todo проверить логин
            // отправить 400, если все плохо
            System.out.println("auth = " + ftpClient.auth(httpRequest.login, httpRequest.password));

            DataAndCode response;
            String putResponse;

            if (httpRequest.isFile()) {
                switch (httpRequest.getMethod()) {
                    case GET: {
                        response = processRequestGET(httpRequest);
                        sendResponse(clientSocket, response);
                    }
                    case PUT: {
                        putResponse = processRequestPUT(httpRequest);
                    }
                }
            } else {
                if (httpRequest.getFtpCommand().equals("pwd")) {
                    response = ftpClient.pwd();
                    sendResponse(clientSocket, response);
                } else if (httpRequest.getFtpCommand().equals("cwd")) {
                    String cwdResponseCode = ftpClient.cwd(httpRequest.getParam());
                    sendResponse(clientSocket, cwdResponseCode);
                }
            }

        }

    }

    private DataAndCode processRequestGET(HTTPRequest httpRequest) throws IOException {

        DataAndCode dataAndCode;
        String path = httpRequest.getPath();
        if (path.charAt(path.length() - 1) == '/')
            dataAndCode = ftpClient.sendDataCommand("list", path.substring(path.indexOf('/')), 'A');
        else
            dataAndCode = ftpClient.sendDataCommand("retr", path.substring(path.indexOf('/')), 'I');

        return dataAndCode;
    }

    private void sendResponse(Socket socket, String code) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(("HTTP/1.1 200 Ok\nContent-Length: " + code.length() +'\n' + '\n' + code.length()).getBytes());
    }

    private void sendResponse(Socket socket, DataAndCode dataAndCode) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(("HTTP/1.1 200 Ok\nContent-Length: " + dataAndCode.data.size() + '\n' + '\n').getBytes());
        if (dataAndCode.data.size() != 0) {
//            os.write('\n');
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
    public static void main1(String[] args) throws IOException {

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

}

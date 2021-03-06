package httpFtpProxy;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class FTPClient {

    public static final String listCommand = "list";
    public static final String retrCommand = "retr";
    public static final String storCommand = "stor";
    public static final String deleCommand = "dele";
    public static final String cwdCommand  =  "cwd";
    public static final String pwdCommand  =  "pwd";
    public static final String authCommand = "auth";
    public static final String quitCommand = "quit";

    private Socket controlSocket;

    private class PasvCodeSocket {
        private String replyCode;
        private Socket dataSocket;

        public PasvCodeSocket(String replyCode, Socket dataSocket) {
            this.replyCode = replyCode;
            this.dataSocket = dataSocket;
        }
    }

    public FTPClient() {}


    public String connect(String address) throws IOException {
        controlSocket = new Socket(address, 21);
        return readResponse(controlSocket).substring(0, 3);
    }

    public boolean isConnected() {
        if (controlSocket == null)
            return false;
        return !controlSocket.isClosed();
    }

    public boolean isAuth() throws IOException {
        Proxy.DataAndCode response = pwd();
        return !response.getCode().equals("530");
    }

    public String disconnect() throws IOException {
        sendCommand(controlSocket, quitCommand);
        String response = readResponse(controlSocket);

        controlSocket.close();
        return response.substring(0, 3);
    }

    public String auth(String user, String pass) throws IOException {
        sendCommand(controlSocket, "user " + user);
        readResponse(controlSocket);

        sendCommand(controlSocket, "pass " + pass);
        return readResponse(controlSocket).substring(0, 3);
    }

    public Proxy.DataAndCode sendDataCommand(String command, String filePath, char type) throws IOException {

        Proxy.DataAndCode dataAndCode = new Proxy.DataAndCode();
        sendCommand(controlSocket, "type " + type);

        String response = readResponse(controlSocket).substring(0, 3);
        if (!response.equals("200")) {
            dataAndCode.setCode(response);
            return dataAndCode;
        }

        PasvCodeSocket pasvCodeSocket = pasv();
        if (pasvCodeSocket.dataSocket == null) {
            dataAndCode.setCode(pasvCodeSocket.replyCode.substring(0, 3));
            return dataAndCode;
        }

        sendCommand(controlSocket, command + " " + filePath);
        response = readResponse(controlSocket).substring(0, 3); // read the first code
        if (!(response.substring(0, 3).equals("125") || response.substring(0, 3).equals("150"))) {
            dataAndCode.setCode(response.substring(0, 3));
            return dataAndCode;
        }

        ArrayList<Character> input = consumePasvData(pasvCodeSocket.dataSocket);
        dataAndCode.setData(input);
        dataAndCode.setCode(readResponse(controlSocket).substring(0, 3)); // read the second code

        return dataAndCode;
    }

    public Proxy.DataAndCode pwd() throws IOException {

        Proxy.DataAndCode dataAndCode = new Proxy.DataAndCode();
        sendCommand(controlSocket, pwdCommand);

        String response = readResponse(controlSocket);
        dataAndCode.setCode(response.substring(0, 3));

        if (dataAndCode.getCode().equals("257")) {
            ArrayList<Character> currentPath = new ArrayList<>();
            for (char c : response.substring(response.indexOf("\"") + 1, response.lastIndexOf("\"")).toCharArray())
                currentPath.add(c);
            dataAndCode.setData(currentPath);
        }

        return dataAndCode;
    }

    public String cwd(String newDir) throws IOException {
        sendCommand(controlSocket, cwdCommand + " " + newDir);
        return readResponse(controlSocket).substring(0, 3);
    }

    // filePath -- place on server
    public String stor(ArrayList<Character> data, String filePath, char type) throws IOException {

        sendCommand(controlSocket, "type " + type);
        readResponse(controlSocket);

        PasvCodeSocket pasvCodeSocket = pasv();
        if (pasvCodeSocket.dataSocket == null)
            return pasvCodeSocket.replyCode;

        sendCommand(controlSocket, storCommand + " " + filePath);

        OutputStream os = pasvCodeSocket.dataSocket.getOutputStream();
        for (char c : data)
            os.write(c);
        os.flush();
        os.close();

        String response = readResponse(controlSocket); // the first code
        if (!(response.substring(0, 3).equals("125") || response.substring(0, 3).equals("150"))) {
            return response.substring(0, 3);
        }

        return readResponse(controlSocket).substring(0, 3); // the second code
    }

    public String dele(String filePath) throws IOException {
        sendCommand(controlSocket, deleCommand + " " + filePath);
        return readResponse(controlSocket).substring(0, 3);
    }

    private void sendCommand(Socket socket, String command) throws IOException {

        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
        bf.write(command);
        bf.newLine();
        bf.flush();
    }

    private String readResponse(Socket socket) throws IOException {

        InputStream in = socket.getInputStream();
        String line;
        StringBuilder reply = new StringBuilder();
        do {
            line = readString(in);
            reply.append(line);
        } while (line.charAt(3) != ' ');

        return reply.toString();
    }

    static private String readString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        char value;

        while (true) {
            value = (char)is.read();
            if (value == '\n') break;
            sb.append(value);
        }

        return sb.toString();
    }

    private PasvCodeSocket pasv() throws IOException {
        sendCommand(controlSocket, "pasv");
        String reply = readResponse(controlSocket);
        String code = reply.substring(0, 3);
        if (!code.equals("227"))
            return new PasvCodeSocket(code, null);

        String rawAddressString = reply.substring(reply.indexOf('(') + 1, reply.indexOf(')'));
        String[] numberStrings = rawAddressString.split(",");

        StringBuilder addressString = new StringBuilder();
        addressString.append(numberStrings[0]);
        for (int i = 1; i < 4; i++) {
            addressString
                    .append(".")
                    .append(numberStrings[i]);
        }
        int dataPort = Integer.parseInt(numberStrings[4]) * 256 + Integer.parseInt(numberStrings[5]);

        return new PasvCodeSocket(code, new Socket(addressString.toString(), dataPort));
    }

    private ArrayList<Character> consumePasvData(Socket dataSocket) throws IOException {
        InputStream dataIn = dataSocket.getInputStream();

        int value;
        ArrayList<Character> data = new ArrayList<>();
        while ((value = dataIn.read()) != -1) {
            data.add((char)value);
        }
        dataIn.close();

        return data;
    }


}

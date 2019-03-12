package httpFtpProxy;


import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class FTPClient {

    private Socket controlSocket;
    private BufferedReader controlIn;
    private BufferedWriter controlOut;

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
        controlIn = new BufferedReader(new InputStreamReader(controlSocket.getInputStream()));
        controlOut = new BufferedWriter(new OutputStreamWriter(controlSocket.getOutputStream()));

        return readReply(controlIn).substring(0, 3);
    }

    public String auth(String user, String pass) throws IOException {
        sendCommand(controlOut, "user " + user);
        readReply(controlIn);

        sendCommand(controlOut, "pass " + pass);

        return readReply(controlIn).substring(0, 3);
    }

    public Proxy.ReplyDataStructure sendDataCommand(String command, String filePath, char type) throws IOException {
        Proxy.ReplyDataStructure replyDataStructure = new Proxy.ReplyDataStructure();

        sendCommand(controlOut, "type " + type);
        readReply(controlIn);

        PasvCodeSocket pasvCodeSocket = pasv();
        if (pasvCodeSocket.dataSocket == null) {
            replyDataStructure.setCode(pasvCodeSocket.replyCode.substring(0, 3));
            return replyDataStructure;
        }

        sendCommand(controlOut, command + " " + filePath);
        System.out.println(readReply(controlIn).substring(0, 3)); // read the first code

        ArrayList<Character> input = consumePasvData(pasvCodeSocket.dataSocket);
        replyDataStructure.setData(input);

        replyDataStructure.setCode(readReply(controlIn).substring(0, 3)); // read the second code


        return replyDataStructure;
    }

    private void sendCommand(BufferedWriter out, String command) throws IOException {
        out.write(command);
        out.newLine();
        out.flush();
    }

    private String readReply(BufferedReader in) throws IOException {
        String line = "1234";
        StringBuilder reply = new StringBuilder();
        while (!line.substring(3, 4).equals(" ")) {
            line = in.readLine();
            reply.append(line);
        }

        return reply.toString();
    }

    private PasvCodeSocket pasv() throws IOException {
        sendCommand(controlOut, "pasv");
        String reply = readReply(controlIn);
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

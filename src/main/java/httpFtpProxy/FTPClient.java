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
        private Socket dataSoket;

        public PasvCodeSocket(String replyCode, Socket dataSoket) {
            this.replyCode = replyCode;
            this.dataSoket = dataSoket;
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

    public Proxy.ReplyDataStructure list(String path) throws IOException {

        Proxy.ReplyDataStructure replyDataStructure = new Proxy.ReplyDataStructure();

        sendCommand(controlOut, "type A");
        readReply(controlIn);

        PasvCodeSocket pasvCodeSocket = pasv();
        if (pasvCodeSocket.dataSoket == null) {
            replyDataStructure.setCode(readReply(controlIn).substring(0, 3));
            return replyDataStructure;
        }

        BufferedReader dataIn = new BufferedReader(new InputStreamReader(pasvCodeSocket.dataSoket.getInputStream()));

        sendCommand(controlOut, "list " + path);
        readReply(controlIn); // read the first code (150)
        replyDataStructure.setCode(readReply(controlIn).substring(0, 3)); // read the second code (226)

        int value;
        ArrayList<Character> inputData = new ArrayList<>();
        while ((value = dataIn.read()) != -1) {
            inputData.add((char)value);
            System.out.print((char) value);
        }
        dataIn.close();

        replyDataStructure.setData(inputData);

        return replyDataStructure;
    }

    public Proxy.ReplyDataStructure retr(String filePath, String destDir) throws IOException {

        Proxy.ReplyDataStructure replyDataStructure = new Proxy.ReplyDataStructure();

        sendCommand(controlOut, "type I");
        readReply(controlIn);

        PasvCodeSocket pasvCodeSocket = pasv();
        if (pasvCodeSocket.dataSoket == null) {
            replyDataStructure.setCode(pasvCodeSocket.replyCode.substring(0, 3));
            return replyDataStructure;
        }

        BufferedReader dataIn = new BufferedReader(new InputStreamReader(pasvCodeSocket.dataSoket.getInputStream()));

        sendCommand(controlOut, "retr " + filePath);
        readReply(controlIn); // read the first code
        replyDataStructure.setCode(readReply(controlIn).substring(0, 3)); // read the second code

        int value;
        ArrayList<Character> input = new ArrayList<>();
        while ((value = dataIn.read()) != -1) {
            input.add((char)value);
        }
        dataIn.close();

        FileWriter fw = new FileWriter(destDir);
        for (char b : input)
            fw.write(b);
        fw.close();

        return replyDataStructure;
    }

//    public void testRetr()

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

    private String readAll(BufferedReader in) throws IOException {
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            data.append(line).append('\n');
        }

        return data.toString();
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

}

package httpFtpProxy;


import java.io.*;
import java.net.Socket;

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

    public String list(String path) throws IOException {

        PasvCodeSocket pasvCodeSocket = pasv();;
        if (pasvCodeSocket.dataSoket == null)
            return pasvCodeSocket.replyCode;

        BufferedReader dataIn = new BufferedReader(new InputStreamReader(pasvCodeSocket.dataSoket.getInputStream()));

        sendCommand(controlOut, "list " + path);
        int value = 0;
        char c = 0;
        while ((value = dataIn.read()) != -1) {
            c = (char)value;
            System.out.print(c);
        }
//        String listReply = readAll(dataIn);
        dataIn.close();

//        System.out.println(listReply);

        return "TODO structure in proxy";
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

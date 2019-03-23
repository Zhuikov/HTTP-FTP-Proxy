package httpFtpProxy;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

public class HTTPHandler {

    private static final String authorizationString = "Authorization: Basic ";
    private static final String fileCommand = "/file";

    public HTTPHandler() {}

    public Proxy.HTTPRequest receiveRequest(Socket socket) throws IOException {

        Proxy.HTTPRequest httpRequest = new Proxy.HTTPRequest();
        String requestHeaders = readRequestHeaders(socket);
        String[] requestLines = requestHeaders.split("\n");

        // process method (GET/PUT)
        String[] firstRequestLine = requestLines[0].split(" ");
        if (firstRequestLine[0].equals("PUT")) httpRequest.setMethod(Proxy.Method.PUT);
        else if (firstRequestLine[0].equals("GET")) httpRequest.setMethod(Proxy.Method.GET);

        int firstSlash = firstRequestLine[1].indexOf('/');

        // process path (ftp.sunet.su[/file]/path/to/file[?param="text"]
        // ip = "ftp.sunet.su"
        String ip = firstRequestLine[1].substring(0, firstSlash);
        String afterSlash = firstRequestLine[1].substring(firstSlash);

        if (afterSlash.length() > fileCommand.length() &&
                afterSlash.substring(0, fileCommand.length()).equals(fileCommand)) {
            httpRequest.setFile(true);
            afterSlash = afterSlash.substring(fileCommand.length());
        }

        int questionSign = afterSlash.indexOf('?');
        if (questionSign != -1) {
            httpRequest.setParam(afterSlash.substring(afterSlash.indexOf('\"') + 1, afterSlash.lastIndexOf('\"')));
            afterSlash = afterSlash.substring(0, questionSign);
        }

        if (httpRequest.isFile()) {
            httpRequest.setPath(ip + afterSlash);
        } else {
            httpRequest.setPath(ip + '/');
            httpRequest.setFtpCommand(afterSlash.substring(1));
        }

        // process the second request line
        String[] secondRequestLine = requestLines[1].split(" ");
        httpRequest.setHostName(secondRequestLine[1]);

        // process authorization line
        for (String s : requestLines) {
            if (s.length() > authorizationString.length() &&
                    s.substring(0, authorizationString.length()).equals(authorizationString)) {
                String decodedLoginPass = new String(Base64.getDecoder().decode(s.substring(authorizationString.length())));
                String[] loginPass = decodedLoginPass.split(":");
                httpRequest.setLogin(loginPass[0]);
                httpRequest.setPassword(loginPass[1]);
            }
        }

        if (httpRequest.getMethod() == Proxy.Method.PUT) {
            // дочитывает из сокета строку после заголовков
            httpRequest.setBody(readRequestBody(socket));
        }

        return httpRequest;
    }

    private String readRequestHeaders(Socket socket) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuilder request = new StringBuilder();
        String line;

        while (!(line = bf.readLine()).isEmpty()) {
            request
                    .append(line)
                    .append("\n");
        }
//        bf.close();

        return request.toString();
    }

    // todo can make error when reading file
    private ArrayList<Character> readRequestBody(Socket socket) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String bodyString = bf.readLine();
        bf.close();

        List body = Arrays.asList(bodyString.toCharArray());

        return new ArrayList<Character>(body);
    }

}

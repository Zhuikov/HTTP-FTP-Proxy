package httpFtpProxy;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Base64;

public class HTTPHandler {

    private static final String authorizationString = "Authorization: Basic ";
    private static final String contentLengthString = "Content-Length: ";
    private static final String fileCommand = "/file";

    public HTTPHandler() {}

    public Proxy.HTTPRequest receiveRequest(Socket socket) throws IOException {

        Proxy.HTTPRequest httpRequest = new Proxy.HTTPRequest();
        InputStream is = socket.getInputStream();

        String line;
        ArrayList<String> headers = new ArrayList<>();
        while (true) {
            line = readString(is);
            if (line.isEmpty()) break;
            headers.add(line);
        }

        // process method (GET/PUT/DELETE)
        String[] firstRequestLine = headers.get(0).split(" ");
        switch (firstRequestLine[0]) {
            case "PUT":
                httpRequest.setMethod(Proxy.Method.PUT);
                break;
            case "GET":
                httpRequest.setMethod(Proxy.Method.GET);
                break;
            case "DELETE":
                httpRequest.setMethod(Proxy.Method.DELETE);
                break;
            default:
                return httpRequest;
        }

        int firstSlash = firstRequestLine[1].indexOf('/');

        // process path (ftp.sunet.su[/file]/path/to/file[?param="text"])
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
        String[] secondRequestLine = headers.get(1).split(" ");
        httpRequest.setHostName(secondRequestLine[1]);

        // process authorization line
        for (String s : headers) {
            if (s.length() > authorizationString.length() &&
                    s.substring(0, authorizationString.length()).equals(authorizationString)) {
                String decodedLoginPass = new String(Base64.getDecoder().decode(s.substring(authorizationString.length())));
                String[] loginPass = decodedLoginPass.split(":");
                httpRequest.setLogin(loginPass[0]);
                httpRequest.setPassword(loginPass[1]);
            }

        }

        if (httpRequest.getMethod() == Proxy.Method.PUT) {
            int bodyLength = 0;
            for (String s : headers) {
                if (s.length() > contentLengthString.length() &&
                        s.substring(0, contentLengthString.length()).equals(contentLengthString)) {
                    String[] contentLen = s.split(" ");
                    bodyLength = Integer.parseInt(contentLen[1]);
                }
            }
            // дочитывает из сокета строку после заголовков
            httpRequest.setBody(readRequestBody(socket, bodyLength));
        }

        return httpRequest;
    }

    private ArrayList<Character> readRequestBody(Socket socket, int bodyLength) throws IOException {
        InputStream is = socket.getInputStream();
        ArrayList<Character> body = new ArrayList<>(bodyLength);
        int readBytes = 0;
        while (readBytes < bodyLength) {
            body.add((char) is.read());
            readBytes++;
        }
        return body;
    }

    private String readString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        char value;

        while (true) {
            value = (char)is.read();
            if (value == '\n') break;
            sb.append(value);
        }

        return sb.toString();
    }
}

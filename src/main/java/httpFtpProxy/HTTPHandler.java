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

    public Proxy.HTTPRequest receiveRequest(Socket socket) throws IOException {
        Proxy.HTTPRequest httpRequest = new Proxy.HTTPRequest();
//        String requestHeaders = readRequestHeaders(socket);
        String requestHeaders = "GET ftp.funet.fi/ HTTP1.1\nHost: 127.0.0.1\n" +
                "Authorization: Basic YW5vbnltb3VzOjEyMzEyMw==\n\n";

        String[] requestLines = requestHeaders.split("\n");

        System.out.println("SOCKET AFTER READ HEADERS = " + socket.isClosed());
        String[] firstRequestLine = requestLines[0].split(" ");
        if (firstRequestLine[0].equals("PUT")) httpRequest.setMethod(Proxy.Method.PUT);
        else if (firstRequestLine[0].equals("GET")) httpRequest.setMethod(Proxy.Method.GET);
        httpRequest.setPath(firstRequestLine[1]);

        String[] secondRequestLine = requestLines[1].split(" ");
        httpRequest.setHostName(secondRequestLine[1]);


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

    private ArrayList<Character> readRequestBody(Socket socket) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String bodyString = bf.readLine();
        bf.close();

        List body = Arrays.asList(bodyString.toCharArray());

        return new ArrayList<Character>(body);
    }

}

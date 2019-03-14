package httpFtpProxy;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HTTPHandler {


    public Proxy.HTTPRequest receiveRequest(Socket socket) throws IOException {
        Proxy.HTTPRequest httpRequest = new Proxy.HTTPRequest();
        String requestHeaders = readRequestHeaders(socket);

        String[] requestLines = requestHeaders.split("\n");

        String[] firstRequestLine = requestLines[0].split(" ");
        if (firstRequestLine[0].equals("PUT")) httpRequest.setMethod(Proxy.Method.PUT);
        else if (firstRequestLine[0].equals("GET")) httpRequest.setMethod(Proxy.Method.GET);
        httpRequest.setPath(firstRequestLine[1]);

        String[] secondRequestLine = requestLines[1].split(" ");
        httpRequest.setHostName(secondRequestLine[1]);

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
        bf.close();

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

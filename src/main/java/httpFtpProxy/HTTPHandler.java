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
        String request = readRequest(socket);

        String[] requestLines = request.split("\n");

        String[] firstRequestLine = requestLines[0].split(" ");
        httpRequest.setMethod(firstRequestLine[0]);
        httpRequest.setPath(firstRequestLine[1]);

        String[] secondRequestLine = requestLines[1].split(" ");
        httpRequest.setHostName(secondRequestLine[1]);

        if (!requestLines[requestLines.length - 1].isEmpty()) {
            char[] lastString = requestLines[requestLines.length - 1].toCharArray();
            List body = Arrays.asList(lastString);
            httpRequest.setBody(new ArrayList<Character>(body));
        }

        return httpRequest;
    }

    private String readRequest(Socket socket) throws IOException {
        BufferedReader bf = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        StringBuilder request = new StringBuilder();
        String line;

        while (!(line = bf.readLine()).isEmpty()) {
            request
                    .append(line)
                    .append("\n");
        }

        return request.toString();
    }

}

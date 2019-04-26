package httpFtpProxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.*;

public class ProxyTest {

    private Thread proxyThread;
    private Proxy proxy;

    static private int proxyPort;
    static private Random r = new Random();
    static private String proxyAddr = "localhost";
//    static private final String serverAddress = proxyAddr;
    static private final String serverAddress = System.getenv("HOST_FTP_SERVER_NAME") == null ?
            "localhost" : System.getenv("HOST_FTP_SERVER_NAME");
    static private final String loginPassword = "dGVzdGZ0cDp0ZXN0ZnRw";
    static private final String contentLength = "Content-Length: ";

    @Before
    public void setUp() throws InterruptedException {
        proxyPort = r.nextInt(500) + 15000;
        System.out.println("SERVER ADDRESS (proxyTest) = " + serverAddress + " port = " + proxyPort);
        proxy = new Proxy();
        proxyThread = new Thread(() -> {
            try {
                proxy.start(proxyPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        proxyThread.start();
        Thread.sleep(600);
    }

    @After
    public void tearDown() throws IOException, InterruptedException {
        proxy.stop();
        proxyThread.join();
    }

    private class HttpResponse {
        HttpResponse() {}
        String header = null;
        ArrayList<Character> body = null;
    }

    @Test
    public void badAuthTest() throws IOException {

        Socket socket = new Socket(proxyAddr, proxyPort);
        String request = "GET " + serverAddress + "/file/?type=\"A\" HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic ZnRwdGVzdDpmdHB0ZXN0\n\n";

        OutputStream os = socket.getOutputStream();
        os.write(request.getBytes());

        InputStream is = socket.getInputStream();
        HttpResponse response = readResponse(is);

        socket.close();

        assertEquals(response.header, "400");
        assertEquals(response.body.size(), 0);
    }

    @Test
    public void badConnectionTest() throws IOException {

        Socket socket = new Socket(proxyAddr, proxyPort);
        String request = "GET " + serverAddress + "dddddd/pwd HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic ZnRwdGVzdDpmdHB0ZXN0\n\n";

        OutputStream os = socket.getOutputStream();
        os.write(request.getBytes());

        InputStream is = socket.getInputStream();
        HttpResponse response = readResponse(is);

        socket.close();

        assertEquals(response.header, "500");
        assertEquals(response.body.size(), 0);
    }

    @Test
    public void listTest() throws IOException {

        Socket socket = new Socket(proxyAddr, proxyPort);
        String request = "GET " + serverAddress + "/file/?type=\"A\" HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n\n";

        OutputStream os = socket.getOutputStream();
        os.write(request.getBytes());

        InputStream is = socket.getInputStream();
        HttpResponse response = readResponse(is);

        socket.close();

        assertEquals(response.header, "200");
        String actualString = "-rw-r--r--    1 ftp      ftp         90416 Apr 23 13:43 test_image.jpg\r\n";
        ArrayList<Character> actualBody = new ArrayList<>();
        for (int i = 0; i < actualString.length(); i++) {
            actualBody.add(actualString.charAt(i));
        }
        assertEquals(response.body, actualBody);
    }

    @Test
    public void getFileTest() throws IOException {

        Socket socket = new Socket(proxyAddr, proxyPort);
        String request = "GET " + serverAddress + "/file/test_image.jpg?type=\"I\" HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n\n";

        OutputStream os = socket.getOutputStream();
        os.write(request.getBytes());

        InputStream is = socket.getInputStream();
        HttpResponse response = readResponse(is);

        socket.close();

        assertEquals(response.header, "200");
        ArrayList<Character> expectedFileData = FTPClientTest.getFileData("./test_files/testftp/test_image.jpg");
        assertEquals(response.body, expectedFileData);
    }

    @Test
    public void putAndDeleteFileTest() throws IOException {

        Socket putSocket = new Socket(proxyAddr, proxyPort);
        ArrayList<Character> rawFileData = FTPClientTest.getFileData("./Dockerfile");
        String putRequest = "PUT " + serverAddress + "/file/Dockerfile?type=\"A\" HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n" +
                 contentLength + rawFileData.size() + "\n\n";
        StringBuilder sb = new StringBuilder(putRequest);
        for (char c : rawFileData) {
            sb.append(c);
        }

        OutputStream os = putSocket.getOutputStream();
        os.write(sb.toString().getBytes());

        InputStream is = putSocket.getInputStream();
        HttpResponse putResponse = readResponse(is);

        assertEquals(putResponse.header, "200");
        assertEquals(putResponse.body.size(), 0);
        putSocket.close();

        // check correct transfer
        Socket getSocket = new Socket(proxyAddr, proxyPort);
        String downloadRequest = "GET " + serverAddress + "/file/Dockerfile?type=\"A\" HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n\n";

        os = getSocket.getOutputStream();
        os.write(downloadRequest.getBytes());

        is = getSocket.getInputStream();
        HttpResponse downloadResponse = readResponse(is);

        assertEquals(downloadResponse.header, "200");
        assertEquals(downloadResponse.body, rawFileData);

        getSocket.close();

        Socket deleteSocket = new Socket(proxyAddr, proxyPort);
        String deleteRequest = "DELETE " + serverAddress + "/file/Dockerfile HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n\n";

        os = deleteSocket.getOutputStream();
        os.write(deleteRequest.getBytes());

        is = deleteSocket.getInputStream();
        HttpResponse deleteResponse = readResponse(is);

        assertEquals(deleteResponse.header, "200");
        assertEquals(deleteResponse.body.size(), 0);

        deleteSocket.close();
    }

    @Test
    public void pwdTest() throws IOException {

        Socket socket = new Socket(proxyAddr, proxyPort);
        String request = "GET " + serverAddress + "/pwd HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n\n";

        OutputStream os = socket.getOutputStream();
        os.write(request.getBytes());

        InputStream is = socket.getInputStream();
        HttpResponse response = readResponse(is);

        socket.close();

        assertEquals(response.header, "200");
        assertEquals(response.body.get(0).charValue(), '/');
    }

    @Test
    public void cwdTest() throws IOException {

        Socket socket = new Socket(proxyAddr, proxyPort);
        String request = "GET " + serverAddress + "/cwd?dir=\"/\" HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n\n";

        OutputStream os = socket.getOutputStream();
        os.write(request.getBytes());

        InputStream is = socket.getInputStream();
        HttpResponse response = readResponse(is);

        socket.close();

        assertEquals(response.header, "200");
        assertEquals(response.body.size(), 0);
    }

    @Test
    public void quitTest() throws IOException {

        Socket socket = new Socket(proxyAddr, proxyPort);
        String request = "GET " + serverAddress + "/quit + HTTP/1.1\n" +
                "Host: " + proxyAddr +
                "\nAuthorization: Basic " + loginPassword + "\n\n";

        OutputStream os = socket.getOutputStream();
        os.write(request.getBytes());

        InputStream is = socket.getInputStream();
        HttpResponse response = readResponse(is);

        socket.close();

        assertEquals(response.header, "200");
        assertEquals(response.body.size(), 0);
    }

    private HttpResponse readResponse(InputStream is) throws IOException {

        HttpResponse httpResponse = new HttpResponse();

        String line;
        ArrayList<String> headers = new ArrayList<>();
        while (true) {
            line = readString(is);
            if (line.isEmpty()) break;
            headers.add(line);
        }

        String[] firstLine = headers.get(0).split(" ");
        httpResponse.header = firstLine[1];

        int bodyLength = 0;
        for (String s : headers) {
            if (s.length() > contentLength.length() &&
                    s.substring(0, contentLength.length()).equals(contentLength)) {
                bodyLength = Integer.parseInt(s.substring(contentLength.length()));
                break;
            }
        }

        int readBytes = 0;
        ArrayList<Character> bodyData = new ArrayList<>();
        while (readBytes < bodyLength) {
            bodyData.add((char) is.read());
            readBytes++;
        }

        httpResponse.body = bodyData;

        return httpResponse;
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
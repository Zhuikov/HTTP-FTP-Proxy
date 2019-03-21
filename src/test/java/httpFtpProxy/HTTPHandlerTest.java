package httpFtpProxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static org.junit.Assert.*;

public class HTTPHandlerTest {

    private static Socket socket;
    private static HTTPHandler handler = new HTTPHandler();
    private static final String login = "anonymous";
    private static final String pass  = "password";
    private static final String host = "127.0.0.1";
    private static final String encodedLoginPass = "YW5vbnltb3VzOnBhc3N3b3Jk";

    private static void send(Socket socket, String s) throws IOException {
        OutputStream os = socket.getOutputStream();
        os.write(s.getBytes());
    }

    @Before
    public void setUp() throws Exception {
        ServerSocket serverSocket = new ServerSocket(7500);
//        serverSocket.accept();
    }

    @After
    public void tearDown() throws Exception {
        socket.close();
    }

    @Test
    public void receiveRequest() throws IOException {
        String response = "GET ftp.sunet.su/file/path/to/file/" +
                host + '\n' +
                "Authorization: Basic " + encodedLoginPass;
        socket = new Socket(host, 7500);
        send(socket, response);

        Proxy.HTTPRequest request = handler.receiveRequest(socket);

        assertEquals(request.getMethod(), Proxy.Method.GET);
        assertEquals(request.getHostName(), "127.0.0.1");
        assertEquals(request.getPath(), "ftp.sunet.su/path/to/file");
        assertTrue(request.isFile());
        assertEquals(request.getPath(), "/path/to/file");
        assertEquals(request.getLogin(), login);
        assertEquals(request.getPassword(), pass);
        assertNull(request.getFtpCommand());
        assertNull(request.getBody());
    }
}
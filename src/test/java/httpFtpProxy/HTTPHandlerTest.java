package httpFtpProxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.*;

public class HTTPHandlerTest {

    private FTPServer ftpServer = new FTPServer();

    private static HTTPHandler handler = new HTTPHandler();
    private static final String login = "anonymous";
    private static final String pass  = "password";
    private static final String encodedLoginPass = "YW5vbnltb3VzOnBhc3N3b3Jk";

    @Before
    public void setUp() throws IOException {
//        ftpServer.start();
        ftpServer.begin();
        ftpServer.start();
    }

    @After
    public void tearDown() throws IOException {
//        ftpServer.join();
        ftpServer.close();
    }

    @Test
    public void receiveRequest() throws IOException {
        String response = "GET ftp.sunet.su/file/path/to/file/\n" +
                "Host: " + FTPServer.host +
                "\nAuthorization: Basic " + encodedLoginPass +
                "\nContent-Length: 0\n\n";

        ftpServer.setMessage(response);
        Socket client = new Socket(FTPServer.host, FTPServer.port);
        Proxy.HTTPRequest request = handler.receiveRequest(client);

        assertEquals(request.getMethod(), Proxy.Method.GET);
        assertEquals(request.getHostName(), FTPServer.host);
        assertEquals(request.getPath(), "ftp.sunet.su/path/to/file/");
        assertTrue(request.isFile());
        assertEquals(request.getLogin(), login);
        assertEquals(request.getPassword(), pass);
        assertNull(request.getFtpCommand());
        assertNull(request.getParam());
        assertNull(request.getBody());
        client.close();
    }
}
package httpFtpProxy;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;

public class HTTPHandlerTest {

    private FTPServer ftpServer = new FTPServer();

    private static HTTPHandler handler = new HTTPHandler();
    private static final String login = "anonymous";
    private static final String pass  = "password";
    private static final String encodedLoginPass = "YW5vbnltb3VzOnBhc3N3b3Jk";

    @Before
    public void setUp() throws IOException {
        ftpServer.begin();
        ftpServer.start();
    }

    @After
    public void tearDown() throws IOException {
        ftpServer.close();
    }

    @Test
    public void receiveGetRequest() throws IOException {
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

    @Test
    public void receivePutRequest() throws IOException {
        ArrayList<Character> body =
                new ArrayList<>(Arrays.asList('1', '2', '3', '4'));
        String response = "PUT ftp.sunet.su/file/serverFile/path\n" +
                "Host: " + FTPServer.host +
                "\nAuthorization: Basic " + encodedLoginPass +
                "\nContent-Length: 4\n\n" + "1234";
        ftpServer.setMessage(response);
        Socket client = new Socket(FTPServer.host, FTPServer.port);
        Proxy.HTTPRequest request = handler.receiveRequest(client);

        assertEquals(request.getMethod(), Proxy.Method.PUT);
        assertEquals(request.getHostName(), FTPServer.host);
        assertEquals(request.getPath(), "ftp.sunet.su/serverFile/path");
        assertTrue(request.isFile());
        assertEquals(request.getLogin(), login);
        assertEquals(request.getPassword(), pass);
        assertNull(request.getFtpCommand());
        assertNull(request.getParam());
        assertEquals(request.getBody(), body);
        client.close();
    }

    @Test
    public void receiveFtpCommand() throws IOException {
        String response = "GET ftp.sunet.su/cwd?dir=\"/path/of/dir/\"\n" +
                "Host: " + FTPServer.host +
                "\nAuthorization: Basic " + encodedLoginPass +
                "\nContent-Length: 0\n\n";
        ftpServer.setMessage(response);
        Socket client = new Socket(FTPServer.host, FTPServer.port);
        Proxy.HTTPRequest request = handler.receiveRequest(client);

        assertEquals(request.getMethod(), Proxy.Method.GET);
        assertEquals(request.getHostName(), FTPServer.host);
        assertEquals(request.getPath(), "ftp.sunet.su/");
        assertFalse(request.isFile());
        assertEquals(request.getLogin(), login);
        assertEquals(request.getPassword(), pass);
        assertEquals(request.getFtpCommand(), FTPClient.cwdCommand);
        assertEquals(request.getParam(), "/path/of/dir/");
        assertNull(request.getBody());
        client.close();
    }

    @Test
    public void receiveRandomHeaders() throws IOException {
        String response = "132213ke932e233e\n\n";
        ftpServer.setMessage(response);
        Socket client = new Socket(FTPServer.host, FTPServer.port);
        Proxy.HTTPRequest request = handler.receiveRequest(client);

        assertNull(request.getMethod());
        assertNull(request.getPath());
        assertNull(request.getParam());
        assertNull(request.getFtpCommand());
        assertNull(request.getLogin());
        assertNull(request.getPassword());
        assertFalse(request.isFile());
        assertNull(request.getHostName());
        assertNull(request.getBody());
        client.close();
    }
}
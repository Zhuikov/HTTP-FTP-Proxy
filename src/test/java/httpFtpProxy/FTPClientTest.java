package httpFtpProxy;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class FTPClientTest {

    private FTPClient ftpClient = new FTPClient();
    static final String serverAddress = "ftp-server";
    static final String username = "testftp";
    static final String password = "testftp";

    @Test
    public void connectionTest() throws IOException {

        assertFalse(ftpClient.isConnected());

        ftpClient.connect(serverAddress);
        assertTrue(ftpClient.isConnected());

        ftpClient.disconnect();
        assertFalse(ftpClient.isConnected());
    }

    @Test
    public void badAuthTest() throws IOException {

        ftpClient.connect(serverAddress);
        assertFalse(ftpClient.isAuth());

        ftpClient.auth(username, password);
        assertTrue(ftpClient.isAuth());

        ftpClient.disconnect();
    }

    @Test
    public void cwdTest() throws IOException {

        ftpClient.connect(serverAddress);
        ftpClient.auth(username, password);
        ftpClient.cwd("/");

        Proxy.DataAndCode response = ftpClient.pwd();
        assertEquals(response.getCode(), "257");
        assertEquals(response.getData().get(0).charValue(), '/');
    }

    @Test
    public void downloadTest() throws IOException {

        ftpClient.connect(serverAddress);
        ftpClient.auth(username, password);

        Proxy.DataAndCode response =
                ftpClient.sendDataCommand("retr", "./test_image.jpg", 'I');

        FileInputStream fileInputStream = new FileInputStream("./test_files/test_image.jpg");
        int i;
        ArrayList<Character> file = new ArrayList<>();
        while ((i = fileInputStream.read()) != -1) {
            file.add((char)i);
        }
        fileInputStream.close();

        assertEquals(response.getCode(), "226");
        assertEquals(file, response.getData());
    }

}
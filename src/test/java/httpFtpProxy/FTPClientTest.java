package httpFtpProxy;

import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.*;

public class FTPClientTest {

    private FTPClient ftpClient = new FTPClient();
    static private final String serverAddress = System.getenv("HOST_FTP_SERVER_NAME") == null ?
            "localhost" : System.getenv("HOST_FTP_SERVER_NAME");
    static private final String username = "testftp";
    static private final String password = "testftp";

    static public ArrayList<Character> getFileData(String path) {

        ArrayList<Character> fileData = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(path)) {
            int i;
            while ((i = fileInputStream.read()) != -1) {
                fileData.add((char) i);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return fileData;
    }

    @Test
    public void connectionTest() throws IOException {

        System.out.println("SERVER ADDRESS (ftpClientTest) = " + serverAddress);

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

        ftpClient.disconnect();
    }

    @Test
    public void downloadTest() throws IOException {

        ftpClient.connect(serverAddress);
        ftpClient.auth(username, password);

        Proxy.DataAndCode response =
                ftpClient.sendDataCommand("retr", "./test_image.jpg", 'I');

        ArrayList<Character> file = getFileData("./test_files/testftp/test_image.jpg");

        assertEquals(response.getCode(), "226");
        assertEquals(file, response.getData());

        ftpClient.disconnect();
    }

    @Test
    public void uploadAndDeleteTest() throws IOException {

        ftpClient.connect(serverAddress);
        ftpClient.auth(username, password);

        ArrayList<Character> fileData = getFileData("./Dockerfile");
        String uploadCode = ftpClient.stor(fileData, "./test_Dockerfile", 'A');

        assertEquals(uploadCode, "226");

        Proxy.DataAndCode downloadFileResponse =
                ftpClient.sendDataCommand("retr", "./test_Dockerfile", 'A');
        assertEquals(downloadFileResponse.getCode(), "226");

        assertEquals(fileData, downloadFileResponse.getData());

        String deleteCode = ftpClient.dele("./test_Dockerfile");

        assertEquals(deleteCode, "250");

        ftpClient.disconnect();
    }

}
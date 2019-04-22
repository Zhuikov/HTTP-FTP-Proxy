package httpFtpProxy;

import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.*;

public class FTPClientTest {

    private FTPClient ftpClient = new FTPClient();

    @Test
    public void connectionTest() throws IOException {

        assertFalse(ftpClient.isConnected());

        ftpClient.connect("ftp-server");
        assertTrue(ftpClient.isConnected());

        ftpClient.disconnect();
        assertFalse(ftpClient.isConnected());
    }

    @Test
    public void badAuthTest() throws IOException {

        ftpClient.connect("ftp-server");
        assertFalse(ftpClient.isAuth());

        ftpClient.auth("testftp", "testftp");
        assertTrue(ftpClient.isAuth());

        ftpClient.disconnect();
    }


    // copy of method FTPClient.sendCommand();
//    private void sendCommand(Socket socket, String command) throws IOException {
//        BufferedWriter bf = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
//        bf.write(command);
//        bf.newLine();
//        bf.flush();
//    }

}
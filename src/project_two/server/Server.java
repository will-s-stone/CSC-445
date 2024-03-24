package project_two.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Server {
    public static void main(String[] args) throws InterruptedException {

    }

    public void sendFile(String host, int port, String filepath, int windowSize) throws IOException {
        Socket socket = new Socket(host, port);
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());




    }


}

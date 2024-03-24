package project_two.server;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

public class Server {
    public static void main(String[] args) throws InterruptedException {

    }

    public void sendFile(String host, int port, String filepath, int windowSize) throws IOException {
        Socket socket = new Socket(host, port);
        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

        File file = new File(filepath);
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] fileData = new byte[(int) file.length()];
        fileInputStream.read(fileData);

        int startPos = 0;
        int nextSeq = 0;

        while (startPos < fileData.length){
            //Cha Cha Slide here
            while(nextSeq < startPos + windowSize && nextSeq < fileData.length){
                Packet packet = new Packet(nextSeq, fileData[nextSeq])
            }
        }



    }


}

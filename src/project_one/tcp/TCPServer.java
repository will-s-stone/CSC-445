package project_one.tcp;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;

public class TCPServer {
    long INITIAL_KEY = 123;
    int port = 27001;
    boolean status = true;
    int numOfBytes = 8;
    public void receiveMessagePartOne(){
        try {
            ServerSocket serverSocket = null;
            Socket socket = null;
            InputStream inputStream = null;
            while(status){
                serverSocket = new ServerSocket(port);
                System.out.println("Server started. Waiting for client...");

                socket = serverSocket.accept(); // Wait for a client to connect
                System.out.println("Client connected.");

                inputStream = socket.getInputStream();
                byte[] receivedBytes = inputStream.readNBytes(numOfBytes);
                System.out.println(Arrays.toString(receivedBytes));
                long[] decryptedMsg = decryptByteArr(receivedBytes);
                System.out.println(Arrays.toString(decryptedMsg));
                byte[] sendBackForValidation = longArrToByteArr(decryptedMsg);

                OutputStream outputStreamForValidation = socket.getOutputStream(); // For sending back and validating
                outputStreamForValidation.write(sendBackForValidation);
                outputStreamForValidation.flush();
            }
            assert inputStream != null;
            inputStream.close();
            socket.close();
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void receiveMessagePartTwo() throws IOException {
        try(ServerSocket serverSocket = new ServerSocket(port); Socket clientSocket = serverSocket.accept(); OutputStream out = clientSocket.getOutputStream(); InputStream in = clientSocket.getInputStream()){
            while(true){
                byte[] recMsg = in.readNBytes(numOfBytes + 8);
                long[] decryptedMsg = decryptByteArr(recMsg);
                byte[] ack = new byte[8];
                System.arraycopy(longArrToByteArr(decryptedMsg),0,ack,0,8);

                out.write(ack);
                out.flush();
            }
        } catch (IOException e){
            throw new RuntimeException();
        }

    }

    byte[] longArrToByteArr(long[] longs){
        byte[] result = new byte[longs.length * 8];
        int numChunks = longs.length/8;
        int chunkSize = 8;
        for (int i = 0; i < longs.length; i++) {
            byte[] temp = longToByteArr(longs[i]);
            // Add byte[] to result.
            System.arraycopy(temp, 0, result, i * chunkSize, chunkSize);
        }
        return result;
    }
    long[] decryptByteArr(byte[] bytes){
        // Convert to an array of longs that we can validate.
        int chunkSize = 8;
        int numOfChunks = bytes.length / chunkSize;
        long[] result = new long[numOfChunks];
        for (int i = 0; i < numOfChunks; i++) {
            byte[] temp = new byte[chunkSize];
            System.arraycopy(bytes, i * chunkSize, temp, 0, chunkSize);

            result[i] = decrypt(byteArrToLong(temp));
        }
        return result;
    }

    //Big endian use-case.
    static long byteArrToLong(byte[] byteArray) {
        long value = 0;
        int len = Math.min(byteArray.length, 8); // Ensure we handle at most 8 bytes
        for (int i = len - 1; i >= 0; i--) {
            value <<= 8; // Shift left by 8 bits
            value |= (byteArray[i] & 0xFFL); // Combine with the byte value
        }
        return value;
    }
    static byte[] longToByteArr(long l){
        byte[] longBytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            longBytes[i] = (byte) (l >> (i * 8));
        }
        return longBytes;
    }
    static long xorShift(long r) { r ^= r << 13; r ^= r >>> 7; r ^= r << 17; return r; }
    long encrypt(long val){
        long key = INITIAL_KEY;
        key = xorShift(key);
        val ^= key;
        return val;
    }
    long decrypt(long encryptedVal){
        long key = INITIAL_KEY;
        key = xorShift(key);
        encryptedVal ^= key;
        //Update key with rng
        INITIAL_KEY = xorShift(INITIAL_KEY);
        return encryptedVal;
    }

    public static void main(String[] args) throws IOException {
        TCPServer server = new TCPServer();
        //server.receiveMessagePartOne();
        server.receiveMessagePartTwo();
    }
}

package project_one.udp;
import java.io.IOException;
import java.net.*;
import java.util.Arrays;

public class UDPServer {
    // For Part 2
    //int numberOfMessages = 0;
    //int sizeOfMessages = 0;
    private long INITIAL_KEY = 123;
    int port = 27001;
    boolean status = true;
    int numOfBytes = 8;
    public void receiveMessagePartOne(){
        try {
            DatagramSocket socket = null;
            while(status){
                socket = new DatagramSocket(port);
                byte[] buffer = new byte[numOfBytes];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                byte[] receivedBytes = packet.getData();
                System.out.println(Arrays.toString(receivedBytes));
                long[] decryptedMsg = decryptByteArr(receivedBytes);
                System.out.println(Arrays.toString(decryptedMsg));
                byte[] sendBackForValidation = longArrToByteArr(decryptedMsg);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(sendBackForValidation, sendBackForValidation.length, address, port);
                socket.send(packet);

                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void receiveMessagePartTwo(){
        try {
            DatagramSocket socket = null;
            socket = new DatagramSocket(port);
            while(status){

                byte[] buffer = new byte[numOfBytes + 8];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                socket.receive(packet);

                byte[] receivedBytes = packet.getData();

                long[] decryptedMsg = decryptByteArr(receivedBytes);
                long ackLong = decryptedMsg[0];
                System.out.println(ackLong);
                byte[] ack = longToByteArr(ackLong);

                InetAddress address = packet.getAddress();
                int port = packet.getPort();
                packet = new DatagramPacket(ack, ack.length, address, port);
                socket.send(packet);

            }
            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
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
        System.out.println("Key is: " + INITIAL_KEY);
        INITIAL_KEY = xorShift(INITIAL_KEY);
        return encryptedVal;
    }

    public static void main(String[] args) {
        // Part 1
        UDPServer server = new UDPServer();
//            server.receiveMessagePartOne();
        server.receiveMessagePartTwo();
    }
}
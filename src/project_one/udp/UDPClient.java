package project_one.udp;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class UDPClient {
    // For Part 2
    int numberOfMessages = 1024;
    // For Part 1
    int numOfBytes = 1024;
    long VAL = 235711131;
    long INITIAL_KEY = 123;
    int port = 27001;
    String url = "moxie.cs.oswego.edu";
    int msgSize = numOfBytes/8;
    public void sendMessagePartOne(){
        try {
            DatagramSocket socket = new DatagramSocket();

            long[] message = createLongArr(msgSize);

            byte[] bytesToSend = null;
            for (int i = 0; i < msgSize; i++) {
                if (bytesToSend == null) {
                    bytesToSend = longToByteArr(encrypt(VAL));
                } else {
                    bytesToSend = concatenateByteArrays(bytesToSend, longToByteArr(encrypt(VAL)));
                }
            }

            InetAddress address = InetAddress.getByName(url);
            assert bytesToSend != null;
            DatagramPacket packet = new DatagramPacket(bytesToSend, bytesToSend.length, address, port);
            socket.send(packet);

            byte[] buffer = new byte[numOfBytes];
            DatagramPacket recPacket = new DatagramPacket(buffer, buffer.length);
            socket.receive(recPacket);
            byte[] receivedBytes = recPacket.getData();
            byte[] validationBytes = longArrToByteArr(message);

            if(Arrays.equals(validationBytes, receivedBytes)){
                System.out.println("Message Validated");
            } else {
                System.out.println("Contents of received message: " + Arrays.toString(receivedBytes));
                System.out.println("Contents of sent message: " + Arrays.toString(validationBytes));
            }


            socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessagePartTwo(){
        try {
            DatagramSocket socket = new DatagramSocket();

            InetAddress address = InetAddress.getByName(url);

            for (int i = 0; i < numberOfMessages; i++) {
                byte[] bytesToSend = null;
                long ackLong = 321;
                byte[] ack = longToByteArr(ackLong);

                for (int j = 0; j < msgSize; j++) {
                    if (bytesToSend == null) {
                        // First 8 bytes of our message is our ack
                        bytesToSend = concatenateByteArrays(longToByteArr(encrypt(ackLong)), longToByteArr(encrypt(VAL)));
                    } else {
                        bytesToSend = concatenateByteArrays(bytesToSend, longToByteArr(encrypt(VAL)));
                    }
                }
                System.out.println(Arrays.toString(bytesToSend));
                assert bytesToSend != null;
                DatagramPacket packet = new DatagramPacket(bytesToSend, bytesToSend.length, address, port);
                socket.send(packet);

                byte[] ackB = new byte[8];
                DatagramPacket recPacket = new DatagramPacket(ackB, ackB.length);
                socket.receive(recPacket);
                byte[] ackPacket = recPacket.getData();
                System.out.println();

                if(Arrays.equals(ackPacket, ack)){
                    System.out.println("Message validated");
                } else {
                    System.out.println("Error: Message not validated");
                    System.out.println("Received Message: " + Arrays.toString(ackPacket));
                    System.out.println("Message: " + Arrays.toString(ack));
                }
            }

            socket.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
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
    byte[] longArrToByteArr(long[] longs){
        byte[] result = new byte[longs.length * 8];
        int chunkSize = 8;
        for (int i = 0; i < longs.length; i++) {
            byte[] temp = longToByteArr(longs[i]);
            // Add byte[] to result.
            System.arraycopy(temp, 0, result, i * chunkSize, chunkSize);
        }
        return result;
    }
    long[] createLongArr(int messageSize){
        long[] result = new long[messageSize];
        Arrays.fill(result, VAL);
        return result;
    }
    static long byteArrToLong(byte[] byteArray) {
        long value = 0;
        int len = Math.min(byteArray.length, 8); // Ensure we handle at most 8 bytes
        for (int i = len - 1; i >= 0; i--) {
            value <<= 8; // Shift left by 8 bits
            value |= (byteArray[i] & 0xFFL); // Combine with the byte value
        }
        return value;
    }

    static byte[] concatenateByteArrays(byte[] a, byte[] b) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(a);
        outputStream.write(b);
        return outputStream.toByteArray();
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
        //Update key with rng
        System.out.println("Key is: " + INITIAL_KEY);
        INITIAL_KEY = xorShift(INITIAL_KEY);
        return val;
    }
    long decrypt(long encryptedVal){
        long key = INITIAL_KEY;
        key = xorShift(key);
        encryptedVal ^= key;
        return encryptedVal;
    }

    public long timeTakenPartOne(){
        //Note, message size is in terms of how many 8 bytes. E.g. 1 would mean
        //you send a single long (8 bytes), 2 would mean you send two longs (16 bytes).
        long timer = System.nanoTime();
        sendMessagePartOne();
        timer = System.nanoTime() - timer;
        return timer;
    }
    public long timeTakenPartTwo(){
        long timer = System.nanoTime();
        sendMessagePartTwo();
        timer = System.nanoTime() - timer;
        System.out.println("Timer: " + timer);
        return timer;
    }
    public double throughputPartTwo(){
        double result = (double) (numberOfMessages * numOfBytes * 8) / timeTakenPartTwo();
        //Nanoseconds to seconds
        return result * 1000000000;
    }

    public static void main(String[] args) {
        // Part 1
        UDPClient client = new UDPClient();
//        System.out.println(client.timeTakenPartOne());
        //client.sendMessagePartTwo();
        System.out.println("Bits per second: "  + client.throughputPartTwo());
    }
}


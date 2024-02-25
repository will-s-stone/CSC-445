package project_one.tcp;
import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class TCPClient {
    int numOfMessages = 1024;
    int numOfBytes = 1024;
    long VAL = 235711131;
    long INITIAL_KEY = 123;
    int port = 27001;
    String host = "moxie.cs.oswego.edu";
    int msgSize = numOfBytes/8;
    public void sendMessagePartOne(){
        try {
            long[] message = createLongArr(msgSize);
            System.out.println(System.currentTimeMillis());
            Socket socket = new Socket(host, port);
            System.out.println("Connected to server.");
            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStreamForValidation = socket.getInputStream();

            byte[] bytesToSend = null;
            for (int i = 0; i < msgSize; i++) {
                if (bytesToSend == null) {
                    bytesToSend = longToByteArr(encrypt(VAL));
                } else {
                    bytesToSend = concatenateByteArrays(bytesToSend, longToByteArr(encrypt(VAL)));
                }
            }

            outputStream.write(bytesToSend);
            outputStream.flush();

            byte[] receivedBytes = inputStreamForValidation.readNBytes(msgSize * 8);

            System.out.println("Length of byte array for validation: " + receivedBytes.length);
            byte[] validationBytes = longArrToByteArr(message);

            if(Arrays.equals(validationBytes, receivedBytes)){
                System.out.println("Message validated");
            } else {
                System.out.println("Contents of received message: " + Arrays.toString(receivedBytes));
                System.out.println("Contents of sent message: " + Arrays.toString(validationBytes));
            }
            inputStreamForValidation.close();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessagePartTwo(){
        try(Socket echoSocket = new Socket(host, port); OutputStream out = echoSocket.getOutputStream(); InputStream in = echoSocket.getInputStream()) {

            for (int i = 0; i < numOfMessages; i++) {
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
                out.write(bytesToSend);
                out.flush();
                byte[] recAck = in.readNBytes(8);

                if (Arrays.equals(recAck, ack)){
                    System.out.println("Message #" + i + " validated.");
                } else {
                    System.out.println("Oops...");
                }
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
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
        INITIAL_KEY = xorShift(INITIAL_KEY);
        return val;
    }
    long decrypt(long encryptedVal){
        long key = INITIAL_KEY;
        key = xorShift(key);
        encryptedVal ^= key;
        return encryptedVal;
    }
    public long timeTaken(){
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
        double result = (double) (numOfMessages * numOfBytes * 8) / timeTakenPartTwo();
        //Nanoseconds to seconds
        return result * 1000000000;
    }

    public static void main(String[] args) {
        TCPClient client = new TCPClient();
        //System.out.println(client.timeTaken());
        System.out.println("Bits per second: " + client.throughputPartTwo());

    }
}

package project_two.additional;

public class Main {

    public static void main(String[] args) {
        long sharedKey = 453; // Shared key
        byte[] originalData = "Hello, world!".getBytes();
        byte[] encryptedData = encrypt(originalData, sharedKey);
        System.out.println("Og: " + new String(originalData));
        System.out.println("Enc: " + new String(encryptedData));

        // Send encryptedData to the receiver...

        // On the receiver's side
        byte[] decryptedData = decrypt(encryptedData, sharedKey);
        System.out.println("final: " + new String(decryptedData));
    }
    public static byte[] encrypt(byte[] data, long key) {
        byte[] encrypted = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            encrypted[i] = (byte) (data[i] ^ (key & 0xFF));
            key = (key >> 8) | ((key & 0xFF) << 56); // Rotate key
        }
        return encrypted;
    }

    public static byte[] decrypt(byte[] encryptedData, long key) {
        return encrypt(encryptedData, key); // XOR encryption is symmetric
    }
}

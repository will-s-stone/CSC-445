package project_two.additional;

public class TFTP {
    //Adapted from Jacob Peck, http://cs.oswego.edu/~jpeck2/445/a2/
    // OP Codes
    public static final byte[] RRQ = {0,1};
    public static final byte[] WRQ = {0,2};
    public static final byte[] DATA = {0,3};
    public static final byte[] ACK = {0,4};
    public static final byte[] ERROR = {0,5};
    public static byte[] OCTET = null;
    public static final int BLOCK_SIZE = 512;
    public static final byte ZERO_BYTE = 0;
    public static final byte ONE_BYTE = 1;
}

package manager.protocol;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.lang3.StringUtils;

import io.netty.util.internal.StringUtil;

public class Header {
    
    public static final int PAYLOAD_LENGTH_OFFSET = 12;
    
    public static final int PAYLOAD_LENGTH_SIZE = 4;
    
    public static final int HEADER_LENGTH = 16;
    
    private int version = 2;
    private OperationType operationType;
    private byte[] deviceIp;
    private int devicePort;
    private int sessionId;
    private int payLoadLength;
    
    public enum OperationType {
        DATA(1),
        CONFIG(2);
        private final int value;

        private OperationType(int value) {
            this.value = value;
        }

        public static OperationType valueOf(int v) {
            for (OperationType value : values()) {
                if (value.value == v) {
                    return value;
                }
            }
            return null;
        }
    }
    
    public Header(OperationType operationType, String ip, int port, int sId, int payLength) {
        this.operationType = operationType;
        this.deviceIp = parseIp2Bytes(ip);
        this.devicePort = port;
        this.sessionId = sId;
        this.payLoadLength = payLength;
    }
    
    public ByteBuffer toByteBuf() {
        byte byte0 = 0;
        byte0 = setBitsAsInteger(byte0, 0, 4, version);
        byte0 = setBitsAsInteger(byte0, 4, 4, operationType.value);

        ByteBuffer buf = ByteBuffer.allocate(HEADER_LENGTH).order(ByteOrder.BIG_ENDIAN);
        buf.put(0, byte0);
        buf.put(2, deviceIp[0]);
        buf.put(3, deviceIp[1]);
        buf.put(4, deviceIp[2]);
        buf.put(5, deviceIp[3]);
        buf.putChar(6, (char) devicePort);
        buf.putInt(8, sessionId);
        buf.putInt(12, payLoadLength);
        
        return buf;
        
    }
    
    public static Header fromByteBuf(ByteBuffer buf) {
        byte byte0 = buf.get(0);
        // byte 0, bits [0, 4)
        final int uscVersion = getBitsAsInteger(byte0, 0, 4);

        // byte 0, bits [4, 8)
        final int operationType = (getBitsAsInteger(byte0, 4, 4));

        // byte 1 reserved

        // bytes [2, 6)
        byte[] ip = new byte[4];
        ip[0] = buf.get(2);
        ip[1] = buf.get(3);
        ip[2] = buf.get(4);
        ip[3] = buf.get(5);

        // bytes [6, 8)
        final int port = buf.getChar(6);
        
//        // bytes [8, 16)
//        byte[]  sessionByte = new byte[8];
//        sessionByte[0] = buf.get(8);
//        sessionByte[1] = buf.get(9);
//        sessionByte[2] = buf.get(10);
//        sessionByte[3] = buf.get(11);
//        sessionByte[4] = buf.get(12);
//        sessionByte[5] = buf.get(13);
//        sessionByte[6] = buf.get(14);
//        sessionByte[7] = buf.get(15);
        
        final int sessionId = buf.getInt(8);
        
        
        // bytes [16, 20)
        final int payloadLength = buf.getInt(PAYLOAD_LENGTH_OFFSET);

        return new Header(OperationType.valueOf(operationType), parseBytes2Str(ip), port, sessionId, payloadLength);
    }
    
    public static String parseBytes2Str(byte[] bytes) {
        String[] items = new String[bytes.length];
        for (int i = 0 ; i < items.length ; i++) {
            items[i] = String.valueOf(0x000000FF  & bytes[i]);
        }
        return StringUtils.join(items, ".");
    }
    
    public static byte[] parseIp2Bytes(String ip) {
        String[] items = ip.split("\\.");
        byte[] bytes = new byte[4];
        if (items.length == 4) {
            int i = 0;
            for (String item : items) {
                bytes[i++] = (byte)(0x000000FF & Integer.parseInt(item, 10));
            }
        } 
        return bytes;
    }
    
    public static byte setBitsAsInteger(byte value, int offset, int size, int target) {
        int shiftedTarget = (target & ((1 << size) - 1)) << offset;
        int mask = ~(((1 << size) - 1) << offset);
        return (byte) (value & mask | shiftedTarget);
    }
    
    public static int getBitsAsInteger(byte value, int offset, int size) {
        return (value >>> offset) & ((1 << size) - 1);
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public byte[] getDeviceIp() {
        return deviceIp;
    }
    
    public String getDeviceIpStr() {
        String[] items = new String[4];
        for (int i = 0 ; i < 4 ; i++) {
            items[i] = String.valueOf( deviceIp[i] & 0x000000ff);
        }
        return StringUtils.join(items, ".");
    }
    
    public static void main(String[] args) {
        Header header = new Header(null, "127.0.0.1", 80, 1, 0);
        
        System.out.println(header.getDeviceIpStr());
        
        System.out.println(parseBytes2Str(parseIp2Bytes("127.0.0.1")));
    }
    
    public void setDeviceIp(byte[] deviceIp) {
        this.deviceIp = deviceIp;
    }

    public int getDevicePort() {
        return devicePort;
    }

    public void setDevicePort(int devicePort) {
        this.devicePort = devicePort;
    }

    public int getSessionId() {
        return sessionId;
    }

    public void setSessionId(int sessionId) {
        this.sessionId = sessionId;
    }

    public int getPayLoadLength() {
        return payLoadLength;
    }

    public void setPayLoadLength(int payLoadLength) {
        this.payLoadLength = payLoadLength;
    }
    
}

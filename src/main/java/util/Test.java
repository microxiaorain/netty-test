package util;

import java.nio.ByteBuffer;

public class Test {
   
    public static void main(String[] args) {
        ByteBuffer bb = ByteBuffer.allocate(6);
        
        byte[] bytes = new byte[] {
                0x01, 0x02, 0x03, 0x04, 0x05
        };
        bb.put((byte)0x07);
        bb.put(bytes);
        
        for (byte item : bb.array()) {
            System.out.println(item);
        }
    }    
}


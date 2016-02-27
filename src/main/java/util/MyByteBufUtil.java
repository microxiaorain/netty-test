package util;


import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class MyByteBufUtil {
    
    public static ByteBuf str2buffer(String str, Charset charset) {
        byte[] bytes  = str.getBytes(charset);
        ByteBuf bb = Unpooled.buffer(bytes.length);
        bb.writeBytes(bytes);
        return bb;
    }
    
    public static ByteBuf str2buffer(String str) {
        return str2buffer(str, Charset.forName("utf-8"));
    }
    
    public static String buffer2str(ByteBuf bb, Charset charset) {
        byte[] bytes = new byte[bb.readableBytes()];
        bb.readBytes(bytes);
        return new String(bytes, charset);
    }
    
    public static String buffer2str(ByteBuf bb) {
        return buffer2str(bb, Charset.forName("utf-8"));
    }
}

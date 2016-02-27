package manager.protocol;

import java.nio.ByteOrder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class FrameDecoder extends LengthFieldBasedFrameDecoder {
    
    public FrameDecoder() {
        super(ByteOrder.BIG_ENDIAN, Integer.MAX_VALUE, Header.PAYLOAD_LENGTH_OFFSET,
                Header.PAYLOAD_LENGTH_SIZE, 0, 0, false);
    }
    
    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
         
        ByteBuf buf = (ByteBuf) super.decode(ctx, in);
         
        if (buf == null) {
            return null;
        }

         return Frame.getFromByteBuf(buf);
    }
    
    @Override
    protected ByteBuf extractFrame(ChannelHandlerContext ctx, ByteBuf buffer, int index, int length) {
        // we avoid making a copy here since UscFrameDecoder already makes a
        // copy of the data
        return buffer.slice(index, length);
    }
    
}

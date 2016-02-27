package test.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import util.MyByteBufUtil;

public class SimpleServer {
    
    private EventLoopGroup boss = new NioEventLoopGroup(1);
    private EventLoopGroup worker = new NioEventLoopGroup(1);
    private ServerBootstrap bootstrap = new ServerBootstrap();
    
    public SimpleServer(Class<? extends ServerChannel> clazz) {
        bootstrap.group(boss, worker).channel(clazz);
    }
    
    public ServerBootstrap bootstrap() {
        return bootstrap;
    }
    
    public static void main(String[] args) throws Exception {
        SimpleServer server = new SimpleServer(NioServerSocketChannel.class);
        server.bootstrap().childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast(new SimpleClientHandler());
                }
            });
 
        // Start the client.
        server.bootstrap().bind(1182).sync();
    }
}

class SimpleClientHandler extends SimpleChannelInboundHandler<ByteBuf> {
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        System.out.println(Integer.toHexString(ctx.channel().hashCode()));
        System.out.println("active : " + ctx.channel());
    }
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println(MyByteBufUtil.buffer2str(msg));
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("inactive : " + ctx.channel());
    }
}

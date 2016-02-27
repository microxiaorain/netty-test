package test.netty;


import java.net.InetSocketAddress;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import util.MyByteBufUtil;

/**
 * Hello world!
 *
 */
public class Server 
{
    static final int PORT = Integer.parseInt(System.getProperty("port", "11830"));
    
    public static void main(String[] args) throws Exception {
//        // Configure SSL.
//        final SslContext sslCtx;
//        if (SSL) {
//            SelfSignedCertificate ssc = new SelfSignedCertificate();
//            sslCtx = SslContext.newServerContext(ssc.certificate(), ssc.privateKey());
//        } else {
//            sslCtx = null;
//        }
 
        // Configure the server.
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 10000)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
//                     if (sslCtx != null) {
//                         p.addLast(sslCtx.newHandler(ch.alloc()));
//                     }
//                     p.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
//                     p.addLast("decoder", new StringDecoder());
//                     p.addLast("encoder", new StringEncoder());
                     p.addLast(new LoggingHandler(LogLevel.INFO));
                     p.addLast(
                         new HelloWorldServerHandler());
                 }
             });
 
            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();
 
            // Wait until the server socket is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}

class HelloWorldServerHandler extends SimpleChannelInboundHandler<ByteBuf> {
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
//        // TODO Auto-generated method stub
//        byte[] bytes = new byte[msg.readableBytes()];
//        msg.readBytes(bytes);
//        System.out.println("server get msg:" + new String(bytes, "utf-8"));
         
//        ctx.channel().writeAndFlush(MyByteBufUtil.str2buffer("server has got.\n"));
        final Channel ch = ctx.channel();
        System.out.println(MyByteBufUtil.buffer2str(msg));
        Thread th =  new Thread(new Runnable() {

            @Override
            public void run() {
                for (;;) {
                    ch.writeAndFlush(MyByteBufUtil.str2buffer("server has got.\n"));
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            
        });
        
        System.out.println( ((InetSocketAddress) ctx.channel().localAddress()).getAddress());
        
//        th.start();
        
    }
}



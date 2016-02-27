package test.netty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import util.MyByteBufUtil;

public class Client {
    
    static final boolean SSL = System.getProperty("ssl") != null;
    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "11830"));
    static String deviceId = null;
    
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Random r = new Random();
            deviceId = String.valueOf(r.nextInt(10000));
        } else {
            deviceId = args[0];
        }
        // Configure SSL.git
//        final SslContext sslCtx;
//        if (SSL) {
//            sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
//        }
//        else {
//            sslCtx = null;
//        }
 
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup(1);
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                .channel(NioSocketChannel.class)
//                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
//                        if (sslCtx != null) {
//                            p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
//                        }
//                        p.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
//                        p.addLast("decoder", new StringDecoder());
//                        p.addLast("encoder", new StringEncoder());
                        p.addLast(new HelloWorldClientHandler());
                    }
                });
 
            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();
            
            f.awaitUninterruptibly();
            
            new UserConsole(f.channel());
            
            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
            
            System.out.println("session closed.");
        }
        finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }
}

class HelloWorldClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        System.out.println("get msg: " + MyByteBufUtil.buffer2str(msg));
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel ch = ctx.channel();
        System.out.println("channel:[{}] is active.".replace("{}", ch.toString()));
        ch.write(MyByteBufUtil.str2buffer(Client.deviceId));
        ch.flush();
        System.out.println("device id : [{}]".replace("{}", Client.deviceId));
    }
}

class UserConsole {
    
    private Channel channel;
    
    public UserConsole(Channel channel) {
        this.channel = channel;
        initConsole();
    }
    
    public void initConsole() {
        Thread th = new Thread (new Runnable () {
            @Override
            public void run() {
                try {
                    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                    
                    String line = null;
                    System.out.println("input:");
                    while ( !("exit".equals((line = br.readLine())))) {
                        byte[] bytes  = line.getBytes("utf-8");
                        ByteBuf bb = Unpooled.buffer(bytes.length);
                        bb.writeBytes(bytes);
                        channel.writeAndFlush(bb);
                        System.out.println("input:");
                    }
                    
                    channel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        th.start();
    }
}

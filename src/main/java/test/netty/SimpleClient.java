package test.netty;

import java.net.InetSocketAddress;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import util.MyByteBufUtil;

public class SimpleClient {
    
    private EventLoopGroup workerLoopGroup = null;
    private Bootstrap clientBootstrap = new Bootstrap();
    
    public SimpleClient(EventLoopGroup worker, Class<? extends Channel> clazz) {
        clientBootstrap.group(worker)
                       .channel(clazz);
    }
    public SimpleClient() {
        workerLoopGroup = new NioEventLoopGroup(1);
        clientBootstrap.group(workerLoopGroup)
                       .channel(NioSocketChannel.class);
    }
    
    public Bootstrap bootstrap() {
        return this.clientBootstrap;
    }
    
    public EventLoopGroup worker() {
        return this.workerLoopGroup;
    }
    
    public static void main(String[] args) throws Exception {
        
        SimpleClient client = new SimpleClient();
        
        client.bootstrap().handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ChannelPipeline p = ch.pipeline();
                p.addLast("mine", new MyHandler());
            }
        });
        
        final Channel ch = client.bootstrap().connect(new InetSocketAddress("127.0.0.1", 1182))
                               .sync().channel();
        
        Thread th1  = new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    for (int i = 0 ; i < 10 ; i++) {
                        ch.write(MyByteBufUtil.str2buffer("th1:" + i));
                        Thread.sleep(1000);
                    }
                    ch.flush();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
            }
        });
        
        Thread th2  = new Thread(new Runnable() {
            
            @Override
            public void run() {
                for (int i = 0 ; i < 10 ; i++) {
                    ch.write(MyByteBufUtil.str2buffer("th2:" + i));
                }
                ch.flush();
            }
        });
        
        th1.start();
        Thread.sleep(1000);
        th2.start();
        
        ch.closeFuture().await();
        
        client.worker().shutdownGracefully();
        
    }
}

class MyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        
    }
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println(ctx);
    }
    
    
    
}
   

package test.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import io.netty.channel.socket.SocketChannel;

public class LocalClient {
    
    private static final LocalAddress localServerAddr = new LocalAddress("xiaorain-local-server");
    
    public static void main(String[] args) throws InterruptedException {
        SimpleClient client = new SimpleClient(new LocalEventLoopGroup(), LocalChannel.class);
            
        client.bootstrap().handler(new ChannelInitializer<LocalChannel>() {
                @Override
                public void initChannel(LocalChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast("mine", new MyHandler());
                }
        });
        
        client.bootstrap().connect(localServerAddr).sync().channel().closeFuture().await();
     }
}

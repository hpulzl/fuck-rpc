package top.huzhurong.fuck.transaction.netty.request;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.Getter;
import lombok.Setter;
import top.huzhurong.fuck.serialization.ISerialization;
import top.huzhurong.fuck.transaction.Client;
import top.huzhurong.fuck.transaction.netty.ClientTransactionHandler;
import top.huzhurong.fuck.transaction.netty.serilize.MessageDecoder;
import top.huzhurong.fuck.transaction.netty.serilize.MessageEncoder;
import top.huzhurong.fuck.transaction.support.Provider;

/**
 * @author luobo.cs@raycloud.com
 * @since 2018/12/1
 */
public class NettyClient implements Client {

    private NioEventLoopGroup work;
    private ChannelFuture channelFuture;

    public NettyClient(Provider provider, ISerialization serialization) {
        this.provider = provider;
        this.serialization = serialization;
    }

    @Getter
    @Setter
    private Provider provider;

    @Getter
    @Setter
    private ISerialization serialization;

    @Override
    public void connect(String host, Integer port) {
        Bootstrap bootstrap = new Bootstrap();
        try {
            work = new NioEventLoopGroup();
            bootstrap.group(work)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new MessageDecoder(serialization))
                                    .addLast(new MessageEncoder(serialization))
                                    .addLast(new ClientTransactionHandler(provider));
                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true);
            channelFuture = bootstrap.connect(host, port).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disConnect() throws InterruptedException {
        channelFuture.channel().closeFuture().sync();
        if (work != null) {
            work.shutdownGracefully();
        }
    }
}
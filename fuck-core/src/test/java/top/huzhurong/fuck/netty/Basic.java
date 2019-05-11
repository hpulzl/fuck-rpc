package top.huzhurong.fuck.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author chenshun00@gmail.com
 * @since 2019/5/9
 */
public class Basic {
    public static void main(String[] args) throws InterruptedException {

        NioEventLoopGroup boss = new NioEventLoopGroup(1, new DefaultThreadFactory("boss-thread"));
        NioEventLoopGroup worker = new NioEventLoopGroup(1, new DefaultThreadFactory("work-thread"));
        DefaultEventExecutorGroup defaultEventExecutorGroup = new DefaultEventExecutorGroup(2, new DefaultThreadFactory("telnet-thread"));
        final ChannelPipeline[] ddd = new ChannelPipeline[1];

        final TelnetHandler telnetHandler = new TelnetHandler();
        ServerBootstrap serverBootstrap = new ServerBootstrap();


        serverBootstrap.group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        //这里是worker线程执行的
                        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
                        for (StackTraceElement stackTraceElement : stackTrace) {
                            System.out.println(stackTraceElement.getLineNumber() + "\t" + stackTraceElement.getClassName() + "\t" + stackTraceElement.getMethodName());
                        }
                        ddd[0] = ch.pipeline();
                        System.out.println(ddd[0]);
                        ch.pipeline()
                                .addLast(defaultEventExecutorGroup, "11", new AA())
                                .addLast(defaultEventExecutorGroup, "telnet", telnetHandler);
                        //.addLast(null, "idle", new IdleStateHandler(3, 3, 4))
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        ChannelFuture f = serverBootstrap.bind(54321).sync();
        // Wait until the server socket is closed.
        ChannelFuture channelFuture = f.channel().closeFuture();
        channelFuture.sync();
    }

    static class AA extends SimpleChannelInboundHandler<ByteBuf> {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (acceptInboundMessage(msg)) {
                @SuppressWarnings("unchecked")
                ByteBuf imsg = (ByteBuf) msg;
                channelRead0(ctx, imsg);
            } else {
                ctx.fireChannelRead(msg);
            }
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
            System.out.print(Thread.currentThread().getName() + "\t");
            ctx.fireChannelRead(msg);
        }
    }

    //sharable 可被加入到多个pipeline中去，但是仅仅在非 new的情况下生效，
    @ChannelHandler.Sharable
    static class TelnetHandler extends SimpleChannelInboundHandler<ByteBuf> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
            int i = msg.readableBytes();
            if (i > 0) {
                byte[] bb = new byte[i];
                msg.readBytes(bb);
                String s = new String(bb) + "\t" + Thread.currentThread().getName();
                System.out.println(s);
                if (s.trim().equalsIgnoreCase("quit")) {
                    ChannelFuture close = ctx.channel().close();
                    close.addListener(future -> {
                        if (future.isSuccess()) {
                            System.out.println("kill:" + ctx.channel());
                        } else {
                            Throwable cause = future.cause();
                            System.out.println(cause);
                        }
                    });
                    new Thread(() -> {
                        try {
                            Thread.sleep(5000L);
                        } catch (Throwable ignore) {

                        }
                        System.gc();
                    }).start();
                } else {
                    ctx.writeAndFlush(Unpooled.wrappedBuffer(s.getBytes()));
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            // Close the connection when an exception is raised.
            cause.printStackTrace();
            ctx.close();
        }
    }
}

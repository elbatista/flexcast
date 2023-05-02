package comms;

import flexcast.messages.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import proxies.ServerProxy;

public class NettyServerChannelHandler extends ChannelInboundHandlerAdapter {

    private ServerProxy server;

    public NettyServerChannelHandler(ServerProxy s){
        this.server = s;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Message m = (Message)msg;
        m.setChannelIn(ctx.channel());
        server.buffer(m);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        System.out.println("NettyServerChannelHandler - Exception -" + cause.toString());
        ctx.close();
        System.exit(0);
    }

}

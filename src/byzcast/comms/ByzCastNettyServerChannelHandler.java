package byzcast.comms;

import byzcast.messages.ByzCastMessage;
import byzcast.proxies.ByzCastServerProxy;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ByzCastNettyServerChannelHandler extends ChannelInboundHandlerAdapter {

    private ByzCastServerProxy server;

    public ByzCastNettyServerChannelHandler(ByzCastServerProxy s){
        this.server = s;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByzCastMessage m = (ByzCastMessage)msg;
        m.setChannelIn(ctx.channel());
        server.buffer(m);
    }

    // @Override
    // public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    //     cause.printStackTrace();
    //     System.out.println("NettyServerChannelHandler - Exception -" + cause.toString());
    //     ctx.close();
    //     System.exit(0);
    // }
}

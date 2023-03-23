package comms;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import messages.Message;
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

}

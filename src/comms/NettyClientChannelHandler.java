package comms;

import java.util.concurrent.CyclicBarrier;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import messages.Message;
import messages.Message.Type;
import proxies.ClientProxy;

public class NettyClientChannelHandler extends ChannelInboundHandlerAdapter {
    private ClientProxy proxy;
    private short dst;
    private CyclicBarrier syncAllConnections;

    public NettyClientChannelHandler(ClientProxy p, short dst, CyclicBarrier syncAllConnections){
        this.proxy = p;
        this.dst = dst;
        this.syncAllConnections = syncAllConnections;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        proxy.setChannelToDest(ctx.channel(), dst);
        if(syncAllConnections != null) 
            syncAllConnections.await();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Message m = (Message)msg;
        if(m.getType() == Type.CONN){
            proxy.receiveReplyInitMsg(m);
            return;
        }
        if(m.getType() == Type.READY){
            proxy.receiveReplyReadyMsg(m);
            return;
        }
        if(m.getType() == Type.END){
            proxy.receiveReplyEndMsg(m);
            return;
        }
        proxy.receiveReply(m);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        System.out.println("NettyClientChannelHandler - Exception - " + cause);
        ctx.close();
        System.exit(0);
    }
}

package byzcast.comms;

import java.util.concurrent.CyclicBarrier;
import byzcast.messages.ByzCastMessage;
import byzcast.messages.ByzCastMessage.Type;
import byzcast.proxies.ByzCastClientProxy;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

public class ByzCastNettyClientChannelHandler extends ChannelInboundHandlerAdapter {
    private ByzCastClientProxy proxy;
    private short dst;
    private CyclicBarrier syncAllConnections;

    public ByzCastNettyClientChannelHandler(ByzCastClientProxy p, short dst, CyclicBarrier syncAllConnections){
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
        ByzCastMessage m = (ByzCastMessage)msg;
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
}

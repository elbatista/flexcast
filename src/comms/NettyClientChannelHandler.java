package comms;

import java.util.concurrent.CyclicBarrier;
import flexcast.messages.Message;
import flexcast.messages.Message.Type;
import flexcast.reconfig.View;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import proxies.ClientProxy;

public class NettyClientChannelHandler extends ChannelInboundHandlerAdapter {
    private ClientProxy proxy;
    private View view;
    private short dst;
    private CyclicBarrier syncAllConnections;

    public NettyClientChannelHandler(ClientProxy p, short dst, CyclicBarrier syncAllConnections, View v){
        this.proxy = p;
        this.dst = dst;
        this.view = v;
        this.syncAllConnections = syncAllConnections;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        while(!ctx.channel().isActive()){}
        view.addConnection(dst, ctx.channel());
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
        if(m.getType() == Type.GC){
            proxy.receiveReplyGCMsg(m);
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

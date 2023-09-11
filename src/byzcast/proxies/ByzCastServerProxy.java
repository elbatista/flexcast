package byzcast.proxies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import byzcast.comms.ByzCastNettyServerChannel;
import byzcast.messages.ByzCastMessage;
import byzcast.messages.ByzCastMessage.Type;
import io.netty.channel.Channel;
import util.FileManager;
import util.MsgSize;

public abstract class ByzCastServerProxy extends ByzCastClientProxy {
    protected ConcurrentLinkedQueue<ByzCastMessage> bufferQueue;
    private HashMap<Integer, Channel> cliChannels;
    protected int numCliEndsRecv = 0, numCliReadyRecv = 0, numClients = 0, localMsgs = 0;
    ArrayList<MsgSize> sizes = new ArrayList<>();

    public ArrayList<MsgSize> getSizes() {
        return sizes;
    }

    public ByzCastServerProxy(short id, int numClients){
        super(id, 0);
        this.numClients = numClients;
        bufferQueue = new ConcurrentLinkedQueue<>();
        cliChannels = new HashMap<>();
        new ByzCastNettyServerChannel(this, this);
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    ByzCastMessage m = bufferQueue.poll();
                    if(m != null) receive(m);
                }
            }
        }).start();
    }

    public void buffer(ByzCastMessage m){
        if(m.getType() == Type.MSG && m.getDst().length == 1){
            localMsgs++;
            sendReply(m);
            return;
        }
        bufferQueue.offer(m);
    }

    private void receive(ByzCastMessage m) {
        switch(m.getType()){
            case MSG: receiveMsg(m); break;
            // message used only to establish a connection to each client
            case CONN: {
                cliChannels.put(m.getCliId(), m.getChannelIn());
                m.setSender(getId());
                m.getChannelIn().writeAndFlush(m);
                print("Channel to client", m.getCliId(), ":", m.getChannelIn());
                break;
            }
            // message used only to ensure all clients are ready (connected) before all clients start multicasting
            case READY: receiveReady(m); break;
            // message used only to end a connection to a client
            case END: receiveEnd(m); break;
            default: {
                print("Should never reach here !");
                new FileManager().stop();
                exit();
            }
        }
    }
    
    // private void receiveMsg(ByzCastMessage m) {
    //     m.setType(Type.STEP1);
    //     for(short dst : m.getDst()){
    //         if(dst != getId())
    //             send(m, dst);
    //     }
    //     SkeenMessage aux = new SkeenMessage(m.getId());
    //     aux.setType(m.getType());
    //     aux.setSender(m.getSender());
    //     aux.setTimestamp(m.getTimestamp());
    //     aux.setCliId(m.getCliId());
    //     aux.setDst(m.getDst());
    //     receiveStep1Msg(aux);
    // }

    protected void sendReply(ByzCastMessage m){
        ByzCastMessage reply = new ByzCastMessage(m.getId());
        reply.setSender(getId());
        reply.setType(Type.REPLY);
        cliChannels.get(m.getCliId()).writeAndFlush(reply);
    }

    private void receiveReady(ByzCastMessage m) {
        numCliReadyRecv++;
        if(numCliReadyRecv == numClients){
            print("All", numClients, " clients are ready. They will start multicasting...");
            for(int i = 0; i < numClients; i++){
                // reply to all clients
                m.setSender(getId());
                cliChannels.get(i).writeAndFlush(m);
            }
        }
    }

    private void receiveEnd(ByzCastMessage m) {
        numCliEndsRecv++;
        m.setSender(getId());
        m.getChannelIn().writeAndFlush(m);
        if(numCliEndsRecv == numClients){
            print("All", numClients, " clients done!");
            finish();
        }
    }
    protected abstract void finish();
    protected abstract void receiveMsg(ByzCastMessage m);
}
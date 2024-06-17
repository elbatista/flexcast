package proxies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import comms.NettyServerChannel;
import flexcast.messages.Message;
import flexcast.messages.Message.Type;
import io.netty.channel.Channel;
import util.MsgSize;
// import util.Stats;
import util.Stats;

public abstract class ServerProxy extends ClientProxy {
    protected ConcurrentLinkedQueue<Message> bufferQueue;
    private HashMap<Integer, Channel> cliChannels;
    protected int numCliEndsRecv = 0, numCliReadyRecv = 0, numClients = 0, localMsgs;
    ArrayList<MsgSize> sizes = new ArrayList<>();
    // int duration = 0;
    public ArrayList<MsgSize> getSizes() {
        return sizes;
    }

    public Stats getStats(){return stats;}

    public ServerProxy(short id, int numClients){
        super(id);
        this.numClients = numClients;
        // this.duration = duration;
        bufferQueue = new ConcurrentLinkedQueue<>();
        cliChannels = new HashMap<>();
        new NettyServerChannel(this, this);
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    Message m = bufferQueue.poll();
                    if(m != null) {
                        receive(m);
                    }
                }
            }
        }).start();
    }

    public void buffer(Message m){
        // client local msgs are immediately delivered 
        if(m.getType() == Type.MSG && m.getDst().length == 1){
            localMsgs++;
            sendReply(m);
            return;
        }
        bufferQueue.offer(m);
    }

    private void receive(Message m) {
        if(
            (m.getType() == Type.MSG || m.getType() == Type.ACK || m.getType() == Type.NOTIF)
            && !validateView(m)
        ){
            return;
        }
        switch(m.getType()){
            case MSG: receiveMsg(m); break;
            case CKFREQ: receiveMsg(m); break;
            case VIEWCHANGE: receiveMsg(m); break;
            case ACK: receiveAck(m); break;
            case NOTIF: receiveNotif(m); break;
            // message used only to establish a connection to each client
            case CONN: {
                cliChannels.put(m.getCliId(), m.getChannelIn());
                m.setSender(getId());
                m.getChannelIn().writeAndFlush(m);
                printF("Channel to client", m.getCliId(), ":", m.getChannelIn());
                break;
            }
            // message used only to ensure all clients are ready (connected) before all clients start multicasting
            case READY: receiveReady(m); break;
            // message used only to end a connection to a client
            case END: receiveEnd(m); break;
            case GC: receiveGC(m); break;
            default: break;
        }
    }

    protected void receiveReady(Message m) {
        numCliReadyRecv++;
        if(numCliReadyRecv == numClients){
            printF("All", numClients, " clients are ready. They will start multicasting...");
            for(int i = 0; i < numClients; i++){
                // reply to all clients
                m.setSender(getId());
                cliChannels.get(i).writeAndFlush(m);
            }
            
        }
    }

    protected void receiveEnd(Message m) {
        numCliEndsRecv++;
        m.setSender(getId());
        m.getChannelIn().writeAndFlush(m);
        if(numCliEndsRecv == numClients){
            printF("All", numClients, " clients done!");
            finish();
        }
    }

    protected void receiveGC(Message m) {
        gc(m.getId());
        m.setSender(getId());
        m.getChannelIn().writeAndFlush(m);
    }

    protected void sendReply(Message m){
        Message reply = new Message(m.getId(), m.getViewId());
        reply.setSender(getId());
        reply.setType(Type.REPLY);
        reply.setDst(m.getDst());

        if(m.getType() == Type.CKFREQ){
            reply.setType(Type.CKFREQ);
            HashMap<String, Integer> dstFreq = new HashMap<>();
            for(String k : currentView.getDstsFreq().keySet()){
                dstFreq.put(k, currentView.getDstsFreq().get(k));
            }
            reply.setDstsFreq(dstFreq);
            currentView.clearDstsFreq();
        }
        cliChannels.get(m.getCliId()).writeAndFlush(reply);
    }

    protected abstract void finish();
    protected abstract void gc(int mid);
    protected abstract void receiveMsg(Message m);
    protected abstract void receiveAck(Message m);
    protected abstract void receiveNotif(Message m);

    // view change related methods
    protected abstract boolean validateView(Message m);
    protected void sendReplyVC(Message m){
        Message reply = new Message(m.getId(), m.getViewId());
        reply.setSender(getId());
        reply.setType(m.getType());
        reply.setViewId(m.getViewId());
        reply.setDst(new short[0]);
        reply.setNewOverlay(m.getNewOverlay());
        cliChannels.get(m.getCliId()).writeAndFlush(reply);
    }
}
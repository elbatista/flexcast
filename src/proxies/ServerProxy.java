package proxies;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import comms.NettyServerChannel;
import io.netty.channel.Channel;
import messages.Message;
import messages.Message.Type;

public abstract class ServerProxy extends ClientProxy {
    private ConcurrentLinkedQueue<Message> bufferQueue;
    private HashMap<Integer, Channel> cliChannels;
    protected int numCliEndsRecv = 0, numCliReadyRecv = 0, numClients = 0, localMsgs;

    public ServerProxy(short id, int numClients){
        super(id);
        this.numClients = numClients;
        bufferQueue = new ConcurrentLinkedQueue<>();
        cliChannels = new HashMap<>();
        new NettyServerChannel(this, this);
        new Thread(new Runnable() {
            public void run(){
                while(true) {
                    Message m = bufferQueue.poll();
                    if(m != null) receive(m);
                }
            }
        }).start();
    }
    public void buffer(Message m){
        // client local msgs are immediatly delivered 
        if(m.getType() == Type.MSG && m.getDst().length == 1){
            localMsgs++;
            sendReply(m);
            return;
        }
        bufferQueue.offer(m);
    }
    private void receive(Message m) {
        switch(m.getType()){
            case MSG: receiveMsg(m, true); break;
            case ACK: receiveAck(m, true); break;
            case NOTIF: receiveNotif(m, true); break;
            case BATCH: receiveBatch(m); break;
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
            default: break;
        }
    }

    protected void receiveReady(Message m) {
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

    protected void receiveEnd(Message m) {
        numCliEndsRecv++;
        m.setSender(getId());
        m.getChannelIn().writeAndFlush(m);
        if(numCliEndsRecv == numClients){
            print("All", numClients, " clients done!");
            finish();
        }
    }

    private void receiveBatch(Message batch) {
        updateDG(batch);
        for(Message m : batch.getBatch()){
            switch(m.getType()){
                case MSG: receiveMsg(m, false); break;
                case ACK: receiveAck(m, false); break;
                case NOTIF: receiveNotif(m, false); break;
                default: break;
            } 
        }
    }

    protected void sendReply(Message m){
        Message reply = new Message(m.getId());
        reply.setSender(getId());
        reply.setType(Type.REPLY);
        cliChannels.get(m.getCliId()).writeAndFlush(reply);
    }

    protected abstract void finish();
    protected abstract void updateDG(Message m);
    protected abstract void receiveMsg(Message m, boolean shouldUPdateDG);
    protected abstract void receiveAck(Message m, boolean shouldUPdateDG);
    protected abstract void receiveNotif(Message m, boolean shouldUPdateDG);
}
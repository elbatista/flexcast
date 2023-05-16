package proxies;

import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import comms.NettyServerChannel;
import flexcast.messages.Message;
import flexcast.messages.Message.Type;
import io.netty.channel.Channel;

public abstract class ServerProxy extends ClientProxy {
    protected ConcurrentLinkedQueue<Message> bufferQueue;
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
                    //Message tmpTest = bufferQueue.peek();
                    // if(m != null && tmpTest.getType() == Type.MSG && tmpTest.getSender() == -1 && hasPendMsg()) {
                    //     bufferQueue.add(bufferQueue.poll());
                    //     continue;
                    // }
                    if(m != null) {
                        if(m.getType() == Type.MSG && m.getSender() == -1 && hasPendMsg()){
                            // print("recv", m, "but hasPendMsg", hasPendMsg());
                            bufferQueue.offer(m);
                            if(bufferQueue.size() == 1) {
                                sleep(1);
                                Thread.yield();
                            }
                        }
                        else {
                            receive(m);
                        }
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
        switch(m.getType()){
            case MSG: receiveMsg(m); break;
            case ACK: receiveAck(m); break;
            case NOTIF: receiveNotif(m); break;
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

    protected void sendReply(Message m){
        Message reply = new Message(m.getId());
        reply.setSender(getId());
        reply.setType(Type.REPLY);

        while(cliChannels.get(m.getCliId()) == null){
            print("Channel to cli ", m.getCliId(), "is null");
            sleep(500);
        }

        cliChannels.get(m.getCliId()).writeAndFlush(reply);
    }

    protected abstract boolean hasPendMsg();
    protected abstract void finish();
    protected abstract void receiveMsg(Message m);
    protected abstract void receiveAck(Message m);
    protected abstract void receiveNotif(Message m);
}
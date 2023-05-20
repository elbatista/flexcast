package proxies;

import java.util.ArrayList;
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
                ArrayList<Message> tmpCliMsgs = new ArrayList<>();
                while(true) {
                    Message m = bufferQueue.poll();
                    if(m != null) {

                        // se for mensagem de cliente
                        if(m.getType() == Type.MSG && m.getLca() == getId()){
                            // joga no arraylist tmpCliMsgs
                            tmpCliMsgs.add(m);
                        }
                        else {
                            // senao recebe:
                            receive(m);
                        }
                    }
                    
                    // se nao tem nada pendente
                    if(!hasPendMsg()){
                        // pega uma msg de tmpCliMsgs e entrega
                        if(tmpCliMsgs.size() > 0){
                            Message mc = tmpCliMsgs.get(0);
                            tmpCliMsgs.remove(0);
                            receive(mc);
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
                printF("Channel to client", m.getCliId(), ":", m.getChannelIn());
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

    protected void sendReply(Message m){
        Message reply = new Message(m.getId());
        reply.setSender(getId());
        reply.setType(Type.REPLY);

        while(cliChannels.get(m.getCliId()) == null){
            printF("Channel to cli ", m.getCliId(), "is null");
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
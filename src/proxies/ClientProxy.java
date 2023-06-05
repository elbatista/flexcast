package proxies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import base.Node;
import comms.NettyClientChannel;
import flexcast.messages.Message;
import flexcast.messages.Message.Type;
import io.netty.channel.Channel;
import util.Stats;

public class ClientProxy extends Node{
    private HashMap<Short, Channel> outChannels;
    private Semaphore sema = new Semaphore(0);
    private ReentrantLock lock = new ReentrantLock();
    private ArrayList<Message> replies = new ArrayList<>();
    private short expectedReplies = 0;
    protected short numNodes = 0;
    protected Stats stats;
    private long startTime;
    private HashMap<Short, Long> latsPerNode = new HashMap<>();
    short lca;
    short[] dsts;

    public ClientProxy(short id){
        super(id);
        outChannels = new HashMap<>();
    }

    public void connectTo(Node dest){
        new NettyClientChannel(dest, this);
    }

    public void connectTo(Node dest, CyclicBarrier syncAllConnections){
        new NettyClientChannel(dest, this, syncAllConnections);
    }

    public void setChannelToDest(Channel c, short dst){
        printF("Channel to node", dst, ":", c);
        try {
            outChannels.put(dst, c);
        }
        catch(Exception e){
            e.printStackTrace();
            printF(e);
            exit();
        }
    }

    public void sendInitMessage(){
        Message m = new Message();
        m.setType(Type.CONN);
        m.setCliId(getId());
        for(short i : outChannels.keySet()){
            try {
                outChannels.get(i).writeAndFlush(m);
                sema.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendReadyMessage(){
        Message m = new Message();
        m.setType(Type.READY);
        m.setCliId(getId());
        try {
            outChannels.get((short)0).writeAndFlush(m);
            sema.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendEndMessage(){
        Message m = new Message();
        m.setType(Type.END);
        m.setCliId(getId());
        for(short i : outChannels.keySet()){
            try {
                outChannels.get(i).writeAndFlush(m);
                sema.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void sendGCMessage(int id){
        Message m = new Message();
        m.setType(Type.GC);
        m.setId(id);
        m.setCliId(getId());
        for(short i = (short)(numNodes-1); i >=0; i--){
            try {
                outChannels.get(i).writeAndFlush(m);
                sema.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void receiveReplyReadyMsg(Message reply){
        printF("Ready OK - Server", reply.getSender());
        sema.release();
    }

    public void receiveReplyEndMsg(Message reply){
        printF("End OK - Server", reply.getSender());
        sema.release();
    }

    public void receiveReplyInitMsg(Message reply){
        printF("Init OK - Server", reply.getSender());
        sema.release();
    }

    public void receiveReplyGCMsg(Message reply){
        print("GC OK - Server", reply.getSender());
        sema.release();
    }

    public void send(Message m, short dst){
        try {
            outChannels.get(dst).writeAndFlush(m);
        }
        catch(Exception e){
            e.printStackTrace();
            printF(e);
            exit();
        }
    }

    public void receiveReply(Message reply){
        lock.lock();

        // armazena latencia por nodo em microsegundo
        latsPerNode.put(reply.getSender(), ((System.nanoTime() - startTime) / 1000));

        replies.add(reply);
        
        if(replies.size() == expectedReplies){
            if(stats != null) stats.store(latsPerNode, expectedReplies>1, dsts);
            sema.release();
        }
        
        lock.unlock();
    }

    public Message multicast(Message m){
        print("Send", m);
        replies.clear();
        expectedReplies = (short) m.getDst().length;
        latsPerNode.clear();
        startTime = System.nanoTime();
        lca = m.getLca();
        dsts = m.getDst();
        send(m, lca);
        try {
            sema.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return replies.get(0);
    }

}
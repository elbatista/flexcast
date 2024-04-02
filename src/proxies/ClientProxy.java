package proxies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import base.Node;
import comms.NettyClientChannel;
import flexcast.messages.Message;
import flexcast.messages.Message.Type;
import flexcast.reconfig.View;
import io.netty.channel.Channel;
import util.Stats;

public abstract class ClientProxy extends Node{
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
    protected View currentView;

    public ClientProxy(short id){
        super(id);
    }

    public void connectToServers(){
        for(Node server : currentView.getNodes())
            new NettyClientChannel(server, this, currentView);
    }

    public void connectToServers(CyclicBarrier syncAllConnections){
        for(Node server : currentView.getNodes())
            new NettyClientChannel(server, this, syncAllConnections, currentView);
    }

    public void sendInitMessage(){
        Message m = new Message();
        m.setType(Type.CONN);
        m.setViewId(currentView.getId());
        m.setCliId(getId());
        for(Channel c : currentView.getConnections()){
            try {
                c.writeAndFlush(m);
                sema.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendReadyMessage(){
        Message m = new Message();
        m.setType(Type.READY);
        m.setViewId(currentView.getId());
        m.setCliId(getId());
        try {
            currentView.getConnection((short)0).writeAndFlush(m);
            sema.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendEndMessage(){
        Message m = new Message();
        m.setType(Type.END);
        m.setViewId(currentView.getId());
        m.setCliId(getId());
        for(Channel c : currentView.getConnections()){
            try {
                c.writeAndFlush(m);
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
        m.setViewId(currentView.getId());
        m.setCliId(getId());
        for(short i = (short)(numNodes-1); i >=0; i--){
            try {
                currentView.getConnection(i).writeAndFlush(m);
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
            currentView.getConnection(dst).writeAndFlush(m);
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

        // if it is a viewchange, changes the view
        if(reply.getType() == Type.VIEWCHANGE){
            if(stats != null) stats.store(latsPerNode, expectedReplies>1, dsts, reply.getType());
            printF("Got a viewchange reply from server", reply.getSender());
            changeView(reply);
            sema.release();
            lock.unlock();
            return;
        }
        
        if(replies.size() == expectedReplies){
            if(stats != null) stats.store(latsPerNode, expectedReplies>1, dsts, reply.getType());
            sema.release();
        }
        
        lock.unlock();
    }

    protected abstract void changeView(Message reply);

    public Message multicast(Message m){
        // printF("Send", m);
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

    public List<Message> multicast2(Message m){
        // printF("Send", m);
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
        ArrayList<Message> rep = new ArrayList<>();
        for (Message ms : replies){
            rep.add(ms);
        }
        return rep;
    }

}
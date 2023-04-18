package byzcast.proxies;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import org.jgrapht.Graph;
import org.jgrapht.alg.lca.TarjanLCAFinder;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultEdge;
import base.Node;
import byzcast.comms.ByzCastNettyClientChannel;
import byzcast.messages.ByzCastMessage;
import byzcast.messages.ByzCastMessage.Type;
import io.netty.channel.Channel;
import util.FileManager;

public class ByzCastClientProxy extends Node {
    private HashMap<Short, Channel> outChannels;
    private Semaphore sema = new Semaphore(0);
    private ReentrantLock lock = new ReentrantLock();
    private ArrayList<ByzCastMessage> replies = new ArrayList<>();
    private short expectedReplies = 0;
    protected Graph<Short,DefaultEdge> tree;
    private TarjanLCAFinder<Short,DefaultEdge> lcafinder;

    public ByzCastClientProxy(){
        super((short)0);
        tree = new FileManager().loadByzCastTreeAsGraph();
        lcafinder = new TarjanLCAFinder<Short,DefaultEdge>(tree, (short)0);
    }

    public ByzCastClientProxy(short id){
        super(id);
        outChannels = new HashMap<>();
        tree = new FileManager().loadByzCastTreeAsGraph();
        lcafinder = new TarjanLCAFinder<Short,DefaultEdge>(tree, (short)0);
    }

    public void connectTo(Node dest){
        new ByzCastNettyClientChannel(dest, this);
    }

    public void connectTo(Node dest, CyclicBarrier syncAllConnections){
        new ByzCastNettyClientChannel(dest, this, syncAllConnections);
    }

    public void setChannelToDest(Channel c, short dst){
        print("Channel to node", dst, ":", c);
        try {
            outChannels.put(dst, c);
        }
        catch(Exception e){
            e.printStackTrace();
            print(e);
            exit();
        }
    }

    public void sendInitMessage(){
        ByzCastMessage m = new ByzCastMessage(-1);
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
        ByzCastMessage m = new ByzCastMessage();
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
        ByzCastMessage m = new ByzCastMessage();
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
    
    public void receiveReplyInitMsg(ByzCastMessage reply){
        print("Init OK - Server", reply.getSender());
        sema.release();
    }

    public void send(ByzCastMessage m, short dst){
        try {
            outChannels.get(dst).writeAndFlush(m);
        }
        catch(Exception e){
            e.printStackTrace();
            print(e);
            exit();
        }
    }

    public void receiveReplyReadyMsg(ByzCastMessage reply){
        print("Ready OK - Server", reply.getSender());
        sema.release();
    }

    public void receiveReplyEndMsg(ByzCastMessage reply){
        print("End OK - Server", reply.getSender());
        sema.release();
    }

    public void receiveReply(ByzCastMessage reply){
        lock.lock();
        replies.add(reply);
        if(replies.size() == expectedReplies)
            sema.release();
        lock.unlock();
    }

    // public ByzCastMessage multicast(ByzCastMessage m, short warehouse){
    //     replies.clear();
    //     expectedReplies = (short) m.getDst().length;
    //     send(m, warehouse);
    //     try {
    //         sema.acquire();
    //     } catch (InterruptedException e) {
    //         e.printStackTrace();
    //     }
    //     return replies.get(0);
    // }

    public ByzCastMessage multicast(ByzCastMessage m){
        replies.clear();
        expectedReplies = (short) m.getDst().length;
        short lca = getLca(m);
        // print("Will send", m, "to lca", lca);
        send(m, lca);

        try {
            sema.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return replies.get(0);
    }

    public short getLca(ByzCastMessage m) {
        if(m.getDst().length == 1) return m.getDst()[0];

        ArrayList<Pair<Short,Short>> list = new ArrayList<>();
        for(int i = 0; i < m.getDst().length; i++){
            if((i+1) < m.getDst().length){
                list.add(new Pair<Short,Short>(m.getDst()[i], m.getDst()[i+1]));
            }
        }
        List<Short> sorted = lcafinder.getBatchLCA(list);
        // print(sorted);
        sorted.sort(Short::compare);
        return sorted.get(0);
    }

    public static void main(String args[]){
        ByzCastMessage m = new ByzCastMessage(0);
        m.setDst( (short)4, (short)5);
        System.out.println(new ByzCastClientProxy().getLca(m));

    }
}
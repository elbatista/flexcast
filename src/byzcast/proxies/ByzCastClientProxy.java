package byzcast.proxies;

import java.io.StringWriter;
import java.io.Writer;
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
import org.jgrapht.nio.dot.DOTExporter;
import base.Node;
import byzcast.comms.ByzCastNettyClientChannel;
import byzcast.messages.ByzCastMessage;
import byzcast.messages.ByzCastMessage.Type;
import io.netty.channel.Channel;
import util.FileManager;
import util.Stats;

public class ByzCastClientProxy extends Node {
    private HashMap<Short, Channel> outChannels;
    private Semaphore sema = new Semaphore(0);
    private ReentrantLock lock = new ReentrantLock();
    private ArrayList<ByzCastMessage> replies = new ArrayList<>();
    private short expectedReplies = 0;
    protected Graph<Short,DefaultEdge> tree;
    protected TarjanLCAFinder<Short,DefaultEdge> lcafinder;
    
    protected Stats stats;
    private long startTime;
    private HashMap<Short, Long> latsPerNode = new HashMap<>();
    short lca;
    short[] dsts;

    public ByzCastClientProxy(short id, int numTree){
        super(id);
        outChannels = new HashMap<>();
        tree = new FileManager().loadByzCastTreeAsGraph();

        
        
        print("ByzCast Tree:", getTreeString());
    }

    protected String getTreeString() {
        DOTExporter<Short, DefaultEdge> exporter = new DOTExporter<>(v->String.valueOf(v));
        Writer writer = new StringWriter();
        exporter.exportGraph(tree, writer);
        return writer.toString();
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

        // armazena latencia por nodo em microsegundo
        latsPerNode.put(reply.getSender(), ((System.nanoTime() - startTime) / 1000));

        replies.add(reply);

        if(replies.size() == expectedReplies){
            if(stats != null) stats.store(latsPerNode, expectedReplies>1, dsts);
            sema.release();
        }
        lock.unlock();
    }

    public ByzCastMessage multicast(ByzCastMessage m){
        replies.clear();
        expectedReplies = (short) m.getDst().length;
        latsPerNode.clear();
        startTime = System.nanoTime();
        dsts = m.getDst();
        lca = getLca(m);
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

        while(true){
            if(sorted.size() > 1){
                list = new ArrayList<>();
                for(int i = 0; i < sorted.size(); i++){
                    if((i+1) < sorted.size()){
                        list.add(new Pair<Short,Short>(sorted.get(i), sorted.get(i+1)));
                    }
                }
                sorted = lcafinder.getBatchLCA(list);
                continue;
            }
            break;
        }

        return sorted.get(0);
    }

}
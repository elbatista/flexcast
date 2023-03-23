package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import com.google.common.math.Stats;

// import java.util.LinkedList;
// import java.util.Queue;
import proxies.ServerProxy;
import util.FileManager;
import base.Node;
import datastructures.AncDst;
import datastructures.LocalDepGraph;
import datastructures.PendingMsg;
import datastructures.PendingMsgNotif;
import messages.LightMessage;
import messages.Message;
import messages.LightMessagesList.Item;
// import messages.LightMessagesList.ItemHst;

public abstract class ServerNode extends ServerProxy implements Runnable {
    // protected HashMap<Short, Queue<Message>> queues;
    // protected HashMap<Integer, Message> tempQueuedMessages;
    protected LocalDepGraph depGraph;
    // protected LightMessagesList myHst = new LightMessagesList();
    // protected LinkedList<Message> pendingNotifs;
    protected FileManager files;
    private short numNodes; // num of nodes
    protected AncDst [][] ancDstInfo;
    // protected boolean special = false, specialNotif = false;
    protected Item dgPointers[];
    protected Item notifPointers[][][][];
    // protected ItemHst dgPointersHst[];
    // protected Item pendNotifPointers[];
    // protected Item pendNotifPointersHst[];
    protected int msgs, notifs, acks;//, pendNotifs;

    HashMap<Integer, PendingMsg> pendingMsgs;
    protected ArrayList<Long> values = new ArrayList<>();

    public ServerNode(short id, int numClients){
        super(id, numClients);
        // this.special = (id == 1);
        // this.specialNotif = !(id > 1);
        // this.queues = new HashMap<>();
        // this.tempQueuedMessages = new HashMap<>();
        // this.pendingAcks = new HashMap<>();
        // this.pendingNotifs = new LinkedList<>();
        this.files = new FileManager();
        for(Node n : files.loadHosts()){
            // a queue for each ancestor (but the special case)
            // if((getId() > 1) && (n.getId() < id)) this.queues.put(n.getId(), new LinkedList<>() );
            if(n.getId() == id) setHost(n.getHost());
            // sets connection to each descendant
            if(n.getId() > id) connectTo(n);
            numNodes++;
        }
        ancDstInfo = new AncDst[numNodes][numNodes];
        this.notifPointers = new Item[numNodes][numNodes][numNodes][numNodes];
        this.dgPointers = new Item[id];//+1];
        this.depGraph = new LocalDepGraph(id);//, this.dgPointers);
        this.pendingMsgs = new HashMap<>();
        // if(getId() > 1 && getId() < (getNumNodes()-1)){
        //     pendNotifPointers = new Item[id];
        //     pendNotifPointersHst = new Item[id];
        // }
        for(short i = 0; i < numNodes; i++)
            for(short j = 0; j < numNodes; j++)
                ancDstInfo[i][j] = new AncDst((short)(id+1));
        print(this, "Start listening... Queues:", this.queues.size());
        new Thread(this).start();
    }
    
    @Override
    public void run(){
        // int count = 0;
        while(true){

            // if(count%30 == 0 && values.size()>0) {
            //     count++;
            //     print(">>>>> us to cmpt deps ("+values.size()+")", TimeUnit.NANOSECONDS.toMicros((long)Stats.of(values).mean()));
            // }

            // pega uma msg de uma fila e processa
            for(short i = 0; i <= getId(); i++){
                Message m = queues.get(i).poll();
                if(m == null) continue;
                // count++;
                switch(m.getType()){
                    case MSG: receiveMsg(m); break;
                    case ACK: receiveAck(m); break;
                    case NOTIF: receiveNotif(m); break;
                    default: break;
                }
            }
        }
    }

    public short getNumNodes() {
        return numNodes;
    }
        
    protected void receiveMsg(Message m){
        // print("receiveMsg", m);
        msgs++;
        
        boolean isLca = (m.getLca() == getId());
        if(isLca){
            aDeliver(m, isLca);
            return;
        }
        
        depGraph.update(m);
        // print("Graph:", depGraph.getDepGraphAsString());
        
        PendingMsg pend = pendingMsgs.get(m.getId());

        // cria entrada de msg pend se nao existir
        if(pend == null){
            pend = new PendingMsg(new LightMessage(m.getId(), m.getDst()), m, getId(), deliveredMsgs, values);
            pendingMsgs.put(m.getId(), pend);
        }
        else {
            pend.setMsg(m);
        }
        pend.addAcksDestsDependencies(m);

        pend.addAcksNotifListsDependencies(m);
        
        pend.addPrevMsgsDependencies(depGraph, dgPointers, pendingMsgs);
        // pend.addPrevMsgsDependencies(depGraph, pendingMsgs);

        reprocessPendingMsgs();
    }

    protected void receiveAck(Message ack){
        // for now, ignore repeated acks for messages already delivered
        if(deliveredMsgs.get(ack.getId()) != null) return;

        // print("receiveAck", ack, "from", ack.getSender());
        acks++;
        depGraph.update(ack);
        //print("Graph:", depGraph.getDepGraphAsString());

        PendingMsg pend = pendingMsgs.get(ack.getId());

        // cria entrada de msg pend se nao existir
        if(pend == null){
            pend = new PendingMsg(new LightMessage(ack.getId(), ack.getDst()), getId(), deliveredMsgs, values);
            pendingMsgs.put(ack.getId(), pend);
        }

        pend.addAcksNotifListsDependencies(ack);

        if (pend.getMsg() != null) pend.addPrevMsgsDependencies(depGraph, dgPointers, pendingMsgs);
        // if (pend.getMsg() != null) pend.addPrevMsgsDependencies(depGraph, pendingMsgs);

        pend.receiveAck(ack);
        // if(pendingMsgs.size()>0) print("<<---");
        // for(PendingMsg p : pendingMsgs.values()){
        //     print(p);
        // }
        // if(pendingMsgs.size()>0) print("--->>");
        reprocessPendingMsgs();
    }
    
    protected void receiveNotif(Message notif){
        // print("receiveNotif", notif, "from", notif.getSender());
        notifs++;
        depGraph.update(notif);
        // print("Graph:", depGraph.getDepGraphAsString());

        PendingMsgNotif pendNotif = (PendingMsgNotif) pendingMsgs.get(notif.getId());

        if(pendNotif == null){
            pendNotif = new PendingMsgNotif(new LightMessage(notif.getId(), notif.getDst()), notif, getId(), deliveredMsgs, values);
            pendingMsgs.put(notif.getId(), pendNotif);
        }
        else {
            pendNotif.addNotif(notif);
        }

        pendNotif.addPrevMsgsDependencies(depGraph, dgPointers, pendingMsgs);
        // pendNotif.addPrevMsgsDependencies(depGraph, pendingMsgs);

        reprocessPendingMsgs();
    }

    @Override
    protected void finish(){
        // if(queues.keySet().stream().anyMatch(key->(queues.get(key).size() > 0))){
        //     print("Some Queue is not empty !!! ");
        //     queues.keySet().forEach(key->{
        //         print("Queue", key, "->", queues.get(key));
        //     });
        //     files.stop();
        //     exit();
        // }
        // print("All Queues empty ! =]");
        files.persistMessages(depGraph.getMyHst(), getId(), false, false);
        print("-------------------------------------");
        print("Total msgs in the history:", depGraph.getMyHst().size());
        print("Total local msgs received:", localMsgs);
        print("Total msgs received:", msgs);
        print("Total notifs:", notifs);
        print("Total acks:", acks);
        print("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }
    
    abstract void sendAck(Message m);
    abstract void reprocessPendingMsgs();
    abstract void aDeliver(Message m, boolean isLca);
}
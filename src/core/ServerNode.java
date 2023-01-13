package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import proxies.ServerProxy;
import util.FileManager;
import base.Node;
import datastructures.AncDst;
import datastructures.LocalDepGraph;
import messages.Message;
import messages.LightMessagesList.Item;
import messages.LightMessagesList.ItemHst;

public abstract class ServerNode extends ServerProxy {
    protected HashMap<Short, Queue<Message>> queues;
    protected HashMap<Integer, Message> tempQueuedMessages;
    protected LocalDepGraph depGraph;
    protected LinkedList<Message> pendingNotifs;
    // protected HashMap<Integer, ArrayList<Message>> pendingAcks;
    protected FileManager files;
    private short numNodes; // num of nodes
    protected AncDst [][] ancDstInfo;
    protected boolean special = false, specialNotif = false;
    protected Item dgPointers[];
    // protected Item notifPointers[][][][];
    protected ItemHst dgPointersHst[];
    protected Item pendNotifPointers[];
    protected Item pendNotifPointersHst[];
    protected int msgs, notifs, acks, pendNotifs;

    public ServerNode(short id, int numClients){
        super(id, numClients);
        this.special = (id == 1);
        this.specialNotif = !(id > 1);
        this.queues = new HashMap<>();
        this.tempQueuedMessages = new HashMap<>();
        // this.pendingAcks = new HashMap<>();
        this.pendingNotifs = new LinkedList<>();
        this.files = new FileManager();
        for(Node n : files.loadHosts()){
            // a queue for each ancestor (but the special case)
            if((getId() > 1) && (n.getId() < id)) this.queues.put(n.getId(), new LinkedList<>() );
            if(n.getId() == id) setHost(n.getHost());
            // sets connection to each descendant
            if(n.getId() > id) connectTo(n);
            numNodes++;
        }
        ancDstInfo = new AncDst[numNodes][numNodes];
        // notifPointers = new Item[numNodes][numNodes][numNodes][numNodes];
        if(id > 1){
            this.dgPointers = new Item[id];
            this.dgPointersHst = new ItemHst[id];
        }
        this.depGraph = new LocalDepGraph(id, this.dgPointers);
        if(getId() > 1 && getId() < (getNumNodes()-1)){
            pendNotifPointers = new Item[id];
            pendNotifPointersHst = new Item[id];
        }
        for(short i = 0; i < numNodes; i++)
            for(short j = 0; j < numNodes; j++)
                ancDstInfo[i][j] = new AncDst((short)(id+1));
        print(this, "Start listening... Queues:", this.queues.size());
        print("########################################################");
        print("ADJUSTED ACKS ON QUEUES");
        print("########################################################");
    }

    public short getNumNodes() {
        return numNodes;
    }

    protected void updateDG(Message m){
        depGraph.update(m);
    }

    @Override
    protected void receiveMsg(Message m, boolean updateDG){
        print("receiveMsg", m);
        msgs++;
        boolean isLca = (m.getLca() == getId());
        if(isLca){
            aDeliver(m, isLca);
        }
        else {
            if(updateDG) depGraph.update(m);
            if(special){
                aDeliverSpecial(m);
                return;
            }
            if(queues.get(m.getLca()).size() > 0) ((Message)((LinkedList) queues.get(m.getLca())).getLast()).setNextInQueue(m);
            queues.get(m.getLca()).offer(m);
            tempQueuedMessages.put(m.getId(), m);
            reprocessQueues();
        }
    }

    @Override
    protected void receiveAck(Message ack, boolean updateDG){
        print("receiveAck", ack, "from", ack.getSender());
        acks++;
        if(updateDG) depGraph.update(ack);
        if(queues.get(ack.getSender()).size() > 0) ((Message)((LinkedList) queues.get(ack.getSender())).getLast()).setNextInQueue(ack);
        queues.get(ack.getSender()).offer(ack);
        //tempQueuedMessages.put(ack.getId(), ack);
        reprocessQueues();

        // Message m = tempQueuedMessages.get(ack.getId());
        // // if not found, stores the ack in pending acks set and returns without reprocessing queues
        // if(m == null){
        //     ArrayList<Message> list = pendingAcks.get(ack.getId());
        //     if(list == null){
        //         list = new ArrayList<>();
        //         pendingAcks.put(ack.getId(), list);
        //     }
        //     list.add(ack);
        //     return;
        // }
        // // when the related message is found, add the ack to it
        // m.getAcks().add(ack);
        // reprocessQueues();
    }
    
    @Override
    protected void receiveNotif(Message notif, boolean updateDG){
        print("receiveNotif", notif, "from", notif.getSender());
        notifs++;
        if(updateDG) depGraph.update(notif);
        if(!specialNotif){
            if(pendingNotifs.size() > 0){
                pendingNotifs.add(notif);
                pendNotifs++;
                return;
            }
            if(!canDeliverNotif(notif)){
                pendingNotifs.add(notif);
                pendNotifs++;
                return;
            }
        }
        sendAck(notif);
    }

    protected void finish(){
        if(queues.keySet().stream().anyMatch(key->(queues.get(key).size() > 0))){
            print("Some Queue is not empty !!! ");
            queues.keySet().forEach(key->{
                print("Queue", key, "->", queues.get(key));
            });
            files.stop();
            exit();
        }
        print("All Queues empty ! =]");
        // if(pendingNotifs.size() > 0 || pendingAcks.size() > 0){
            if(pendingNotifs.size() > 0){
                print("Warning: pendingNotifs is not empty... =[");
                //for(Message pend : pendingNotifs) print(pend, pend.getPendNotifOrigins());
            }
            // if(pendingAcks.size() > 0){
            //     print("Warning: pendingAcks is not empty... =[");
                //for(ArrayList<Message> pendList : pendingAcks.values()) 
                    //for(Message pend : pendList) print(pend, pend.getSender());
            // }
            if(tempQueuedMessages.size() > 0){
                print("Warning: tempQueuedMessages is not empty... =[");
            }
        //     files.stop();
        //     exit();
        // }
        files.persistMessages(depGraph.getMyHst(), getId(), false, false);
        print("-------------------------------------");
        print("Total msgs in the history:", depGraph.getMyHst().size());
        print("Total local msgs received:", localMsgs);
        print("Total msgs received:", msgs);
        print("Total notifs:", notifs);
        print("Total acks:", acks);
        print("Total pendNotifs:", pendNotifs);
        print("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }
    
    abstract void sendAck(Message m);
    abstract void reprocessQueues();
    abstract void aDeliver(Message m, boolean isLca);
    abstract void aDeliverSpecial(Message m);
    abstract boolean canDeliverNotif(Message notif);
}
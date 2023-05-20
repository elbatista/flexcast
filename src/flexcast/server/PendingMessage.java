package flexcast.server;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.javatuples.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.*;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.nio.dot.DOTExporter;
import flexcast.messages.LightMessage;
import flexcast.messages.LightMessagesList;
import flexcast.messages.Message;
import flexcast.messages.LightMessagesList.Item;

public class PendingMessage {
    private int id;
    private Message m;
    private int acksFromDstsNeeded;
    private Set<Integer> msgDeps;
    private boolean msgsDepsFlag = false;
    private ArrayList<NotifListAckObj> notifLists;
    private Graph<Integer, DefaultEdge> hstGraph;
    private HashSet<LightMessage> hstGraphMsgs = new HashSet<>();

    public PendingMessage(int id) {
        this.id = id;
        this.msgDeps = new HashSet<>();
        this.notifLists = new ArrayList<>();
        hstGraph = GraphTypeBuilder.<Integer, DefaultEdge> directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .weighted(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();
    }

    public int getId() {
        return this.id;
    }

    public boolean getMsgsDepsFlag() {
        return msgsDepsFlag;
    }

    public void setMsgsDepsFlag(boolean msgsDepsFlag) {
        this.msgsDepsFlag = msgsDepsFlag;
    }

    public Collection<LightMessage> getHstMsgs() {
        return hstGraphMsgs;
    }

    public void addHst(HashMap<Short, LightMessagesList> hst) {
        for(LightMessagesList list : hst.values()){
            Item item = list.getFirst();
            while(item != null){
                hstGraphMsgs.add(item.get());
                if(!hstGraph.containsVertex(item.get().getId())) {
                    hstGraph.addVertex(item.get().getId());
                }
                if(item.getPrev() != null){
                    if(!hstGraph.containsEdge(item.getPrev().get().getId(), item.get().getId())) {
                        hstGraph.addEdge(item.getPrev().get().getId(), item.get().getId());
                    }
                }
                item = item.getNext();
            }
        }
    }

    public Message getMsg() {
        return m;
    }

    public void setMsg(Message m) {
        this.m = m;
    }

    public boolean gotAllAcksFromDsts() {
        return acksFromDstsNeeded == 0;
    }

    public void incAcksFromDstsNeeded() {
        this.acksFromDstsNeeded++;
    }

    public void decAcksFromDstsNeeded() {
        this.acksFromDstsNeeded--;
    }

    public Set<Integer> getMsgDeps() {
        return msgDeps;
    }

    public void setMsgDeps(Set<Integer> msgDeps) {
        this.msgDeps = msgDeps;
    }

    public ArrayList<NotifListAckObj>  getNotifLists() {
        return notifLists;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PendingMessage other = (PendingMessage) obj;
        if (id != other.id)
            return false;
        return true;
    }

    public String graphString(){
        DOTExporter<Integer, DefaultEdge> exporter = new DOTExporter<>();
        Writer writer = new StringWriter();
        exporter.exportGraph(hstGraph, writer);
        return writer.toString();
    }

    public boolean msgLmPrecedesM(LightMessage lm, LightMessage m) {
        BellmanFordShortestPath<Integer, DefaultEdge> bellmanFordShortestPath  = new BellmanFordShortestPath<>(hstGraph);
        return (bellmanFordShortestPath.getPath(lm.getId(), m.getId()) != null);
    }

    public void addNotifList(short notifier, ArrayList<Pair<Short, Integer>> arrayList) {
        for(Pair<Short, Integer> p : arrayList){
            NotifListAckObj item = new NotifListAckObj(notifier, p.getValue0(), p.getValue1());
            if(!notifLists.contains(item))
                notifLists.add(item);
        }
    }

    public boolean gotAllAcksFromNotifLists() {
        for(NotifListAckObj notifAck : notifLists){
            if (!notifAck.isReceived()) return false;
        }
        return true;
    }

    public void addAckFromNotifList(short notifier, short sender, int idNotif) {
        for(NotifListAckObj nl : notifLists){
            if(nl.getNotifier() == notifier && nl.getAckSender() == sender && nl.getIdNotif() == idNotif && !nl.isReceived()){
                nl.setReceived();
                return;
            }
        }
        // se chegou aqui, eh pq nao achou correspondente (a msg/ack nao chegou ainda)
        // entao insere um registro ja resolvido
        notifLists.add(new NotifListAckObj(notifier, sender, idNotif, true));
    }

    class NotifListAckObj {
        private short notifier;
        private int idNotif;
        private short ackSender;
        private boolean received = false;
        public NotifListAckObj (short notifier, short ackSender, int idNotif){
            this.notifier = notifier;
            this.ackSender = ackSender;
            this.idNotif = idNotif;
        }
        public NotifListAckObj (short notifier, short ackSender, int idNotif, boolean rcvd){
            this.notifier = notifier;
            this.ackSender = ackSender;
            this.idNotif = idNotif;
            this.received = rcvd;
        }
        public short getAckSender() {
            return ackSender;
        }
        public short getNotifier() {
            return notifier;
        }
         public int getIdNotif() {
            return idNotif;
        }
       public void setReceived(){
            received = true;
        }
        public boolean isReceived(){
            return received;
        }
        public String toString(){
            return "(n:"+getNotifier()+", s:"+getAckSender()+", id:"+getIdNotif()+(isReceived()?", recvd)":")");
        }
        public boolean equals(Object o){
            if(!(o instanceof NotifListAckObj)) return false;
            return (getNotifier() == ((NotifListAckObj)o).getNotifier() && getAckSender() == ((NotifListAckObj)o).getAckSender() && getIdNotif() == ((NotifListAckObj)o).getIdNotif());
        }
    }
}

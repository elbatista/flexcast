package flexcast.server;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import org.javatuples.Pair;
import flexcast.messages.Message;

public class PendingMessage {
    private int id;
    private Message m;
    private int acksFromDstsNeeded;
    private Set<Integer> msgDeps;
    private boolean msgsDepsFlag = false;
    private ArrayList<NotifListAckObj> notifLists;

    public PendingMessage(int id) {
        this.id = id;
        this.msgDeps = new HashSet<>();
        this.notifLists = new ArrayList<>();
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

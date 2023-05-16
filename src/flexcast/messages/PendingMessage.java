package flexcast.messages;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import util.BaseObj;

public class PendingMessage extends BaseObj {
    private int id;
    private Message m;
    private int acksFromDstsNeeded;
    private Set<Integer> msgDeps;
    private HashSet<ArrayList<LightMessage>> hsts;
    private boolean msgsDepsFlag = false;
    private ArrayList<NotifListAckObj> notifLists;

    public PendingMessage(int id) {
        this.id = id;
        this.msgDeps = new HashSet<>();
        this.hsts = new HashSet<>();
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

    public HashSet<ArrayList<LightMessage>> getHsts() {
        return hsts;
    }

    public Message getMsg() {
        return m;
    }

    public void setMsg(Message m) {
        hsts.addAll(m.getHst());
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

    public boolean msgLmPrecedesM(LightMessage lm, Message m2) {

        ArrayList<LightMessage> toCheck = new ArrayList<>();

        // para cada historico
        for(ArrayList<LightMessage> hst : getHsts()){
            boolean foundLM = false;
            boolean foundM = false;
            int state = 0; // 1 - found lm first; 2 - found m first
            for(LightMessage aux : hst){

                if (state == 1){
                    // if we first found lm, then
                    // all next messages should also be checked whether they come before m
                    toCheck.add(aux);
                }

                if(aux.getId() == lm.getId()) {
                    foundLM = true;
                    if(state == 0) state = 1;
                }

                if(aux.getId() == m.getId()) {
                    foundM = true;
                    if(state == 0) state = 2;
                }

            }
            if(foundLM && foundM){
                return state == 1;
            }
        }

        for (LightMessage lm2 : toCheck){
            if(msgLmPrecedesM(lm2, m)) return true;
        }

        // print("Not able to tell which one comes first. lm:", lm, "m:", m);
        // print("Pend hst:", getHsts());
        //exit();

        return false;
    }

    public void addNotifList(short notifier, ArrayList<Short> notifList) {
        for(short d : notifList){
            NotifListAckObj item = new NotifListAckObj(notifier, d);
            if(!notifLists.contains(item)) notifLists.add(item);
        }
    }

    public boolean gotAllAcksFromNotifLists() {
        // print("Checking nls:", notifLists);
        for(NotifListAckObj notifAck : notifLists){
            if (!notifAck.isReceived()) return false;
        }
        return true;
    }

    class NotifListAckObj {
        private short notifier;
        private short ackSender;
        private boolean received = false;
        public NotifListAckObj (short notifier, short ackSender){
            this.notifier = notifier;
            this.ackSender = ackSender;
        }
        public NotifListAckObj (short notifier, short ackSender, boolean rcvd){
            this.notifier = notifier;
            this.ackSender = ackSender;
            this.received = rcvd;
        }
        public short getAckSender() {
            return ackSender;
        }
        public short getNotifier() {
            return notifier;
        }
        public void setReceived(){
            received = true;
        }
        public boolean isReceived(){
            return received;
        }
        public String toString(){
            return "(n:"+getNotifier()+", s:"+getAckSender()+(isReceived()?", recvd)":")");
        }
        public boolean equals(Object o){
            if(!(o instanceof NotifListAckObj)) return false;
            return (getNotifier() == ((NotifListAckObj)o).getNotifier() && getAckSender() == ((NotifListAckObj)o).getAckSender());
        }
    }

    public void addAckFromNotifList(short notifier, short sender) {
        for(NotifListAckObj nl : notifLists){
            if(nl.getNotifier() == notifier && nl.getAckSender() == sender && !nl.isReceived()){
                nl.setReceived();
                return;
            }
        }
        // se chegou aqui, eh pq nao achou correspondente (a msg/ack nao chegou ainda)
        // entao insere um registro ja resolvido
        notifLists.add(new NotifListAckObj(notifier, sender, true));

    }
}

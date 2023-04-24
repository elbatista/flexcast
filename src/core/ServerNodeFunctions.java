package core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import messages.LightMessage;
import messages.LightMessagesList;
import messages.Message;
import messages.LightMessagesList.Item;
import messages.LightMessagesList.ItemHst;
import datastructures.MessageDepGraph;
import datastructures.NotifList;
import messages.Message.Type;
import util.ArgsParser;

public class ServerNodeFunctions extends ServerNode {

    public ServerNodeFunctions(short id, ArgsParser args) {
        super(id, args.getClientCount());
    }

    @Override
    protected void reprocessQueues() {
        //print("reprocessQueues");
        boolean delivered = true;
        while(delivered){
            delivered = false;
            for(Queue<Message> q : queues.values()){
                Message m = q.peek();
                if(m != null && canDeliver(m)){
                    aDeliver(m, false);
                    delivered = true;
                }
            }
        }
    }

    @Override
    protected void aDeliver(Message m, boolean lca) {
        ItemHst myHstItem = depGraph.addToHst(new LightMessage(m.getId(), m.getDst()));
        if(lca){
            forward(m);
        }
        else{
            queues.get(m.getLca()).poll();
            tempQueuedMessages.remove(m.getId());
            updatePointers(m, myHstItem);
            sendAck(m);
        }
        if(!specialNotif) processPendingNotifs(m);
        //print("delivered", m);
        sendReply(m);
    }

    private void updatePointers(Message m, ItemHst myHstItem) {
        short anc = m.getLca();
        LightMessagesList.Item itemDG = dgPointers[anc] == null ? depGraph.getGraph()[anc].getFirst() : dgPointers[anc];
        while(itemDG != null){
            if(itemDG.get().getId() == m.getId()){
                dgPointers[anc] = itemDG;
                break;
            }
            itemDG = itemDG.getNext();
        }
        dgPointersHst[anc] = myHstItem;
    }

    @Override
    protected void aDeliverSpecial(Message m) {
        depGraph.addToHst(new LightMessage(m.getId(), m.getDst()));
        sendAck(m);            
        sendReply(m);
        //print("delivered", m);
    }

    @Override
    protected boolean canDeliverNotif(Message notif){
        boolean pend = false;
        for(short i = 0; i < getId(); i++){
            Item start = pendNotifPointers[i] == null ? depGraph.getGraph()[i].getFirst() : pendNotifPointers[i];
            while(start != null){
                pendNotifPointers[i] = start;
                if(start.get().isAddressedTo(getId())){
                    boolean delivered = false;
                    Item startHst = pendNotifPointersHst[i] == null ? depGraph.getMyHst().getFirst() : pendNotifPointersHst[i];
                    while(startHst != null){
                        pendNotifPointersHst[i] = startHst;
                        if(start.get().getId() == startHst.get().getId()){
                            delivered = true;
                            break;
                        }
                        startHst = startHst.getNext();
                    }
                    if(!delivered){
                        pend = true;
                        notif.getPendNotifOrigins().add(start.get().getId());
                    }
                }
                start = start.getNext();
            }
        }
        return !pend;
    }

    @Override
    protected void sendAck(Message m) {
        if(getId() == (getNumNodes()-1)) return; // last one doesnt have someone to send acks
        Set<Short> notifList = null;
        if(getId() < (getNumNodes()-2)) notifList = sendNotif(m); // last 2 nodes never have someone to notify
        for(short dst : m.getDst()){
            if(dst > getId()){
                Message ack = new Message(m.getId());
                ack.setType(Type.ACK);
                ack.setDst(m.getDst());
                ack.setSender(getId());
                if(m.getType() == Type.NOTIF) ack.setIdNotifier(m.getSender());
                ack.ackIsFromDst(m.getType() == Type.MSG && m.isAddressedTo(getId()));

                newHst(ack, dst);

                if(notifList != null && notifList.size() > 0)
                    ack.addNotifList(notifList, getId());

                send(ack, dst);
            }
        }
    }

    private void processPendingNotifs(Message delivered_m) {
        if(pendingNotifs.size() == 0) return;
        Message notif = pendingNotifs.peek();
        notif.getPendNotifOrigins().remove(Integer.valueOf(delivered_m.getId()));
        if(notif.getPendNotifOrigins().size() > 0) return;
        for(;;){
            sendAck(notif);
            pendingNotifs.removeFirst();
            notif = pendingNotifs.peek();
            if(notif == null) return;
            if(!canDeliverNotif(notif)) return;
        }
    }

    private void forward(Message m) {
        Set<Short> notifList = null;
        if(getId() < (getNumNodes()-2)) notifList = sendNotif(m); // last 2 nodes never have someone to notify
        for(short dst : m.getDst()){
            if(dst > getId()) {
                Message toSend = new Message(m.getId());
                toSend.setType(m.getType());
                toSend.setDst(m.getDst());
                toSend.setCliId(m.getCliId());
                toSend.setSender(getId());
                newHst(toSend, dst);

                if(notifList != null && notifList.size() > 0)
                    toSend.addNotifList(notifList, getId());

                send(toSend, dst);
            }
        }
    }

    private void newHst(Message m, short dst) {
        MessageDepGraph mg = new MessageDepGraph((short)(getId()+1));
        for(short anc = 0; anc <= getId(); anc++){
            Item last = ancDstInfo[anc][dst].getLastInPfx();
            if(last != null)
                last = last.getNext();
            else if(depGraph.getGraph()[anc].getFirst() != null) 
                last = depGraph.getGraph()[anc].getFirst();
            while(last != null){
                mg.add(last.get(), anc);
                last = last.getNext();
            }
            ancDstInfo[anc][dst].setLastInPfx(depGraph.getGraph()[anc].getLast());
        }
        m.setDepGraph(mg);
    }

    private Set<Short> sendNotif(Message m){
        Set<Short> notifList = new HashSet<>();
        for(short son = (short)(getId()+1); son < getNumNodes(); son++){
            for(short dst : m.getDst()){
                if(son < dst && !m.isAddressedTo(son) && isThereMsgTo(m.getLca(), son, dst, (m.getType() == Type.MSG))){
                    notifList.add(son);
                }
            }
        }
        for(short node : notifList){
            Message notif = new Message(m.getId());
            notif.setSender(getId());
            notif.setDst(m.getDst());
            notif.setType(Type.NOTIF);
            for (NotifList nl : m.getNotifList())
                notif.addNotifList(nl.getNotifList(), nl.getNotifier());
            notif.addNotifList(notifList, getId());
            newHst(notif, node);
            send(notif, node);
        }
        return notifList;
    }

    private boolean isThereMsgTo(short lca, short son, short dst, boolean shouldUpdateLast) {
        for(short i = 0; i <= getId(); i++){
            Item start = notifPointers[lca][son][dst][i] == null ? depGraph.getGraph()[i].getFirst() : notifPointers[lca][son][dst][i];
            while(start != null){
                if(shouldUpdateLast) notifPointers[lca][son][dst][i] = start.getNext() == null ? start : start.getNext();
                if(start.get().isAddressedTo(son))
                    return true;
                start = start.getNext();
            }
        }
        //print("no message to", son, "in my hst that i should notify.");
        return false;
    }

    // methods for the delivery process:
    private boolean canDeliver(Message m) {
        //print("canDeliver", m);
        addPendingAcks(m);
        if(!checkAcksFromDsts(m)) return false;

        for(NotifList nl : m.getNotifList())
            if(!checkNotifList(m, nl)) return false;

        if(!checkMessageAcks(m)) return false;

        return checkDepGraph(m);
    }

    private boolean checkDepGraph(Message m) {
        for(short i = 0; i < getId(); i++){
            Item item = dgPointers[i];
            if(item == null) item = depGraph.getGraph()[i].getFirst();
            while(item != null){
                LightMessage m2 = item.get();
                if(m2.getId() == m.getId()) break;

                if(m2.isAddressedTo(getId()) && !checkHistory(m, m2.getId(), i)){
                    short lcd = m.getLcd(m2);
                    LightMessage lm = new LightMessage(m.getId(), m.getDst());

                    // Exception 1 - i am lcd
                    if(lcd == getId() && !depGraph.generatesCycleOnDelivering(lm, m2, dgPointers, dgPointersHst)){
                        item = item.getNext();
                        continue;
                    }

                    // Exception 2 - i will follow lcd's order
                    if(lcd != getId() && depGraph.doesMessageComesFirstThan(lcd, lm, m2, dgPointers)){
                        item = item.getNext();
                        continue;
                    }
                    //print("cant deliver", lm, "missing", m2, "in my hst");
                    return false;
                }
                item = item.getNext();
            }
        }
        return true;
    }

    private boolean checkHistory(Message m, int m2id, short ancIdx) {
        Item item = dgPointersHst[ancIdx];
        if(item == null) item = depGraph.getMyHst().getFirst();
        while(item != null){
            if(item.get().getId() == m2id)
                return true;
            item = item.getNext();
        }
        return false;
    }

    private boolean checkMessageAcks(Message m) {
        for(Message ack : m.getAcks())
            for(NotifList nl : ack.getNotifList())
                if(!checkNotifList(m, nl)) 
                    return false;
        return true;
    }

    private boolean checkNotifList(Message m, NotifList nl){
        for(short anc : nl.getNotifList()){
            if(anc > m.getLca() && anc < getId()){
                if(!m.getAcks().stream().anyMatch(a->{
                    return a.getSender() == anc 
                    && a.getIdNotifier() == nl.getNotifier()
                    && !a.ackIsFromDst();
                }) ){
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkAcksFromDsts(Message m) {
        for(short anc : m.getDst())
            if(anc > m.getLca() && anc < getId() && !m.getAcks().stream().anyMatch(a->{return a.getSender() == anc && a.ackIsFromDst();}))
                return false;
        return true;
    }

    private void addPendingAcks(Message m) {
        ArrayList<Message> l = pendingAcks.get(m.getId());
        if(l == null) return;
        //if there was any pending ack for this message, add them to the message
        m.getAcks().addAll(l);
        // remove from pending acks
        pendingAcks.remove(m.getId());
    }
}
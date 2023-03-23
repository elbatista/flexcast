package core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import messages.LightMessage;
import messages.LightMessagesList;
import messages.Message;
import messages.LightMessagesList.Item;
import messages.LightMessagesList.ItemHst;
import datastructures.MessageDepGraph;
import datastructures.NotifList;
import datastructures.PendingMsg;
import datastructures.PendingMsgNotif;
import messages.Message.Type;
import util.ArgsParser;

public class ServerNodeFunctions extends ServerNode {

    // private Item [] notifPtrs;

    public ServerNodeFunctions(short id, ArgsParser args) {
        super(id, args.getClientCount());
        // notifPtrs = new Item[getNumNodes()];
    }

    public void reprocessPendingMsgs() {
        boolean reprocess;
        do {
            reprocess = false;
            ArrayList<Integer> toRemove = new ArrayList<>();
            if(getId()>2 && pendingMsgs.size()>0) print("\n\n");
            for(PendingMsg pend : pendingMsgs.values()){
                if(getId()>2 )print(pend);
                // notifs
                if(pend instanceof PendingMsgNotif){
                    if(pend.allDepsOK()) {
                        for(Message n : ((PendingMsgNotif)pend).getNotifs()){
                            sendAck(n);
                        }
                        toRemove.add(pend.getLMessage().getId());
                    }
                }
                // msgs normais
                else {
                    // se ainda nao recebi a respectiva msg, nao posso entregar obviamente
                    if(pend.getMsg() == null) continue;
                    // verifico demais dependencias
                    if(pend.allDepsOK()) {
                        aDeliver(pend.getMsg(), false);
                        reprocess = true;
                        toRemove.add(pend.getLMessage().getId());
                        // visto que entreguei essa msg, removo ela 
                        // de dependencias de todas msgs/notifs pendentes
                        for(PendingMsg pend2 : pendingMsgs.values()){
                            pend2.removeMsgDep(pend.getMsg());
                        }
                    }
                }
            }
            // if(getId()>2 && pendingMsgs.size()>0) print("--->>");
            for(int key : toRemove) pendingMsgs.remove(key);
        }
        while(reprocess);
        if(getId()>2 && !reprocess) print("Graph:", depGraph.getDepGraphAsString());
    }

    @Override
    protected void aDeliver(Message m, boolean lca) {
        ItemHst myHstItem = depGraph.addToHst(new LightMessage(m.getId(), m.getDst()));
        deliveredMsgs.put(m.getId(), true);
        if(lca){
            forward(m);
        }
        else {
            updatePointers(m, myHstItem);
            sendAck(m);
        }
        // if(getId()>2) print("delivered", m);
        sendReply(m);
    }

    private void updatePointers(Message m, ItemHst myHstItem) {
        short anc = m.getLca();
        //1
        // LightMessagesList.Item itemDG = dgPointers[anc] == null ? depGraph.getGraph()[anc].getFirst() : dgPointers[anc];
        // while(itemDG != null){
        //     if(itemDG.get().getId() == m.getId()){
        //         dgPointers[anc] = itemDG;
        //         break;
        //     }
        //     itemDG = itemDG.getNext();
        // }

        //2
        // dgPointers[anc] = dgPointers[anc] == null ? depGraph.getGraph()[anc].getFirst() : (dgPointers[anc].getNext() == null ? dgPointers[anc] : dgPointers[anc].getNext());
        
        //3
        // Item start = dgPointers[anc] == null ? depGraph.getGraph()[anc].getFirst() : dgPointers[anc];
        Item start = depGraph.getGraph()[anc].getFirst();
        while(start != null){
            if(start.get().isAddressedTo(getId())){
                if(deliveredMsgs.get(start.get().getId()) == null){
                    break;
                }
                else {
                    dgPointers[anc] = start;
                }
            }
            start = start.getNext();
        }
        
        //dgPointers[getId()] = myHstItem;
        // dgPointers[getId()] = dgPointers[getId()] == null ? depGraph.getMyHst().getFirst() : (dgPointers[getId()].getNext() == null ? dgPointers[getId()] : dgPointers[getId()].getNext());;
    }

    @Override
    protected void sendAck(Message m) {

        if(getId() == (getNumNodes()-1)) return; // last one doesnt have someone to send acks
        Set<Short> notifList = null;

        // se nao vou enviar acks, tbm nao envio notifs
        boolean shouldSendNotifs = false;
        for(short dst : m.getDst()){
            if(dst > getId()){
                shouldSendNotifs = true;
                break;
            }
        }
        if(shouldSendNotifs && getId() < (getNumNodes()-2)) notifList = sendNotif(m); // last 2 nodes never have someone to notify

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
                // print("sent ack", ack, "to", dst);
            }
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
                // print("fwd", toSend, "to", dst);
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
                if(son < dst && !m.isAddressedTo(son) && isThereMsgTo(m.getLca(), son, dst)){
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
            // print("sent notif", notif, "to", node);
        }
        return notifList;
    }

    private boolean isThereMsgTo(short lca, short son, short dst){
        for(short i = 0; i <= getId(); i++){
            Item start = notifPointers[lca][son][dst][i] == null ? depGraph.getGraph()[i].getFirst() : notifPointers[lca][son][dst][i];
            // Item start = depGraph.getMyHst().getFirst();
            while(start != null){
                notifPointers[lca][son][dst][i] = (start.getNext() == null) ? start : start.getNext();
                // notifPtrs[dst] = start;
                if(start.get().isAddressedTo(son)){
                    return true;
                }
                start = start.getNext();
            }
        }
        return false;
    }
}
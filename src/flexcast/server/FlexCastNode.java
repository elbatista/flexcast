package flexcast.server;

import proxies.ServerProxy;
import util.ArgsParser;
import util.FileManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import org.javatuples.Pair;
import com.google.common.math.Stats;
import base.Node;
import flexcast.messages.LightMessage;
import flexcast.messages.Message;
import flexcast.messages.LightMessagesList.Item;
import flexcast.messages.Message.Type;

// @SuppressWarnings("unused")
public class FlexCastNode extends ServerProxy {
    private short numNodes;
    private int idNotif = 0;
    private FileManager files;
    private History history;
    private ArrayList<Short> ancestors = new ArrayList<>();
    private ArrayList<Short> descendants = new ArrayList<>();
    private HashMap<Short, ArrayList<Message>> queues = new HashMap<>();
    protected LinkedList<Message> pendingNotifs = new LinkedList<>();
    private HashMap<Short, HashMap<Short, Item>> ancHstPointersPerDesc = new HashMap<>();
    private HashMap<Short, Item> hstPointersPerDesc = new HashMap<>();
    
    private ArrayList<Message> pendCliMsgs = new ArrayList<>();


    // TODO
    // TO REMOVE !
    private int msgs=0, acks=0, notifs=0, gcs=0;
    HashMap<Integer, HashMap<Integer, Boolean>> map = new HashMap<>();
    private ArrayList<Integer> gsizes = new ArrayList<>();


    public FlexCastNode(short id, ArgsParser p){
        super(id, p.getClientCount());
        this.files = new FileManager();
        if(!p.getLog()) setPrint(false);
        for(Node n : files.loadHosts()){
            // data for each ancestor
            if(n.getId() < id) {
                ancestors.add(n.getId());
                queues.put(n.getId(), new ArrayList<>());
                ancHstPointersPerDesc.put(n.getId(), new HashMap<>());
            }

            // set data for myself
            if(n.getId() == id) setHost(n.getHost());

            // sets connection to each descendant
            if(n.getId() > id) {
                descendants.add(n.getId());
                connectTo(n);
                for(short anc : ancHstPointersPerDesc.keySet())
                    ancHstPointersPerDesc.get(anc).put(n.getId(), null);
            }
            numNodes++;
        }
        // notifPointers = new Item[numNodes][numNodes][numNodes][numNodes];
        history = new History(ancestors, getId());
        printF(this, "FlexCast - Start listening...");
    }

    @Override
    protected void receiveMsg(Message m){
        print("Received msg", m, "queues", queues);
        msgs++;
        history.addHst(m);
        if(getId() == m.getLca()){
            if(history.getPenMsgs().size() > 0){
                pendCliMsgs.add(m);
                return;
            }
            deliver(m);
        }
        else {
            queues.get(m.getLca()).add(m);
        
            PendingMessage pend = history.getPendMsg(m.getId());
            if(pend == null){
                pend = new PendingMessage(m.getId());
                history.addPendMsg(m.getId(), pend);
            }
            pend.setMsg(m);
            for(short d : m.getDst()) if(d > m.getLca() && d < getId()) 
                pend.incAcksFromDstsNeeded();

            // cria pendencias de ack para os notificados da notif list
            pend.addNotifList(m.getSender(), m.getNotifList());

            reprocessQueues();
        }
    }

    @Override
    protected void receiveAck(Message ack){
        print("Received ack", ack, "from", ack.getSender(), "queues", queues);
        acks++;
        history.addHst(ack);
        PendingMessage pend = history.getPendMsg(ack.getId());
        if(pend == null){
            pend = new PendingMessage(ack.getId());
            history.addPendMsg(ack.getId(), pend);
        }

        // cria pendencias de ack para os notificados da notif list desse ack
        pend.addNotifList(ack.getSender(), ack.getNotifList());

        // "entrega" o ack
        if(ack.isAddressedTo(ack.getSender())){
            pend.decAcksFromDstsNeeded();
        } else {
            pend.addAckFromNotifList(ack.getIdNotifier(), ack.getSender(), ack.getIdNotif());
        }

        reprocessQueues();
    }

    @Override
    protected void receiveNotif(Message notif){
        print("Received notif", notif, "from", notif.getSender(), "queues", queues);
        notifs++;
        history.addHst(notif);

        if(pendingNotifs.size() > 0){
            pendingNotifs.add(notif);
            return;
        }
        if(!canDeliverNotif(notif)){
            pendingNotifs.add(notif);
            return;
        }

        sendAcks(notif);
    }

    private boolean canDeliverNotif(Message notif) {
        boolean pend = false;
        for(short a : ancestors){
            for(LightMessage lm : history.getAncHstToMe(a)){
                if(history.getDeliveredMsgs().get(lm.getId()) == null){
                    pend = true;
                    notif.getPendNotifOrigins().add(lm.getId());
                }
            }
        }
        return !pend;
    }

    private void processPendingNotifs(Message delivered_m) {
        if(pendingNotifs.size() == 0) return;
        Message notif = pendingNotifs.peek();
        notif.getPendNotifOrigins().remove(Integer.valueOf(delivered_m.getId()));
        if(notif.getPendNotifOrigins().size() > 0) return;
        for(;;){
            sendAcks(notif);
            pendingNotifs.removeFirst();
            notif = pendingNotifs.peek();
            if(notif == null) return;
            if(!canDeliverNotif(notif)) return;
        }
    }

    private void reprocessQueues() {
        boolean retry = true;
        while(retry){
            retry = false;
            for(ArrayList<Message> queue : queues.values()){
                Message m = null;
                try{ m = queue.get(0); } catch(IndexOutOfBoundsException ignore){}

                if(m != null && canDeliver(m)){
                    deliver(m);
                    retry = true;
                }
            }
        }
        if(history.getPenMsgs().isEmpty()){
            for(Message x : pendCliMsgs)
                deliver(x);
            pendCliMsgs.clear();
        }
    }

    private boolean canDeliver(Message m) {
        // check all types of dependencies
        PendingMessage pend = history.getPendMsg(m.getId());
        if(pend == null) return false;

        if(pend.getMsg() == null) return false;

        if(!pend.gotAllAcksFromDsts()) return false;

        if(!pend.gotAllAcksFromNotifLists()) {
            print("Didnt get all acks for msg", m, "from notifs", pend.getNotifLists());
            return false;
        }

        return noOpenDepenciesFound(pend.getLM());
    }

    private boolean noOpenDepenciesFound(LightMessage m) {
        for(short a : ancestors){
            for(LightMessage lm : history.getAncHstToMe(a)){
                
                if(lm.equals(m)) break; // verifico somente mensagens que vem antes de m nos hsts de ancestrais

                // se lm eh para mim e ainda nao entreguei lm
                if(history.getDeliveredMsgs().get(lm.getId()) == null){
                    // se lm precede m no grafo global, retornara falso
                    if(history.messageM1preceedesM2(lm, m)) {
                        print("Cant deliver", m, "needs to wait for", lm);
                        
                        if(map.get(m.getId()) == null) map.put(m.getId(), new HashMap<>());
                        map.get(m.getId()).put(lm.getId(), true);

                        if(map.get(lm.getId()) != null){
                            if(map.get(lm.getId()).get(m.getId()) != null){
                                printF("Deadlock: Cant deliver m", m, "needs to wait for lm ", lm, "but lm depends on m");
                                printF(queues);
                                printF(history.getGraphString());
                                new FileManager().stop();
                                exit();
                            }
                        }

                        return false;
                    }
                }
            }
        }

        return true;
    }

    private void deliver(Message m) {
        history.addToMyHst(m);
        history.getDeliveredMsgs().put(m.getId(), true);
        if(m.getLca() == getId()){
            forward(m);
        }
        else {
            queues.get(m.getLca()).remove(0);
            history.getPenMsgs().remove(m.getId());
            processPendingNotifs(m);
            sendAcks(m);
        }
        sendReply(m);
        print("Delivered", m);
    }

    private void forward(Message m){
        //send possible notifs
        ArrayList<Pair<Short, Integer>> notifs = sendNotifs(m);
        for(short dest : m.getDst()){
            if(dest > getId()){
                Message toSend = new Message(m.getId());
                toSend.setType(Type.MSG);
                toSend.setDst(m.getDst());
                toSend.setCliId(m.getCliId());
                toSend.setSender(getId());
                //add notif list
                if(notifs != null && notifs.size() > 0) {
                    toSend.setNotifList(notifs);
                }
                addHst(toSend, dest);
                send(toSend, dest);
                print("Sent msg", toSend, "to", dest);
            }
        }
    }

    private ArrayList<Pair<Short, Integer>> sendNotifs(Message m) {
        
        if(m.getDst().length == 1) return null;

        ArrayList<Pair<Short, Integer>> dstsPair = new ArrayList<>();
        ArrayList<Short> dsts = new ArrayList<>();

        // notifico todos abaixo que nao sao dsts para qem eu tenha enviado msg:
        for(short f = (short)(getId()+1); f < m.getDst()[m.getDst().length-1] && !m.isAddressedTo(f); f++){
            if(!dsts.contains(f) && isThereMsgTo(f)) {
                dsts.add(f);
            }
        }

        for(short d : dsts){
            Message notif = new Message(m.getId());
            notif.setSender(getId());
            notif.setDst(m.getDst());
            notif.setType(Type.NOTIF);
            notif.setIdNotif(idNotif);
            addHst(notif, d);
            send(notif, d);
            dstsPair.add(new Pair<Short,Integer>(d, idNotif));
            print("Sent notif", notif, "to", d, "with id", idNotif);
            idNotif++;
        }
        return dstsPair;
    }

    private boolean isThereMsgTo(short son) {
        // olha hst de cada ancestral
        for(short anc : ancestors){
            for(LightMessage lm : history.getAncHst(anc)){
                if(lm.isAddressedTo(son)){
                    return true;
                }
            }
        }
        // olha meu hst
        for(LightMessage lm : history.getMyHst()){
            if(lm.isAddressedTo(son)){
                return true;
            }
        }
        return false;
    }

    private void addHst(Message toSend, short dest) {

        for(short anc : ancestors){
            // pega ponteiro para o meu hst de ancestral do destinatario em questao
            Item item = ancHstPointersPerDesc.get(anc).get(dest);
            if(item == null) item = history.getAncHst(anc).getFirst();
            ArrayList<LightMessage> hst = new ArrayList<>();
            while(item != null){
                hst.add(item.get());
                item = item.getNext();
            }
            if(hst.size() > 0) toSend.addHst(anc, hst);
            if(toSend.getType() == Type.MSG) ancHstPointersPerDesc.get(anc).put(dest, history.getAncHst(anc).getLast());
        }

        // pega ponteiro para o meu hst do destinatario em questao
        Item item = hstPointersPerDesc.get(dest);
        if(item == null) item = history.getMyHst().getFirst();
        ArrayList<LightMessage> myhst = new ArrayList<>();
        while(item != null){
            myhst.add(item.get());
            item = item.getNext();
        }
        if(myhst.size() > 0) toSend.addHst(getId(), myhst);
        if(toSend.getType() == Type.MSG) hstPointersPerDesc.put(dest, history.getMyHst().getLast());
    }

    private void sendAcks(Message m) {
        if(getId() == (numNodes-1)) return; // last one doesnt have someone to send acks
        
        ArrayList<Pair<Short, Integer>> notifs = null;
        // last 2 nodes never have someone to notify
        if(getId() < (numNodes-2)) notifs = sendNotifs(m);

        for(short dst : m.getDst()){
            if(dst > getId()){
                Message ack = new Message(m.getId());
                ack.setType(Type.ACK);
                ack.setDst(m.getDst());
                ack.setSender(getId());

                if(m.getType() == Type.NOTIF) {
                    ack.setIdNotifier(m.getSender());
                    ack.setIdNotif(m.getIdNotif());
                }
                addHst(ack, dst);
                if(notifs != null && notifs.size() > 0) ack.setNotifList(notifs);
                send(ack, dst);
                print("Sent ack", ack, "to", dst);
            }
        }
    }

    @Override
    protected void gc(int mid) {
        gsizes.add(history.getGraphSize());
        gcs++;
        // faco o corte no hst dos ancestrais
        for(short anc : ancestors){
            history.getAncHst(anc).setAsFirst(mid);
            history.getAncHstToMe(anc).setAsFirst(mid);
        }
        // reinicializo os ponteiros
        for(short anc : ancestors) {
            ancHstPointersPerDesc.put(anc, new HashMap<>());
            for(short des : descendants) {
                ancHstPointersPerDesc.get(anc).put(des, null);
            }
        }

        // corto o meu historico
        history.getMyHst().setAsFirst(mid);
        // reinicializo os ponteiros
        hstPointersPerDesc = new HashMap<>();

        history.rebuildGraph();
    }


    @Override
    protected void finish(){
        for(short anc : queues.keySet()){
            if(queues.get(anc).size() > 0){
                printF("Queue of ancestor", anc, "is not empty!!!");
                printF(queues.get(anc));
                files.stop();
                exit();
            }
        }
        printF("Queues are empty ! =]");
        files.persistMessages(history.getMyFullHst(), getId(), false, false);
        printF("-------------------------------------");
        printF("pendingMessages size:", history.getPenMsgs().size());
        printF("deliveredMsgs size:", history.getDeliveredMsgs().size());
        printF("full history size:", history.getMyFullHst().size());
        printF("msgs:", msgs);
        printF("acks:", acks);
        printF("notifs:", notifs);
        printF("gcs:", gcs);
        if(gsizes != null && gsizes.size() > 0) printF("Avg Graph size:", Stats.of(gsizes).mean());
        printF("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }
}
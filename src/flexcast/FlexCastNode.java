package flexcast;

import proxies.ServerProxy;
import util.ArgsParser;
import util.FileManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import base.Node;
import flexcast.messages.LightMessage;
import flexcast.messages.LightMessagesList;
import flexcast.messages.Message;
import flexcast.messages.PendingMessage;
import flexcast.messages.LightMessagesList.Item;
import flexcast.messages.Message.Type;

public class FlexCastNode extends ServerProxy {
    private short numNodes;
    private FileManager files;
    private HashMap<Short, ArrayList<Message>> queues = new HashMap<>();
    private LightMessagesList history = new LightMessagesList();
    private HashMap<Short, HashSet<ArrayList<LightMessage>>> hstSetsPerDesc = new HashMap<>();
    // private HashMap<Short, HashMap<Integer, Boolean>> ancHstMsgsDelivered = new HashMap<>();
    private HashMap<Integer, PendingMessage> pendingMessages;
    private HashMap<Integer, Boolean> deliveredMsgs = new HashMap<>();
    private HashMap<Integer, HashSet<PendingMessage>> msgsToPendMsgsMap = new HashMap<>();
    private HashMap<Short, Item> hstPointersPerDesc = new HashMap<>();
    // private boolean[][][] notifFlags;

    public FlexCastNode(short id, ArgsParser p){
        super(id, p.getClientCount());
        this.files = new FileManager();
        this.pendingMessages = new HashMap<>();

        for(Node n : files.loadHosts()){
            // data for each ancestor
            if(n.getId() < id) {
                queues.put(n.getId(), new ArrayList<>());
                // ancHstMsgsDelivered.put(n.getId(), new HashMap<>());
            }

            // set data for myself
            if(n.getId() == id) setHost(n.getHost());

            // sets connection to each descendant
            if(n.getId() > id) {
                connectTo(n);
                hstSetsPerDesc.put(n.getId(), new HashSet<>());
            }
            numNodes++;
        }
        // notifFlags = new boolean[numNodes][numNodes][numNodes];
        print(this, "FlexCast - Start listening...");
    }

    @Override
    protected void receiveMsg(Message m){
        print("Received msg", m, "; hasPendMsgs", hasPendMsg(), "; queues", queues);

        // lca entrega e faz fwd
        if(m.getLca() == getId()){
            deliver(m);
            return;
        }

        // agrega o cjt de hst recebido em um conjunto de hsts por descendente
        for(HashSet<ArrayList<LightMessage>> set : hstSetsPerDesc.values()){
            set.addAll(m.getHst());
        }

        // outros nodes enfileiram
        queues.get(m.getLca()).add(m);
        
        PendingMessage pend = pendingMessages.get(m.getId());
        if(pend == null){
            pend = new PendingMessage(m.getId());
            pendingMessages.put(m.getId(), pend);
        }
        pend.setMsg(m);

        // cria possiveis pendencias de ack para os dests ancestrais
        for(short d : m.getDst()){
            if(d > m.getLca() && d < getId()) pend.incAcksFromDstsNeeded();
        }

        // update: somente cria as deps depois de receber todos acks
        // cria possiveis pendencias de msgs, oriundas do hst dessa msg, na msg pendente correspondente
        //addMessageDeps(m, pend);

        // cria pendencias de ack para os notificados da notif list
        pend.addNotifList(m.getSender(), m.getNotifList());

        // reprocessa filas
        reprocessQueues(m.getLca());
    }

    private void addMessageDeps(Message m, PendingMessage pend) {
        for(ArrayList<LightMessage> hst : pend.getHsts()){
            for(LightMessage lm : hst){
                //if(lm.getId() == m.getId()) break; // only msgs that precede m itself
                // if(!pend.msgLmPrecedesM(lm, m)) break;
                if(lm.isAddressedTo(getId()) && deliveredMsgs.get(lm.getId()) == null && m.getLca() != lm.getLca() && pend.msgLmPrecedesM(lm, m)){
                    pend.getMsgDeps().add(lm.getId());

                    // create a pointer in the msgsToPendMsgsMap set
                    HashSet<PendingMessage> set = msgsToPendMsgsMap.get(lm.getId());
                    if(set == null){
                        set = new HashSet<>();
                        msgsToPendMsgsMap.put(lm.getId(), set);
                    }
                    set.add(pend);
                }
            }
        }
        pend.setMsgsDepsFlag(true);
    }

    @Override
    protected void receiveAck(Message ack){
        print("Received ack", ack, "from", ack.getSender());
        
        // agrega o cjt de hst recebido em um conjunto de hsts por descendente
        for(HashSet<ArrayList<LightMessage>> set : hstSetsPerDesc.values()){
            set.addAll(ack.getHst());
        }

        PendingMessage pend = pendingMessages.get(ack.getId());
        if(pend == null){
            pend = new PendingMessage(ack.getId());
            pendingMessages.put(ack.getId(), pend);
        }

        // update: somente cria as deps depois de receber todos acks
        // cria possiveis pendencias de msgs oriundas do hst desse ack na msg pendente correspondente
        // addMessageDeps(ack, pend);
        pend.getHsts().addAll(ack.getHst());

        // cria pendencias de ack para os notificados da notif list desse ack
        pend.addNotifList(ack.getSender(), ack.getNotifList());

        // place the ack in the senders queue:
        queues.get(ack.getSender()).add(ack);

        // update: vai resolver a pendencia quando "entregar" o ack
        // resolve pendencia relativa a este ack em questao
        // pend.decAcksFromDstsNeeded();


        // all msgs pending in the sender's queue are deps of this msg for me
        //if(ack.isAddressedTo(ack.getSender())){
            // for(Message m : queues.get(ack.getSender())){
            //     if(m.getId() != ack.getId()) {
            //         pend.getMsgDeps().add(m.getId());
            //         // create a pointer in the msgsToPendMsgsMap set
            //         HashSet<PendingMessage> set = msgsToPendMsgsMap.get(m.getId());
            //         if(set == null){
            //             set = new HashSet<>();
            //             msgsToPendMsgsMap.put(m.getId(), set);
            //         }
            //         set.add(pend);
            //     }
            // }
        //}

        // reprocessa filas
        reprocessQueues(ack.getSender());
    }

    @Override
    protected void receiveNotif(Message notif){
        print("Received notif", notif, "from", notif.getSender());
        
        // agrega o cjt de hst recebido em um conjunto de hsts por descendente
        for(HashSet<ArrayList<LightMessage>> set : hstSetsPerDesc.values()){
            set.addAll(notif.getHst());
        }

        // enviar o ack
        sendAcks(notif);
        // vai precisar ficar pendente mesmo ?
    }

    private void reprocessQueues(short lca) {
        boolean [] retry = new boolean[1];

        Message m = null;
        try{ m = queues.get(lca).get(0); } catch(IndexOutOfBoundsException ignore){}

        if(m != null && m.getType() == Type.ACK){
            deliverAck(m);
            retry[0] = true;
        }
        else if(m != null && canDeliver(m, retry)){
            deliver(m);
            retry[0] = true;
        }
        
        while(retry[0]){
            retry[0] = false;
            for(ArrayList<Message> queue : queues.values()){
                m = null;
                try{ m = queue.get(0); } catch(IndexOutOfBoundsException ignore){}

                if(m != null && m.getType() == Type.ACK){
                    deliverAck(m);
                    retry[0] = true;
                }
                else if(m != null && canDeliver(m, retry)){
                    deliver(m);
                    retry[0] = true;
                }
            }
        }
    }

    private void deliverAck(Message ack){
        PendingMessage pend = pendingMessages.get(ack.getId());
        if(ack.isAddressedTo(ack.getSender())){
            pend.decAcksFromDstsNeeded();
        } else {
            pend.addAckFromNotifList(ack.getIdNotifier(), ack.getSender());
        }
        queues.get(ack.getSender()).remove(0);
    }

    private boolean canDeliver(Message m, boolean [] retry) {

        // check all types of dependencies
        PendingMessage pend = pendingMessages.get(m.getId());
        if(pend == null) return false;

        if(pend.getMsg() == null) return false;

        if(!pend.gotAllAcksFromDsts()) return false;

        if(!pend.gotAllAcksFromNotifLists()) return false;

        if(!pend.getMsgsDepsFlag()) addMessageDeps(m, pend);

        if(pend.getMsgDeps().size() > 0) {


            // exception:
            // para cada dep
            for(int dep : pend.getMsgDeps()){
                PendingMessage mDep = pendingMessages.get(dep);
                if(mDep == null) continue;
                ArrayList<Message> mDepAcks = new ArrayList<>();
                // se a msg que eh dependencia de m, esta na cabeca fila do seu lca
                if(mDep.getMsg() != null && queues.get(mDep.getMsg().getLca()).get(0).getId() == mDep.getMsg().getId()){
                    // vejo se nao tem acks trancados atras de mim na minha fila
                    for(Message ack : queues.get(m.getLca())){
                        if(ack.getType() == Type.ACK && ack.getId() == mDep.getId()){
                            mDepAcks.add(ack);
                        }
                    }
                }
                // passa os acks qe estavam trancados na fila para frente e reprocessa
                if(mDepAcks.size() > 0){
                    queues.get(m.getLca()).removeAll(mDepAcks);
                    for(Message ack : mDepAcks){
                        queues.get(m.getLca()).add(0, ack);
                        print("Passei o ack", ack, "( sender", ack.getSender() ,")", "pra frente na fila do anc", m.getLca());
                    }
                    retry[0]=true;
                }
            }

            print("Cant deliver", m, "MsgDeps:", pend.getMsgDeps(), "Queues", queues);
            return false;
        }

        return true;
    }

    private void deliver(Message m){
        //adds to local history
        history.add(new LightMessage(m.getId(), m.getDst()));
        deliveredMsgs.put(m.getId(), true);
        
        if(m.getLca() == getId()){
            // lca forwards it
            forward(m);
        }
        else {
            // send acks (and possible notifs)
            sendAcks(m);
            // remove from queue and auxiliary structures
            queues.get(m.getLca()).remove(0);
            pendingMessages.remove(m.getId());

            HashSet<PendingMessage> set = msgsToPendMsgsMap.get(m.getId());
            if(set != null){
                // TODO: remove possible dependencies related to this message (either from other messages or for notifs)
                for(PendingMessage pm : set){
                    pm.getMsgDeps().remove(m.getId());
                }
                msgsToPendMsgsMap.remove(m.getId());
            }
        }

        // updateNotifFlags(m);

        sendReply(m);
        print("Delivered", m);
    }

    // private void updateNotifFlags(Message m) {
    //     // para cada filho f nos dests
    //     for(short f : m.getDst()){
    //         if(f > getId()){
    //             // para cada outro filho f' abaixo de f, que nao eh dest
    //             for(short fp = (short)(f+1); fp < numNodes; fp++){
    //                 // set notifflags indicating that should send notif to f in case there is a msg to f' in the future
    //                 if(!m.isAddressedTo(fp)) {
    //                     for(short i = 0; i < numNodes; i++)
    //                         notifFlags[i][f][fp] = true;
    //                 }
    //             }
    //         }
    //     }
    // }

    private void forward(Message m){
        //send possible notifs
        ArrayList<Short> notifs = sendNotifs(m);

        for(short dest : m.getDst()){
            if(dest > getId()){
                Message toSend = new Message(m.getId());
                toSend.setType(Type.MSG);
                toSend.setDst(m.getDst());
                toSend.setCliId(m.getCliId());
                toSend.setSender(getId());

                //add notif list
                if(notifs != null && notifs.size() > 0) toSend.setNotifList(notifs);

                addHst(toSend, dest, true);
                send(toSend, dest);
            }
        }
    }

    private ArrayList<Short> sendNotifs(Message m) {
        
        if(m.getDst().length == 1) return null;

        ArrayList<Short> dsts = new ArrayList<>();

        // pra cada filho, nao dest de m
        // verifico se devo notificar
        // for(short f = (short)(getId()+1); f < numNodes && !m.isAddressedTo(f); f++){
        //     for(short dst : m.getDst()){
        //         // condicao de verificar se enviei msg para f esta no fato de que seto a flag para true quando entrego msgs de f
        //         if(dst > getId() && notifFlags[m.getLca()][f][dst]){
        //             if(!dsts.contains(f)) {
        //                 dsts.add(f);
        //                 notifFlags[m.getLca()][f][dst] = false;
        //             }
        //         }
        //         if(dst > getId() && !notifFlags[m.getLca()][f][dst]){
        //             print("Decided not send notif", m, f, dst, "because flags is false");
        //         }
        //     }
        // }

        // teste:
        // notifico todos abaixo que nao sao dsts:
        for(short f = (short)(getId()+1); f < m.getDst()[m.getDst().length-1] && !m.isAddressedTo(f); f++){
            if(!dsts.contains(f)) {
                dsts.add(f);
            }
        }

        for(short d : dsts){
            Message notif = new Message(m.getId());
            notif.setSender(getId());
            notif.setDst(m.getDst());
            notif.setType(Type.NOTIF);
            //TODO: should it have a "notiflist"?
            // for (NotifList nl : m.getNotifList())
            //     notif.addNotifList(nl.getNotifList(), nl.getNotifier());
            // notif.addNotifList(notifList, getId());
            addHst(notif, d, false);
            send(notif, d);
            print("Sent notif", notif, "to", d);
        }
        return dsts;
    }

    private void addHst(Message toSend, short dest, boolean clear) {
        toSend.getHst().addAll(hstSetsPerDesc.get(dest));
        if(clear) hstSetsPerDesc.get(dest).clear();
        Item item = hstPointersPerDesc.get(dest);
        
        if(item == null) item = history.getFirst();
        else item = item.getNext();

        if(item == null) return;
        if(item.get().getId() == toSend.getId()) return;

        ArrayList<LightMessage> myhst = new ArrayList<>();
        while(item != null){
            myhst.add(item.get());
            item = item.getNext();
        }
        if(clear) hstPointersPerDesc.put(dest, history.getLast());
        if(myhst.size() > 0) toSend.getHst().add(myhst);
    }

    private void sendAcks(Message m) {
        if(getId() == (numNodes-1)) return; // last one doesnt have someone to send acks
        
        // Set<Short> notifList = null;
        // if(getId() < (getNumNodes()-2)) notifList = sendNotif(m); // last 2 nodes never have someone to notify
        ArrayList<Short> notifs = sendNotifs(m);

        for(short dst : m.getDst()){
            if(dst > getId()){
                Message ack = new Message(m.getId());
                ack.setType(Type.ACK);
                ack.setDst(m.getDst());
                ack.setSender(getId());

                if(m.getType() == Type.NOTIF) ack.setIdNotifier(m.getSender());
                // ack.ackIsFromDst(m.getType() == Type.MSG && m.isAddressedTo(getId()));

                addHst(ack, dst, false);

                // if(notifList != null && notifList.size() > 0)
                //     ack.addNotifList(notifList, getId());
                if(notifs != null && notifs.size() > 0) ack.setNotifList(notifs);

                send(ack, dst);
                print("Sent ack", ack, "to", dst);
            }
        }
    }

    protected boolean hasPendMsg(){
        if(pendingMessages == null) return false;
        return !pendingMessages.isEmpty();
    }

    protected void finish(){
        for(short anc : queues.keySet()){
            if(queues.get(anc).size() > 0){
                print("Queue of ancestor", anc, "is not empty!!!");
                print(queues.get(anc));
                files.stop();
                exit();
            }
        }
        print("Queues are empty ! =]");
        files.persistMessages(history, getId(), false, false);
        print("-------------------------------------");
        print("Total msgs in the history:", history.size());
        print("Total local msgs received:", localMsgs);
        print("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }
    
}
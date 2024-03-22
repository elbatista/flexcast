package flexcast.server;

import proxies.ServerProxy;
import util.ArgsParser;
import util.FileManager;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.LinkedList;
import org.javatuples.Pair;
import base.Host;
import base.Node;
import flexcast.messages.LightMessage;
import flexcast.messages.Message;
import flexcast.messages.LightMessagesList.Item;
import flexcast.messages.Message.Type;
import flexcast.reconfig.View;

// @SuppressWarnings("unused")
public class FlexCastNode extends ServerProxy {
    private FileManager files;    
    private View nextView;
    // TODO: REMOVE
    private int msgs=0, acks=0, notifs=0, gcs=0;

    public FlexCastNode(short id, ArgsParser p){
        super(id, p.getClientCount());
        this.files = new FileManager();
        if(!p.getLog()) setPrint(false);
        currentView = new View(0, getId(), files.loadHosts());
        setHost(currentView.getHost());
        connectToServers();
        printF(this, "FlexCast - Start listening...");
        printF("Current view:", currentView);
    }

    @Override
    protected void receiveMsg(Message m){
        print("Received msg", m, "queues", getQueues());
        msgs++;
        getHistory().addHst(m);
        if(getId() == m.getLca()){
            deliver(m);
        }
        else {
            getQueues().get(m.getLca()).add(m);
        
            PendingMessage pend = getHistory().getPendMsg(m.getId());
            if(pend == null){
                pend = new PendingMessage(m.getId());
                getHistory().addPendMsg(m.getId(), pend);
            }
            pend.setMsg(m);
            // for(short d : m.getDst()) {
            //     if(d > m.getLca() && d < getId()) {
            //         pend.incAcksFromDstsNeeded();
            //     }
            // }
            // para pegar os dests antes do lca:
            // dsts estao ordenados pela sua posicao no CDAG
            // partindo do segundo (pula o lca), retorno os dests antes de mim (node) no array de dests da msg
            // for(int i=1; m.getDst()[i] != getId() && i < m.getDst().length; i++){
            //     pend.incAcksFromDstsNeeded();
            // }
           
            for(short d : getAncestors()){
                if(d != m.getLca() && m.isAddressedTo(d)) pend.incAcksFromDstsNeeded();
            }
            
            // cria pendencias de ack para os notificados da notif list
            pend.addNotifList(m.getSender(), m.getNotifList(), currentView);

            reprocessQueues();
        }
    }

    @Override
    protected void receiveAck(Message ack){
        print("Received ack", ack, "from", ack.getSender(), "queues", getQueues());
        acks++;
        getHistory().addHst(ack);
        PendingMessage pend = getHistory().getPendMsg(ack.getId());
        if(pend == null){
            pend = new PendingMessage(ack.getId());
            getHistory().addPendMsg(ack.getId(), pend);
        }

        // cria pendencias de ack para os notificados da notif list desse ack
        pend.addNotifList(ack.getSender(), ack.getNotifList(), currentView);

        // "entrega" o ack
        // se for um ack de dest:
        if(ack.isAddressedTo(ack.getSender())){
            pend.decAcksFromDstsNeeded();
        }
        // ack de notificado: 
        else {
            pend.addAckFromNotifList(ack.getIdNotifier(), ack.getSender(), ack.getIdNotif());
        }

        reprocessQueues();
    }

    @Override
    protected void receiveNotif(Message notif){
        print("Received notif", notif, "from", notif.getSender(), "queues", getQueues());
        notifs++;
        getHistory().addHst(notif);

        if(getPendingNotifs().size() > 0){
            print("Adding (direct) to pend notifs");
            getPendingNotifs().add(notif);
            return;
        }
        if(!canDeliverNotif(notif)){
            print("Adding (direct) to pend notifs");
            getPendingNotifs().add(notif);
            return;
        }

        sendAcks(notif);
    }

    private List<Short> getAncestors(){
        return currentView.getAncestors();
    }

    private List<Short> getDescendants(){
        return currentView.getDescendants();
    }

    private HashMap<Short, ArrayList<Message>> getQueues(){
        return currentView.getQueues();
    }

    private LinkedList<Message> getPendingNotifs(){
        return currentView.getPendingNotifs();
    }

    private History getHistory(){
        return currentView.getHistory();
    }

    private HashMap<Short, HashMap<Short, Item>> getAncHstPointersPerDesc(){
        return currentView.getAncHstPointersPerDesc();
    }

    private HashMap<Short, Item> getHstPointersPerDesc(){
        return currentView.getHstPointersPerDesc();
    }

    private boolean canDeliverNotif(Message notif) {
        boolean pend = false;
        for(short a : getAncestors()){
            for(LightMessage lm : getHistory().getAncHstToMe(a)){
                if(getHistory().getDeliveredMsgs().get(lm.getId()) == null){
                    pend = true;
                    notif.getPendNotifOrigins().add(lm.getId());
                }
            }
        }
        return !pend;
    }

    private void processPendingNotifs(Message delivered_m) {
        if(getPendingNotifs().size() == 0) return;
        Message notif = getPendingNotifs().peek();
        notif.getPendNotifOrigins().remove(Integer.valueOf(delivered_m.getId()));
        if(notif.getPendNotifOrigins().size() > 0) return;
        for(;;){
            sendAcks(notif);
            getPendingNotifs().removeFirst();
            notif = getPendingNotifs().peek();
            if(notif == null) return;
            if(!canDeliverNotif(notif)) return;
        }
    }

    private void reprocessQueues() {
        boolean retry = true;
        while(retry){
            retry = false;
            for(ArrayList<Message> queue : getQueues().values()){
                Message m = null;
                try{ m = queue.get(0); } catch(IndexOutOfBoundsException ignore){}

                if(m != null && canDeliver(m)){
                    deliver(m);
                    retry = true;
                }
            }
        }
    }

    private boolean canDeliver(Message m) {
        // check all types of dependencies
        PendingMessage pend = getHistory().getPendMsg(m.getId());
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
        for(short a : getAncestors()){
            for(LightMessage lm : getHistory().getAncHstToMe(a)){
                
                if(lm.equals(m)) break; // verifico somente mensagens que vem antes de m nos hsts de ancestrais

                // se lm eh para mim e ainda nao entreguei lm
                if(getHistory().getDeliveredMsgs().get(lm.getId()) == null){
                    // se lm precede m no grafo global, retornara falso
                    if(getHistory().messageM1preceedesM2(lm, m)) {
                        print("Cant deliver", m, "needs to wait for", lm);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void deliver(Message m) {
        getHistory().addToMyHst(m);
        getHistory().getDeliveredMsgs().put(m.getId(), true);
        if(m.getLca() == getId()){
            forward(m);
            if(m.getType() == Type.MSG && m.getDst().length < getNumNodes()) currentView.addDstsFreq(m.getDst());
        }
        else {
            getQueues().get(m.getLca()).remove(0);
            getHistory().getPenMsgs().remove(m.getId());
            processPendingNotifs(m);
            sendAcks(m);
        }
        sendReply(m);
        print("Delivered", m);

        if(m.getType() == Type.VIEWCHANGE){
            changeView(m);
        }
    }

    protected void changeView(Message m) {
        if (nextView == null){
            nextView = new View(currentView.getId()+1);
        }
        // get the nodes from the current view
        // change their position acording to new overlay
        int pos = 0;
        ArrayList<Node> newnodes = new ArrayList<>();
        for(short s : m.getNewOverlay()){
            Host h = currentView.getHostFromNode(s);
            newnodes.add(new Node(s,h,pos));
            pos++;
        }
        printF("Created new nodes array:", newnodes);
        // store the new nodes in the new view, creating the structures in the new view following the new overlay
        nextView.prepareConnections(getId(), newnodes);
        // transfer fisical connections from prev view
        nextView.setConnections(currentView.getConnections());
        // set current view as next, and next to null
        currentView = nextView;
        nextView = null;


        // TODO: ?????
        // process any buffered message in the new view
        printF("Changed to a new view:", currentView);
        
    }

    private void forward(Message m){
        //send possible notifs
        ArrayList<Pair<Short, Integer>> notifs = sendNotifs(m);
        for(short dest : m.getDst()){
            if(dest != getId()){
                Message toSend = new Message(m.getId(), m.getViewId());
                toSend.setType(m.getType());
                toSend.setDst(m.getDst());
                toSend.setCliId(m.getCliId());
                toSend.setSender(getId());

                toSend.setTransaction(m.getTransaction());
                toSend.setOrderDate(m.getOrderDate());
                toSend.setItems(m.getItems());
                toSend.setPaymentAmount(m.getPaymentAmount());
                toSend.setCarrierid_or_threshold(m.getCarrierid_or_threshold());

                if(m.getType() == Type.VIEWCHANGE){
                    toSend.setNewOverlay(m.getNewOverlay());
                }

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
        // for(short f = (short)(getId()+1); f < m.getDst()[m.getDst().length-1] && !m.isAddressedTo(f); f++){
        for(short f : currentView.getInterNodes(m)){
            if(!dsts.contains(f) && isThereMsgTo(f)) {
                dsts.add(f);
            }
        }

        for(short d : dsts){
            Message notif = new Message(m.getId(), m.getViewId());
            notif.setSender(getId());
            notif.setDst(m.getDst());
            notif.setType(Type.NOTIF);
            notif.setIdNotif(currentView.getIdNotif());
            addHst(notif, d);
            send(notif, d);
            dstsPair.add(new Pair<Short,Integer>(d, currentView.getIdNotif()));
            print("Sent notif", notif, "to", d, "with id", currentView.getIdNotif());
            // idNotif++;
            currentView.incIdNotif();
        }
        return dstsPair;
    }

    private boolean isThereMsgTo(short son) {
        // olha hst de cada ancestral
        for(short anc : getAncestors()){
            for(LightMessage lm : getHistory().getAncHst(anc)){
                if(lm.isAddressedTo(son)){
                    return true;
                }
            }
        }
        // olha meu hst
        for(LightMessage lm : getHistory().getMyHst()){
            if(lm.isAddressedTo(son)){
                return true;
            }
        }
        return false;
    }

    private void addHst(Message toSend, short dest) {

        for(short anc : getAncestors()){
            // pega ponteiro para o meu hst de ancestral do destinatario em questao
            Item item = getAncHstPointersPerDesc().get(anc).get(dest);
            if(item == null) item = getHistory().getAncHst(anc).getFirst();
            ArrayList<LightMessage> hst = new ArrayList<>();
            while(item != null){
                hst.add(item.get());
                item = item.getNext();
            }
            if(hst.size() > 0) toSend.addHst(anc, hst);
            if(toSend.getType() == Type.MSG) getAncHstPointersPerDesc().get(anc).put(dest, getHistory().getAncHst(anc).getLast());
        }

        // pega ponteiro para o meu hst do destinatario em questao
        Item item = getHstPointersPerDesc().get(dest);
        if(item == null) item = getHistory().getMyHst().getFirst();
        ArrayList<LightMessage> myhst = new ArrayList<>();
        while(item != null){
            myhst.add(item.get());
            item = item.getNext();
        }
        if(myhst.size() > 0) toSend.addHst(getId(), myhst);
        if(toSend.getType() == Type.MSG) getHstPointersPerDesc().put(dest, getHistory().getMyHst().getLast());
    }

    private int getNumNodes() {
        return currentView.getNumNodes();
    }

    private void sendAcks(Message m) {
        // if(getId() == (getNumNodes()-1)) return; // last one doesnt have someone to send acks
        
        ArrayList<Pair<Short, Integer>> notifs = null;
        // last 2 nodes never have someone to notify
        // if(getId() < (getNumNodes()-2)) 
        notifs = sendNotifs(m);

        for(short dst : getDescendants()){
            if(m.isAddressedTo(dst)){
                Message ack = new Message(m.getId(), m.getViewId());
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
        // gsizes.add(history.getGraphSize());
        gcs++;
        // faco o corte no hst dos ancestrais
        for(short anc : getAncestors()){
            getHistory().getAncHst(anc).setAsFirst(mid);
            getHistory().getAncHstToMe(anc).setAsFirst(mid);
        }
        // reinicializo os ponteiros
        for(short anc : getAncestors()) {
            getAncHstPointersPerDesc().put(anc, new HashMap<>());
            for(short des : getDescendants()) {
                getAncHstPointersPerDesc().get(anc).put(des, null);
            }
        }

        // corto o meu historico
        getHistory().getMyHst().setAsFirst(mid);
        // reinicializo os ponteiros
        // hstPointersPerDesc = new HashMap<>();
        currentView.resetHstPointersPerDesc();

        getHistory().rebuildGraph();
    }

    @Override
    protected void finish(){
        for(short anc : getQueues().keySet()){
            if(getQueues().get(anc).size() > 0){
                printF("Queue of ancestor", anc, "is not empty!!!");
                printF(getQueues().get(anc));
                files.stop();
                exit();
            }
        }
        printF("Queues are empty ! =]");
        files.persistMessages(getHistory().getMyFullHst(), getId(), false, false);
        printF("-------------------------------------");
        printF("pendingMessages size:", getHistory().getPenMsgs().size());
        printF("deliveredMsgs size:", getHistory().getDeliveredMsgs().size());
        printF("full history size:", getHistory().getMyFullHst().size());
        printF("msgs:", msgs);
        printF("acks:", acks);
        printF("notifs:", notifs);
        printF("gcs:", gcs);
        printF("DstsFreq:", currentView.getDstsFreq());
        // if(gsizes != null && gsizes.size() > 0) printF("Avg Graph size:", Stats.of(gsizes).mean());
        // printF("Avg msg size", Stats.of(getSizes()).mean());
        files.persistMsgSizes(getSizes(), getId());
        printF("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }

    /// View change related methods
    @Override
    protected boolean validateView(Message m){
        if(m.getViewId() > currentView.getId()){
            if((m.getViewId() > currentView.getId()+1)){
                printF("Found a View greater than both current and next views", currentView.getId(), m.getViewId());
                files.stop();
                exit();
            }

            // if the current view is different from the message view, it means there is a new view but
            // I didnt deliver the viewchange message yet, so i cannot switch to 
            // the new view. Messages related to the new view are simply buffered in the new view object
            // for later processing once the viewchange is complete
            if(nextView == null){
                nextView = new View(currentView.getId()+1);
                printF("Created a new view:", nextView.getId());
            }

            nextView.bufferMessage(m);
            printF("Buffered message", m ,"in view", nextView.getId());
            files.stop();
            exit();

            return false;
        }
        else if(m.getViewId() < currentView.getId()){
            printF("Client", m.getCliId(), "is in a old View. Sending new view.", m);
            Message vc = new Message(m.getId(), currentView.getId());
            vc.setType(Type.VIEWCHANGE);
            vc.setNewOverlay(currentView.getOverlay());
            vc.setDst(m.getDst());
            vc.setCliId(m.getCliId());
            sendReplyVC(vc);
            return false;
        }
        return true;
    }

}
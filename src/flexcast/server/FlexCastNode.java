package flexcast.server;

import proxies.ServerProxy;
import util.ArgsParser;
import util.FileManager;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import org.javatuples.Pair;
import org.jgrapht.Graph;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jgrapht.nio.dot.DOTExporter;
import base.Node;
import flexcast.messages.LightMessage;
import flexcast.messages.LightMessagesList;
import flexcast.messages.Message;
import flexcast.messages.LightMessagesList.Item;
import flexcast.messages.Message.Type;

public class FlexCastNode extends ServerProxy {
    private short numNodes;
    private FileManager files;
    private ArrayList<Short> ancestors = new ArrayList<>();
    private ArrayList<Short> descendants = new ArrayList<>();
    private HashMap<Short, ArrayList<Message>> queues = new HashMap<>();
    private HashMap<Integer, PendingMessage> pendingMessages;
    private HashMap<Integer, Boolean> deliveredMsgs = new HashMap<>();
    private HashMap<Integer, HashSet<PendingMessage>> msgsToPendMsgsMap = new HashMap<>();
    
    // historicos e ponteiros
    private LightMessagesList history = new LightMessagesList();
    private LightMessagesList fullHistory = new LightMessagesList();
    private HashMap<Short, LightMessagesList> ancHistory = new HashMap<>();
    private HashMap<Short, Item> hstPointersPerDesc = new HashMap<>();
    private HashMap<Short, HashMap<Short, Item>> ancHstPointersPerDesc = new HashMap<>();
    private HashMap<Short, HashMap<Integer, Boolean>> ancHstDeliveredMsgs = new HashMap<>();

    //notifid
    private int idNotif = 0;

    private Graph<Integer, DefaultEdge> globalHstGraph;
    private HashSet<LightMessage> allKnownMsgsToMe = new HashSet<>();
    private BellmanFordShortestPath<Integer, DefaultEdge> bellmanFordShortestPath=null;

    public FlexCastNode(short id, ArgsParser p){
        super(id, p.getClientCount());
        this.files = new FileManager();
        globalHstGraph = GraphTypeBuilder.<Integer, DefaultEdge> directed()
        .allowingMultipleEdges(false)
        .allowingSelfLoops(false)
        .weighted(false)
        .edgeClass(DefaultEdge.class)
        .buildGraph();
        this.pendingMessages = new HashMap<>();
        if(!p.getLog()) setPrint(false);
        for(Node n : files.loadHosts()){
            // data for each ancestor
            if(n.getId() < id) {
                ancestors.add(n.getId());
                queues.put(n.getId(), new ArrayList<>());
                ancHistory.put(n.getId(), new LightMessagesList());
                ancHstPointersPerDesc.put(n.getId(), new HashMap<>());
                ancHstDeliveredMsgs.put(n.getId(), new HashMap<>());
            }

            // set data for myself
            if(n.getId() == id) setHost(n.getHost());

            // sets connection to each descendant
            if(n.getId() > id) {
                descendants.add(n.getId());
                connectTo(n);
                for(short anc : ancHstPointersPerDesc.keySet()){
                    ancHstPointersPerDesc.get(anc).put(n.getId(), null);
                }
            }
            numNodes++;
        }
        printF(this, "FlexCast - Start listening...");
    }

    @Override
    protected void receiveMsg(Message m){
        print("Received msg", m, "queues", queues);

        // lca entrega e faz fwd
        if(m.getLca() == getId()){
            deliver(m);
            return;
        }

        // agrega o cjt de hst recebido em um conjunto de hsts por descendente
        // for(HashSet<ArrayList<LightMessage>> set : hstSetsPerDesc.values()){
        //     set.addAll(m.getHst());
        // }
        updateLocalAncHst(m);

        // outros nodes enfileiram
        queues.get(m.getLca()).add(m);
        
        PendingMessage pend = pendingMessages.get(m.getId());
        if(pend == null){
            pend = new PendingMessage(m.getId());
            pendingMessages.put(m.getId(), pend);
        }

        pend.setMsg(m);

        // update 20/may: vai inserir o hst no grafo global, dentro da funcao updateLocalAncHst
        // pend.addHst(m.getHst());

        // cria possiveis pendencias de ack para os dests ancestrais
        for(short d : m.getDst()) if(d > m.getLca() && d < getId()) pend.incAcksFromDstsNeeded();

        // update: somente cria as deps de msg depois de receber todos acks
        // cria possiveis pendencias de msgs, oriundas do hst dessa msg, na msg pendente correspondente
        //addMessageDeps(m, pend);

        // cria pendencias de ack para os notificados da notif list
        pend.addNotifList(m.getSender(), m.getNotifList());

        // reprocessa filas
        reprocessQueues(m.getLca());
    }

    private void updateLocalAncHst(Message m) {
        for(short anc : m.getHst().keySet()){
            Item item = m.getHst().get(anc).getFirst();
            while(item != null){

                // atualiza hst de ancestrais
                if(ancHstDeliveredMsgs.get(anc).get(item.get().getId()) == null) {
                    ancHistory.get(anc).add(item.get());
                    ancHstDeliveredMsgs.get(anc).put(item.get().getId(), true);
                }

                if(item.get().isAddressedTo(getId())) allKnownMsgsToMe.add(item.get());

                //if(!globalHstGraph.containsVertex(item.get().getId())) {
                    globalHstGraph.addVertex(item.get().getId());
                //}//
                if(item.getPrev() != null){
                    //if(!globalHstGraph.containsEdge(item.getPrev().get().getId(), item.get().getId())) {
                        globalHstGraph.addEdge(item.getPrev().get().getId(), item.get().getId());
                    //}
                }

                item = item.getNext();
            }
        }

        // for(LightMessagesList list : m.getHst().values()){
        //     Item item = list.getFirst();
        //     while(item != null){
        //         allKnownMsgs.add(item.get());
        //         if(!globalHstGraph.containsVertex(item.get().getId())) {
        //             globalHstGraph.addVertex(item.get().getId());
        //         }
        //         if(item.getPrev() != null){
        //             if(!globalHstGraph.containsEdge(item.getPrev().get().getId(), item.get().getId())) {
        //                 globalHstGraph.addEdge(item.getPrev().get().getId(), item.get().getId());
        //             }
        //         }
        //         item = item.getNext();
        //     }
        // }

    }

    @Override
    protected void receiveAck(Message ack){
        print("Received ack", ack, "from", ack.getSender(), "queues", queues);
        
        // agrega o cjt de hst recebido em um conjunto de hsts por descendente
        // for(HashSet<ArrayList<LightMessage>> set : hstSetsPerDesc.values()){
        //     set.addAll(ack.getHst());
        // }
        updateLocalAncHst(ack);

        PendingMessage pend = pendingMessages.get(ack.getId());
        if(pend == null){
            pend = new PendingMessage(ack.getId());
            pendingMessages.put(ack.getId(), pend);
        }

        // update: somente cria as deps depois de receber todos acks
        // cria possiveis pendencias de msgs oriundas do hst desse ack na msg pendente correspondente
        // addMessageDeps(ack, pend);


        // update 20/may: vai inserir o hst no grafo global, dentro da funcao updateLocalAncHst
        // pend.addHst(ack.getHst());

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
        print("Received notif", notif, "from", notif.getSender(), "queues", queues);
        
        // agrega o cjt de hst recebido em um conjunto de hsts por descendente
        // for(HashSet<ArrayList<LightMessage>> set : hstSetsPerDesc.values()){
        //     set.addAll(notif.getHst());
        // }
        updateLocalAncHst(notif);


        // update 20/may: vai inserir o hst no grafo global, dentro da funcao updateLocalAncHst
        // for(PendingMessage p : pendingMessages.values()){
        //     p.addHst(notif.getHst());
        // }

        // enviar o ack
        // sendAcks(notif);
        
        // vai precisar ficar pendente mesmo ?
        // talvez deve ir para a fila do sender, como os acks?
        queues.get(notif.getSender()).add(notif);

        // reprocessa filas
        reprocessQueues(notif.getSender());

    }

    private void reprocessQueues(short lca) {
        boolean [] retry = new boolean[1];

        Message m = null;
        try{ m = queues.get(lca).get(0); } catch(IndexOutOfBoundsException ignore){}

        if(m != null && m.getType() == Type.ACK){
            deliverAck(m);
            retry[0] = true;
        }
        else if(m != null && m.getType() == Type.NOTIF){
            sendAcks(m);
            // tira notif da fila do sender
            queues.get(m.getSender()).remove(0);
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
                else if(m != null && m.getType() == Type.NOTIF){
                    sendAcks(m);
                    // tira notif da fila do sender
                    queues.get(m.getSender()).remove(0);
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

        if(pend == null) {
            printF("Ack sem PendMsg relacionada:", ack);
            exit();
        }

        if(ack.isAddressedTo(ack.getSender())){
            pend.decAcksFromDstsNeeded();
        } else {
            pend.addAckFromNotifList(ack.getIdNotifier(), ack.getSender(), ack.getIdNotif());
        }

        // tira o ack da fila do sender
        queues.get(ack.getSender()).remove(0);
    }

    private boolean canDeliver(Message m, boolean [] retry) {

        // check all types of dependencies
        PendingMessage pend = pendingMessages.get(m.getId());
        if(pend == null) return false;

        if(pend.getMsg() == null) return false;

        if(!pend.gotAllAcksFromDsts()) return false;

        if(!pend.gotAllAcksFromNotifLists()) {
            print("Didnt get all acks for msg", m, "from notifs", pend.getNotifLists());
            return false;
        }

        // essa flag garante que soh vou calcular as dependencias depois que chegarem todos os acks
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
                        // print("Passei o ack", ack, "( sender", ack.getSender() ,")", "pra frente na fila do anc", m.getLca());
                    }
                    retry[0]=true;
                }
            }
            print("Cant deliver", m, "MsgDeps:", pend.getMsgDeps(), "queues", queues, "Graph", graphToString());
            return false;
        }
        return true;
    }

    private String graphToString() {
        DOTExporter<Integer, DefaultEdge> exporter = new DOTExporter<>();
        Writer writer = new StringWriter();
        exporter.exportGraph(globalHstGraph, writer);
        return writer.toString();
    }

    private void addMessageDeps(Message msg, PendingMessage pend) {

        LightMessage m = new LightMessage(msg.getId(), msg.getDst());

        ArrayList<LightMessage> array = new ArrayList<>();

        for(LightMessage lm : allKnownMsgsToMe){
            if(
                !lm.equals(m) &&
                deliveredMsgs.get(lm.getId()) == null && 
                m.getLca() != lm.getLca()
            ){
                array.add(lm);
            }
        }

        for(LightMessage lm : array){
            if(msgLmPrecedesM(lm, m)){
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

        pend.setMsgsDepsFlag(true); 
    }

    private boolean msgLmPrecedesM(LightMessage lm, LightMessage m) {
        if(bellmanFordShortestPath == null)
            bellmanFordShortestPath  = new BellmanFordShortestPath<>(globalHstGraph);
        return (bellmanFordShortestPath.getPath(lm.getId(), m.getId()) != null);
    }

    private void deliver(Message m){
        //adds to local history
        LightMessage lm = new LightMessage(m.getId(), m.getDst());
        history.add(lm);
        fullHistory.add(lm);
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
            allKnownMsgsToMe.remove(lm);

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

        gc(m);

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

    private void gc(Message m) {
        // se essa msg entregue eh destinada para todo mundo
        if(m.getDst().length == numNodes){
            
            // faco o corte no hst dos ancestrais
            // private HashMap<Short, LightMessagesList> ancHistory = new HashMap<>();
            for(LightMessagesList list : ancHistory.values()){
                list.setAsFirst(m.getId());
            }
            // private HashMap<Short, HashMap<Short, Item>> ancHstPointersPerDesc
            for(short anc : ancestors) {
                ancHstPointersPerDesc.put(anc, new HashMap<>());
            }
            for(short des : descendants) {
                for(short anc : ancHstPointersPerDesc.keySet()){
                    ancHstPointersPerDesc.get(anc).put(des, null);
                }
            }

            // corto o meu historico
            // history = new LightMessagesList();
            history.setAsFirst(m.getId());
            hstPointersPerDesc = new HashMap<>();

            // reconstruo o grafo global com base nos hsts atualizados
            bellmanFordShortestPath = null;
            globalHstGraph = GraphTypeBuilder.<Integer, DefaultEdge> directed()
            .allowingMultipleEdges(false)
            .allowingSelfLoops(false)
            .weighted(false)
            .edgeClass(DefaultEdge.class)
            .buildGraph();

            for(LightMessagesList list : ancHistory.values()){
                Item item = list.getFirst();
                while(item != null){
                    globalHstGraph.addVertex(item.get().getId());
                    if(item.getPrev() != null){
                        globalHstGraph.addEdge(item.getPrev().get().getId(), item.get().getId());
                    }
                    item = item.getNext();
                }
            }
            Item item = history.getFirst();
            while(item != null){
                globalHstGraph.addVertex(item.get().getId());
                if(item.getPrev() != null){
                    globalHstGraph.addEdge(item.getPrev().get().getId(), item.get().getId());
                }
                item = item.getNext();
            }
        }
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

        // teste para evitar ponteiros de notificacoes por enquanto:
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
            notif.setIdNotif(idNotif);
            //TODO: should it have a "notiflist"?
            // for (NotifList nl : m.getNotifList())
            //     notif.addNotifList(nl.getNotifList(), nl.getNotifier());
            // notif.addNotifList(notifList, getId());
            addHst(notif, d);
            send(notif, d);
            dstsPair.add(new Pair<Short,Integer>(d, idNotif));
            print("Sent notif", notif, "to", d, "with id", idNotif);
            idNotif++;
        }
        return dstsPair;
    }

    private void addHst(Message toSend, short dest) {

        // add hst dos ancestrais
        for(short anc : ancestors){
            // pega ponteiro para o meu hst de ancestral do destinatario em questao
            Item item = ancHstPointersPerDesc.get(anc).get(dest);
                    
            // se ponteiro ta null, pega a primeira msg no hst do ancestral, senao pega a proxima depois do ponteiro
            // pq o ponteiro aponta na verdade para a ultima que foi enviada anteriormente, e nao queremos envia-la novamente
            if(item == null) item = ancHistory.get(anc).getFirst();
            else item = item.getNext();

            ArrayList<LightMessage> hst = new ArrayList<>();
            while(item != null){
                hst.add(item.get());
                item = item.getNext();
            }

            // ancHstPointersPerDesc.get(anc).put(dest, ancHistory.get(anc).getLast());
            if(toSend.getType() == Type.MSG) ancHstPointersPerDesc.get(anc).put(dest, ancHistory.get(anc).getLast());

            if(hst.size() > 0) toSend.addHst(anc, hst);
        }

        // pega ponteiro para o meu hst do destinatario em questao
        Item item = hstPointersPerDesc.get(dest);
        
        // se ponteiro ta null, pega a primeira msg no meu hst, senao pega a proxima depois do ponteiro
        // pq o ponteiro aponta na verdade para a ultima que foi enviada anteriormente, e nao queremos envia-la novamente
        if(item == null) item = history.getFirst();
        else item = item.getNext();

        // se nao tem nada para enviar, ja retorna?
        //if(item == null) return;

        // nao envia a propria msg ?
        //if(item.get().getId() == toSend.getId()) return;

        ArrayList<LightMessage> myhst = new ArrayList<>();
        while(item != null){
            myhst.add(item.get());
            item = item.getNext();
        }

        // atualiza o ponteiro para a ultima no meu hst, que eh a ultima que eu enviei
        // hstPointersPerDesc.put(dest, history.getLast());
        if(toSend.getType() == Type.MSG) hstPointersPerDesc.put(dest, history.getLast());

        if(myhst.size() > 0) toSend.addHst(getId(), myhst);
    }

    private void sendAcks(Message m) {
        if(getId() == (numNodes-1)) return; // last one doesnt have someone to send acks
        
        // Set<Short> notifList = null;
        // if(getId() < (getNumNodes()-2)) notifList = sendNotif(m); // last 2 nodes never have someone to notify
        ArrayList<Pair<Short, Integer>> notifs = sendNotifs(m);

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
                // ack.ackIsFromDst(m.getType() == Type.MSG && m.isAddressedTo(getId()));

                addHst(ack, dst);

                // if(notifList != null && notifList.size() > 0)
                //     ack.addNotifList(notifList, getId());
                if(notifs != null && notifs.size() > 0) ack.setNotifList(notifs);

                send(ack, dst);
                print("Sent ack", ack, "to", dst);
            }
        }
    }

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
        files.persistMessages(fullHistory, getId(), false, false);
        printF("-------------------------------------");
        printF("Total msgs in the history:", fullHistory.size());
        printF("Total local msgs received:", localMsgs);
        printF("-------------------------------------");
        files.nodeFinished(getId());
        exit();
    }

    @Override
    protected boolean hasPendMsg() {
        if(pendingMessages == null) return false;
        return !pendingMessages.isEmpty();
    }
    
}
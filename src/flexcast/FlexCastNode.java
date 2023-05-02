package flexcast;

import proxies.ServerProxy;
import util.FileManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import base.Node;
import flexcast.messages.LightMessage;
import flexcast.messages.LightMessagesList;
import flexcast.messages.Message;
import flexcast.messages.LightMessagesList.Item;
import flexcast.messages.Message.Type;

public abstract class FlexCastNode extends ServerProxy {
    protected short numNodes;
    protected FileManager files;
    protected LightMessagesList history = new LightMessagesList();
    protected HashMap<Short, ArrayList<Message>> queues = new HashMap<>();
    protected HashMap<Short, LightMessagesList> ancHst = new HashMap<>();
    protected HashMap<Short, HashMap<Integer, Boolean>> ancHstMsgsDelivered = new HashMap<>();
    protected HashMap<Integer, Message> tempQueuedMessages;
    // protected HashMap<Integer, ArrayList<Message>> pendingAcks;
    protected HashMap<Integer, Boolean> msgsDelivered = new HashMap<>();
    protected HashMap<Integer, HashSet<Message>> msgsThatAreDeps = new HashMap<>();
    //foreach  descendand d, points to each ancestor's (and mine) last item in its hst sent to d
    protected HashMap<Short, HashMap<Short, Item>> ancHstMsgsPointers = new HashMap<>();

    public FlexCastNode(short id, int numClients){
        super(id, numClients);
        this.files = new FileManager();
        this.tempQueuedMessages = new HashMap<>();
        // this.pendingAcks = new HashMap<>();
        for(Node n : files.loadHosts()){
            // data for each ancestor
            if(n.getId() < id) {
                queues.put(n.getId(), new ArrayList<>());
                ancHst.put(n.getId(), new LightMessagesList());
                ancHstMsgsDelivered.put(n.getId(), new HashMap<>());
            }
            // sets host info for myself
            if(n.getId() == id) setHost(n.getHost());
            // sets connection to each descendant
            if(n.getId() > id) {
                connectTo(n);
                ancHstMsgsPointers.put(n.getId(), new HashMap<>());
            }
            numNodes++;
        }
        print(this, "FlexCast - Start listening...");
    }

    @Override
    protected void receiveMsg(Message m){
        print("Received msg", m);

        // se sou lca, entrego
        if(getId() == m.getLca()){
            deliver(m);
        }
        else {
            // if(m.isAddressedTo(getId())){
                // coloco na fila do lca
                queues.get(m.getLca()).add(m);
                tempQueuedMessages.put(m.getId(), m);

                // atualizo o hst local dos ancestrais com o hst da msg e cria as deps de msg
                updateAncHst(m);

                if(m.isAddressedTo(getId())){
                    // crio as dependencias de ack
                    for(short anc = (short)(m.getLca()+1); anc < getId(); anc++){
                        m.addAckFromAncDep();
                    }
                }

                // reprocess queues
                reprocessQueues(m.getLca());
            // }
            // else {
            //     // atualizo o hst local dos ancestrais com o hst da msg
            //     updateAncHst(m);
            //     possiblySendAck(m);
            // }
        }
    }

    private void updateAncHst(Message m) {
        for(short anc : m.getHst().keySet()){
            for(LightMessage lm : m.getHst().get(anc)){
                if(ancHstMsgsDelivered.get(anc).get(lm.getId()) == null){
                    ancHst.get(anc).add(lm);
                    ancHstMsgsDelivered.get(anc).put(lm.getId(), true);
                }
            }
        }

        //se for um ack, atualiza a msg do ack com as dependencias que vieram nesse ack
        if(m.getType() == Type.ACK) {
            Message m2 = tempQueuedMessages.get(m.getId());
            if(m2 != null){
                m2.updateDeps(getId(), msgsDelivered, msgsThatAreDeps, m.getHst());
                m.alreadyUpdtOrigin(true);
            }
            return;
        }

        m.updateDeps(getId(), msgsDelivered, msgsThatAreDeps);

    }

    @Override
    protected void receiveAck(Message ack){

        print("Received ack", ack, "from", ack.getSender());

        queues.get(ack.getSender()).add(ack);

        // atualizo o hst local dos ancestrais com o hst do ack
        updateAncHst(ack);

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

        // // associa ack a mensagem correspondente, resolvendo uma pendencia de ack
        // m.recvAckFromAnc();

        // (e a todas as msgs anteriores nao entregues do mesmo sender para preencher um possivel gap ?? )
        // crio uma pendencia de msg para cada msg addr pra mim que veio no hst desse ack
        //createDeps(m, false);
        
        // reprocess queues
        reprocessQueues(ack.getSender());
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
    
    abstract void reprocessQueues(short anc);
    abstract void sendAck(Message m);
    // abstract void possiblySendAck(Message m);
    // abstract boolean canDeliver(Message m);
    abstract void deliver(Message m);
}
package flexcast;

import java.util.ArrayList;
import java.util.HashSet;
import flexcast.messages.LightMessage;
import flexcast.messages.Message;
import flexcast.messages.LightMessagesList.Item;
import flexcast.messages.Message.Type;
import util.ArgsParser;

public class FlexCastFunctions extends FlexCastNode {

    // private HashMap<Integer, HashSet<Message>> msgsThatAreDepsOfAcks = new HashMap<>();

    public FlexCastFunctions(short id, ArgsParser args) {
        super(id, args.getClientCount());
    }

    protected void reprocessQueues(short anc) {
        boolean[] shouldRetry = new boolean[]{true};
        while(shouldRetry[0]){
            shouldRetry[0] = false;

            Message m = null;
            if (queues.get(anc).size()>0) m = queues.get(anc).get(0);

            if(m != null && canDeliver(m, shouldRetry)){
                deliver(m);
                shouldRetry[0] = true;
                continue;
            }

            for(short anc2 : queues.keySet()){
                if (anc == anc2) continue;
                Message m2 = null;
                if(queues.get(anc2).size() > 0) m2 = queues.get(anc2).get(0);
                if(m2 != null && canDeliver(m2, shouldRetry)){
                    deliver(m2);
                    shouldRetry[0] = true;
                }
            }
        }
    }

    @Override
    protected void deliver(Message m) {

        if(m.getType() == Type.ACK){
            Message orig = tempQueuedMessages.get(m.getId());
            orig.recvAckFromAnc();
            if(!m.alreadyUpdtOrigin()){
                orig.updateDeps(getId(), msgsDelivered, msgsThatAreDeps, m.getHst());
                m.alreadyUpdtOrigin(true);
            }
            queues.get(m.getSender()).remove(0);
            return;
        }

        if(m.isAddressedTo(getId())){
            print("delivered", m);
            history.add(new LightMessage(m.getId(), m.getDst()));
            msgsDelivered.put(m.getId(), true);
        }
        if(getId() == m.getLca()) {
            forward(m);
        }
        else {
            sendAck(m);
            queues.get(m.getLca()).remove(0);
            tempQueuedMessages.remove(m.getId());

            // removo a pendencia das msgs que tinham essa como pendencia
            HashSet<Message> deps = msgsThatAreDeps.get(m.getId());
            if(deps != null){
                for(Message d : deps){
                    d.getMsgDeps().remove(m.getId());
                }
                msgsThatAreDeps.remove(m.getId());
            }

            // removo pendencia de acks que tinham essa como pendencia
            // HashSet<Message> depsAcks = msgsThatAreDepsOfAcks.get(m.getId());
            // if(depsAcks != null){
            //     for(Message d : depsAcks){
            //         d.getMsgDeps().remove(m.getId());
            //         if(d.getMsgDeps().size() == 0){
            //             sendAck(d);
            //         }
            //     }
            //     msgsThatAreDepsOfAcks.remove(m.getId());
            // }
        }
        if(m.isAddressedTo(getId())) sendReply(m);
        print("queues:", queues);
    }

    private void forward(Message m) {
        for(short d = (short)(getId()+1); d <= m.getDst()[m.getDst().length-1]; d++){
            Message newMsg = new Message(m.getId());
            newMsg.setDst(m.getDst());
            newMsg.setType(m.getType());
            newMsg.setCliId(m.getCliId());
            newMsg.setSender(getId());
            send(addHst(newMsg, d), d);
        }
    }

    private Message addHst(Message m, short d) {
        // para cada ancestral a meu
        // pego o ponteiro dest d ancestral a, e adiciono as msgs depois do ponteiro
        for(short anc = 0; anc <= getId(); anc++){
            ArrayList<LightMessage> list = new ArrayList<>();
            Item first = ancHstMsgsPointers.get(d).get(anc);
            if(first == null){
                if(anc == getId())
                    first = history.getFirst();
                else
                    first = ancHst.get(anc).getFirst();
            }
            else {
                first = first.getNext();
            }
            while(first != null){
                list.add(first.get());
                ancHstMsgsPointers.get(d).put(anc, first);
                first = first.getNext();
            }
            if(list.size() > 0) m.getHst().put(anc, list);
        }
        return m;
    }
    
    @Override
    protected void sendAck(Message m) {
        if(getId() == (numNodes-1)) return; // last one doesnt have someone to send acks

        for(short desc : m.getDst()){
            if(desc > getId()){

                Message ack = new Message(m.getId());
                ack.setType(Type.ACK);
                ack.setDst(m.getDst());
                ack.setSender(getId());

                send(addHst(ack, desc), desc);
                print("Sending ack", ack, "to", desc);
            }
        }
    }

    // @Override
    // protected void possiblySendAck(Message m) {
    //     // se no hst desse msg m tem alguma msg m2 para mim que ainda nao entreguei
    //     // coloco m como pendente de envio de ack, com m2 como dependencia
    //     boolean shouldSend = true;
    //     for(short anc : m.getHst().keySet()){
    //         for(LightMessage lm : m.getHst().get(anc)){
    //             if(lm.isAddressedTo(getId()) && msgsDelivered.get(lm.getId()) == null){
    //                 m.getMsgDeps().add(lm.getId());
    //                 if(msgsThatAreDepsOfAcks.get(lm.getId()) == null){
    //                     msgsThatAreDepsOfAcks.put(lm.getId(), new HashSet<>());
    //                 }
    //                 msgsThatAreDepsOfAcks.get(lm.getId()).add(m);
    //                 shouldSend = false;
    //             }
    //         }
    //     }
    //     if(shouldSend){
    //         sendAck(m);
    //     }
    // }

    // @Override
    protected boolean canDeliver(Message m, boolean[] shouldRetry) {

        // addPendingAcks(m);
        if(m.getType() == Type.ACK){
            if(tempQueuedMessages.get(m.getId()) == null) return false;
            return true;
        }

        // recebeu todos acks de ancestrais ?
        if(m.getAcksFromAncsDeps() > 0)  {
            // print("Cant deliver", m, "- not enough acks");
            // print("Queues", queues);
            return false;
        }
        
        // tem alguma pendencia msg ainda nao entregue ?
        if(m.getMsgDeps().size() > 0)  {

            // tenta uma excecao aqui
            for(int dep : m.getMsgDeps()){
                Message mDep = tempQueuedMessages.get(dep);
                if(mDep != null){
                    Message nxtInQ = null;
                    try{nxtInQ = queues.get(m.getLca()).get(1);} catch(IndexOutOfBoundsException e){}
                    if(nxtInQ != null && nxtInQ.getId() == dep && nxtInQ.getType() == Type.ACK){
                        mDep.recvAckFromAnc();
                        queues.get(m.getLca()).remove(1);
                        shouldRetry[0] = true;
                    }
                }
            }

            print("Cant deliver", m, "msg deps:", m.getMsgDeps());
            print("Queues", queues);
            return false;
        }

        return true;
    }

    // @SuppressWarnings("unused")
    // private void addPendingAcks(Message m) {
    //     ArrayList<Message> pend = pendingAcks.get(m.getId());
    //     if(pend == null) return;
    //     //if there was any pending ack for this message, add them to the message
    //     for (Message ack : pend){
    //         m.recvAckFromAnc();
    //     }
    //     // remove from pending acks
    //     pendingAcks.remove(m.getId());
    // }

}
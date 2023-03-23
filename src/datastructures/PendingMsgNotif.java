package datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import messages.LightMessage;
import messages.Message;
import messages.LightMessagesList.Item;

public class PendingMsgNotif extends PendingMsg {

    private ArrayList<Message> notifs = new ArrayList<>();

    public PendingMsgNotif(LightMessage lm, Message notif, short nodeId, HashMap<Integer, Boolean> deliveredMsgs, ArrayList<Long> values) {
        super(lm, nodeId, deliveredMsgs,values);
        notifs.add(notif);
    }
    
    public void addPrevMsgsDependencies(LocalDepGraph depGraph, Item[] dgPointers, HashMap<Integer, PendingMsg> pendingMsgs){
    // public void addPrevMsgsDependencies(LocalDepGraph depGraph, HashMap<Integer, PendingMsg> pendingMsgs){
        // todas as msgs pendentes atualmente
        // long ini = System.nanoTime();
        for(PendingMsg p : pendingMsgs.values()){
            if(!(p instanceof PendingMsgNotif) && p.getLMessage().getId() != getLMessage().getId() && !dependsOn(p.getLMessage())){
                dependencies.add(new Dependency(false, false, false, -1, -1, p.getLMessage()));
            }
        }
        // mais as que eu encontrar no grafo (atualizado agora pela notif) que ainda nao estao pendentes
        for(short i = 0; i < nodeId; i++){
            Item aux = dgPointers[i] != null ? dgPointers[i] : depGraph.getGraph()[i].getFirst();
            // Item aux = depGraph.getGraph()[i].getFirst();
            while(aux != null){
                LightMessage lm = aux.get();
                if(lm.isAddressedTo(nodeId) && (deliveredMsgs.get(lm.getId())==null) && !dependsOn(lm)){
                    dependencies.add(new Dependency(false, false, false, -1, -1, lm));
                }
                aux = aux.getNext();
            }
        }
        // values.add(System.nanoTime() - ini);
    }

    public void addNotif(Message notif){
        notifs.add(notif);
    }

    public ArrayList<Message> getNotifs(){
        return notifs;
    }

    public String toString(){
        return "PendNotif{id:"+getLMessage().getId()+", msg:"+getMsg()+", deps:"+dependencies+", notifs:"+notifs+"}";
    }
}

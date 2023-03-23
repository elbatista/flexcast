package datastructures;

import java.util.ArrayList;
import java.util.HashMap;
import messages.LightMessage;
import messages.Message;
import messages.LightMessagesList.Item;
import util.BaseObj;

public class PendingMsg extends BaseObj {
    protected LightMessage lmessage;
    private Message message;
    protected ArrayList<Dependency> dependencies = new ArrayList<>();
    protected short nodeId;
    protected HashMap<Integer, Boolean> deliveredMsgs;

    protected ArrayList<Long> values;

    public PendingMsg(LightMessage lmessage, Message m, short nodeId, HashMap<Integer, Boolean> deliveredMsgs, ArrayList<Long> values) {
        this.lmessage = lmessage;
        this.message = m;
        this.nodeId = nodeId;
        this.deliveredMsgs = deliveredMsgs;
        this.values = values;
    }

    public PendingMsg(LightMessage lmessage, short nodeId, HashMap<Integer, Boolean> deliveredMsgs, ArrayList<Long> values) {
        this.lmessage = lmessage;
        this.nodeId = nodeId;
        this.deliveredMsgs = deliveredMsgs;
        this.values = values;
    }

    public LightMessage getLMessage(){
        return lmessage;
    }

    public void setMsg(Message m){
        this.message = m;
    }

    public Message getMsg(){
        return message;
    }

    public void addAcksDestsDependencies(Message m) {
        // preciso de acks de todos os dests abaixo do lca e acima de mim
        for(short anc : m.getDst()){
            if(anc > m.getLca() && anc < nodeId){
                if(!dependsOnAckDest(anc)) dependencies.add(new Dependency(true, true, false, anc, -1, null));
            }
        }
    }
    
    public void addAcksNotifListsDependencies(Message m) {
        for(NotifList nl : m.getNotifList()){
            for(short anc : nl.getNotifList()){
                if(anc < nodeId && !dependsOnAckNL(anc, nl.getNotifier())) dependencies.add(new Dependency(true, false, true, anc, nl.getNotifier(), null));
            }
        }
    }

    public boolean dependsOnAckDest(short anc){
        for(Dependency d: dependencies){
            if(d.ack && d.fromDst && !d.notifList && d.sender == anc) return true;
        }
        return false;
    }

    public boolean dependsOnAckNL(short anc, short notifier){
        for(Dependency d: dependencies){
            if(d.ack && !d.fromDst && d.notifList && d.sender == anc && d.idNotifier == notifier) return true;
        }
        return false;
    }
    
    protected boolean dependsOn(LightMessage lm) {
        for(Dependency d: dependencies){
            if(d.msgDep != null && d.msgDep.equals(lm)) return true;
        }
        return false;
    }

    public void receiveAck(Message ack) {
        // print("Pend - receiveAck", ack);
        // addiciona o ack como recebido na respectiva dependencia (ou cria a dependencia ja como resolvida)
        Dependency depToSolve = null;
        for(Dependency d : dependencies){
            if(!d.ack) continue;
            if(ack.ackIsFromDst()){
                if(d.fromDst && !d.notifList && d.sender == ack.getSender()){
                    depToSolve = d;
                    break;
                }
            }
            else {
                if(!d.fromDst && d.notifList && d.sender == ack.getSender() && d.idNotifier == ack.getIdNotifier()){
                    depToSolve = d;
                    break;
                }
            }
        }
        if(depToSolve == null){
            depToSolve = new Dependency(true, ack.ackIsFromDst(), !ack.ackIsFromDst(), ack.getSender(), ack.getIdNotifier(), null);
            dependencies.add(depToSolve);
        }
        depToSolve.solve();
    }

    public String toString(){
        return "Pend{id:"+lmessage.getId()+", msg:"+getMsg()+", deps:"+dependencies+"}";
    }

    public boolean allDepsOK() {
        if(dependencies.isEmpty()) return true;
        // verificar se todas as outras deps estao solucionadas...
        return dependencies.stream().filter(d->!d.solved).count() == 0;
    }

    public void removeMsgDep(Message msg) {
        for(Dependency d : dependencies){
            if(!d.ack && d.msgDep.getId() == msg.getId()){
                d.solve();
            }
        }
    }

    private class ItemPrevMsg{
        LightMessage msg;
        boolean chk;
        boolean foundPrevs;
        public ItemPrevMsg(LightMessage m, boolean chk, boolean foundPrevs) {
            this.msg = m;
            this.chk = chk;
            this.foundPrevs = foundPrevs;
        }
        public void check() {
            this.chk = true;
        }
        public void foundPrevs() {
            this.foundPrevs = true;
        }
        public String toString(){
            return "ipm{msg:"+msg+(chk?" ck":"")+(foundPrevs?" fp":"")+"}";
        }
    }

    public void addPrevMsgsDependencies(LocalDepGraph depGraph, Item[] dgPointers, HashMap<Integer, PendingMsg> pendingMsgs){
    // public void addPrevMsgsDependencies(LocalDepGraph depGraph, HashMap<Integer, PendingMsg> pendingMsgs){
            // print("Inside addPrevMsgsDependencies", this);

        // long ini = System.nanoTime();

        ArrayList<ItemPrevMsg> items = new ArrayList<>();
        HashMap<Integer, Boolean> itemsmap = new HashMap<>();
        items.add(new ItemPrevMsg(getLMessage(), true, false));
        itemsmap.put(getLMessage().getId(), true);

        // TO DO:
        // preciso de um hashmap aqui tbm com os items ja achados
        // para passar para dentro da foundPrevsItem e validar no hash map durantes as passadas e 
        // ao inserir novos items, para evitar duplicados

        // int it = 0;
        // todo: se eu for remover os ja resolvidos, aqui posso somente olhar para o size:
        while(items.size() > 0){ //stream().anyMatch(item->!item.chk||!item.foundPrevs)){
            // it++;
            // print(items, this);
            ItemPrevMsg item = null;

            // TO DO:
            // vou tirando daqui:

            do {
                item = items.get(0);
                //if(item == null) break;
                if(item.chk && item.foundPrevs){
                    items.remove(0);
                    if (items.size()==0)return;
                }
            } while(item.chk && item.foundPrevs);


            // for(ItemPrevMsg tmp : items){
            //     if(!tmp.chk || !tmp.foundPrevs){
            //         item = tmp;
            //         break;
            //     }
            // }

            if(item != null && !item.chk) checkItem(item,  pendingMsgs);
            if(item != null && !item.foundPrevs) foundPrevsItem(depGraph, dgPointers, item, items, itemsmap);
            // if(item != null && !item.foundPrevs) foundPrevsItem(depGraph, item, items);
        }

        // values.add(System.nanoTime() - ini);

        // print("values", values.size(), "items", items.size(), "iterations", it);
    }

    private void foundPrevsItem(LocalDepGraph depGraph, Item[] dgPointers, ItemPrevMsg item, ArrayList<ItemPrevMsg> items, HashMap<Integer, Boolean> itemsmap) {
    // private void foundPrevsItem(LocalDepGraph depGraph, ItemPrevMsg item, ArrayList<ItemPrevMsg> items) {
        // ArrayList<ItemPrevMsg> tmpFound = new ArrayList<>();
        // percorrer o grafo, partindo dos ponteiros
        // exceto o meu hst
        // indo ateh uma das mensagens em items
        // quando chega em uma das msgs em items, adiciona a anterior em tmpfound
        // e vai para proxima linha
        // no fim add all in tmpfoun to items
        // marcar item como foundPrevs
        for(short i = 0; i < nodeId; i++){
            Item aux = dgPointers[i] != null ? dgPointers[i] : depGraph.getGraph()[i].getFirst();
            // Item aux = depGraph.getGraph()[i].getFirst();
            while(aux != null){
                LightMessage lm = aux.get();

                // se eu bater nessa msg aqui pendente, ou em alguma que ja identifiquei como sendo prev
                // TODO: ? volto na mesma linha ateh o inicio adicionando os demais prevs

                // if(lm.getId() == lmessage.getId() || items.stream().anyMatch(ipm->ipm.msg.getId() == lm.getId())){
                if(lm.getId() == lmessage.getId() || itemsmap.get(lm.getId())!=null){
                    // final int previd = aux.getPrev() != null ? aux.getPrev().get().getId() : -1;
                    if(
                        aux.getPrev() != null && 
                        //aux.getPrev() != (dgPointers[i] != null ? dgPointers[i] : depGraph.getGraph()[i].getFirst()) &&
                        // !items.stream().anyMatch(ipm->ipm.msg.getId() == previd)
                        itemsmap.get(aux.getPrev().get().getId()) == null
                    ){
                        items.add(new ItemPrevMsg(aux.getPrev().get(), false, false));
                        itemsmap.put(aux.getPrev().get().getId(), true);
                        break;
                    }
                }
                aux = aux.getNext();
            }
        }
        // items.addAll(tmpFound);
        item.foundPrevs();
    }

    private void checkItem(ItemPrevMsg item, HashMap<Integer, PendingMsg> pendingMsgs) {
        // se o item eh para mim (diferente dessa msg aqui - lmessage)
        // e eu nao entreguei ainda
        // insiro ele como dependencia dessa msg
        // se ele ainda nao esta la nas dependencias
        // e marco ele como checked
        if(
            item.msg.isAddressedTo(nodeId) && 
            item.msg.getId() != getLMessage().getId() && 
            (deliveredMsgs.get(item.msg.getId())==null) && 
            !dependsOn(item.msg)
        ){
            // excecao?
            // se o item esta pendente, tem essa aqui como pendente, e eh do mesmo lca, nao insiro:
            // boolean add=true;
            // for(PendingMsg p : pendingMsgs.values()){
            //     if(
            //         !(p instanceof PendingMsgNotif) && 
            //         p.getLMessage().getId() == item.msg.getId() && 
            //         item.msg.getLca() == getLMessage().getLca() &&
            //         p.dependsOn(getLMessage())
            //     ){
            //         add=false;
            //     }
            // }
            // if(add)
                dependencies.add(new Dependency(false, false, false, -1, -1, item.msg));
        }
        item.check();
    }
}
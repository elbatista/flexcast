package flexcast.reconfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import base.Node;
import base.Host;
import flexcast.messages.LightMessagesList.Item;
import flexcast.messages.Message;
import flexcast.server.History;
import io.netty.channel.Channel;
import util.BaseObj;

public class View extends BaseObj{
    private int id;
    private short nodeid;
    private int nodepos;
    private Host host;
    private int idNotif = 0;
    private History history;
    private ArrayList<Node> nodes;
    private ArrayList<Short> ancestors;
    private ArrayList<Short> descendants;
    private HashMap<Short, ArrayList<Message>> queues;
    protected LinkedList<Message> pendingNotifs;
    private HashMap<Short, HashMap<Short, Item>> ancHstPointersPerDesc;
    private HashMap<Short, Item> hstPointersPerDesc;
    private Channel [] serverConnections;
    private ArrayList<Message> initBuffer;

    public View(int id){
        this.id = id;
        this.ancestors              = new ArrayList<>();
        this.initBuffer             = new ArrayList<>();
        this.descendants            = new ArrayList<>();
        this.queues                 = new HashMap<>();
        this.pendingNotifs          = new LinkedList<>();
        this.ancHstPointersPerDesc  = new HashMap<>();
        this.hstPointersPerDesc     = new HashMap<>();
    }

    public View (int id, ArrayList<Node> nodes){
        this.id = id;
        this.nodes = nodes;
        this.serverConnections = new Channel[nodes.size()];
    }

    public void prepareConnections(ArrayList<Node> nodes){
        this.nodes = nodes;
        this.serverConnections = new Channel[nodes.size()];
    }

    public View (int id, short nodeid, ArrayList<Node> nodes){
        this.id = id;
        this.nodeid = nodeid;
        this.nodes = nodes;
        for(Node n : nodes){
            if(n.getId() == nodeid){
                this.nodepos = n.getPosition();
                break;
            }
        }
        this.ancestors              = new ArrayList<>();
        this.initBuffer             = new ArrayList<>();
        this.descendants            = new ArrayList<>();
        this.queues                 = new HashMap<>();
        this.pendingNotifs          = new LinkedList<>();
        this.ancHstPointersPerDesc  = new HashMap<>();
        this.hstPointersPerDesc     = new HashMap<>();
        this.serverConnections      = new Channel[nodes.size()];
        createConnStructures();
    }

    public void prepareConnections(short nodeid, ArrayList<Node> nodes){
        this.nodeid = nodeid;
        this.nodes = nodes;
        for(Node n : nodes){
            if(n.getId()==id){
                this.nodepos = n.getPosition();
                break;
            }
        }
        this.serverConnections = new Channel[nodes.size()];
        createConnStructures();
    }

    public void createConnStructures(){
        for(Node n : nodes){
            // data for each ancestor
            if(n.getPosition() < nodepos) {
                ancestors.add(n.getId());
                queues.put(n.getId(), new ArrayList<>());
                ancHstPointersPerDesc.put(n.getId(), new HashMap<>());
            }
            // set data for myself
            if(n.getId() == nodeid) 
                setHost(n.getHost());
            // sets data to each descendant
            if(n.getPosition() > nodepos) {
                descendants.add(n.getId());
                for(short anc : ancHstPointersPerDesc.keySet())
                    ancHstPointersPerDesc.get(anc).put(n.getId(), null);
            }
        }
        history = new History(ancestors, nodeid);
    }

    public int getId() {
        return id;
    }
    public Host getHost() {
        return host;
    }
    public void setHost(Host host) {
        this.host = host;
    }
    public int getNumNodes() {
        return nodes.size();
    }
    public List<Node> getNodes(){
        return this.nodes;
    }
    public int getIdNotif() {
        return idNotif;
    }
    public void incIdNotif(){
        idNotif++;
    }
    public History getHistory() {
        return history;
    }
    public ArrayList<Short> getAncestors() {
        return ancestors;
    }
    public ArrayList<Short> getDescendants() {
        return descendants;
    }
    public HashMap<Short, ArrayList<Message>> getQueues() {
        return queues;
    }
    public LinkedList<Message> getPendingNotifs() {
        return pendingNotifs;
    }
    public HashMap<Short, HashMap<Short, Item>> getAncHstPointersPerDesc() {
        return ancHstPointersPerDesc;
    }
    public HashMap<Short, Item> getHstPointersPerDesc() {
        return hstPointersPerDesc;
    }
    public void resetHstPointersPerDesc(){
        hstPointersPerDesc = new HashMap<>();
    }
    public void addConnection(short dest, Channel conn){
        serverConnections[dest] = conn;
    }
    public Channel getConnection(short dest){
        return serverConnections[dest];
    }
    public Channel [] getConnections(){
        return serverConnections;
    }
    public void bufferMessage(Message m){
        initBuffer.add(m);
    }

    public List<Short> getAncestorsButTheLca(Message m) {
        ArrayList<Short> anc = new ArrayList<>();
        // para pegar os dests antes do lca:
        // dsts estao ordenados pela sua posicao no CDAG
        // partindo do segundo (pula o lca), retorno os dests antes de mim (node) no array de dests da msg
        for(int i=1; m.getDst()[i] != nodeid && i < m.getDst().length; i++){
            anc.add(m.getDst()[i]);
        }
        return anc;
    }

    public List<Short> getInterNodes(Message m) {
        ArrayList<Short> anc = new ArrayList<>();
        // todos abaixo na hierarquia que nao sao dsts:
        for (Node n : nodes){
            if(n.getPosition() > nodepos && !m.isAddressedTo(n.getId())){
                anc.add(n.getId());
            }
            //somente ateh o ultimo dest:
            if(n.getId() == m.getDst()[m.getDst().length-1]) break;
        }
        return anc;
    }

    public boolean isDescendant(Short d) {
        return getDescendants().contains(d);
    }
}
